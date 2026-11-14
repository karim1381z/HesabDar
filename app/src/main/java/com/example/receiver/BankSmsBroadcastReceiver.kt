package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.local.OrosiDatabase
import com.example.data.repository.OrosiFinanceRepository
import com.example.domain.JibemanSmsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BankSmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val senderAddress = messages.firstOrNull()?.displayOriginatingAddress.orEmpty().trim()
        val fullBody = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }.trim()
        if (senderAddress.isBlank() || fullBody.isBlank()) return

        // 1. Resolve bank strictly via JibemanSmsEngine.resolveBank(senderAddress).
        // If null, reject immediately so Digikala, Tapsi, Snapp, and promo SMS are 100% ignored.
        val bankProfile = JibemanSmsEngine.resolveBank(senderAddress) ?: return

        // 2. Parse body via JibemanSmsEngine.parse(fullBody). If null, reject.
        val parsed = JibemanSmsEngine.parse(fullBody) ?: return
        if (parsed.amount / 10L <= 0L) return

        val smsTimestamp = messages.firstOrNull()?.timestampMillis?.takeIf { it > 0L }
            ?: System.currentTimeMillis()

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = OrosiDatabase.getInstance(appContext).orosiDao()
                val repository = OrosiFinanceRepository(dao, appContext)
                repository.processIncomingBankSms(
                    rawSms = fullBody,
                    timestampMillis = smsTimestamp,
                    senderAddress = senderAddress
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
