package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object PersianNumberUtils {
    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '0'..'9' -> sb.append(persianDigits[ch - '0'])
                in '٠'..'٩' -> sb.append(persianDigits[ch - '٠'])
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '۰'..'۹' -> sb.append(('0'.code + (ch - '۰')).toChar())
                in '٠'..'٩' -> sb.append(('0'.code + (ch - '٠')).toChar())
                '،', ',', '٬' -> {} // strip thousand separators when parsing
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatToman(amount: Long, showUnit: Boolean = false, showSign: Boolean = false): String {
        val absVal = kotlin.math.abs(amount)
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formatted = toPersianDigits(formatter.format(absVal))
        val signPrefix = when {
            !showSign -> ""
            amount > 0 -> "+"
            amount < 0 -> "−"
            else -> ""
        }
        return if (showUnit) {
            "$signPrefix$formatted تومان"
        } else {
            "$signPrefix$formatted"
        }
    }

    fun formatDecimal(value: Double, decimals: Int = 2): String {
        val pattern = if (decimals <= 0) "#" else "#." + "#".repeat(decimals)
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            decimalSeparator = '.'
        }
        val formatted = DecimalFormat(pattern, symbols).format(value)
        return toPersianDigits(formatted.replace('.', '٫'))
    }
}

data class ParsedSmsResult(
    val bankName: String,
    val cardLast4: String,
    val cardLabel: String, // e.g., "کارت بلو ۶۲۱۹"
    val rawAmountRial: Long,
    val amountToman: Long,
    val isIncome: Boolean,
    val isMicroFee: Boolean,
    val isSelfTransfer: Boolean,
    val isSalaryDeposit: Boolean,
    val isUntaggedPurchase: Boolean,
    val weekdayDate: String, // e.g., "جمعه ۱ آبان"
    val timeStr: String,     // e.g., "۱۸:۱۰"
    val detectedTitle: String,
    val detectedStore: String,
    val detectedPurpose: String,
    val summaryMessage: String
) {
    val subtitle: String
        get() = "$weekdayDate • $timeStr • $cardLabel"
}

object BankSmsParserEngine {
    // User's own registered 4-digit card suffixes for automatic self-transfer detection
    private val ownCards = mapOf(
        "۶۲۱۹" to "کارت بلو ۶۲۱۹",
        "۸۴۱۲" to "کارت ملت ۸۴۱۲",
        "۵۰۴۱" to "کارت ملی ۵۰۴۱",
        "۷۳۹۰" to "کارت سامان ۷۳۹۰",
        "۱۱۸۴" to "کارت رسالت ۱۱۸۴"
    )

    private val persianMonths = mapOf(
        1 to "فروردین", 2 to "اردیبهشت", 3 to "خرداد",
        4 to "تیر", 5 to "مرداد", 6 to "شهریور",
        7 to "مهر", 8 to "آبان", 9 to "آذر",
        10 to "دی", 11 to "بهمن", 12 to "اسفند"
    )

    fun parseBankSms(rawSms: String): ParsedSmsResult {
        val normalized = rawSms.trim()
        val englishText = PersianNumberUtils.toEnglishDigits(normalized)

        // 1. Detect Bank Name & Card Last 4
        val bankName = when {
            normalized.contains("بلو") || normalized.lowercase().contains("blu") -> "بلو"
            normalized.contains("ملت") -> "ملت"
            normalized.contains("ملی") -> "ملی"
            normalized.contains("سامان") -> "سامان"
            normalized.contains("رسالت") -> "رسالت"
            else -> "بلو"
        }

        val cardRegex = Regex("""(?:کارت|حساب|card)[:\s*]*([0-9]{4})""")
        val matchedCard4 = cardRegex.find(englishText)?.groupValues?.get(1)
            ?: when (bankName) {
                "بلو" -> "6219"
                "ملت" -> "8412"
                "ملی" -> "5041"
                "سامان" -> "7390"
                "رسالت" -> "1184"
                else -> "6219"
            }
        val persianCard4 = PersianNumberUtils.toPersianDigits(matchedCard4)
        val cardLabel = "کارت $bankName $persianCard4"

        // 2. Extract Amount (Rial vs Toman)
        // Look for numbers near مبلغ or ریال or تومان
        val amountRegex = Regex("""(?:مبلغ|برداشت|واریز|خرید|انتقال)?[:\s]*([0-9]{4,12})\s*(ریال|تومان|تومن)?""")
        val allNumbers = amountRegex.findAll(englishText)
            .mapNotNull { m ->
                val num = m.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val unit = m.groupValues[2]
                if (num in 1390..1410) null else Pair(num, unit)
            }.toList()

        val primaryAmountPair = allNumbers.firstOrNull() ?: Pair(3500000L, "ریال")
        val rawNumber = primaryAmountPair.first
        val explicitUnit = primaryAmountPair.second
        val isRial = explicitUnit == "ریال" || (explicitUnit.isEmpty() && (normalized.contains("ریال") || rawNumber >= 50000L))
        val amountToman = if (isRial) rawNumber / 10L else rawNumber
        val rawAmountRial = if (isRial) rawNumber else rawNumber * 10L

        // 3. Extract Time (HH:MM)
        val timeRegex = Regex("""([0-2]?[0-9]:[0-5][0-9])""")
        val rawTime = timeRegex.find(englishText)?.groupValues?.get(1) ?: "18:10"
        val timeStr = PersianNumberUtils.toPersianDigits(rawTime)

        // 4. Extract Weekday & Date
        val weekdays = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
        val foundWeekday = weekdays.firstOrNull { normalized.contains(it) } ?: "جمعه"

        val slashDateRegex = Regex("""(?:140[0-9]/)?([0-1]?[0-9])/([0-3]?[0-9])""")
        val slashMatch = slashDateRegex.find(englishText)
        val datePart = if (slashMatch != null) {
            val monthNum = slashMatch.groupValues[1].toIntOrNull() ?: 8
            val dayNum = slashMatch.groupValues[2].toIntOrNull() ?: 1
            val monthName = persianMonths[monthNum] ?: "آبان"
            "${PersianNumberUtils.toPersianDigits(dayNum.toString())} $monthName"
        } else {
            val textDateRegex = Regex("""([۰-۹0-9]{1,2})\s*(فروردین|اردیبهشت|خرداد|تیر|مرداد|شهریور|مهر|آبان|آذر|دی|بهمن|اسفند)""")
            val textMatch = textDateRegex.find(normalized)
            if (textMatch != null) {
                "${PersianNumberUtils.toPersianDigits(textMatch.groupValues[1])} ${textMatch.groupValues[2]}"
            } else {
                "۱ آبان"
            }
        }
        val weekdayDate = "$foundWeekday $datePart"

        // 5. Special Detections: Micro-fee, Self-transfer, Salary Deposit, Income vs Expense
        val isMicroFee = (normalized.contains("کارمزد") || normalized.contains("آبونمان") || normalized.contains("پیامک")) && amountToman <= 5_000L
        val matchedOwnCardsCount = ownCards.keys.count { persianCard ->
            normalized.contains(persianCard) || englishText.contains(PersianNumberUtils.toEnglishDigits(persianCard))
        }
        val isSelfTransfer = normalized.contains("کارت خود") ||
            normalized.contains("حساب خود") ||
            normalized.contains("انتقال خودی") ||
            matchedOwnCardsCount >= 2

        val isSalaryDeposit = normalized.contains("حقوق") || normalized.contains("پاداش") ||
            (normalized.contains("پایا") && amountToman >= 12_000_000L)

        val isIncome = isSalaryDeposit || normalized.contains("واریز") || normalized.contains("افزایش")

        // 6. Store / Purpose detection vs Untagged SMS
        val hasKnownMerchant = normalized.contains("اسنپ") || normalized.contains("دیجی") ||
            normalized.contains("کافه") || normalized.contains("هایپر") || normalized.contains("رستوران")

        val isUntaggedPurchase = !isIncome && !isMicroFee && !isSelfTransfer && !hasKnownMerchant

        val detectedStore = when {
            isMicroFee -> "شبکه بانکی شاپرک"
            isSelfTransfer -> "بین کارت‌های خودی ($bankName)"
            isSalaryDeposit -> "واریز حقوق ماهانه"
            normalized.contains("اسنپ") -> "اسنپ‌مارکت"
            normalized.contains("دیجی") -> "دیجی‌کالا"
            normalized.contains("کافه") -> "کافه لمیز"
            isIncome -> "واریز بانکی"
            else -> "نامشخص (نیازمند ثبت صوتی)"
        }

        val detectedPurpose = when {
            isMicroFee -> "کارمزد خرد بانکی (نادیده گرفته شده)"
            isSelfTransfer -> "جابه‌جایی داخلی بدون اثر بر تراز"
            isSalaryDeposit -> "شارژ ماه حقوقی جدید"
            hasKnownMerchant -> "خرید روزمره"
            isIncome -> "افزایش موجودی"
            else -> "تکمیل نشده • با لمس میکروفون بگویید"
        }

        val detectedTitle = when {
            isMicroFee -> "کارمزد خرد بانکی (فیلتر شده)"
            isSelfTransfer -> "انتقال بین بانکی خودی ($bankName)"
            isSalaryDeposit -> "واریز حقوق و آغاز ماه حقوقی"
            hasKnownMerchant -> "$detectedStore ($detectedPurpose)"
            isIncome -> "واریز به $cardLabel"
            else -> "برداشت کارتی نامشخص (پیامک $bankName)"
        }

        val summaryMessage = when {
            isMicroFee -> "کارمزد ناچیز (${PersianNumberUtils.formatToman(amountToman, true)}) برای حفظ آرامش ذهنی شما نادیده گرفته شد."
            isSelfTransfer -> "انتقال بین کارت‌های خودی تشخیص داده شد و از «خیال راحت امروز» کسر نگردید."
            isSalaryDeposit -> "واریز حقوق (${PersianNumberUtils.formatToman(amountToman, true)}) تشخیص داده شد! چرخه ماه حقوقی و تراز ماه بروزرسانی شد."
            isUntaggedPurchase -> "مبلغ ${PersianNumberUtils.formatToman(rawAmountRial)} ریال به ${PersianNumberUtils.formatToman(amountToman, true)} تبدیل شد. با لمس آیکون میکروفون فیروزه‌ای، فروشگاه و بابت را با صدا تکمیل کنید."
            else -> "پیامک بانک $bankName به مبلغ ${PersianNumberUtils.formatToman(amountToman, true)} با موفقیت ثبت شد."
        }

        return ParsedSmsResult(
            bankName = bankName,
            cardLast4 = persianCard4,
            cardLabel = cardLabel,
            rawAmountRial = rawAmountRial,
            amountToman = amountToman,
            isIncome = isIncome,
            isMicroFee = isMicroFee,
            isSelfTransfer = isSelfTransfer,
            isSalaryDeposit = isSalaryDeposit,
            isUntaggedPurchase = isUntaggedPurchase,
            weekdayDate = weekdayDate,
            timeStr = timeStr,
            detectedTitle = detectedTitle,
            detectedStore = detectedStore,
            detectedPurpose = detectedPurpose,
            summaryMessage = summaryMessage
        )
    }
}

data class ParsedVoiceExpense(
    val recognizedSpeech: String,
    val totalAmountToman: Long,
    val userShareToman: Long,
    val dongAmountToman: Long,
    val dongFriendName: String,
    val storeName: String,
    val purposeCategory: String,
    val cardLabel: String
)

object PersianVoiceSlangParserEngine {
    private val wordMultipliers = listOf(
        "یک میلیون و پانصد" to 1_500_000L,
        "یک میلیون و دویست" to 1_200_000L,
        "یک و نیم میلیون" to 1_500_000L,
        "یک میلیون" to 1_000_000L,
        "نهصد هزار" to 900_000L,
        "نهصد تومن" to 900_000L,
        "هشتصد هزار" to 800_000L,
        "هشتصد تومن" to 800_000L,
        "هفتصد هزار" to 700_000L,
        "هفتصد تومن" to 700_000L,
        "ششصد هزار" to 600_000L,
        "ششصد تومن" to 600_000L,
        "پانصد هزار" to 500_000L,
        "پونصد هزار" to 500_000L,
        "پانصد تومن" to 500_000L,
        "پونصد تومن" to 500_000L,
        "چهارصد و پنجاه" to 450_000L,
        "چهارصد هزار" to 400_000L,
        "چهارصد تومن" to 400_000L,
        "سیصد و پنجاه" to 350_000L,
        "سیصد هزار" to 300_000L,
        "سیصد تومن" to 300_000L,
        "دویست و پنجاه" to 250_000L,
        "دویست هزار" to 200_000L,
        "دویست تومن" to 200_000L,
        "صد و پنجاه" to 150_000L,
        "صد هزار" to 100_000L,
        "صد تومن" to 100_000L
    )

    fun parseSpokenText(text: String, fallbackTotalToman: Long? = null): ParsedVoiceExpense {
        val cleaned = text.trim()
        val engText = PersianNumberUtils.toEnglishDigits(cleaned)

        // 1. Parse Total Amount in Toman
        var totalToman: Long = fallbackTotalToman ?: 0L
        if (totalToman == 0L) {
            val matchedWord = wordMultipliers.firstOrNull { cleaned.contains(it.first) }
            if (matchedWord != null) {
                totalToman = matchedWord.second
            } else {
                val numRegex = Regex("""([0-9]+)\s*(میلیون|هزار|تومان|تومن)""")
                val firstMatch = numRegex.find(engText)
                if (firstMatch != null) {
                    val value = firstMatch.groupValues[1].toLongOrNull() ?: 500L
                    val unit = firstMatch.groupValues[2]
                    totalToman = when {
                        unit == "میلیون" -> value * 1_000_000L
                        unit == "هزار" -> value * 1_000L
                        value < 2000L -> value * 1_000L // Iranian slang: "500 تومن" = 500,000 Toman
                        else -> value
                    }
                } else {
                    totalToman = 500_000L
                }
            }
        }

        // 2. Parse Store (فروشگاه)
        val knownStores = listOf(
            "اسنپ‌مارکت" to "اسنپ‌مارکت",
            "اسنپ مارکت" to "اسنپ‌مارکت",
            "اسنپمارکت" to "اسنپ‌مارکت",
            "دیجی‌کالا" to "دیجی‌کالا",
            "دیجی کالا" to "دیجی‌کالا",
            "کافه لمیز" to "کافه لمیز",
            "کافه نادری" to "کافه نادری",
            "رستوران نایب" to "رستوران نایب",
            "هایپراستار" to "هایپراستار",
            "افق کوروش" to "افق کوروش",
            "شهروند" to "فروشگاه شهروند",
            "تپسی" to "تاکسی اینترنتی تپسی",
            "اسنپ" to "اسنپ"
        )
        var storeName = knownStores.firstOrNull { cleaned.contains(it.first) }?.second
        if (storeName == null) {
            val fromRegex = Regex("""از\s+([^\s]+(?:\s+[^\s]+)?)""")
            val match = fromRegex.find(cleaned)?.groupValues?.get(1)
                ?.replace("با کارت", "")
                ?.replace("که", "")
                ?.trim()
            storeName = if (!match.isNullOrBlank()) match else "اسنپ‌مارکت"
        }

        // 3. Parse Purpose / Category (بابت)
        val knownPurposes = listOf(
            "خرید میوه" to "خرید میوه و تره‌بار",
            "میوه" to "خرید میوه و تره‌بار",
            "شام" to "شام و رستوران",
            "ناهار" to "ناهار و رستوران",
            "قهوه و کیک" to "کافه و عصرانه",
            "قهوه" to "کافه و عصرانه",
            "لباس" to "خرید پوشاک",
            "دارو" to "داروخانه و سلامت",
            "بنزین" to "سوخت و خودرو"
        )
        var purposeCategory = knownPurposes.firstOrNull { cleaned.contains(it.first) }?.second
        if (purposeCategory == null) {
            val purposeRegex = Regex("""(?:خرید|بابت)\s+([^\s]+(?:\s+[^\s]+)?)""")
            val match = purposeRegex.find(cleaned)?.groupValues?.get(1)
                ?.replace("از", "")
                ?.trim()
            purposeCategory = if (!match.isNullOrBlank()) "خرید $match" else "خرید روزمره"
        }

        // 4. Parse Card (کارت)
        val cardLabel = when {
            cleaned.contains("بلو") -> "کارت بلو ۶۲۱۹"
            cleaned.contains("ملت") -> "کارت ملت ۸۴۱۲"
            cleaned.contains("ملی") -> "کارت ملی ۵۰۴۱"
            cleaned.contains("سامان") -> "کارت سامان ۷۳۹۰"
            cleaned.contains("رسالت") -> "کارت رسالت ۱۱۸۴"
            else -> "کارت بلو ۶۲۱۹"
        }

        // 5. Parse Dong Split (سهم دنگ)
        var dongAmount = 0L
        var dongFriend = ""
        if (cleaned.contains("دنگ") || cleaned.contains("دُنگ") || cleaned.contains("سهم")) {
            // Friend name after دنگ or سهم
            val friendRegex = Regex("""(?:دنگ|دُنگ|سهم)\s+([^\s]+)""")
            val rawFriend = friendRegex.find(cleaned)?.groupValues?.get(1)
                ?.replace("بود", "")
                ?.replace("شد", "")
                ?.trim()
            dongFriend = if (!rawFriend.isNullOrBlank()) rawFriend else "علی"

            if (cleaned.contains("نصفش") || cleaned.contains("نصف")) {
                dongAmount = totalToman / 2L
            } else {
                // Look for number right before "تومنش" or "هزارش" or near "که"
                val dongNumRegex = Regex("""(?:که\s*)?([0-9]+)\s*(?:هزار\s*)?(?:تومنش|تومانش|تومن|هزار)""")
                val dongMatch = dongNumRegex.find(engText)
                if (dongMatch != null) {
                    val rawDongNum = dongMatch.groupValues[1].toLongOrNull() ?: 200L
                    dongAmount = if (rawDongNum < 2000L) rawDongNum * 1_000L else rawDongNum
                } else {
                    val wordDong = wordMultipliers.firstOrNull {
                        cleaned.substringAfter("که", "").contains(it.first)
                    }
                    dongAmount = wordDong?.second ?: 200_000L
                }
            }
        }

        val safeDongAmount = dongAmount.coerceIn(0L, totalToman)
        val userShare = (totalToman - safeDongAmount).coerceAtLeast(0L)

        return ParsedVoiceExpense(
            recognizedSpeech = cleaned,
            totalAmountToman = totalToman,
            userShareToman = userShare,
            dongAmountToman = safeDongAmount,
            dongFriendName = dongFriend,
            storeName = storeName,
            purposeCategory = purposeCategory,
            cardLabel = cardLabel
        )
    }
}
