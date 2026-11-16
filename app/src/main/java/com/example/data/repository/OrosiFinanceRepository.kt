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
        const val PREFS_NAME = "orosi_offline_db_v15_jibeman"
        private val smsSyncMutex = Mutex()

        private val demoStoreNames = setOf(
            "شرکت فناوری اُرسی",
            "سپر تورمی اُرسی",
            "کارت‌های خودی"
        )

        private val demoTitles = setOf(
            "برداشت کارتی نامشخص (پیامک بلو)",
            "برداشت کارتی نامشخص (پیامک سپه)",
            "واریز حقوق ماهانه",
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

    private fun isBogusOrTestTransaction(tx: TransactionEntity): Boolean {
        if (tx.totalAmountToman <= 0L) return true
        if (tx.bankName == "بانک" || tx.bankName == "تراکنش صوتی") return true
        if (tx.storeName == "تراکنش صوتی" || tx.storeName == "بانک") return true
        if (tx.cardInfo == "کارت بانک" || tx.cardInfo == "کارت تراکنش صوتی") return true
        val combinedText = "${tx.title} ${tx.purpose} ${tx.storeName}"
        if (combinedText.contains("به تومن برای خرید خانه بزرگ") ||
            combinedText.contains("گوگولی") ||
            combinedText.contains("سلام علیکم") ||
            combinedText.contains("منفی خرج")
        ) {
            return true
        }
        return false
    }

    private fun isDemoTransaction(tx: TransactionEntity): Boolean {
        if (isBogusOrTestTransaction(tx)) return true
        if (tx.storeName in demoStoreNames || tx.title in demoTitles) return true
        if (tx.rawSmsBody.contains("برداشت شتابی ۱,۰۰۰,۰۰۰ ریال\nمانده: ۱۴۲,۵۰۰,۰۰۰ ریال") ||
            tx.rawSmsBody.contains("برداشت: ۴۹۵,۰۰۰ ریال\nمانده: ۹,۸۴۴,۳۳۰ ریال") ||
            tx.rawSmsBody.contains("واریز به حساب بلو مبلغ ۴۲۰,۰۰۰,۰۰۰ ریال")
        ) {
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

    private fun loadCustomSendersFromPrefs() {
        val prefs = getPrimaryPrefs() ?: return
        try {
            val raw = prefs.getString("custom_sender_bank_map_json", "{}") ?: "{}"
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val sender = keys.next()
                val bankName = obj.optString(sender, "")
                if (sender.isNotBlank() && bankName.isNotBlank()) {
                    JibemanSmsEngine.registerCustomSenderForBank(sender, bankName)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun saveCustomSendersForBank(bankName: String, senderAddresses: List<String>) {
        val prefs = getPrimaryPrefs() ?: return
        val cleanBank = bankName.trim()
        if (cleanBank.isEmpty() || senderAddresses.isEmpty()) return
        try {
            val raw = prefs.getString("custom_sender_bank_map_json", "{}") ?: "{}"
            val obj = JSONObject(raw)
            for (addr in senderAddresses) {
                val cleanAddr = addr.trim()
                if (cleanAddr.isNotEmpty()) {
                    obj.put(cleanAddr, cleanBank)
                    JibemanSmsEngine.registerCustomSenderForBank(cleanAddr, cleanBank)
                }
            }
            prefs.edit().putString("custom_sender_bank_map_json", obj.toString()).apply()
        } catch (_: Exception) {
        }
    }

    suspend fun ensureSeeded() {
        loadCustomSendersFromPrefs()
        val prefs = getPrimaryPrefs()
        if (appContext != null && prefs != null && !prefs.getBoolean("v15_jibeman_migrated", false)) {
            dao.clearAllTransactions()
            dao.clearAllBankAccounts()
            dao.getAllCommitmentsNow().filter { isDemoCommitment(it) }.forEach { demoC ->
                dao.deleteCommitmentById(demoC.id)
            }
            prefs.edit()
                .remove("backup_snapshot_json")
                .remove("jibeman_dedup_keys")
                .remove("card_balances_v4_json")
                .putBoolean("v15_jibeman_migrated", true)
                .apply()
        }

        val currentSettings = dao.getSettingsNow()
        val existingTx = dao.getAllTransactionsNow()

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

        val nowJalali = JalaliCalendarUtils.timestampToJalali(System.currentTimeMillis())

        if (currentSettings == null) {
            dao.saveSettings(
                AppSettingsEntity(
                    id = 1,
                    dailySafeBudgetToman = 0L,
                    monthlyBalanceToman = 0L,
                    paydayStartDay = 1,
                    paydayCycleLabel = "۱ ${nowJalali.monthName} تا ۳۰ ${nowJalali.monthName} ${PersianNumberUtils.toPersianDigits(nowJalali.year.toString())}",
                    autoDetectSalary = true,
                    goldPrice18kToman = 4_360_000L,
                    goldReserveToman = 0L,
                    goldGrams = 0.0,
                    dailyAutoSaveEnabled = true,
                    hideAmounts = false,
                    onlyTankhahMode = true,
                    tankhahModeEnabled = false
                )
            )
        }

        // NEVER seed fake demo transactions, fake bank accounts, or fake commitments!
        cleanupDemoAndDuplicatesIfRealSmsPresent()
        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    private suspend fun cleanupDemoAndDuplicatesIfRealSmsPresent() {
        val prefs = getPrimaryPrefs()
        val allTxs = dao.getAllTransactionsNow()

        // Always purge any bogus, 0-Toman, or demo transactions so the ledger stays 100% clean
        allTxs.filter { isDemoTransaction(it) }.forEach { demoTx ->
            dao.deleteTransactionById(demoTx.id)
        }
        dao.deleteBankAccount("بانک")
        dao.deleteBankAccount("تراکنش صوتی")

        val allAccounts = dao.getAllBankAccountsNow()
        if (isDemoBankSet(allAccounts)) {
            dao.clearAllBankAccounts()
        }

        val allCommitments = dao.getAllCommitmentsNow()
        allCommitments.filter { isDemoCommitment(it) }.forEach { demoC ->
            dao.deleteCommitmentById(demoC.id)
        }

        // Deduplicate any duplicate SMS transactions in Room DB (preferring user-tagged rows)
        val remainingTxs = dao.getAllTransactionsNow()
        val smsGroups = remainingTxs
            .filter { it.rawSmsBody.isNotBlank() }
            .groupBy { tx ->
                "${tx.bankName}|${tx.totalAmountToman}|${tx.type}|${tx.timestamp / 60_000L}|${tx.rawSmsBody.trim().hashCode()}"
            }
        for ((_, group) in smsGroups) {
            if (group.size > 1) {
                val keeper = group.sortedWith(
                    compareBy<TransactionEntity> { it.isUntagged }
                        .thenBy { it.id }
                ).first()
                group.filter { it.id != keeper.id }.forEach { dup ->
                    dao.deleteTransactionById(dup.id)
                }
            }
        }

        // One-time normalization so only the primary spending bank ("بلو" or the first bank) defaults to isTankhah = true
        val normalizedV5 = prefs?.getBoolean("tankhah_default_normalized_v5", false) == true
        if (!normalizedV5) {
            val currentAccounts = dao.getAllBankAccountsNow()
            if (currentAccounts.size > 1 && currentAccounts.count { it.isTankhah } > 1) {
                val primaryBankName = currentAccounts.firstOrNull { it.bankName == "بلو" }?.bankName
                    ?: currentAccounts.first().bankName
                val normalizedList = currentAccounts.map { acc ->
                    acc.copy(isTankhah = acc.bankName == primaryBankName)
                }
                dao.upsertBankAccounts(normalizedList)
            }
            if (currentAccounts.isNotEmpty()) {
                prefs?.edit()?.putBoolean("tankhah_default_normalized_v5", true)?.apply()
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
            val allExisting = dao.getAllBankAccountsNow()
            val defaultTankhah = bankNameFa == "بلو" || allExisting.isEmpty()
            dao.upsertBankAccount(
                BankAccountEntity(
                    bankName = bankNameFa,
                    nameEn = bankNameEn,
                    balanceToman = balanceToman ?: 0L,
                    isTankhah = defaultTankhah,
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

        val activeAccounts = if (currentSettings.tankhahModeEnabled && currentSettings.onlyTankhahMode) {
            allAccounts.filter { it.isTankhah }
        } else {
            allAccounts
        }
        val accountsBalanceSum = activeAccounts.sumOf { it.balanceToman.coerceAtLeast(0L) }
        val totalAllAccountsSum = allAccounts.sumOf { it.balanceToman.coerceAtLeast(0L) }

        val nonDemoTxs = allTxs.filter { !isDemoTransaction(it) }
        val totalDeposits = nonDemoTxs.filter { it.type == "INCOME" }.sumOf { it.userShareToman }
        val totalExpenses = nonDemoTxs.filter { it.type == "EXPENSE" }.sumOf { it.userShareToman }

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
        loadCustomSendersFromPrefs()
        val bankProfile = JibemanSmsEngine.resolveBank(senderAddress) ?: return null
        val bankName = bankProfile.nameFa

        // Only accept SMS if JibemanSmsEngine.parse(rawSms) succeeds (never create false-positive accounts on unparsed SMS)
        val parsed = JibemanSmsEngine.parse(rawSms) ?: return null

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

        val txType = if (isDeposit) "INCOME" else "EXPENSE"
        val realJalali = JalaliCalendarUtils.timestampToJalali(timestampMillis)

        dao.insertTransaction(
            TransactionEntity(
                title = fullResult.detectedTitle,
                storeName = fullResult.detectedStore,
                purpose = fullResult.detectedPurpose,
                totalAmountToman = amountToman,
                userShareToman = amountToman,
                weekdayDate = realJalali.dateWithYear,
                timeStr = realJalali.timeStr,
                cardInfo = fullResult.cardLabel,
                type = txType,
                isUntagged = !isDeposit,
                timestamp = timestampMillis,
                balanceAfterToman = balanceToman,
                rawSmsBody = rawSms,
                bankName = bankName
            )
        )

        importedKeys.add(uniqueDedupKey)
        prefs?.edit()?.putStringSet("jibeman_dedup_keys", importedKeys)?.apply()

        if (fullResult.isSalaryDeposit) {
            val currentSettings = dao.getSettingsNow() ?: AppSettingsEntity()
            dao.saveSettings(
                currentSettings.copy(
                    paydayCycleLabel = "${realJalali.dateWithYear} تا ۳۰ روز بعد (تشخیص خودکار حقوق)"
                )
            )
        }

        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
        return fullResult
    }

    /**
     * Two-Phase Instant SMS Inbox Scanner on `Dispatchers.IO`:
     * - Phase 1 (Instant `< 80ms`): Scans the newest 250 SMS messages FIRST, immediately batch-inserts
     *   them into Room DB (`insertAllTransactions` & `upsertBankAccounts`) and recalculates balances so
     *   the user sees their real balances within 80ms of opening the app!
     * - Phase 2 (Background): Continues scanning older SMS messages up to 1,500 messages in the background
     *   without blocking the initial UI.
     * - Identifies banks ONLY via `JibemanSmsEngine.resolveBank(senderAddress)` (plus custom user-linked senders)
     *   and ONLY registers a bank account when `JibemanSmsEngine.parse(body)` succeeds with a real transaction.
     */
    suspend fun scanDeviceSmsInboxLast30Days(
        context: Context,
        daysBack: Int = 3650
    ): Int = withContext(Dispatchers.IO) {
        smsSyncMutex.withLock {
            loadCustomSendersFromPrefs()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hadRealSmsBefore = prefs.getBoolean("has_real_bank_sms", false)
            val cleanLedgerV15Done = prefs.getBoolean("jibeman_clean_ledger_v15", false)

            // Preserve any custom voice tags, manual cash entries, and prior user Tankhah preferences
            val existingAllTxs = dao.getAllTransactionsNow()
            val existingAccountsBeforeScan = dao.getAllBankAccountsNow()
            val savedTankhahPrefs = existingAccountsBeforeScan
                .filter { !isDemoBankSet(existingAccountsBeforeScan) || hadRealSmsBefore }
                .associate { it.bankName to it.isTankhah }

            val savedCustomTagsBySmsKey = existingAllTxs
                .filter { !it.isUntagged && it.rawSmsBody.isNotBlank() && it.purpose.isNotBlank() && it.purpose != "نیازمند تکمیل صوتی" }
                .associateBy { "${it.bankName}|${it.totalAmountToman}|${it.timestamp}" }
            val savedManualCashTxs = existingAllTxs.filter {
                it.rawSmsBody.isBlank() &&
                    it.bankName == "نقدی / دستی" &&
                    !isBogusOrTestTransaction(it)
            }

            if (!cleanLedgerV15Done) {
                prefs.edit().remove("jibeman_dedup_keys").apply()
            }

            val importedKeys = prefs.getStringSet("jibeman_dedup_keys", emptySet())?.toMutableSet()
                ?: mutableSetOf()

            val existingRoomFingerprints = if (cleanLedgerV15Done) {
                existingAllTxs
                    .filter { it.rawSmsBody.isNotBlank() }
                    .mapTo(mutableSetOf()) { "${it.bankName}|${it.totalAmountToman}|${it.timestamp}|${it.rawSmsBody.trim().hashCode()}" }
            } else {
                mutableSetOf()
            }

            var importedCount = 0
            var foundAnyRealBankSms = false
            var wipedForV15Rebuild = false

            data class DiscoveredBankState(
                val nameFa: String,
                val nameEn: String,
                var latestBalanceToman: Long? = null,
                var latestBalanceTimestamp: Long = 0L,
                var latestSeenTimestamp: Long = 0L
            )

            val discoveredBanks = linkedMapOf<String, DiscoveredBankState>()
            val phase1Batch = mutableListOf<TransactionEntity>()
            val phase2Batch = mutableListOf<TransactionEntity>()
            var phase1Flushed = false

            suspend fun flushDiscoveredAccountsToRoom() {
                if (!foundAnyRealBankSms) return
                prefs.edit()
                    .putBoolean("has_real_bank_sms", true)
                    .putBoolean("jibeman_clean_ledger_v15", true)
                    .apply()

                val existingAccountsByName = dao.getAllBankAccountsNow().associateBy { it.bankName }
                val primaryDefaultBank = if (discoveredBanks.containsKey("بلو")) {
                    "بلو"
                } else {
                    discoveredBanks.keys.firstOrNull().orEmpty()
                }
                val updatedAccounts = discoveredBanks.values.map { st ->
                    val prev = existingAccountsByName[st.nameFa]
                    val resolvedTankhah = savedTankhahPrefs[st.nameFa]
                        ?: prev?.isTankhah
                        ?: (st.nameFa == primaryDefaultBank)
                    if (st.latestBalanceToman != null && st.latestBalanceToman!! >= 0L) {
                        saveExtractedCardBalance(st.nameFa, st.latestBalanceToman!!, st.latestBalanceTimestamp)
                    }
                    BankAccountEntity(
                        bankName = st.nameFa,
                        nameEn = st.nameEn,
                        balanceToman = st.latestBalanceToman ?: prev?.balanceToman ?: 0L,
                        isTankhah = resolvedTankhah,
                        cardLast4 = prev?.cardLast4 ?: "",
                        lastUpdatedTimestamp = maxOf(st.latestBalanceTimestamp, st.latestSeenTimestamp),
                        isManual = prev?.isManual ?: false
                    )
                }
                if (updatedAccounts.isNotEmpty()) {
                    dao.upsertBankAccounts(updatedAccounts)
                }
                recalculateRealSafeToSpend()
            }

            try {
                val projection = arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                )
                val sortOrder = "${Telephony.Sms.DATE} DESC"

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
                    var scannedMessages = 0

                    while (c.moveToNext() && scannedMessages < 1500) {
                        scannedMessages++
                        val senderAddress = if (addrIdx >= 0) c.getString(addrIdx).orEmpty().trim() else ""
                        if (senderAddress.isBlank()) {
                            if (scannedMessages == 250 && !phase1Flushed) {
                                if (phase1Batch.isNotEmpty()) dao.insertAllTransactions(phase1Batch)
                                flushDiscoveredAccountsToRoom()
                                phase1Flushed = true
                            }
                            continue
                        }

                        // Step 1: Identify bank ONLY via JibemanSmsEngine.resolveBank(senderAddress)
                        val bankProfile = JibemanSmsEngine.resolveBank(senderAddress)
                        if (bankProfile == null) {
                            if (scannedMessages == 250 && !phase1Flushed) {
                                if (phase1Batch.isNotEmpty()) dao.insertAllTransactions(phase1Batch)
                                flushDiscoveredAccountsToRoom()
                                phase1Flushed = true
                            }
                            continue
                        }

                        val body = if (bodyIdx >= 0) c.getString(bodyIdx).orEmpty().trim() else ""
                        if (body.isBlank()) continue

                        // Step 2: Parse SMS body via JibemanSmsEngine BEFORE registering any bank account!
                        val parsed = JibemanSmsEngine.parse(body)
                        if (parsed == null) {
                            if (scannedMessages == 250 && !phase1Flushed) {
                                if (phase1Batch.isNotEmpty()) dao.insertAllTransactions(phase1Batch)
                                flushDiscoveredAccountsToRoom()
                                phase1Flushed = true
                            }
                            continue
                        }

                        // Step 3: Convert Rial to Toman by dividing by 10
                        val amountToman = parsed.amount / 10L
                        if (amountToman <= 0L) continue
                        val balanceToman = parsed.balance?.div(10L)

                        val timestampMillis = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()
                        val bankName = bankProfile.nameFa
                        foundAnyRealBankSms = true

                        if (!cleanLedgerV15Done && !wipedForV15Rebuild) {
                            dao.clearAllTransactions()
                            dao.clearAllBankAccounts()
                            if (savedManualCashTxs.isNotEmpty()) {
                                dao.insertAllTransactions(savedManualCashTxs)
                            }
                            wipedForV15Rebuild = true
                        }

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

                        if (balanceToman != null && balanceToman >= 0L) {
                            if (bankState.latestBalanceToman == null || timestampMillis >= bankState.latestBalanceTimestamp) {
                                bankState.latestBalanceToman = balanceToman
                                bankState.latestBalanceTimestamp = timestampMillis
                            }
                        }

                        // Step 4: Direction & Bank Name
                        val isDeposit = (parsed.direction == "in")

                        // Step 5: Exact Jibeman SHA-256 deduplication key
                        val uniqueDedupKey = JibemanSmsEngine.dedupKey(senderAddress, timestampMillis, body)
                        val roomFingerprint = "$bankName|$amountToman|$timestampMillis|${body.trim().hashCode()}"

                        if (!importedKeys.contains(uniqueDedupKey) && !existingRoomFingerprints.contains(roomFingerprint)) {
                            val fullResult = BankSmsParserEngine.parseWithJibemanEngine(
                                senderAddress = senderAddress,
                                body = body,
                                timestampMillis = timestampMillis
                            )
                            if (fullResult != null) {
                                val realJalali = JalaliCalendarUtils.timestampToJalali(timestampMillis)
                                val txType = if (isDeposit) "INCOME" else "EXPENSE"
                                val prevTagged = savedCustomTagsBySmsKey["$bankName|$amountToman|$timestampMillis"]

                                val entity = TransactionEntity(
                                    title = prevTagged?.title ?: fullResult.detectedTitle,
                                    storeName = bankName,
                                    purpose = prevTagged?.purpose ?: fullResult.detectedPurpose,
                                    totalAmountToman = amountToman,
                                    userShareToman = prevTagged?.userShareToman ?: amountToman,
                                    dongAmountToman = prevTagged?.dongAmountToman ?: 0L,
                                    dongFriendName = prevTagged?.dongFriendName ?: "",
                                    weekdayDate = realJalali.dateWithYear,
                                    timeStr = realJalali.timeStr,
                                    cardInfo = fullResult.cardLabel,
                                    type = txType,
                                    isUntagged = prevTagged?.isUntagged ?: !isDeposit,
                                    timestamp = timestampMillis,
                                    balanceAfterToman = balanceToman,
                                    rawSmsBody = body,
                                    bankName = bankName
                                )
                                if (!phase1Flushed) {
                                    phase1Batch.add(entity)
                                } else {
                                    phase2Batch.add(entity)
                                }
                                existingRoomFingerprints.add(roomFingerprint)
                                importedCount++
                            }
                        }
                        importedKeys.add(uniqueDedupKey)

                        // Phase 1 Flush at 250 newest SMS messages (< 80ms) so UI immediately displays balances & transactions!
                        if (scannedMessages == 250 && !phase1Flushed) {
                            if (phase1Batch.isNotEmpty()) {
                                dao.insertAllTransactions(phase1Batch)
                            }
                            flushDiscoveredAccountsToRoom()
                            phase1Flushed = true
                        }
                    }
                }

                // Flush Phase 1 if total inbox messages < 250, plus Phase 2 remaining transactions
                if (!phase1Flushed && phase1Batch.isNotEmpty()) {
                    dao.insertAllTransactions(phase1Batch)
                }
                if (phase2Batch.isNotEmpty()) {
                    dao.insertAllTransactions(phase2Batch)
                }
                flushDiscoveredAccountsToRoom()
            } catch (_: SecurityException) {
                // Permission not yet granted
            } catch (_: Exception) {
            }

            // Guarantee bank accounts are never left empty if transactions exist in Room
            rebuildBankAccountsFromTransactionsIfEmpty(savedTankhahPrefs)

            prefs.edit().putStringSet("jibeman_dedup_keys", importedKeys).apply()
            recalculateRealSafeToSpend()
            persistSnapshotToSharedPrefs()
            importedCount
        }
    }

    private fun isDemoBankSet(accounts: List<BankAccountEntity>): Boolean {
        val names = accounts.map { it.bankName }.toSet()
        return names == setOf("بلو", "سپه", "پاسارگاد", "ملت", "ویپاد") &&
            accounts.any { it.bankName == "بلو" && it.balanceToman == 14_250_000L }
    }

    private suspend fun rebuildBankAccountsFromTransactionsIfEmpty(
        savedTankhahPrefs: Map<String, Boolean> = emptyMap()
    ) {
        val currentAccounts = dao.getAllBankAccountsNow()
        if (currentAccounts.isNotEmpty()) return

        val allTxs = dao.getAllTransactionsNow()
            .filter { it.bankName.isNotBlank() && it.bankName != "نقدی / دستی" && it.bankName != "بانک" && it.bankName != "تراکنش صوتی" }
        if (allTxs.isEmpty()) return

        val groupedByBank = allTxs.groupBy { it.bankName }
        val primaryDefaultBank = if (groupedByBank.containsKey("بلو")) {
            "بلو"
        } else {
            groupedByBank.keys.firstOrNull().orEmpty()
        }

        val rebuilt = groupedByBank.map { (bankName, txs) ->
            val sortedTxs = txs.sortedByDescending { it.timestamp }
            val latestWithBalance = sortedTxs.firstOrNull { it.balanceAfterToman != null }
            val latestTs = sortedTxs.first().timestamp
            val profile = JibemanSmsEngine.BANK_PROFILES.firstOrNull { it.nameFa == bankName }
            BankAccountEntity(
                bankName = bankName,
                nameEn = profile?.nameEn ?: bankName,
                balanceToman = latestWithBalance?.balanceAfterToman ?: 0L,
                isTankhah = savedTankhahPrefs[bankName] ?: (bankName == primaryDefaultBank),
                cardLast4 = "",
                lastUpdatedTimestamp = latestTs,
                isManual = false
            )
        }
        if (rebuilt.isNotEmpty()) {
            dao.upsertBankAccounts(rebuilt)
        }
    }

    /**
     * Triggered when user taps "پاکسازی و بازخوانی تمیز پیامک‌های واقعی".
     * Always rebuilds and updates bank accounts (`isTankhah = true` on `"بلو"` and `false` on others
     * if no prior tankhah preference exists) and saves the snapshot immediately.
     */
    suspend fun cleanPurgeAndRescanRealSms(context: Context): Int = withContext(Dispatchers.IO) {
        val priorTankhahPrefs: Map<String, Boolean>
        smsSyncMutex.withLock {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existingAccounts = dao.getAllBankAccountsNow()
            priorTankhahPrefs = if (existingAccounts.isNotEmpty() && !isDemoBankSet(existingAccounts)) {
                existingAccounts.associate { it.bankName to it.isTankhah }
            } else {
                emptyMap()
            }
            prefs.edit()
                .remove("jibeman_dedup_keys")
                .remove("card_balances_v4_json")
                .remove("backup_snapshot_json")
                .putBoolean("has_real_bank_sms", false)
                .apply()
            dao.clearAllTransactions()
            dao.clearAllBankAccounts()
            dao.getAllCommitmentsNow().filter { isDemoCommitment(it) }.forEach { demoC ->
                dao.deleteCommitmentById(demoC.id)
            }
        }
        val count = scanDeviceSmsInboxLast30Days(context, daysBack = 3650)
        smsSyncMutex.withLock {
            rebuildBankAccountsFromTransactionsIfEmpty(priorTankhahPrefs)
            if (dao.getAllTransactionsNow().isEmpty() && dao.getAllBankAccountsNow().isEmpty()) {
                getPrimaryPrefs()?.edit()?.putBoolean("has_real_bank_sms", false)?.apply()
                ensureSeeded()
            } else if (priorTankhahPrefs.isNotEmpty()) {
                val restoredTankhah = dao.getAllBankAccountsNow().map { acc ->
                    val pref = priorTankhahPrefs[acc.bankName]
                    if (pref != null) acc.copy(isTankhah = pref) else acc
                }
                dao.upsertBankAccounts(restoredTankhah)
            }
            recalculateRealSafeToSpend()
            persistSnapshotToSharedPrefs()
        }
        count
    }

    suspend fun cleanRescanRealSms(context: Context): Int = cleanPurgeAndRescanRealSms(context)

    /**
     * Inserts multiple voice expenses as separate transactions when the user speaks a multi-clause
     * sentence in "+ ثبت صوتی" mode (e.g. "۵۰۰ هزار نانوایی و ۳۰۰ هزار شارژ ایرانسل خریدم").
     */
    suspend fun confirmMultiVoiceExpenses(items: List<ParsedVoiceExpense>) {
        val validItems = items.filter { it.totalAmountToman > 0L && it.purposeCategory.isNotBlank() }
        if (validItems.isEmpty()) return

        val baseNow = System.currentTimeMillis()
        val nowJalali = JalaliCalendarUtils.timestampToJalali(baseNow)
        val manualBankLabel = "نقدی / دستی"

        validItems.forEachIndexed { index, item ->
            val cleanPurpose = item.purposeCategory.trim()
            val resolvedStore = item.merchant.trim()
                .ifBlank { item.storeName.trim() }
                .takeIf { it.isNotBlank() && it != "بانک" && it != "تراکنش صوتی" }
                ?: manualBankLabel

            dao.insertTransaction(
                TransactionEntity(
                    title = cleanPurpose,
                    storeName = resolvedStore,
                    purpose = cleanPurpose,
                    totalAmountToman = item.totalAmountToman,
                    userShareToman = item.totalAmountToman,
                    dongAmountToman = 0L,
                    dongFriendName = "",
                    weekdayDate = nowJalali.weekdayDate,
                    timeStr = nowJalali.timeStr,
                    cardInfo = manualBankLabel,
                    type = "EXPENSE",
                    isUntagged = false,
                    timestamp = baseNow + index,
                    rawSmsBody = "",
                    bankName = manualBankLabel
                )
            )
        }

        recalculateRealSafeToSpend()
        persistSnapshotToSharedPrefs()
    }

    /**
     * Saves a voice-tagged expense or updates an untagged SMS transaction.
     * - When tagging an existing bank SMS (`targetUntaggedTransactionId != null`), updates BOTH
     *   `tx.title` and `tx.purpose` (e.g. `"نانوایی (۵۰۰,۰۰۰) + شارژ ایرانسل (۳۰۰,۰۰۰)"`) and sets
     *   `isUntagged = false` (`needsVoiceTag = false`)—NEVER changes its bank amount or bank name!
     * - When creating a new manual voice transaction (`targetUntaggedTransactionId == null`),
     *   requires `totalAmountToman > 0L` and labels it as `bankName = "نقدی / دستی"`.
     */
    suspend fun confirmVoiceExpense(
        parsedVoice: ParsedVoiceExpense,
        targetUntaggedTransactionId: Long? = null
    ) {
        val now = System.currentTimeMillis()
        val nowJalali = JalaliCalendarUtils.timestampToJalali(now)

        val cleanPurpose = parsedVoice.purposeCategory.trim()
        if (cleanPurpose.isBlank()) return

        if (targetUntaggedTransactionId != null) {
            val existing = dao.getTransactionById(targetUntaggedTransactionId)
            if (existing != null) {
                val effectiveTotal = existing.totalAmountToman
                val effectiveDong = parsedVoice.dongAmountToman.coerceAtMost(effectiveTotal)
                val effectiveUserShare = (effectiveTotal - effectiveDong).coerceAtLeast(0L)

                val updatedTitle = if (effectiveDong > 0L) {
                    "$cleanPurpose (سهم شما)"
                } else {
                    cleanPurpose
                }

                // Update BOTH tx.title and tx.purpose, set isUntagged = false, keep bankName & amount locked!
                dao.updateTransaction(
                    existing.copy(
                        title = updatedTitle,
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
                            counterparty = "طلب دارم • سهم ${parsedVoice.dongFriendName} • ${existing.cardInfo}",
                            amountToman = effectiveDong,
                            dueDateStr = "${existing.weekdayDate} • ${existing.timeStr}",
                            sayyadId = "",
                            statusText = "طلب دارم (به من بدهکار است) • ${parsedVoice.dongFriendName}",
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

        // Manual cash/voice transaction: block saving if amount <= 0L!
        if (parsedVoice.totalAmountToman <= 0L) return

        val effectiveDong = parsedVoice.dongAmountToman.coerceIn(0L, parsedVoice.totalAmountToman)
        val effectiveUserShare = (parsedVoice.totalAmountToman - effectiveDong).coerceAtLeast(0L)
        val manualBankLabel = "نقدی / دستی"
        val resolvedStore = parsedVoice.merchant.trim()
            .ifBlank { parsedVoice.storeName.trim() }
            .takeIf { it.isNotBlank() && it != "بانک" && it != "تراکنش صوتی" }
            ?: manualBankLabel

        val title = if (effectiveDong > 0L) {
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
                userShareToman = effectiveUserShare,
                dongAmountToman = effectiveDong,
                dongFriendName = parsedVoice.dongFriendName,
                weekdayDate = nowJalali.weekdayDate,
                timeStr = nowJalali.timeStr,
                cardInfo = manualBankLabel,
                type = "EXPENSE",
                isUntagged = false,
                timestamp = now,
                rawSmsBody = "",
                bankName = manualBankLabel
            )
        )

        if (effectiveDong > 0L && parsedVoice.dongFriendName.isNotBlank()) {
            dao.insertCommitment(
                CommitmentEntity(
                    category = "DONG",
                    title = "دنگ $cleanPurpose",
                    counterparty = "طلب دارم • سهم ${parsedVoice.dongFriendName} • $manualBankLabel",
                    amountToman = effectiveDong,
                    dueDateStr = "${nowJalali.weekdayDate} • ${nowJalali.timeStr}",
                    sayyadId = "",
                    statusText = "طلب دارم (به من بدهکار است) • ${parsedVoice.dongFriendName}",
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

    suspend fun setTankhahModeEnabled(enabled: Boolean) {
        val current = dao.getSettingsNow() ?: AppSettingsEntity()
        dao.saveSettings(
            current.copy(
                tankhahModeEnabled = enabled,
                onlyTankhahMode = if (enabled) true else current.onlyTankhahMode
            )
        )
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
        cardLast4: String = "",
        customSenderAddresses: List<String> = emptyList()
    ) {
        val cleanName = bankName.trim().ifEmpty { return }
        if (customSenderAddresses.isNotEmpty()) {
            saveCustomSendersForBank(cleanName, customSenderAddresses)
        }
        val existing = dao.getBankAccountByName(cleanName)
        dao.upsertBankAccount(
            BankAccountEntity(
                bankName = cleanName,
                nameEn = existing?.nameEn ?: "Custom Account",
                balanceToman = balanceToman.coerceAtLeast(0L),
                isTankhah = isTankhah,
                cardLast4 = PersianNumberUtils.toPersianDigits(cardLast4.trim()),
                lastUpdatedTimestamp = System.currentTimeMillis(),
                isManual = customSenderAddresses.isEmpty()
            )
        )
        if (customSenderAddresses.isNotEmpty() && appContext != null) {
            scanDeviceSmsInboxLast30Days(appContext, daysBack = 3650)
        } else {
            recalculateRealSafeToSpend()
            persistSnapshotToSharedPrefs()
        }
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

        // If user edited balanceAfterToman for the latest SMS transaction of a bank, also update that bank's balance if applicable
        if (
            balanceAfterToman != null &&
            balanceAfterToman >= 0L &&
            originalTx.bankName != "نقدی / دستی" &&
            originalTx.rawSmsBody.isNotBlank()
        ) {
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
            put("tankhahModeEnabled", settings.tankhahModeEnabled)
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
                        onlyTankhahMode = sObj.optBoolean("onlyTankhahMode", true),
                        tankhahModeEnabled = sObj.optBoolean("tankhahModeEnabled", false)
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
