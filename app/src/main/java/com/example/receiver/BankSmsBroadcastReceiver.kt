package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.local.OrosiDatabase
import com.example.data.repository.OrosiFinanceRepository
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

        val fullBody = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }.trim()
        if (fullBody.isBlank()) return

        // Filter only Iranian banking SMS messages
        val bankingKeywords = listOf(
            "بانک", "بلو", "ملت", "ملی", "سامان", "رسالت",
            "برداشت", "واریز", "ریال", "تومان", "کارت", "حساب", "پایا", "ساتنا"
        )
        val isBankSms = bankingKeywords.any { fullBody.contains(it, ignoreCase = true) }
        if (!isBankSms) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = OrosiDatabase.getInstance(appContext).orosiDao()
                val repository = OrosiFinanceRepository(dao, appContext)
                repository.processIncomingBankSms(fullBody)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
