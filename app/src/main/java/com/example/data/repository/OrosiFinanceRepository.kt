package com.example.data.repository

import com.example.data.local.AppSettingsEntity
import com.example.data.local.CommitmentEntity
import com.example.data.local.OrosiDao
import com.example.data.local.TransactionEntity
import com.example.domain.BankSmsParserEngine
import com.example.domain.ParsedSmsResult
import com.example.domain.ParsedVoiceExpense
import com.example.domain.PersianNumberUtils
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class OrosiFinanceRepository(private val dao: OrosiDao) {

    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val commitmentsFlow: Flow<List<CommitmentEntity>> = dao.observeCommitments()
    val settingsFlow: Flow<AppSettingsEntity?> = dao.observeSettings()

    suspend fun ensureSeeded() {
        val currentSettings = dao.getSettingsNow()
        if (currentSettings == null) {
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
        }
        val existingTx = dao.getAllTransactionsNow()
        if (existingTx.isEmpty()) {
            val now = System.currentTimeMillis()
            dao.insertTransactions(
                listOf(
                    TransactionEntity(
                        title = "برداشت کارتی نامشخص (پیامک بلو)",
                        storeName = "نامشخص",
                        purpose = "نیازمند تکمیل صوتی",
                        totalAmountToman = 350_000L,
                        userShareToman = 350_000L,
                        weekdayDate = "جمعه ۱ آبان",
                        timeStr = "۱۸:۱۰",
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
                        weekdayDate = "جمعه ۱ آبان",
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
                        weekdayDate = "پنجشنبه ۳۰ مهر",
                        timeStr = "۲۱:۴۰",
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
                        weekdayDate = "پنجشنبه ۳۰ مهر",
                        timeStr = "۲۰:۱۵",
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
                        weekdayDate = "چهارشنبه ۲۹ مهر",
                        timeStr = "۱۱:۰۵",
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
                        weekdayDate = "سه‌شنبه ۲۸ مهر",
                        timeStr = "۰۹:۳۰",
                        cardInfo = "کارت ملت ۸۴۱۲",
                        type = "INCOME",
                        isUntagged = false,
                        timestamp = now - 1_000L * 60 * 2880
                    )
                )
            )
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
    }

    suspend fun processIncomingBankSms(rawSms: String): ParsedSmsResult {
        val parsed = BankSmsParserEngine.parseBankSms(rawSms)
        if (parsed.isMicroFee) {
            // Ignored micro-fee: do not clutter transactions or reduce daily peace-of-mind balance
            return parsed
        }

        val currentSettings = dao.getSettingsNow() ?: AppSettingsEntity()
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
                timestamp = System.currentTimeMillis()
            )
        )

        if (parsed.isSalaryDeposit) {
            val newMonthly = currentSettings.monthlyBalanceToman + parsed.amountToman
            val newDaily = (currentSettings.dailySafeBudgetToman + (parsed.amountToman / 30L).coerceAtMost(600_000L))
            dao.saveSettings(
                currentSettings.copy(
                    monthlyBalanceToman = newMonthly,
                    dailySafeBudgetToman = newDaily,
                    paydayCycleLabel = "${parsed.weekdayDate} تا ۳۰ روز بعد (تشخیص خودکار حقوق)"
                )
            )
        } else if (parsed.isIncome && !parsed.isSelfTransfer) {
            dao.saveSettings(
                currentSettings.copy(
                    monthlyBalanceToman = currentSettings.monthlyBalanceToman + parsed.amountToman,
                    dailySafeBudgetToman = currentSettings.dailySafeBudgetToman + (parsed.amountToman / 20L)
                )
            )
        } else if (!parsed.isSelfTransfer) {
            // Deduct expense from today's peace-of-mind budget and monthly balance
            val updatedDaily = (currentSettings.dailySafeBudgetToman - parsed.amountToman).coerceAtLeast(0L)
            val updatedMonthly = (currentSettings.monthlyBalanceToman - parsed.amountToman).coerceAtLeast(0L)
            dao.saveSettings(
                currentSettings.copy(
                    dailySafeBudgetToman = updatedDaily,
                    monthlyBalanceToman = updatedMonthly
                )
            )
        }

        return parsed
    }

    suspend fun confirmVoiceExpense(
        parsedVoice: ParsedVoiceExpense,
        targetUntaggedTransactionId: Long? = null
    ) {
        val currentSettings = dao.getSettingsNow() ?: AppSettingsEntity()
        val now = System.currentTimeMillis()

        if (targetUntaggedTransactionId != null) {
            val existing = dao.getTransactionById(targetUntaggedTransactionId)
            if (existing != null) {
                val effectiveTotal = existing.totalAmountToman
                val effectiveDong = parsedVoice.dongAmountToman.coerceAtMost(effectiveTotal)
                val effectiveUserShare = (effectiveTotal - effectiveDong).coerceAtLeast(0L)
                val refundFromDong = existing.userShareToman - effectiveUserShare

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

                // If part of the untagged SMS expense was actually a friend's Dong, restore that share to daily budget!
                if (refundFromDong > 0L) {
                    dao.saveSettings(
                        currentSettings.copy(
                            dailySafeBudgetToman = currentSettings.dailySafeBudgetToman + refundFromDong,
                            monthlyBalanceToman = currentSettings.monthlyBalanceToman + refundFromDong
                        )
                    )
                }

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
                return
            }
        }

        // Creating a brand-new voice transaction:
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
                weekdayDate = "جمعه ۱ آبان",
                timeStr = "۱۹:۰۵",
                cardInfo = parsedVoice.cardLabel,
                type = "EXPENSE",
                isUntagged = false,
                timestamp = now
            )
        )

        // Deduct ONLY the user's share from the daily safe budget and monthly balance!
        val newDaily = (currentSettings.dailySafeBudgetToman - parsedVoice.userShareToman).coerceAtLeast(0L)
        val newMonthly = (currentSettings.monthlyBalanceToman - parsedVoice.userShareToman).coerceAtLeast(0L)
        dao.saveSettings(
            currentSettings.copy(
                dailySafeBudgetToman = newDaily,
                monthlyBalanceToman = newMonthly
            )
        )

        // Add the friend's share to the Dong tab if present
        if (parsedVoice.dongAmountToman > 0L && parsedVoice.dongFriendName.isNotBlank()) {
            dao.insertCommitment(
                CommitmentEntity(
                    category = "DONG",
                    title = "دنگ ${parsedVoice.purposeCategory} از ${parsedVoice.storeName}",
                    counterparty = "سهم ${parsedVoice.dongFriendName} • پرداخت با ${parsedVoice.cardLabel}",
                    amountToman = parsedVoice.dongAmountToman,
                    dueDateStr = "جمعه ۱ آبان • ۱۹:۰۵",
                    sayyadId = "",
                    statusText = "در انتظار تسویه ${parsedVoice.dongFriendName}",
                    isSettled = false,
                    timestamp = now
                )
            )
        }
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
    }

    suspend fun toggleCommitmentSettled(commitment: CommitmentEntity) {
        val newSettled = !commitment.isSettled
        dao.updateCommitment(
            commitment.copy(
                isSettled = newSettled,
                statusText = if (newSettled) "تسویه و پاس شده ✓" else "فعال در تعهدات ماه"
            )
        )
        if (newSettled && commitment.category == "DONG") {
            val currentSettings = dao.getSettingsNow() ?: AppSettingsEntity()
            dao.saveSettings(
                currentSettings.copy(
                    dailySafeBudgetToman = currentSettings.dailySafeBudgetToman + commitment.amountToman,
                    monthlyBalanceToman = currentSettings.monthlyBalanceToman + commitment.amountToman
                )
            )
        }
    }

    suspend fun convertSurplusToGold(amountToman: Long) {
        val settings = dao.getSettingsNow() ?: AppSettingsEntity()
        val safeAmount = amountToman.coerceAtLeast(100_000L)
        val addedGrams = safeAmount.toDouble() / settings.goldPrice18kToman.coerceAtLeast(1L).toDouble()
        val newGoldGrams = ((settings.goldGrams + addedGrams) * 100.0).toLong() / 100.0
        val newGoldValue = settings.goldReserveToman + safeAmount
        val newMonthly = (settings.monthlyBalanceToman - safeAmount).coerceAtLeast(0L)

        dao.saveSettings(
            settings.copy(
                goldReserveToman = newGoldValue,
                goldGrams = newGoldGrams,
                monthlyBalanceToman = newMonthly
            )
        )

        dao.insertTransaction(
            TransactionEntity(
                title = "تبدیل مازاد تراز به طلای ۱۸ عیار (${PersianNumberUtils.formatDecimal(addedGrams)} گرم)",
                storeName = "سپر تورمی اُرسی",
                purpose = "تبدیل مازاد تراز به طلا",
                totalAmountToman = safeAmount,
                userShareToman = safeAmount,
                weekdayDate = "جمعه ۱ آبان",
                timeStr = "۱۹:۳۰",
                cardInfo = "کارت بلو ۶۲۱۹",
                type = "GOLD_SAVE",
                isUntagged = false,
                timestamp = System.currentTimeMillis()
            )
        )
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
    }

    suspend fun toggleDailyAutoSaveGold(enabled: Boolean) {
        val settings = dao.getSettingsNow() ?: AppSettingsEntity()
        dao.saveSettings(settings.copy(dailyAutoSaveEnabled = enabled))
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
    }

    suspend fun exportBackupJson(): String {
        val settings = dao.getSettingsNow() ?: AppSettingsEntity()
        val txs = dao.getAllTransactionsNow()
        val commitments = dao.getAllCommitmentsNow()

        val root = JSONObject()
        root.put("app", "OrosiFinance_KhialRahat")
        root.put("version", 1)

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
        return try {
            val root = JSONObject(jsonString)
            val sObj = root.optJSONObject("settings")
            if (sObj != null) {
                dao.saveSettings(
                    AppSettingsEntity(
                        id = 1,
                        dailySafeBudgetToman = sObj.optLong("dailySafeBudgetToman", 680_000L),
                        monthlyBalanceToman = sObj.optLong("monthlyBalanceToman", 18_450_000L),
                        paydayStartDay = sObj.optInt("paydayStartDay", 1),
                        paydayCycleLabel = sObj.optString("paydayCycleLabel", "۱ آبان تا ۳۰ آبان ۱۴۰۳"),
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
                    list.add(
                        TransactionEntity(
                            title = o.optString("title"),
                            storeName = o.optString("storeName"),
                            purpose = o.optString("purpose"),
                            totalAmountToman = o.optLong("totalAmountToman"),
                            userShareToman = o.optLong("userShareToman"),
                            dongAmountToman = o.optLong("dongAmountToman"),
                            dongFriendName = o.optString("dongFriendName"),
                            weekdayDate = o.optString("weekdayDate", "جمعه ۱ آبان"),
                            timeStr = o.optString("timeStr", "۱۸:۱۰"),
                            cardInfo = o.optString("cardInfo", "کارت بلو ۶۲۱۹"),
                            type = o.optString("type", "EXPENSE"),
                            isUntagged = o.optBoolean("isUntagged", false),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
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
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun resetToDefaultSeedData() {
        dao.clearAllTransactions()
        dao.clearAllCommitments()
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
