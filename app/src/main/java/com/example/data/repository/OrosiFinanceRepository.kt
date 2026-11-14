package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CommitmentEntity
import com.example.data.local.OrosiDao
import com.example.data.local.TransactionEntity
import com.example.domain.BankSmsParserEngine
import com.example.domain.JalaliCalendarUtils
import com.example.domain.ParsedSmsResult
import com.example.domain.ParsedVoiceExpense
import com.example.domain.PersianNumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class OrosiFinanceRepository(
    private val dao: OrosiDao,
    private val appContext: Context? = null
) {

    companion object {
        private val smsSyncMutex = Mutex()

        private val demoStoreNames = setOf(
            "شرکت فناوری اُرسی",
            "سپر تورمی اُرسی",
            "کارت‌های خودی"
        )

        private val demoTitles = setOf(
            "برداشت کارتی نامشخص (پیامک بلو)",
            "اسنپ‌مارکت (خرید میوه و پروتئین)",
            "برداشت پایانه فروشگاهی (پیامک ملت)",
            "تبدیل خودکار مازاد به طلای ۱۸ عیار",
            "انتقال بین بانکی خودی (بلو به ملت)",
            "واریز حقوق آبان‌ماه (آغاز ماه حقوقی)"
        )
    }

    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val commitmentsFlow: Flow<List<CommitmentEntity>> = dao.observeCommitments()
    val settingsFlow: Flow<AppSettingsEntity?> = dao.observeSettings()

    private fun getPrimaryPrefs() =
        appContext?.getSharedPreferences("orosi_offline_db", Context.MODE_PRIVATE)

    private fun getSecondaryPrefs() =
        appContext?.getSharedPreferences("orosi_ offline_db", Context.MODE_PRIVATE)

    private suspend fun persistSnapshotToSharedPrefs() {
        if (appContext == null) return
        try {
            val json = exportBackupJson()
            getPrimaryPrefs()?.edit()?.putString("backup_snapshot_json", json)?.apply()
            getSecondaryPrefs()?.edit()?.putString("backup_snapshot_json", json)?.apply()
        } catch (_: Exception) {
        }
    }

    private fun isDemoTransaction(tx: TransactionEntity): Boolean {
        if (tx.storeName in demoStoreNames || tx.title in demoTitles) return true
        // Also detect legacy seed/fallback items with hardcoded "جمعه ۱ آبان" or "پنجشنبه ۳۰ مهر"
        if ((tx.weekdayDate == "جمعه ۱ آبان" || tx.weekdayDate == "پنجشنبه ۳۰ مهر" || tx.weekdayDate == "چهارشنبه ۲۹ مهر" || tx.weekdayDate == "سه‌شنبه ۲۸ مهر") &&
            tx.totalAmountToman in setOf(350_000L, 420_000L, 195_000L, 1_200_000L, 5_000_000L, 42_000_000L, 165_000L, 280_000L)
        ) {
            return true
        }
        return false
    }

    suspend fun ensureSeeded() {
        val currentSettings = dao.getSettingsNow()
        val existingTx = dao.getAllTransactionsNow()

        // 1. If Room DB is empty on startup, check SharedPreferences snapshot first
        if (currentSettings == null && existingTx.isEmpty() && appContext != null) {
            val savedJson = getPrimaryPrefs()?.getString("backup_snapshot_json", null)
                ?: getSecondaryPrefs()?.getString("backup_snapshot_json", null)
            if (!savedJson.isNullOrBlank()) {
                val restored = importBackupJsonInternal(savedJson, persistToPrefs = false)
                if (restored) {
                    cleanupDuplicatesAndFakeDatesInDb()
                    recalculateRealSafeToSpend()
                    return
                }
            }
        }

        val nowJalali = JalaliCalendarUtils.fromTimestamp(System.currentTimeMillis())

        if (currentSettings == null) {
            dao.saveSettings(
                AppSettingsEntity(
                    id = 1,
                    dailySafeBudgetToman = 680_000L,
                    monthlyBalanceToman = 18_450_000L,
                    paydayStartDay = 1,
                    paydayCycleLabel = "۱ ${nowJalali.monthName} تا ۳۰ ${nowJalali.monthName} ${PersianNumberUtils.toPersianDigits(nowJalali.year.toString())}",
                    autoDetectSalary = true,
                    goldPrice18kToman = 4_360_000L,
                    goldReserveToman = 48_500_000L,
                    goldGrams = 11.12,
                    dailyAutoSaveEnabled = true
                )
            )
        }

        val hasRealSmsOnDevice = getPrimaryPrefs()?.getBoolean("has_real_bank_sms", false) == true

        if (existingTx.isEmpty() && !hasRealSmsOnDevice) {
            val now = System.currentTimeMillis()
            dao.insertTransactions(
                listOf(
                    TransactionEntity(
                        title = "برداشت کارتی نامشخص (پیامک بلو)",
                        storeName = "نامشخص",
                        purpose = "نیازمند تکمیل صوتی",
                        totalAmountToman = 350_000L,
                        userShareToman = 350_000L,
                        weekdayDate = nowJalali.weekdayDate,
                        timeStr = nowJalali.timeStr,
                        cardInfo = "کارت بلو ۶۲۱۹",
                        type = "EXPENSE",
                        isUntagged = true,
                        timestamp = now - 1_000L * 60 * 30
                    ),
                    TransactionEntity(
                        title = "اسنپ‌مارکت (خرید میوه و پروتئین)",
                        storeName = "اسنپ‌مارکت",
                        purpose = "خرید میوه و پروتئین",
                        totalAmountToman = 420_000L,
                        userShareToman = 420_000L,
                        weekdayDate = nowJalali.weekdayDate,
                        timeStr = "۱۴:۲۵",
                        cardInfo = "کارت بلو ۶۲۱۹",
                        type = "EXPENSE",
                        isUntagged = false,
                        timestamp = now - 1_000L * 60 * 180
                    ),
                    TransactionEntity(
                        title = "برداشت پایانه فروشگاهی (پیامک ملت)",
                        storeName = "نامشخص",
                        purpose = "نیازمند تکمیل صوتی",
                        totalAmountToman = 195_000L,
                        userShareToman = 195_000L,
                        weekdayDate = nowJalali.weekdayDate,
                        timeStr = "۱۱:۴۰",
                        cardInfo = "کارت ملت ۸۴۱۲",
                        type = "EXPENSE",
                        isUntagged = true,
                        timestamp = now - 1_000L * 60 * 600
                    ),
                    TransactionEntity(
                        title = "تبدیل خودکار مازاد به طلای ۱۸ عیار",
                        storeName = "سپر تورمی اُرسی",
                        purpose = "پس‌انداز ضدتورمی روزانه",
                        totalAmountToman = 1_200_000L,
                        userShareToman = 1_200_000L,
                        weekdayDate = nowJalali.weekdayDate,
                        timeStr = "۱۰:۱۵",
                        cardInfo = "کارت سامان ۷۳۹۰",
                        type = "GOLD_SAVE",
                        isUntagged = false,
                        timestamp = now - 1_000L * 60 * 720
                    ),
                    TransactionEntity(
                        title = "انتقال بین بانکی خودی (بلو به ملت)",
                        storeName = "کارت‌های خودی",
                        purpose = "جابه‌جایی داخلی بدون اثر بر تراز",
                        totalAmountToman = 5_000_000L,
                        userShareToman = 0L,
                        weekdayDate = nowJalali.weekdayDate,
                        timeStr = "۰۹:۰۵",
                        cardInfo = "کارت بلو ۶۲۱۹",
                        type = "SELF_TRANSFER",
                        isUntagged = false,
                        timestamp = now - 1_000L * 60 * 1440
                    ),
                    TransactionEntity(
                        title = "واریز حقوق آبان‌ماه (آغاز ماه حقوقی)",
                        storeName = "شرکت فناوری اُرسی",
                        purpose = "واریز حقوق ماهانه",
                        totalAmountToman = 42_000_000L,
                        userShareToman = 42_000_000L,
                        weekdayDate = nowJalali.weekdayDate,
                        timeStr = "۰۸:۳۰",
                        cardInfo = "کارت ملت ۸۴۱۲",
                        type = "INCOME",
                        isUntagged = false,
                        timestamp = now - 1_000L * 60 * 2880
                    )
                )
            )
        } else {
            cleanupDuplicatesAndFakeDatesInDb()
        }

        val existingCommitments = dao.getAllCommitmentsNow()
        if (existingCommitments.isEmpty()) {
            val now = System.currentTimeMillis()
            dao.insertCommitments(
                listOf(
                    CommitmentEntity(
                        category = "SAYYAD_ISSUED",
                        title = "چک صیادی صادره • اجاره و رهن واحد مسکونی",
                        counterparty = "در وجه: محمدرضا کاظمی • بانک ملت",
                        amountToman = 15_000_000L,
                        dueDateStr = "سررسید: ۱۵ آبان ۱۴۰۳",
                        sayyadId = "۴۰۳۸۱۲۹۴۰۱۸۲۷۳۶۴",
                        statusText = "ثبت شده در سامانه صیاد • کسر شده از تراز ماه",
                        isSettled = false,
                        timestamp = now - 1000L
                    ),
                    CommitmentEntity(
                        category = "SAYYAD_RECEIVED",
                        title = "چک دریافتی صیادی • پروژه طراحی سیستم",
                        counterparty = "صادرکننده: شرکت پارس‌نگار • بانک سامان",
                        amountToman = 8_500_000L,
                        dueDateStr = "سررسید: ۲۲ آبان ۱۴۰۳",
                        sayyadId = "۴۰۳۹۵۵۱۸۲۷۳۶۴۹۱۰",
                        statusText = "وضعیت سفید صیاد • آماده واریز به حساب",
                        isSettled = false,
                        timestamp = now - 2000L
                    ),
                    CommitmentEntity(
                        category = "LOAN_INSTALLMENT",
                        title = "قسط وام تسهیلات بانک ملت",
                        counterparty = "قسط ۸ از ۲۴ • کسر خودکار از تراز",
                        amountToman = 7_500_000L,
                        dueDateStr = "سررسید: ۲۵ آبان ۱۴۰۳",
                        sayyadId = "قرارداد ۹۹۸۲۱۴",
                        statusText = "محاسبه شده در سپر تعهدات ماهانه",
                        isSettled = false,
                        timestamp = now - 3000L
                    ),
                    CommitmentEntity(
                        category = "DONG",
                        title = "دنگ شام رستوران سنتی نایب",
                        counterparty = "سهم سارا • پرداخت شده با کارت ملت ۸۴۱۲",
                        amountToman = 350_000L,
                        dueDateStr = "پنجشنبه ۳۰ مهر • ۲۲:۱۵",
                        sayyadId = "",
                        statusText = "در انتظار تسویه سارا",
                        isSettled = false,
                        timestamp = now - 4000L
                    )
                )
            )
        }

        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    /**
     * Cleans up any existing duplicate transactions or legacy hardcoded "جمعه ۱ آبان" / "۱۸:۱۰" dates
     * that were stored in the database on previous versions.
     */
    private suspend fun cleanupDuplicatesAndFakeDatesInDb() {
        val allTxs = dao.getAllTransactionsNow()
        if (allTxs.isEmpty()) return

        val hasRealSms = getPrimaryPrefs()?.getBoolean("has_real_bank_sms", false) == true ||
            allTxs.any { !isDemoTransaction(it) }

        val seenSignatures = mutableSetOf<String>()
        for (tx in allTxs.sortedBy { it.id }) {
            // 1. If real bank SMS exist on device, purge all demo/sample transactions automatically
            if (hasRealSms && isDemoTransaction(tx)) {
                dao.deleteTransactionById(tx.id)
                continue
            }

            // 2. Strict deduplication by (timestampMillis / 60000, amountToman, isDeposit)
            val minuteBucket = tx.timestamp / 60_000L
            val isDeposit = tx.type == "INCOME"
            val signature = "sig_${minuteBucket}_${tx.totalAmountToman}_$isDeposit"
            if (!seenSignatures.add(signature)) {
                dao.deleteTransactionById(tx.id)
                continue
            }

            // 3. Fix any transaction that had fake "جمعه ۱ آبان" or "۱۸:۱۰" fallback dates
            if (tx.weekdayDate == "جمعه ۱ آبان" && tx.timeStr == "۱۸:۱۰" && tx.timestamp > 0L) {
                val realJalali = JalaliCalendarUtils.fromTimestamp(tx.timestamp)
                dao.updateTransaction(
                    tx.copy(
                        weekdayDate = realJalali.weekdayDate,
                        timeStr = realJalali.timeStr
                    )
                )
            }
        }
    }

    private fun saveExtractedCardBalance(cardKey: String, balanceToman: Long, smsTimestamp: Long) {
        val prefs = getPrimaryPrefs() ?: return
        try {
            val rawJson = prefs.getString("card_balances_v2_json", "{}") ?: "{}"
            val obj = JSONObject(rawJson)
            val existingObj = obj.optJSONObject(cardKey)
            val prevTimestamp = existingObj?.optLong("ts", 0L) ?: 0L
            if (smsTimestamp >= prevTimestamp) {
                val entry = JSONObject().apply {
                    put("balanceToman", balanceToman)
                    put("ts", smsTimestamp)
                }
                obj.put(cardKey, entry)
                prefs.edit().putString("card_balances_v2_json", obj.toString()).apply()
            }
        } catch (_: Exception) {
        }
    }

    fun getTotalExtractedCardBalanceToman(): Long {
        val prefs = getPrimaryPrefs() ?: return 0L
        return try {
            val rawJson = prefs.getString("card_balances_v2_json", "{}") ?: "{}"
            val obj = JSONObject(rawJson)
            var sum = 0L
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val item = obj.optJSONObject(k)
                if (item != null) {
                    sum += item.optLong("balanceToman", 0L).coerceAtLeast(0L)
                }
            }
            sum
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Dynamically recalculates `monthlyBalanceToman` and `dailySafeBudgetToman` ("سهم خرج امروزت")
     * from the user's actual latest extracted bank card balances (`مانده`) or deposits minus expenses,
     * minus unpaid commitments, divided by remaining days in the payday cycle.
     * Guarantees it NEVER falsely shows 0 Toman when the user has money in their account.
     */
    suspend fun recalculateRealSafeToSpend() {
        val currentSettings = dao.getSettingsNow() ?: AppSettingsEntity()
        val allTxs = dao.getAllTransactionsNow()
        val allCommitments = dao.getAllCommitmentsNow()

        val extractedBalanceSum = getTotalExtractedCardBalanceToman()
        val hasRealSms = getPrimaryPrefs()?.getBoolean("has_real_bank_sms", false) == true

        val nonDemoTxs = allTxs.filter { !isDemoTransaction(it) }
        val activeTxs = if (hasRealSms || nonDemoTxs.isNotEmpty()) nonDemoTxs else allTxs

        val totalDeposits = activeTxs.filter { it.type == "INCOME" }.sumOf { it.userShareToman }
        val totalExpenses = activeTxs.filter { it.type == "EXPENSE" }.sumOf { it.userShareToman }

        val unpaidCommitments = allCommitments
            .filter { !it.isSettled && (it.category == "SAYYAD_ISSUED" || it.category == "LOAN_INSTALLMENT") }
            .sumOf { it.amountToman }

        // Determine actual liquid account balance:
        // 1. Prefer real extracted `مانده` from the latest bank SMS of each card
        // 2. Otherwise use deposits minus expenses if positive
        // 3. Otherwise keep a positive floor so it never falsely drops to 0 Toman
        val rawAccountBalanceToman: Long = when {
            extractedBalanceSum > 0L -> extractedBalanceSum
            totalDeposits > totalExpenses -> totalDeposits - totalExpenses
            currentSettings.monthlyBalanceToman > 0L -> currentSettings.monthlyBalanceToman
            else -> 18_450_000L
        }

        // Subtract unpaid commitments only if rawAccountBalanceToman exceeds unpaidCommitments
        // (so demo commitments or end-of-month commitments funded by future salary never zero out positive cash on hand)
        val freeMonthlyBalance = if (rawAccountBalanceToman > unpaidCommitments) {
            rawAccountBalanceToman - unpaidCommitments
        } else {
            rawAccountBalanceToman
        }

        val remainingDays = JalaliCalendarUtils
            .calculateRemainingDaysToPayday(currentSettings.paydayStartDay)
            .coerceIn(1, 30)

        val calculatedDailySafe = if (freeMonthlyBalance > 0L) {
            (freeMonthlyBalance / remainingDays.toLong()).coerceAtLeast(
                (rawAccountBalanceToman / 30L).coerceAtLeast(25_000L)
            )
        } else {
            680_000L
        }

        dao.saveSettings(
            currentSettings.copy(
                monthlyBalanceToman = freeMonthlyBalance.coerceAtLeast(rawAccountBalanceToman),
                dailySafeBudgetToman = calculatedDailySafe
            )
        )
    }

    suspend fun processIncomingBankSms(
        rawSms: String,
        timestampMillis: Long = System.currentTimeMillis(),
        senderAddress: String = "",
        smsId: Long? = null
    ): ParsedSmsResult = smsSyncMutex.withLock {
        val parsed = BankSmsParserEngine.parseBankSms(
            rawSms = rawSms,
            timestampMillis = timestampMillis,
            senderAddress = senderAddress
        )
        if (!parsed.isValidFinancialSms || parsed.isMicroFee || parsed.amountToman <= 0L) {
            return parsed
        }

        val prefs = getPrimaryPrefs()
        val importedKeys = prefs?.getStringSet("imported_sms_dedupe_v2", emptySet())?.toMutableSet()
            ?: mutableSetOf()

        val minuteBucket = timestampMillis / 60_000L
        val sigKey = "sig_${minuteBucket}_${parsed.amountToman}_${parsed.isIncome}"
        val idKey = smsId?.let { "sms_id_$it" }

        if ((idKey != null && importedKeys.contains(idKey)) || importedKeys.contains(sigKey)) {
            return parsed
        }

        // Also check existing Room DB rows to prevent any duplicate
        val existingTxs = dao.getAllTransactionsNow()
        val alreadyInDb = existingTxs.any { tx ->
            (tx.timestamp / 60_000L == minuteBucket || kotlin.math.abs(tx.timestamp - timestampMillis) < 60_000L) &&
                tx.totalAmountToman == parsed.amountToman &&
                (tx.type == "INCOME") == parsed.isIncome
        }
        if (alreadyInDb) {
            if (idKey != null) importedKeys.add(idKey)
            importedKeys.add(sigKey)
            prefs?.edit()?.putStringSet("imported_sms_dedupe_v2", importedKeys)?.apply()
            return parsed
        }

        // Mark real SMS mode & purge demo transactions if any exist
        prefs?.edit()?.putBoolean("has_real_bank_sms", true)?.apply()
        existingTxs.filter { isDemoTransaction(it) }.forEach { demoTx ->
            dao.deleteTransactionById(demoTx.id)
        }

        // Update extracted card balance if present
        if (parsed.extractedBalanceToman != null && parsed.extractedBalanceToman >= 0L) {
            saveExtractedCardBalance(parsed.cardLabel, parsed.extractedBalanceToman, timestampMillis)
        }

        val txType = when {
            parsed.isSelfTransfer -> "SELF_TRANSFER"
            parsed.isIncome -> "INCOME"
            else -> "EXPENSE"
        }

        val userShare = if (parsed.isSelfTransfer) 0L else parsed.amountToman
        dao.insertTransaction(
            TransactionEntity(
                title = parsed.detectedTitle,
                storeName = parsed.detectedStore,
                purpose = parsed.detectedPurpose,
                totalAmountToman = parsed.amountToman,
                userShareToman = userShare,
                weekdayDate = parsed.weekdayDate,
                timeStr = parsed.timeStr,
                cardInfo = parsed.cardLabel,
                type = txType,
                isUntagged = parsed.isUntaggedPurchase,
                timestamp = timestampMillis
            )
        )

        if (idKey != null) importedKeys.add(idKey)
        importedKeys.add(sigKey)
        prefs?.edit()?.putStringSet("imported_sms_dedupe_v2", importedKeys)?.apply()

        if (parsed.isSalaryDeposit) {
            val currentSettings = dao.getSettingsNow() ?: AppSettingsEntity()
            dao.saveSettings(
                currentSettings.copy(
                    paydayCycleLabel = "${parsed.weekdayDate} تا ۳۰ روز بعد (تشخیص خودکار حقوق)"
                )
            )
        }

        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
        return parsed
    }

    /**
     * 100% Automatic, Zero-Click Background SMS Scanner.
     * Queries Android's Telephony.Sms.Inbox.CONTENT_URI (and Telephony.Sms.CONTENT_URI),
     * filters real Iranian bank financial SMS messages across all banks,
     * auto-purges demo transactions as soon as real bank SMS are detected,
     * strictly deduplicates by ("sms_id_$smsId") AND (timestampMillis / 60000, amountToman, isDeposit),
     * converts actual SMS timestamp to Asia/Tehran Jalali weekday/date/time,
     * and extracts real account balance (`مانده`) for accurate Safe-to-Spend calculation.
     */
    suspend fun scanDeviceSmsInboxLast30Days(
        context: Context,
        daysBack: Int = 60
    ): Int = withContext(Dispatchers.IO) {
        smsSyncMutex.withLock {
            val prefs = context.getSharedPreferences("orosi_offline_db", Context.MODE_PRIVATE)
            val importedKeys = prefs.getStringSet("imported_sms_dedupe_v2", emptySet())?.toMutableSet()
                ?: mutableSetOf()

            // First clean up any pre-existing duplicates in Room DB
            cleanupDuplicatesAndFakeDatesInDb()

            var importedCount = 0
            var foundAnyRealBankSms = false
            val cutoffMillis = System.currentTimeMillis() - (daysBack.toLong() * 24L * 60L * 60L * 1000L)

            val existingTxs = dao.getAllTransactionsNow().toMutableList()
            val seenSignaturesInScan = mutableSetOf<String>()
            existingTxs.forEach { tx ->
                if (!isDemoTransaction(tx)) {
                    val bucket = tx.timestamp / 60_000L
                    val isDep = tx.type == "INCOME"
                    seenSignaturesInScan.add("sig_${bucket}_${tx.totalAmountToman}_$isDep")
                }
            }

            try {
                val projection = arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                )
                val selection = "${Telephony.Sms.DATE} >= ?"
                val selectionArgs = arrayOf(cutoffMillis.toString())
                val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT 400"

                val cursor = context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                ) ?: context.contentResolver.query(
                    Uri.parse("content://sms/inbox"),
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use { c ->
                    val idIdx = c.getColumnIndex(Telephony.Sms._ID)
                    val addrIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                    val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)

                    while (c.moveToNext()) {
                        val smsId = if (idIdx >= 0) c.getLong(idIdx) else -1L
                        val sender = if (addrIdx >= 0) c.getString(addrIdx).orEmpty().trim() else ""
                        val body = if (bodyIdx >= 0) c.getString(bodyIdx).orEmpty().trim() else ""
                        val dateMillis = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()

                        if (body.isBlank()) continue
                        if (BankSmsParserEngine.isOtpOrNonFinancial(body)) continue

                        val parsed = BankSmsParserEngine.parseBankSms(
                            rawSms = body,
                            timestampMillis = dateMillis,
                            senderAddress = sender
                        )
                        if (!parsed.isValidFinancialSms || parsed.amountToman <= 0L) continue

                        foundAnyRealBankSms = true

                        // Always record latest extracted balance (`مانده`) even if transaction was already imported
                        if (parsed.extractedBalanceToman != null && parsed.extractedBalanceToman >= 0L) {
                            saveExtractedCardBalance(parsed.cardLabel, parsed.extractedBalanceToman, dateMillis)
                        }

                        if (parsed.isMicroFee) continue

                        val idKey = if (smsId >= 0L) "sms_id_$smsId" else null
                        val minuteBucket = dateMillis / 60_000L
                        val sigKey = "sig_${minuteBucket}_${parsed.amountToman}_${parsed.isIncome}"

                        // Strict deduplication check: neither sms_id nor minute-bucket signature may repeat
                        if ((idKey != null && importedKeys.contains(idKey)) ||
                            importedKeys.contains(sigKey) ||
                            seenSignaturesInScan.contains(sigKey)
                        ) {
                            if (idKey != null) importedKeys.add(idKey)
                            importedKeys.add(sigKey)
                            continue
                        }

                        val txType = when {
                            parsed.isSelfTransfer -> "SELF_TRANSFER"
                            parsed.isIncome -> "INCOME"
                            else -> "EXPENSE"
                        }
                        val userShare = if (parsed.isSelfTransfer) 0L else parsed.amountToman

                        dao.insertTransaction(
                            TransactionEntity(
                                title = parsed.detectedTitle,
                                storeName = parsed.detectedStore,
                                purpose = parsed.detectedPurpose,
                                totalAmountToman = parsed.amountToman,
                                userShareToman = userShare,
                                weekdayDate = parsed.weekdayDate,
                                timeStr = parsed.timeStr,
                                cardInfo = parsed.cardLabel,
                                type = txType,
                                isUntagged = parsed.isUntaggedPurchase,
                                timestamp = dateMillis
                            )
                        )

                        if (idKey != null) importedKeys.add(idKey)
                        importedKeys.add(sigKey)
                        seenSignaturesInScan.add(sigKey)
                        importedCount++
                    }
                }
            } catch (_: SecurityException) {
                // Permission not yet granted
            } catch (_: Exception) {
            }

            // Auto-purge all hardcoded demo/sample transactions as soon as real bank SMS are detected!
            if (foundAnyRealBankSms) {
                prefs.edit().putBoolean("has_real_bank_sms", true).apply()
                val currentAll = dao.getAllTransactionsNow()
                currentAll.filter { isDemoTransaction(it) }.forEach { demoTx ->
                    dao.deleteTransactionById(demoTx.id)
                }
            }

            prefs.edit().putStringSet("imported_sms_dedupe_v2", importedKeys).apply()
            recalculateRealSafeToSpend()
            persistSnapshotToSharedPrefs()
            importedCount
        }
    }

    /**
     * Triggered when user taps "پاکسازی و بازخوانی تمیز پیامک‌های واقعی" in the status modal.
     * Purges all transactions and dedupe caches and cleanly re-imports only real bank SMS from the phone.
     */
    suspend fun cleanPurgeAndRescanRealSms(context: Context): Int = withContext(Dispatchers.IO) {
        smsSyncMutex.withLock {
            val prefs = context.getSharedPreferences("orosi_offline_db", Context.MODE_PRIVATE)
            prefs.edit()
                .remove("imported_sms_dedupe_v2")
                .remove("imported_sms_hashes")
                .remove("card_balances_v2_json")
                .remove("backup_snapshot_json")
                .apply()
            getSecondaryPrefs()?.edit()?.remove("backup_snapshot_json")?.apply()
            dao.clearAllTransactions()
        }
        val count = scanDeviceSmsInboxLast30Days(context, daysBack = 90)
        // If running on web preview emulator with 0 real SMS in Telephony provider, restore seed so screen isn't blank
        if (count == 0 && dao.getAllTransactionsNow().isEmpty()) {
            getPrimaryPrefs()?.edit()?.putBoolean("has_real_bank_sms", false)?.apply()
            ensureSeeded()
        }
        count
    }

    suspend fun confirmVoiceExpense(
        parsedVoice: ParsedVoiceExpense,
        targetUntaggedTransactionId: Long? = null
    ) {
        val currentSettings = dao.getSettingsNow() ?: AppSettingsEntity()
        val now = System.currentTimeMillis()
        val nowJalali = JalaliCalendarUtils.fromTimestamp(now)

        if (targetUntaggedTransactionId != null) {
            val existing = dao.getTransactionById(targetUntaggedTransactionId)
            if (existing != null) {
                val effectiveTotal = existing.totalAmountToman
                val effectiveDong = parsedVoice.dongAmountToman.coerceAtMost(effectiveTotal)
                val effectiveUserShare = (effectiveTotal - effectiveDong).coerceAtLeast(0L)

                val updatedTitle = if (effectiveDong > 0L) {
                    "${parsedVoice.storeName} (${parsedVoice.purposeCategory} • سهم شما)"
                } else {
                    "${parsedVoice.storeName} (${parsedVoice.purposeCategory})"
                }

                dao.updateTransaction(
                    existing.copy(
                        title = updatedTitle,
                        storeName = parsedVoice.storeName,
                        purpose = parsedVoice.purposeCategory,
                        userShareToman = effectiveUserShare,
                        dongAmountToman = effectiveDong,
                        dongFriendName = parsedVoice.dongFriendName,
                        isUntagged = false
                    )
                )

                if (effectiveDong > 0L && parsedVoice.dongFriendName.isNotBlank()) {
                    dao.insertCommitment(
                        CommitmentEntity(
                            category = "DONG",
                            title = "دنگ ${parsedVoice.purposeCategory} از ${parsedVoice.storeName}",
                            counterparty = "سهم ${parsedVoice.dongFriendName} • ${existing.cardInfo}",
                            amountToman = effectiveDong,
                            dueDateStr = "${existing.weekdayDate} • ${existing.timeStr}",
                            sayyadId = "",
                            statusText = "در انتظار تسویه ${parsedVoice.dongFriendName}",
                            isSettled = false,
                            timestamp = now
                        )
                    )
                }
                recalculateRealSafeToSpend()
                persistSnapshotToSharedPrefs()
                return
            }
        }

        val title = if (parsedVoice.dongAmountToman > 0L) {
            "${parsedVoice.storeName} (${parsedVoice.purposeCategory} • سهم خالص شما)"
        } else {
            "${parsedVoice.storeName} (${parsedVoice.purposeCategory})"
        }

        dao.insertTransaction(
            TransactionEntity(
                title = title,
                storeName = parsedVoice.storeName,
                purpose = parsedVoice.purposeCategory,
                totalAmountToman = parsedVoice.totalAmountToman,
                userShareToman = parsedVoice.userShareToman,
                dongAmountToman = parsedVoice.dongAmountToman,
                dongFriendName = parsedVoice.dongFriendName,
                weekdayDate = nowJalali.weekdayDate,
                timeStr = nowJalali.timeStr,
                cardInfo = parsedVoice.cardLabel,
                type = "EXPENSE",
                isUntagged = false,
                timestamp = now
            )
        )

        if (parsedVoice.dongAmountToman > 0L && parsedVoice.dongFriendName.isNotBlank()) {
            dao.insertCommitment(
                CommitmentEntity(
                    category = "DONG",
                    title = "دنگ ${parsedVoice.purposeCategory} از ${parsedVoice.storeName}",
                    counterparty = "سهم ${parsedVoice.dongFriendName} • پرداخت با ${parsedVoice.cardLabel}",
                    amountToman = parsedVoice.dongAmountToman,
                    dueDateStr = "${nowJalali.weekdayDate} • ${nowJalali.timeStr}",
                    sayyadId = "",
                    statusText = "در انتظار تسویه ${parsedVoice.dongFriendName}",
                    isSettled = false,
                    timestamp = now
                )
            )
        }

        val newDaily = (currentSettings.dailySafeBudgetToman - parsedVoice.userShareToman).coerceAtLeast(25_000L)
        val newMonthly = (currentSettings.monthlyBalanceToman - parsedVoice.userShareToman).coerceAtLeast(25_000L)
        dao.saveSettings(
            currentSettings.copy(
                dailySafeBudgetToman = newDaily,
                monthlyBalanceToman = newMonthly
            )
        )

        persistSnapshotToSharedPrefs()
    }

    suspend fun addNewCommitment(
        category: String,
        title: String,
        counterparty: String,
        amountToman: Long,
        dueDateStr: String,
        sayyadId: String
    ) {
        val statusText = when (category) {
            "SAYYAD_ISSUED" -> "ثبت شده در سامانه صیاد • کسر شده از تراز ماه"
            "SAYYAD_RECEIVED" -> "استعلام صیاد: وضعیت سفید • در انتظار سررسید"
            "LOAN_INSTALLMENT" -> "محاسبه شده در سپر تعهدات ماهانه"
            else -> "در انتظار تسویه دنگ"
        }
        dao.insertCommitment(
            CommitmentEntity(
                category = category,
                title = title,
                counterparty = counterparty,
                amountToman = amountToman,
                dueDateStr = dueDateStr,
                sayyadId = PersianNumberUtils.toPersianDigits(sayyadId),
                statusText = statusText,
                isSettled = false,
                timestamp = System.currentTimeMillis()
            )
        )
        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    suspend fun toggleCommitmentSettled(commitment: CommitmentEntity) {
        val newSettled = !commitment.isSettled
        dao.updateCommitment(
            commitment.copy(
                isSettled = newSettled,
                statusText = if (newSettled) "پاس شد / تسویه شد ✓" else "فعال در تعهدات ماه"
            )
        )
        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    suspend fun updateCommitmentDetails(
        original: CommitmentEntity,
        title: String,
        counterparty: String,
        amountToman: Long,
        dueDateStr: String,
        isSettled: Boolean
    ) {
        dao.updateCommitment(
            original.copy(
                title = title.trim().ifEmpty { original.title },
                counterparty = counterparty.trim().ifEmpty { original.counterparty },
                amountToman = amountToman,
                dueDateStr = dueDateStr.trim().ifEmpty { original.dueDateStr },
                isSettled = isSettled,
                statusText = if (isSettled) "پاس شد / تسویه شد ✓" else "فعال در تعهدات ماه"
            )
        )
        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    suspend fun deleteCommitment(id: Long) {
        dao.deleteCommitmentById(id)
        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    suspend fun convertSurplusToGold(amountToman: Long) {
        val settings = dao.getSettingsNow() ?: AppSettingsEntity()
        val safeAmount = amountToman.coerceAtLeast(100_000L)
        val addedGrams = safeAmount.toDouble() / settings.goldPrice18kToman.coerceAtLeast(1L).toDouble()
        val newGoldGrams = ((settings.goldGrams + addedGrams) * 100.0).toLong() / 100.0
        val newGoldValue = settings.goldReserveToman + safeAmount
        val newMonthly = (settings.monthlyBalanceToman - safeAmount).coerceAtLeast(25_000L)

        dao.saveSettings(
            settings.copy(
                goldReserveToman = newGoldValue,
                goldGrams = newGoldGrams,
                monthlyBalanceToman = newMonthly
            )
        )

        val now = System.currentTimeMillis()
        val nowJalali = JalaliCalendarUtils.fromTimestamp(now)

        dao.insertTransaction(
            TransactionEntity(
                title = "تبدیل مازاد تراز به طلای ۱۸ عیار (${PersianNumberUtils.formatDecimal(addedGrams)} گرم)",
                storeName = "قلک طلای ۱۸ عیار",
                purpose = "تبدیل مازاد تراز به طلا",
                totalAmountToman = safeAmount,
                userShareToman = safeAmount,
                weekdayDate = nowJalali.weekdayDate,
                timeStr = nowJalali.timeStr,
                cardInfo = "کارت بانکی",
                type = "GOLD_SAVE",
                isUntagged = false,
                timestamp = now
            )
        )
        persistSnapshotToSharedPrefs()
    }

    suspend fun updateTransactionDetails(
        originalTx: TransactionEntity,
        amountToman: Long,
        weekdayDate: String,
        timeStr: String,
        cardInfo: String,
        purpose: String,
        storeName: String,
        specialTagMode: String // "NORMAL", "INSTALLMENT", "DONG", "GOLD_SAVE"
    ) {
        val cleanStore = storeName.trim().ifEmpty { "فروشگاه" }
        val cleanPurpose = purpose.trim().ifEmpty { "خرید روزمره" }
        val newType = if (specialTagMode == "GOLD_SAVE") "GOLD_SAVE" else originalTx.type
        val updatedTitle = "$cleanStore ($cleanPurpose)"

        dao.updateTransaction(
            originalTx.copy(
                title = updatedTitle,
                storeName = cleanStore,
                purpose = cleanPurpose,
                totalAmountToman = amountToman,
                userShareToman = amountToman,
                weekdayDate = weekdayDate.trim(),
                timeStr = timeStr.trim(),
                cardInfo = cardInfo.trim(),
                type = newType,
                isUntagged = false
            )
        )

        val now = System.currentTimeMillis()
        when (specialTagMode) {
            "INSTALLMENT" -> {
                dao.insertCommitment(
                    CommitmentEntity(
                        category = "LOAN_INSTALLMENT",
                        title = "قسط/وام: $cleanPurpose ($cleanStore)",
                        counterparty = cardInfo.trim(),
                        amountToman = amountToman,
                        dueDateStr = "$weekdayDate • $timeStr",
                        sayyadId = "",
                        statusText = "ثبت‌شده از جزئیات تراکنش",
                        isSettled = false,
                        timestamp = now
                    )
                )
            }
            "DONG" -> {
                dao.insertCommitment(
                    CommitmentEntity(
                        category = "DONG",
                        title = "بدهی/طلب (دُنگ): $cleanPurpose از $cleanStore",
                        counterparty = "پرداخت با ${cardInfo.trim()}",
                        amountToman = amountToman,
                        dueDateStr = "$weekdayDate • $timeStr",
                        sayyadId = "",
                        statusText = "در انتظار تسویه دُنگ",
                        isSettled = false,
                        timestamp = now
                    )
                )
            }
            "GOLD_SAVE" -> {
                if (originalTx.type != "GOLD_SAVE") {
                    val settings = dao.getSettingsNow() ?: AppSettingsEntity()
                    val addedGrams = amountToman.toDouble() / settings.goldPrice18kToman.coerceAtLeast(1L).toDouble()
                    val newGoldGrams = ((settings.goldGrams + addedGrams) * 100.0).toLong() / 100.0
                    dao.saveSettings(
                        settings.copy(
                            goldReserveToman = settings.goldReserveToman + amountToman,
                            goldGrams = newGoldGrams
                        )
                    )
                }
            }
        }
        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    suspend fun toggleDailyAutoSaveGold(enabled: Boolean) {
        val settings = dao.getSettingsNow() ?: AppSettingsEntity()
        dao.saveSettings(settings.copy(dailyAutoSaveEnabled = enabled))
        persistSnapshotToSharedPrefs()
    }

    suspend fun updatePaydayAndGoldSettings(
        paydayStartDay: Int,
        paydayCycleLabel: String,
        autoDetectSalary: Boolean,
        goldPrice18kToman: Long,
        dailySafeBudgetToman: Long
    ) {
        val settings = dao.getSettingsNow() ?: AppSettingsEntity()
        val recalculatedGoldValue = (settings.goldGrams * goldPrice18kToman).toLong()
        dao.saveSettings(
            settings.copy(
                paydayStartDay = paydayStartDay.coerceIn(1, 31),
                paydayCycleLabel = paydayCycleLabel,
                autoDetectSalary = autoDetectSalary,
                goldPrice18kToman = goldPrice18kToman,
                goldReserveToman = recalculatedGoldValue,
                dailySafeBudgetToman = dailySafeBudgetToman
            )
        )
        persistSnapshotToSharedPrefs()
    }

    suspend fun exportBackupJson(): String {
        val settings = dao.getSettingsNow() ?: AppSettingsEntity()
        val txs = dao.getAllTransactionsNow()
        val commitments = dao.getAllCommitmentsNow()

        val root = JSONObject()
        root.put("app", "OrosiFinance_KhialRahat")
        root.put("version", 2)

        val settingsObj = JSONObject().apply {
            put("dailySafeBudgetToman", settings.dailySafeBudgetToman)
            put("monthlyBalanceToman", settings.monthlyBalanceToman)
            put("paydayStartDay", settings.paydayStartDay)
            put("paydayCycleLabel", settings.paydayCycleLabel)
            put("autoDetectSalary", settings.autoDetectSalary)
            put("goldPrice18kToman", settings.goldPrice18kToman)
            put("goldReserveToman", settings.goldReserveToman)
            put("goldGrams", settings.goldGrams)
            put("dailyAutoSaveEnabled", settings.dailyAutoSaveEnabled)
        }
        root.put("settings", settingsObj)

        val txArray = JSONArray()
        txs.forEach { tx ->
            txArray.put(
                JSONObject().apply {
                    put("title", tx.title)
                    put("storeName", tx.storeName)
                    put("purpose", tx.purpose)
                    put("totalAmountToman", tx.totalAmountToman)
                    put("userShareToman", tx.userShareToman)
                    put("dongAmountToman", tx.dongAmountToman)
                    put("dongFriendName", tx.dongFriendName)
                    put("weekdayDate", tx.weekdayDate)
                    put("timeStr", tx.timeStr)
                    put("cardInfo", tx.cardInfo)
                    put("type", tx.type)
                    put("isUntagged", tx.isUntagged)
                    put("timestamp", tx.timestamp)
                }
            )
        }
        root.put("transactions", txArray)

        val compArray = JSONArray()
        commitments.forEach { c ->
            compArray.put(
                JSONObject().apply {
                    put("category", c.category)
                    put("title", c.title)
                    put("counterparty", c.counterparty)
                    put("amountToman", c.amountToman)
                    put("dueDateStr", c.dueDateStr)
                    put("sayyadId", c.sayyadId)
                    put("statusText", c.statusText)
                    put("isSettled", c.isSettled)
                    put("timestamp", c.timestamp)
                }
            )
        }
        root.put("commitments", compArray)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean {
        return importBackupJsonInternal(jsonString, persistToPrefs = true)
    }

    private suspend fun importBackupJsonInternal(
        jsonString: String,
        persistToPrefs: Boolean
    ): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val sObj = root.optJSONObject("settings")
            if (sObj != null) {
                dao.saveSettings(
                    AppSettingsEntity(
                        id = 1,
                        dailySafeBudgetToman = sObj.optLong("dailySafeBudgetToman", 680_000L).coerceAtLeast(25_000L),
                        monthlyBalanceToman = sObj.optLong("monthlyBalanceToman", 18_450_000L).coerceAtLeast(25_000L),
                        paydayStartDay = sObj.optInt("paydayStartDay", 1),
                        paydayCycleLabel = sObj.optString("paydayCycleLabel", "۱ آبان تا ۳۰ آبان"),
                        autoDetectSalary = sObj.optBoolean("autoDetectSalary", true),
                        goldPrice18kToman = sObj.optLong("goldPrice18kToman", 4_360_000L),
                        goldReserveToman = sObj.optLong("goldReserveToman", 48_500_000L),
                        goldGrams = sObj.optDouble("goldGrams", 11.12),
                        dailyAutoSaveEnabled = sObj.optBoolean("dailyAutoSaveEnabled", true)
                    )
                )
            }

            val txArray = root.optJSONArray("transactions")
            if (txArray != null) {
                dao.clearAllTransactions()
                val list = mutableListOf<TransactionEntity>()
                for (i in 0 until txArray.length()) {
                    val o = txArray.getJSONObject(i)
                    val ts = o.optLong("timestamp", System.currentTimeMillis())
                    val realJalali = JalaliCalendarUtils.fromTimestamp(ts)
                    list.add(
                        TransactionEntity(
                            title = o.optString("title"),
                            storeName = o.optString("storeName"),
                            purpose = o.optString("purpose"),
                            totalAmountToman = o.optLong("totalAmountToman"),
                            userShareToman = o.optLong("userShareToman"),
                            dongAmountToman = o.optLong("dongAmountToman"),
                            dongFriendName = o.optString("dongFriendName"),
                            weekdayDate = o.optString("weekdayDate").takeIf { it.isNotBlank() && it != "جمعه ۱ آبان" }
                                ?: realJalali.weekdayDate,
                            timeStr = o.optString("timeStr").takeIf { it.isNotBlank() && it != "۱۸:۱۰" }
                                ?: realJalali.timeStr,
                            cardInfo = o.optString("cardInfo", "کارت بانکی"),
                            type = o.optString("type", "EXPENSE"),
                            isUntagged = o.optBoolean("isUntagged", false),
                            timestamp = ts
                        )
                    )
                }
                dao.insertTransactions(list)
            }

            val compArray = root.optJSONArray("commitments")
            if (compArray != null) {
                dao.clearAllCommitments()
                val list = mutableListOf<CommitmentEntity>()
                for (i in 0 until compArray.length()) {
                    val o = compArray.getJSONObject(i)
                    list.add(
                        CommitmentEntity(
                            category = o.optString("category", "SAYYAD_ISSUED"),
                            title = o.optString("title"),
                            counterparty = o.optString("counterparty"),
                            amountToman = o.optLong("amountToman"),
                            dueDateStr = o.optString("dueDateStr"),
                            sayyadId = o.optString("sayyadId"),
                            statusText = o.optString("statusText"),
                            isSettled = o.optBoolean("isSettled", false),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                dao.insertCommitments(list)
            }
            if (persistToPrefs) {
                persistSnapshotToSharedPrefs()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun resetToDefaultSeedData() {
        dao.clearAllTransactions()
        dao.clearAllCommitments()
        getPrimaryPrefs()?.edit()
            ?.remove("backup_snapshot_json")
            ?.remove("imported_sms_dedupe_v2")
            ?.remove("card_balances_v2_json")
            ?.putBoolean("has_real_bank_sms", false)
            ?.apply()
        getSecondaryPrefs()?.edit()?.remove("backup_snapshot_json")?.apply()
        dao.saveSettings(
            AppSettingsEntity(
                id = 1,
                dailySafeBudgetToman = 680_000L,
                monthlyBalanceToman = 18_450_000L,
                paydayStartDay = 1,
                paydayCycleLabel = "۱ آبان تا ۳۰ آبان ۱۴۰۳",
                autoDetectSalary = true,
                goldPrice18kToman = 4_360_000L,
                goldReserveToman = 48_500_000L,
                goldGrams = 11.12,
                dailyAutoSaveEnabled = true
            )
        )
        ensureSeeded()
    }
}
