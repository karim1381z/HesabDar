package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.example.data.local.AppSettingsEntity
import com.example.data.local.BankAccountEntity
import com.example.data.local.CommitmentEntity
import com.example.data.local.OrosiDao
import com.example.data.local.TransactionEntity
import com.example.domain.BankSmsParserEngine
import com.example.domain.JalaliCalendarUtils
import com.example.domain.JibemanSmsEngine
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
        const val PREFS_NAME = "orosi_offline_db_v4_jibeman"
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

        private val demoCommitmentSayyadIds = setOf(
            "۴۰۳۸۱۲۹۴۰۱۸۲۷۳۶۴",
            "۴۰۳۹۵۵۱۸۲۷۳۶۴۹۱۰",
            "قرارداد ۹۹۸۲۱۴"
        )
    }

    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val bankAccountsFlow: Flow<List<BankAccountEntity>> = dao.observeBankAccounts()
    val commitmentsFlow: Flow<List<CommitmentEntity>> = dao.observeCommitments()
    val settingsFlow: Flow<AppSettingsEntity?> = dao.observeSettings()

    private fun getPrimaryPrefs() =
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private suspend fun persistSnapshotToSharedPrefs() {
        if (appContext == null) return
        try {
            val json = exportBackupJson()
            getPrimaryPrefs()?.edit()?.putString("backup_snapshot_json", json)?.apply()
        } catch (_: Exception) {
        }
    }

    private fun isDemoTransaction(tx: TransactionEntity): Boolean {
        if (tx.storeName in demoStoreNames || tx.title in demoTitles) return true
        if (tx.cardInfo == "کارت بانک" && (tx.storeName.contains("دیجی") || tx.storeName.contains("تپسی") || tx.storeName.contains("اسنپ"))) {
            return true
        }
        if ((tx.weekdayDate == "جمعه ۱ آبان" || tx.weekdayDate == "پنجشنبه ۳۰ مهر" || tx.weekdayDate == "چهارشنبه ۲۹ مهر" || tx.weekdayDate == "سه‌شنبه ۲۸ مهر") &&
            tx.totalAmountToman in setOf(350_000L, 420_000L, 195_000L, 1_200_000L, 5_000_000L, 42_000_000L, 165_000L, 280_000L)
        ) {
            return true
        }
        return false
    }

    private fun isDemoCommitment(c: CommitmentEntity): Boolean {
        if (c.sayyadId in demoCommitmentSayyadIds) return true
        if (c.counterparty.contains("محمدرضا کاظمی") ||
            c.counterparty.contains("شرکت پارس‌نگار") ||
            c.title == "دنگ شام رستوران سنتی نایب" ||
            c.title == "قسط وام تسهیلات بانک ملت"
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
            if (!savedJson.isNullOrBlank()) {
                val restored = importBackupJsonInternal(savedJson, persistToPrefs = false)
                if (restored) {
                    cleanupDemoAndDuplicatesIfRealSmsPresent()
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
                    dailyAutoSaveEnabled = true,
                    hideAmounts = false,
                    onlyTankhahMode = false
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
                        storeName = "بلو",
                        purpose = "نیازمند تکمیل صوتی",
                        totalAmountToman = 100_000L,
                        userShareToman = 100_000L,
                        weekdayDate = nowJalali.weekdayDate,
                        timeStr = nowJalali.timeStr,
                        cardInfo = "کارت بلو",
                        type = "EXPENSE",
                        isUntagged = true,
                        timestamp = now - 1_000L * 60 * 30,
                        balanceAfterToman = 14_250_000L,
                        rawSmsBody = "بانک سامان - بلوبانک\nبرداشت شتابی ۱,۰۰۰,۰۰۰ ریال\nمانده: ۱۴۲,۵۰۰,۰۰۰ ریال",
                        bankName = "بلو"
                    ),
                    TransactionEntity(
                        title = "برداشت کارتی نامشخص (پیامک سپه)",
                        storeName = "سپه",
                        purpose = "نیازمند تکمیل صوتی",
                        totalAmountToman = 49_500L,
                        userShareToman = 49_500L,
                        weekdayDate = nowJalali.weekdayDate,
                        timeStr = "۱۴:۲۵",
                        cardInfo = "کارت سپه",
                        type = "EXPENSE",
                        isUntagged = true,
                        timestamp = now - 1_000L * 60 * 180,
                        balanceAfterToman = 984_433L,
                        rawSmsBody = "بانک سپه\nبرداشت: ۴۹۵,۰۰۰ ریال\nمانده: ۹,۸۴۴,۳۳۰ ریال",
                        bankName = "سپه"
                    ),
                    TransactionEntity(
                        title = "واریز حقوق ماهانه",
                        storeName = "شرکت فناوری اُرسی",
                        purpose = "واریز حقوق ماهانه",
                        totalAmountToman = 42_000_000L,
                        userShareToman = 42_000_000L,
                        weekdayDate = nowJalali.weekdayDate,
                        timeStr = "۰۸:۳۰",
                        cardInfo = "کارت بلو",
                        type = "INCOME",
                        isUntagged = false,
                        timestamp = now - 1_000L * 60 * 2880,
                        balanceAfterToman = 42_350_000L,
                        rawSmsBody = "واریز به حساب بلو مبلغ ۴۲۰,۰۰۰,۰۰۰ ریال\nمانده: ۴۲۳,۵۰۰,۰۰۰ ریال",
                        bankName = "بلو"
                    )
                )
            )
        }

        val existingAccounts = dao.getAllBankAccountsNow()
        if (existingAccounts.isEmpty() && !hasRealSmsOnDevice) {
            val now = System.currentTimeMillis()
            dao.upsertBankAccounts(
                listOf(
                    BankAccountEntity(
                        bankName = "بلو",
                        nameEn = "Blu Bank",
                        balanceToman = 14_250_000L,
                        isTankhah = true,
                        cardLast4 = "۶۲۱۹",
                        lastUpdatedTimestamp = now - 1000L,
                        isManual = false
                    ),
                    BankAccountEntity(
                        bankName = "سپه",
                        nameEn = "Bank Sepah",
                        balanceToman = 984_433L,
                        isTankhah = true,
                        cardLast4 = "۵۸۹۲",
                        lastUpdatedTimestamp = now - 2000L,
                        isManual = false
                    ),
                    BankAccountEntity(
                        bankName = "پاسارگاد",
                        nameEn = "Bank Pasargad",
                        balanceToman = 1_000L,
                        isTankhah = false,
                        cardLast4 = "۵۰۲۲",
                        lastUpdatedTimestamp = now - 3000L,
                        isManual = false
                    ),
                    BankAccountEntity(
                        bankName = "ملت",
                        nameEn = "Bank Mellat",
                        balanceToman = 0L,
                        isTankhah = false,
                        cardLast4 = "۶۱۰۴",
                        lastUpdatedTimestamp = now - 4000L,
                        isManual = false
                    ),
                    BankAccountEntity(
                        bankName = "ویپاد",
                        nameEn = "Wepod",
                        balanceToman = 0L,
                        isTankhah = false,
                        cardLast4 = "۵۰۲۲",
                        lastUpdatedTimestamp = now - 5000L,
                        isManual = false
                    )
                )
            )
        }

        val existingCommitments = dao.getAllCommitmentsNow()
        if (existingCommitments.isEmpty() && !hasRealSmsOnDevice) {
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

        cleanupDemoAndDuplicatesIfRealSmsPresent()
        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    private suspend fun cleanupDemoAndDuplicatesIfRealSmsPresent() {
        val hasRealSms = getPrimaryPrefs()?.getBoolean("has_real_bank_sms", false) == true
        val allTxs = dao.getAllTransactionsNow()

        if (hasRealSms) {
            allTxs.filter { isDemoTransaction(it) }.forEach { demoTx ->
                dao.deleteTransactionById(demoTx.id)
            }
            val allCommitments = dao.getAllCommitmentsNow()
            allCommitments.filter { isDemoCommitment(it) }.forEach { demoC ->
                dao.deleteCommitmentById(demoC.id)
            }
        }
    }

    private suspend fun upsertDiscoveredBankAccount(
        bankNameFa: String,
        bankNameEn: String,
        balanceToman: Long?,
        smsTimestamp: Long
    ) {
        val existing = dao.getBankAccountByName(bankNameFa)
        if (existing == null) {
            dao.upsertBankAccount(
                BankAccountEntity(
                    bankName = bankNameFa,
                    nameEn = bankNameEn,
                    balanceToman = balanceToman ?: 0L,
                    isTankhah = true,
                    cardLast4 = "",
                    lastUpdatedTimestamp = smsTimestamp,
                    isManual = false
                )
            )
        } else if (!existing.isManual) {
            val shouldUpdateBalance = balanceToman != null && smsTimestamp >= existing.lastUpdatedTimestamp
            dao.upsertBankAccount(
                existing.copy(
                    nameEn = bankNameEn.ifBlank { existing.nameEn },
                    balanceToman = if (shouldUpdateBalance) balanceToman!! else existing.balanceToman,
                    lastUpdatedTimestamp = maxOf(existing.lastUpdatedTimestamp, smsTimestamp)
                )
            )
        }
    }

    fun getTotalExtractedCardBalanceToman(): Long {
        val prefs = getPrimaryPrefs() ?: return 0L
        return try {
            val rawJson = prefs.getString("card_balances_v4_json", "{}") ?: "{}"
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

    private fun saveExtractedCardBalance(bankKey: String, balanceToman: Long, smsTimestamp: Long) {
        val prefs = getPrimaryPrefs() ?: return
        try {
            val rawJson = prefs.getString("card_balances_v4_json", "{}") ?: "{}"
            val obj = JSONObject(rawJson)
            val existingObj = obj.optJSONObject(bankKey)
            val prevTimestamp = existingObj?.optLong("ts", 0L) ?: 0L
            if (smsTimestamp >= prevTimestamp) {
                val entry = JSONObject().apply {
                    put("balanceToman", balanceToman)
                    put("ts", smsTimestamp)
                }
                obj.put(bankKey, entry)
                prefs.edit().putString("card_balances_v4_json", obj.toString()).apply()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Dynamically recalculates `monthlyBalanceToman` and `dailySafeBudgetToman`
     * from the user's actual bank accounts in Room (either all accounts or only Tankhah accounts),
     * minus unpaid commitments of this month.
     */
    suspend fun recalculateRealSafeToSpend() {
        val currentSettings = dao.getSettingsNow() ?: AppSettingsEntity()
        val allAccounts = dao.getAllBankAccountsNow()
        val allTxs = dao.getAllTransactionsNow()
        val allCommitments = dao.getAllCommitmentsNow()

        val activeAccounts = if (currentSettings.onlyTankhahMode) {
            allAccounts.filter { it.isTankhah }
        } else {
            allAccounts
        }
        val accountsBalanceSum = activeAccounts.sumOf { it.balanceToman.coerceAtLeast(0L) }
        val totalAllAccountsSum = allAccounts.sumOf { it.balanceToman.coerceAtLeast(0L) }

        val hasRealSms = getPrimaryPrefs()?.getBoolean("has_real_bank_sms", false) == true
        val nonDemoTxs = allTxs.filter { !isDemoTransaction(it) }
        val activeTxs = if (hasRealSms || nonDemoTxs.isNotEmpty()) nonDemoTxs else allTxs

        val totalDeposits = activeTxs.filter { it.type == "INCOME" }.sumOf { it.userShareToman }
        val totalExpenses = activeTxs.filter { it.type == "EXPENSE" }.sumOf { it.userShareToman }

        val unpaidCommitments = allCommitments
            .filter { !it.isSettled && (it.category == "SAYYAD_ISSUED" || it.category == "LOAN_INSTALLMENT") }
            .sumOf { it.amountToman }

        val rawAccountBalanceToman: Long = when {
            allAccounts.isNotEmpty() -> accountsBalanceSum
            totalDeposits > totalExpenses -> totalDeposits - totalExpenses
            currentSettings.monthlyBalanceToman > 0L -> currentSettings.monthlyBalanceToman
            else -> 0L
        }

        val freeMonthlyBalance = (rawAccountBalanceToman - unpaidCommitments).coerceAtLeast(0L)

        val remainingDays = JalaliCalendarUtils
            .calculateRemainingDaysToPayday(currentSettings.paydayStartDay)
            .coerceIn(1, 30)

        val rawDailySafe = if (freeMonthlyBalance > 0L) {
            (freeMonthlyBalance / remainingDays.toLong()).coerceAtLeast(1_000L)
        } else {
            0L
        }

        val roundedDailySafe = PersianNumberUtils.roundDownToCleanThousands(rawDailySafe)

        dao.saveSettings(
            currentSettings.copy(
                monthlyBalanceToman = totalAllAccountsSum.coerceAtLeast(rawAccountBalanceToman),
                dailySafeBudgetToman = roundedDailySafe
            )
        )
    }

    /**
     * Processes an incoming SMS from `BankSmsBroadcastReceiver` using `JibemanSmsEngine`:
     * 1. Check `val bankProfile = JibemanSmsEngine.resolveBank(senderAddress)`. If null, reject.
     * 2. Call `val parsed = JibemanSmsEngine.parse(body) ?: return null`.
     * 3. Convert Rial to Toman (`parsed.amount / 10L` and `parsed.balance?.div(10L)`).
     * 4. Set `isDeposit = (parsed.direction == "in")` and `bankName = bankProfile.nameFa`.
     * 5. Deduplicate using `JibemanSmsEngine.dedupKey(senderAddress, timestampMillis, body)`.
     */
    suspend fun processIncomingBankSms(
        rawSms: String,
        timestampMillis: Long = System.currentTimeMillis(),
        senderAddress: String = ""
    ): ParsedSmsResult? = smsSyncMutex.withLock {
        val bankProfile = JibemanSmsEngine.resolveBank(senderAddress) ?: return null
        val bankName = bankProfile.nameFa

        val parsed = JibemanSmsEngine.parse(rawSms)
        if (parsed == null) {
            upsertDiscoveredBankAccount(bankName, bankProfile.nameEn, null, timestampMillis)
            return null
        }

        val amountToman = parsed.amount / 10L
        if (amountToman <= 0L) return null

        val balanceToman = parsed.balance?.div(10L)
        val isDeposit = (parsed.direction == "in")
        val uniqueDedupKey = JibemanSmsEngine.dedupKey(senderAddress, timestampMillis, rawSms)

        val fullResult = BankSmsParserEngine.parseWithJibemanEngine(
            senderAddress = senderAddress,
            body = rawSms,
            timestampMillis = timestampMillis
        ) ?: return null

        val prefs = getPrimaryPrefs()
        val hadRealSmsBefore = prefs?.getBoolean("has_real_bank_sms", false) == true
        val importedKeys = prefs?.getStringSet("jibeman_dedup_keys", emptySet())?.toMutableSet()
            ?: mutableSetOf()

        if (!hadRealSmsBefore) {
            prefs?.edit()?.putBoolean("has_real_bank_sms", true)?.apply()
            dao.clearAllBankAccounts()
            cleanupDemoAndDuplicatesIfRealSmsPresent()
        }

        upsertDiscoveredBankAccount(bankName, bankProfile.nameEn, balanceToman, timestampMillis)
        if (balanceToman != null && balanceToman >= 0L) {
            saveExtractedCardBalance(bankName, balanceToman, timestampMillis)
        }

        if (importedKeys.contains(uniqueDedupKey)) {
            recalculateRealSafeToSpend()
            return fullResult
        }

        if (!fullResult.isMicroFee) {
            val txType = when {
                fullResult.isSelfTransfer -> "SELF_TRANSFER"
                isDeposit -> "INCOME"
                else -> "EXPENSE"
            }
            val userShare = if (fullResult.isSelfTransfer) 0L else amountToman

            dao.insertTransaction(
                TransactionEntity(
                    title = fullResult.detectedTitle,
                    storeName = fullResult.detectedStore,
                    purpose = fullResult.detectedPurpose,
                    totalAmountToman = amountToman,
                    userShareToman = userShare,
                    weekdayDate = fullResult.weekdayDate,
                    timeStr = fullResult.timeStr,
                    cardInfo = fullResult.cardLabel,
                    type = txType,
                    isUntagged = fullResult.isUntaggedPurchase,
                    timestamp = timestampMillis,
                    balanceAfterToman = balanceToman,
                    rawSmsBody = rawSms,
                    bankName = bankName
                )
            )
        }

        importedKeys.add(uniqueDedupKey)
        prefs?.edit()?.putStringSet("jibeman_dedup_keys", importedKeys)?.apply()

        if (fullResult.isSalaryDeposit) {
            val currentSettings = dao.getSettingsNow() ?: AppSettingsEntity()
            dao.saveSettings(
                currentSettings.copy(
                    paydayCycleLabel = "${fullResult.weekdayDate} تا ۳۰ روز بعد (تشخیص خودکار حقوق)"
                )
            )
        }

        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
        return fullResult
    }

    /**
     * Scans the FULL SMS Inbox (no 30-day limit, up to 3000 messages ordered by DATE DESC)
     * using `JibemanSmsEngine` so ALL of the user's banks (Blu, Sepah, Pasargad, Mellat, Wepod, etc.)
     * are detected and their latest `مانده` (balance) is extracted.
     */
    suspend fun scanDeviceSmsInboxLast30Days(
        context: Context,
        daysBack: Int = 3650
    ): Int = withContext(Dispatchers.IO) {
        smsSyncMutex.withLock {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hadRealSmsBefore = prefs.getBoolean("has_real_bank_sms", false)
            val importedKeys = prefs.getStringSet("jibeman_dedup_keys", emptySet())?.toMutableSet()
                ?: mutableSetOf()

            var importedCount = 0
            var foundAnyRealBankSms = false

            data class DiscoveredBankState(
                val nameFa: String,
                val nameEn: String,
                var latestBalanceToman: Long? = null,
                var latestBalanceTimestamp: Long = 0L,
                var latestSeenTimestamp: Long = 0L
            )

            val discoveredBanks = linkedMapOf<String, DiscoveredBankState>()

            try {
                val projection = arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                )
                // Remove 30-day cutoff: scan up to 3000 inbox messages ordered newest first
                val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT 3000"

                val cursor = context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                ) ?: context.contentResolver.query(
                    Uri.parse("content://sms/inbox"),
                    projection,
                    null,
                    null,
                    sortOrder
                )

                cursor?.use { c ->
                    val addrIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                    val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)

                    while (c.moveToNext()) {
                        val senderAddress = if (addrIdx >= 0) c.getString(addrIdx).orEmpty().trim() else ""
                        val body = if (bodyIdx >= 0) c.getString(bodyIdx).orEmpty().trim() else ""
                        val timestampMillis = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()

                        if (senderAddress.isBlank() || body.isBlank()) continue

                        // Step 1: Resolve bank strictly from senderAddress via JibemanSmsEngine
                        // Rejects Digikala, Tapsi, Snapp, and all promo SMS 100%
                        val bankProfile = JibemanSmsEngine.resolveBank(senderAddress) ?: continue
                        val bankName = bankProfile.nameFa
                        foundAnyRealBankSms = true

                        val bankState = discoveredBanks.getOrPut(bankName) {
                            DiscoveredBankState(
                                nameFa = bankName,
                                nameEn = bankProfile.nameEn,
                                latestSeenTimestamp = timestampMillis
                            )
                        }
                        if (timestampMillis > bankState.latestSeenTimestamp) {
                            bankState.latestSeenTimestamp = timestampMillis
                        }

                        // Step 2: Parse SMS body via JibemanSmsEngine
                        val parsed = JibemanSmsEngine.parse(body) ?: continue

                        // Step 3: Convert Rial to Toman by dividing by 10
                        val amountToman = parsed.amount / 10L
                        if (amountToman <= 0L) continue
                        val balanceToman = parsed.balance?.div(10L)

                        // Record latest balance for this bank (cursor is DATE DESC, or check timestamp)
                        if (balanceToman != null && balanceToman >= 0L) {
                            if (bankState.latestBalanceToman == null || timestampMillis >= bankState.latestBalanceTimestamp) {
                                bankState.latestBalanceToman = balanceToman
                                bankState.latestBalanceTimestamp = timestampMillis
                            }
                            saveExtractedCardBalance(bankName, balanceToman, timestampMillis)
                        }

                        // Step 4: Direction & Bank Name
                        val isDeposit = (parsed.direction == "in")

                        // Step 5: Exact Jibeman SHA-256 deduplication key
                        val uniqueDedupKey = JibemanSmsEngine.dedupKey(senderAddress, timestampMillis, body)

                        if (importedKeys.contains(uniqueDedupKey)) {
                            continue
                        }

                        val fullResult = BankSmsParserEngine.parseWithJibemanEngine(
                            senderAddress = senderAddress,
                            body = body,
                            timestampMillis = timestampMillis
                        ) ?: continue

                        if (fullResult.isMicroFee) {
                            importedKeys.add(uniqueDedupKey)
                            continue
                        }

                        val txType = when {
                            fullResult.isSelfTransfer -> "SELF_TRANSFER"
                            isDeposit -> "INCOME"
                            else -> "EXPENSE"
                        }
                        val userShare = if (fullResult.isSelfTransfer) 0L else amountToman

                        dao.insertTransaction(
                            TransactionEntity(
                                title = fullResult.detectedTitle,
                                storeName = fullResult.detectedStore,
                                purpose = fullResult.detectedPurpose,
                                totalAmountToman = amountToman,
                                userShareToman = userShare,
                                weekdayDate = fullResult.weekdayDate,
                                timeStr = fullResult.timeStr,
                                cardInfo = fullResult.cardLabel,
                                type = txType,
                                isUntagged = fullResult.isUntaggedPurchase,
                                timestamp = timestampMillis,
                                balanceAfterToman = balanceToman,
                                rawSmsBody = body,
                                bankName = bankName
                            )
                        )

                        importedKeys.add(uniqueDedupKey)
                        importedCount++
                    }
                }
            } catch (_: SecurityException) {
                // Permission not yet granted
            } catch (_: Exception) {
            }

            // When real bank SMS exist on the phone, wipe demo transactions, demo commitments, and demo accounts
            if (foundAnyRealBankSms) {
                prefs.edit().putBoolean("has_real_bank_sms", true).apply()
                if (!hadRealSmsBefore) {
                    dao.clearAllBankAccounts()
                }
                cleanupDemoAndDuplicatesIfRealSmsPresent()

                val existingAccountsByName = dao.getAllBankAccountsNow().associateBy { it.bankName }
                val updatedAccounts = discoveredBanks.values.map { st ->
                    val prev = existingAccountsByName[st.nameFa]
                    BankAccountEntity(
                        bankName = st.nameFa,
                        nameEn = st.nameEn,
                        balanceToman = st.latestBalanceToman ?: prev?.balanceToman ?: 0L,
                        isTankhah = prev?.isTankhah ?: true,
                        cardLast4 = prev?.cardLast4 ?: "",
                        lastUpdatedTimestamp = maxOf(st.latestBalanceTimestamp, st.latestSeenTimestamp),
                        isManual = prev?.isManual ?: false
                    )
                }
                if (updatedAccounts.isNotEmpty()) {
                    dao.upsertBankAccounts(updatedAccounts)
                }
            }

            prefs.edit().putStringSet("jibeman_dedup_keys", importedKeys).apply()
            recalculateRealSafeToSpend()
            persistSnapshotToSharedPrefs()
            importedCount
        }
    }

    /**
     * Triggered when user taps "پاکسازی و بازخوانی تمیز پیامک‌های واقعی".
     */
    suspend fun cleanPurgeAndRescanRealSms(context: Context): Int = withContext(Dispatchers.IO) {
        smsSyncMutex.withLock {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove("jibeman_dedup_keys")
                .remove("card_balances_v4_json")
                .remove("backup_snapshot_json")
                .apply()
            dao.clearAllTransactions()
            dao.clearAllBankAccounts()
            dao.getAllCommitmentsNow().filter { isDemoCommitment(it) }.forEach { demoC ->
                dao.deleteCommitmentById(demoC.id)
            }
        }
        val count = scanDeviceSmsInboxLast30Days(context, daysBack = 3650)
        if (count == 0 && dao.getAllTransactionsNow().isEmpty()) {
            getPrimaryPrefs()?.edit()?.putBoolean("has_real_bank_sms", false)?.apply()
            ensureSeeded()
        }
        count
    }

    /**
     * Saves a voice-tagged expense or updates an untagged SMS transaction.
     * NEVER defaults to "اسنپ‌مارکت" or "خرید روزمره"!
     * Uses the user's exact spoken/typed purpose directly as `title` and `purpose`.
     */
    suspend fun confirmVoiceExpense(
        parsedVoice: ParsedVoiceExpense,
        targetUntaggedTransactionId: Long? = null
    ) {
        val now = System.currentTimeMillis()
        val nowJalali = JalaliCalendarUtils.fromTimestamp(now)

        val cleanPurpose = parsedVoice.purposeCategory.trim()
        if (cleanPurpose.isBlank()) return

        if (targetUntaggedTransactionId != null) {
            val existing = dao.getTransactionById(targetUntaggedTransactionId)
            if (existing != null) {
                val effectiveTotal = existing.totalAmountToman
                val effectiveDong = parsedVoice.dongAmountToman.coerceAtMost(effectiveTotal)
                val effectiveUserShare = (effectiveTotal - effectiveDong).coerceAtLeast(0L)

                val resolvedStore = parsedVoice.storeName.trim().ifEmpty { existing.shortBankName }
                val updatedTitle = if (effectiveDong > 0L) {
                    "$cleanPurpose (سهم شما)"
                } else {
                    cleanPurpose
                }

                dao.updateTransaction(
                    existing.copy(
                        title = updatedTitle,
                        storeName = resolvedStore,
                        purpose = cleanPurpose,
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
                            title = "دنگ $cleanPurpose",
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

        val resolvedStore = parsedVoice.storeName.trim().ifEmpty { "تراکنش صوتی" }
        val title = if (parsedVoice.dongAmountToman > 0L) {
            "$cleanPurpose (سهم خالص شما)"
        } else {
            cleanPurpose
        }

        dao.insertTransaction(
            TransactionEntity(
                title = title,
                storeName = resolvedStore,
                purpose = cleanPurpose,
                totalAmountToman = parsedVoice.totalAmountToman,
                userShareToman = parsedVoice.userShareToman,
                dongAmountToman = parsedVoice.dongAmountToman,
                dongFriendName = parsedVoice.dongFriendName,
                weekdayDate = nowJalali.weekdayDate,
                timeStr = nowJalali.timeStr,
                cardInfo = parsedVoice.cardLabel,
                type = "EXPENSE",
                isUntagged = false,
                timestamp = now,
                bankName = resolvedStore
            )
        )

        if (parsedVoice.dongAmountToman > 0L && parsedVoice.dongFriendName.isNotBlank()) {
            dao.insertCommitment(
                CommitmentEntity(
                    category = "DONG",
                    title = "دنگ $cleanPurpose",
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

        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    suspend fun toggleHideAmounts() {
        val current = dao.getSettingsNow() ?: AppSettingsEntity()
        dao.saveSettings(current.copy(hideAmounts = !current.hideAmounts))
        persistSnapshotToSharedPrefs()
    }

    suspend fun setOnlyTankhahMode(onlyTankhah: Boolean) {
        val current = dao.getSettingsNow() ?: AppSettingsEntity()
        dao.saveSettings(current.copy(onlyTankhahMode = onlyTankhah))
        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    suspend fun toggleAccountTankhah(bankName: String) {
        val account = dao.getBankAccountByName(bankName) ?: return
        dao.upsertBankAccount(account.copy(isTankhah = !account.isTankhah))
        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    suspend fun addOrUpdateBankAccount(
        bankName: String,
        balanceToman: Long,
        isTankhah: Boolean,
        cardLast4: String = ""
    ) {
        val cleanName = bankName.trim().ifEmpty { return }
        val existing = dao.getBankAccountByName(cleanName)
        dao.upsertBankAccount(
            BankAccountEntity(
                bankName = cleanName,
                nameEn = existing?.nameEn ?: "Custom Account",
                balanceToman = balanceToman.coerceAtLeast(0L),
                isTankhah = isTankhah,
                cardLast4 = PersianNumberUtils.toPersianDigits(cardLast4.trim()),
                lastUpdatedTimestamp = System.currentTimeMillis(),
                isManual = true
            )
        )
        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteTransactionById(id)
        recalculateRealSafeToSpend()
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

    suspend fun updateTransactionDetails(
        originalTx: TransactionEntity,
        amountToman: Long,
        balanceAfterToman: Long?,
        weekdayDate: String,
        timeStr: String,
        cardInfo: String,
        purpose: String,
        storeName: String,
        specialTagMode: String
    ) {
        val cleanStore = storeName.trim().ifEmpty { originalTx.shortBankName }
        val cleanPurpose = purpose.trim()
        val updatedTitle = if (cleanPurpose.isNotEmpty() && cleanPurpose != "نیازمند تکمیل صوتی") {
            cleanPurpose
        } else {
            originalTx.title
        }

        dao.updateTransaction(
            originalTx.copy(
                title = updatedTitle,
                storeName = cleanStore,
                purpose = cleanPurpose.ifEmpty { originalTx.purpose },
                totalAmountToman = amountToman,
                userShareToman = amountToman,
                balanceAfterToman = balanceAfterToman,
                weekdayDate = weekdayDate.trim().ifEmpty { originalTx.weekdayDate },
                timeStr = timeStr.trim().ifEmpty { originalTx.timeStr },
                cardInfo = cardInfo.trim().ifEmpty { originalTx.cardInfo },
                isUntagged = cleanPurpose.isEmpty() || cleanPurpose == "نیازمند تکمیل صوتی"
            )
        )

        // If user edited balanceAfterToman for the latest transaction of a bank, also update that bank's balance if applicable
        if (balanceAfterToman != null && balanceAfterToman >= 0L) {
            val bankKey = originalTx.shortBankName
            val acc = dao.getBankAccountByName(bankKey)
            if (acc != null && originalTx.timestamp >= acc.lastUpdatedTimestamp) {
                dao.upsertBankAccount(acc.copy(balanceToman = balanceAfterToman))
            }
        }

        val now = System.currentTimeMillis()
        when (specialTagMode) {
            "INSTALLMENT" -> {
                dao.insertCommitment(
                    CommitmentEntity(
                        category = "LOAN_INSTALLMENT",
                        title = "قسط/وام: ${cleanPurpose.ifEmpty { originalTx.title }}",
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
                        title = "بدهی/طلب (دُنگ): ${cleanPurpose.ifEmpty { originalTx.title }}",
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
        }
        recalculateRealSafeToSpend()
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
        dao.saveSettings(
            settings.copy(
                paydayStartDay = paydayStartDay.coerceIn(1, 31),
                paydayCycleLabel = paydayCycleLabel,
                autoDetectSalary = autoDetectSalary,
                goldPrice18kToman = goldPrice18kToman,
                dailySafeBudgetToman = PersianNumberUtils.roundDownToCleanThousands(dailySafeBudgetToman)
            )
        )
        persistSnapshotToSharedPrefs()
    }

    suspend fun exportBackupJson(): String {
        val settings = dao.getSettingsNow() ?: AppSettingsEntity()
        val txs = dao.getAllTransactionsNow()
        val accounts = dao.getAllBankAccountsNow()
        val commitments = dao.getAllCommitmentsNow()

        val root = JSONObject()
        root.put("app", "OrosiFinance_KhialRahat")
        root.put("version", 4)

        val settingsObj = JSONObject().apply {
            put("dailySafeBudgetToman", PersianNumberUtils.roundDownToCleanThousands(settings.dailySafeBudgetToman))
            put("monthlyBalanceToman", settings.monthlyBalanceToman)
            put("paydayStartDay", settings.paydayStartDay)
            put("paydayCycleLabel", settings.paydayCycleLabel)
            put("autoDetectSalary", settings.autoDetectSalary)
            put("hideAmounts", settings.hideAmounts)
            put("onlyTankhahMode", settings.onlyTankhahMode)
        }
        root.put("settings", settingsObj)

        val accArray = JSONArray()
        accounts.forEach { acc ->
            accArray.put(
                JSONObject().apply {
                    put("bankName", acc.bankName)
                    put("nameEn", acc.nameEn)
                    put("balanceToman", acc.balanceToman)
                    put("isTankhah", acc.isTankhah)
                    put("cardLast4", acc.cardLast4)
                    put("lastUpdatedTimestamp", acc.lastUpdatedTimestamp)
                    put("isManual", acc.isManual)
                }
            )
        }
        root.put("bankAccounts", accArray)

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
                    if (tx.balanceAfterToman != null) put("balanceAfterToman", tx.balanceAfterToman)
                    put("rawSmsBody", tx.rawSmsBody)
                    put("bankName", tx.bankName)
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
                val rawDaily = sObj.optLong("dailySafeBudgetToman", 680_000L).coerceAtLeast(0L)
                dao.saveSettings(
                    AppSettingsEntity(
                        id = 1,
                        dailySafeBudgetToman = PersianNumberUtils.roundDownToCleanThousands(rawDaily),
                        monthlyBalanceToman = sObj.optLong("monthlyBalanceToman", 18_450_000L).coerceAtLeast(0L),
                        paydayStartDay = sObj.optInt("paydayStartDay", 1),
                        paydayCycleLabel = sObj.optString("paydayCycleLabel", "۱ آبان تا ۳۰ آبان"),
                        autoDetectSalary = sObj.optBoolean("autoDetectSalary", true),
                        hideAmounts = sObj.optBoolean("hideAmounts", false),
                        onlyTankhahMode = sObj.optBoolean("onlyTankhahMode", false)
                    )
                )
            }

            val accArray = root.optJSONArray("bankAccounts")
            if (accArray != null) {
                dao.clearAllBankAccounts()
                val accList = mutableListOf<BankAccountEntity>()
                for (i in 0 until accArray.length()) {
                    val o = accArray.getJSONObject(i)
                    accList.add(
                        BankAccountEntity(
                            bankName = o.optString("bankName"),
                            nameEn = o.optString("nameEn"),
                            balanceToman = o.optLong("balanceToman", 0L),
                            isTankhah = o.optBoolean("isTankhah", true),
                            cardLast4 = o.optString("cardLast4", ""),
                            lastUpdatedTimestamp = o.optLong("lastUpdatedTimestamp", System.currentTimeMillis()),
                            isManual = o.optBoolean("isManual", false)
                        )
                    )
                }
                dao.upsertBankAccounts(accList)
            }

            val txArray = root.optJSONArray("transactions")
            if (txArray != null) {
                dao.clearAllTransactions()
                val list = mutableListOf<TransactionEntity>()
                for (i in 0 until txArray.length()) {
                    val o = txArray.getJSONObject(i)
                    val ts = o.optLong("timestamp", System.currentTimeMillis())
                    val realJalali = JalaliCalendarUtils.fromTimestamp(ts)
                    val balanceAfter = if (o.has("balanceAfterToman")) o.optLong("balanceAfterToman") else null
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
                            timestamp = ts,
                            balanceAfterToman = balanceAfter,
                            rawSmsBody = o.optString("rawSmsBody", ""),
                            bankName = o.optString("bankName", "")
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
        dao.clearAllBankAccounts()
        dao.clearAllCommitments()
        getPrimaryPrefs()?.edit()
            ?.remove("backup_snapshot_json")
            ?.remove("jibeman_dedup_keys")
            ?.remove("card_balances_v4_json")
            ?.putBoolean("has_real_bank_sms", false)
            ?.apply()
        dao.saveSettings(
            AppSettingsEntity(
                id = 1,
                dailySafeBudgetToman = 680_000L,
                monthlyBalanceToman = 18_450_000L,
                paydayStartDay = 1,
                paydayCycleLabel = "۱ آبان تا ۳۰ آبان ۱۴۰۴",
                autoDetectSalary = true,
                hideAmounts = false,
                onlyTankhahMode = false
            )
        )
        ensureSeeded()
    }
}
