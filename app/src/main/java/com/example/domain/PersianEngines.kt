package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object PersianNumberUtils {
    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun normalizePersianText(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                'ي', 'ى', 'ئ' -> sb.append('ی')
                'ك' -> sb.append('ک')
                'ة' -> sb.append('ه')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

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
                '،', ',', '٬', '_' -> {} // strip thousand separators when parsing
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

data class JalaliDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val monthName: String,
    val weekdayName: String,
    val hour: Int,
    val minute: Int
) {
    val weekdayDate: String
        get() = "$weekdayName ${PersianNumberUtils.toPersianDigits(day.toString())} $monthName"

    val fullWeekdayDateWithYear: String
        get() = "$weekdayName ${PersianNumberUtils.toPersianDigits(day.toString())} $monthName ${PersianNumberUtils.toPersianDigits(year.toString())}"

    val timeStr: String
        get() = PersianNumberUtils.toPersianDigits(String.format(Locale.US, "%02d:%02d", hour, minute))
}

object JalaliCalendarUtils {
    private val persianMonths = arrayOf(
        "", "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun fromTimestamp(timestampMillis: Long): JalaliDateTime {
        val effectiveMillis = if (timestampMillis > 946684800000L) {
            timestampMillis
        } else {
            System.currentTimeMillis()
        }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"), Locale.US).apply {
            timeInMillis = effectiveMillis
        }
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val weekdayName = when (dayOfWeek) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یکشنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنجشنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> "شنبه"
        }

        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        val monthName = persianMonths.getOrElse(jm) { "آبان" }

        return JalaliDateTime(
            year = jy,
            month = jm,
            day = jd,
            monthName = monthName,
            weekdayName = weekdayName,
            hour = hour,
            minute = minute
        )
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gDaysInMonth[(gm - 1).coerceIn(0, 11)]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return Triple(jy, jm.coerceIn(1, 12), jd.coerceIn(1, 31))
    }

    fun calculateRemainingDaysToPayday(paydayStartDay: Int, nowMillis: Long = System.currentTimeMillis()): Int {
        val jalali = fromTimestamp(nowMillis)
        val currentDay = jalali.day
        val targetDay = paydayStartDay.coerceIn(1, 30)
        return when {
            currentDay < targetDay -> (targetDay - currentDay).coerceIn(1, 30)
            currentDay > targetDay -> (30 - currentDay + targetDay).coerceIn(1, 30)
            else -> 30
        }
    }
}

data class ParsedSmsResult(
    val bankName: String,
    val cardLast4: String,
    val cardLabel: String, // e.g., "کارت ملت ۸۴۱۲" or "کارت صادرات"
    val rawAmountRial: Long,
    val amountToman: Long,
    val extractedBalanceToman: Long?, // Extracted from مانده / موجودی (in Tomans)
    val isIncome: Boolean,
    val isMicroFee: Boolean,
    val isSelfTransfer: Boolean,
    val isSalaryDeposit: Boolean,
    val isUntaggedPurchase: Boolean,
    val isValidFinancialSms: Boolean,
    val weekdayDate: String, // Real Jalali weekday + day + month from SMS timestamp
    val jalaliYear: Int,
    val timeStr: String,     // Real HH:mm in Asia/Tehran from SMS timestamp
    val detectedTitle: String,
    val detectedStore: String,
    val detectedPurpose: String,
    val summaryMessage: String
) {
    val subtitle: String
        get() = "$weekdayDate • $timeStr • $cardLabel"
}

object BankSmsParserEngine {

    private val otpAndNonFinancialKeywords = listOf(
        "رمز پویا",
        "رمزپویا",
        "رمز دوم",
        "رمز یکبار مصرف",
        "رمز یک‌بار مصرف",
        "کد تایید",
        "کد تأیید",
        "کد ورود",
        "کد فعالسازی",
        "کد فعال‌سازی",
        "کد امنیتی",
        "کد اعتبارسنجی",
        "ورود به همراه بانک",
        "ورود به اینترنت بانک",
        "ورود به سامانه",
        "otp",
        "verification code",
        "security code"
    )

    private val financialActionKeywords = listOf(
        "برداشت", "واریز", "خرید", "انتقال", "حواله", "پایا", "ساتنا", "پل",
        "دریافت", "پرداخت", "کسر", "افزایش", "کاهش", "مانده", "موجودی",
        "مبلغ", "ریال", "تومان", "حساب", "کارت", "کارمزد", "قسط", "سود"
    )

    fun isOtpOrNonFinancial(rawSms: String): Boolean {
        val norm = PersianNumberUtils.normalizePersianText(rawSms).lowercase(Locale.ROOT)
        return otpAndNonFinancialKeywords.any { norm.contains(it) }
    }

    fun detectIranianBankName(rawSms: String, senderAddress: String = ""): String {
        val combined = PersianNumberUtils.normalizePersianText("$senderAddress\n$rawSms")
        val lower = combined.lowercase(Locale.ROOT)
        val engDigits = PersianNumberUtils.toEnglishDigits(combined)

        return when {
            combined.contains("بلوبانک") || combined.contains("بلو بانک") || combined.contains("بلو") ||
                lower.contains("blubank") || Regex("""\bblu\b""").containsMatchIn(lower) -> "بلو"

            combined.contains("ویپاد") || lower.contains("wepod") -> "ویپاد"

            combined.contains("آبانک") || combined.contains("ابانک") || lower.contains("abank") -> "آبانک"

            combined.contains("اقتصاد نوین") || combined.contains("اقتصادنوین") ||
                lower.contains("enbank") || lower.contains("eghtesad") || engDigits.contains("627412") -> "اقتصاد نوین"

            combined.contains("مهر ایران") || combined.contains("مهرایران") ||
                combined.contains("قرض الحسنه مهر") || combined.contains("قرض‌الحسنه مهر") ||
                lower.contains("qmb") || lower.contains("mehriran") || engDigits.contains("606373") -> "مهر ایران"

            combined.contains("پاسارگاد") || lower.contains("pasargad") ||
                lower.contains("bpi") || engDigits.contains("502229") -> "پاسارگاد"

            combined.contains("پارسیان") || lower.contains("parsian") ||
                lower.contains("pec") || engDigits.contains("622106") -> "پارسیان"

            combined.contains("صادرات") || combined.contains("سپینو") ||
                lower.contains("saderat") || lower.contains("bsi") || lower.contains("sepino") ||
                engDigits.contains("603769") -> "صادرات"

            combined.contains("ملت") || lower.contains("mellat") ||
                lower.contains("behpardakht") || engDigits.contains("610433") ||
                engDigits.contains("9820000012") -> "ملت"

            combined.contains("ملی") || combined.contains("بام") ||
                lower.contains("melli") || lower.contains("bmi") || lower.contains("baam") ||
                engDigits.contains("603799") || engDigits.contains("9830009417") -> "ملی"

            combined.contains("تجارت") || lower.contains("tejarat") ||
                engDigits.contains("585983") || engDigits.contains("627353") -> "تجارت"

            combined.contains("سامان") || lower.contains("saman") ||
                lower.contains("sb24") || engDigits.contains("621986") -> "سامان"

            combined.contains("رسالت") || lower.contains("resalat") ||
                lower.contains("rqbank") || engDigits.contains("504172") -> "رسالت"

            combined.contains("سپه") || combined.contains("انصار") || combined.contains("کوثر") ||
                combined.contains("قوامین") || combined.contains("حکمت") || combined.contains("مهر اقتصاد") ||
                lower.contains("sepah") || engDigits.contains("589210") -> "سپه"

            combined.contains("مسکن") || lower.contains("maskan") ||
                engDigits.contains("628023") -> "مسکن"

            combined.contains("رفاه") || lower.contains("refah") ||
                engDigits.contains("589463") -> "رفاه"

            combined.contains("آینده") || combined.contains("اینده") ||
                lower.contains("ayandeh") || lower.contains("ba24") || engDigits.contains("636214") -> "آینده"

            combined.contains("کشاورزی") || lower.contains("keshavarzi") ||
                lower.contains("bki") || engDigits.contains("603770") -> "کشاورزی"

            combined.contains("سینا") || lower.contains("sina") ||
                engDigits.contains("639346") -> "سینا"

            combined.contains("شهر") || lower.contains("shahr") ||
                engDigits.contains("502806") || engDigits.contains("504706") -> "شهر"

            combined.contains("گردشگری") || combined.contains("توبانک") ||
                lower.contains("gardeshgari") || lower.contains("tobank") || engDigits.contains("505416") -> "گردشگری"

            combined.contains("پست بانک") || combined.contains("پست‌بانک") ||
                lower.contains("postbank") || engDigits.contains("627760") -> "پست بانک"

            combined.contains("کارآفرین") || combined.contains("کارافرین") ||
                lower.contains("karafarin") || engDigits.contains("627488") -> "کارآفرین"

            combined.contains("ایران زمین") || combined.contains("ایران‌زمین") ||
                lower.contains("iranzamin") || engDigits.contains("505785") -> "ایران زمین"

            combined.contains("سرمایه") || lower.contains("sarmayeh") ||
                engDigits.contains("639607") -> "سرمایه"

            combined.contains("خاورمیانه") || lower.contains("middleeast") ||
                engDigits.contains("585947") -> "خاورمیانه"

            combined.contains("توسعه تعاون") || engDigits.contains("502908") -> "توسعه تعاون"

            combined.contains("بانک دی") || lower.contains("daybank") ||
                engDigits.contains("502938") -> "دی"

            else -> "بانک"
        }
    }

    fun parseBankSms(
        rawSms: String,
        timestampMillis: Long = System.currentTimeMillis(),
        senderAddress: String = ""
    ): ParsedSmsResult {
        val normalized = PersianNumberUtils.normalizePersianText(rawSms.trim())
        val englishText = PersianNumberUtils.toEnglishDigits(normalized)
        val realJalali = JalaliCalendarUtils.fromTimestamp(timestampMillis)

        // 0. Reject OTP / verification codes immediately
        val isOtp = isOtpOrNonFinancial(normalized)
        val hasFinancialKeyword = financialActionKeywords.count {
            normalized.contains(it, ignoreCase = true)
        } >= 2

        // 1. Detect Bank Name from both sender address and SMS body (never default unknown to "بلو")
        val bankName = detectIranianBankName(normalized, senderAddress)

        // 2. Extract Card or Account Last 4 Digits (without inventing fake 6219)
        val maskedCardRegex = Regex("""(?:[0-9]{4,6}[*xX#-]+([0-9]{4}))|(?:([0-9]{4})[*xX#-]+[0-9]{4,6})""")
        val labeledCardRegex = Regex("""(?:کارت|حساب|سپرده|card)[:\s*#-]*([0-9]{4,16})""", RegexOption.IGNORE_CASE)

        val maskedMatch = maskedCardRegex.find(englishText)
        val rawLast4 = when {
            maskedMatch != null -> {
                maskedMatch.groupValues[1].ifEmpty { maskedMatch.groupValues[2] }
            }
            else -> {
                val labeledDigits = labeledCardRegex.find(englishText)?.groupValues?.get(1).orEmpty()
                if (labeledDigits.length >= 4) labeledDigits.takeLast(4) else ""
            }
        }
        val persianCard4 = PersianNumberUtils.toPersianDigits(rawLast4)
        val cardLabel = if (persianCard4.isNotEmpty()) {
            "کارت $bankName $persianCard4"
        } else {
            "کارت $bankName"
        }

        // 3. Separate Account Balance (مانده / موجودی / قابل برداشت) from Transaction Amount (مبلغ)
        val balanceRegex = Regex(
            """(?:مانده\s*قابل\s*برداشت|مانده\s*حساب|مانده\s*فعلی|مانده|موجودی\s*قابل\s*برداشت|موجودی\s*حساب|موجودی|قابل\s*برداشت|balance|avail)[^\n0-9]*([0-9]{2,16})\s*(ریال|تومان|تومن|irr|rial)?""",
            RegexOption.IGNORE_CASE
        )
        val balanceMatch = balanceRegex.find(englishText)
        val extractedBalanceToman: Long? = balanceMatch?.let { m ->
            val rawBal = m.groupValues[1].toLongOrNull() ?: return@let null
            val balUnit = m.groupValues[2]
            val balIsToman = balUnit == "تومان" || balUnit == "تومن"
            if (balIsToman) rawBal else rawBal / 10L
        }

        // Strip out balance clauses/lines, dates, times, and card/reference numbers before extracting transaction amount
        val textWithoutBalance = englishText
            .lines()
            .map { line ->
                // Remove any trailing balance clause on the same line
                line.replace(
                    Regex(
                        """(?:مانده\s*قابل\s*برداشت|مانده\s*حساب|مانده\s*فعلی|مانده|موجودی\s*قابل\s*برداشت|موجودی\s*حساب|موجودی|قابل\s*برداشت|balance|avail).*$""",
                        RegexOption.IGNORE_CASE
                    ),
                    ""
                )
            }
            .filter { line ->
                val trimmed = line.trim()
                trimmed.isNotEmpty() &&
                    !trimmed.startsWith("مانده") &&
                    !trimmed.startsWith("موجودی") &&
                    !trimmed.startsWith("قابل برداشت") &&
                    !trimmed.startsWith("پیگیری") &&
                    !trimmed.startsWith("مرجع") &&
                    !trimmed.startsWith("شماره سند") &&
                    !trimmed.startsWith("ترمینال") &&
                    !trimmed.startsWith("پایانه")
            }
            .joinToString("\n")
            // Strip dates (e.g. 1403/08/14 or 03/08/14 or 2024-11-04)
            .replace(Regex("""\b(?:13|14|20)?[0-9]{2}[/-][0-1]?[0-9][/-][0-3]?[0-9]\b"""), " ")
            // Strip times (e.g. 18:30 or 18:30:45)
            .replace(Regex("""\b[0-2]?[0-9]:[0-5][0-9](?::[0-5][0-9])?\b"""), " ")
            // Strip masked card patterns
            .replace(Regex("""[0-9]{4,6}[*xX#-]+[0-9]{2,6}"""), " ")
            // Strip labeled account/card/reference/sheba numbers unless followed by ریال/تومان
            .replace(
                Regex("""(?:کارت|حساب|سپرده|شبا|پیگیری|مرجع|سند|ترمینال|پایانه|شناسه)[:\s#*.-]*[0-9/-]+(?!\s*(?:ریال|تومان|تومن))"""),
                " "
            )

        // Extract Transaction Amount from cleaned non-balance text
        val explicitLabeledAmountRegex = Regex(
            """(?:مبلغ|برداشت|واریز|خرید|انتقال|حواله|پرداخت|دریافت|کسر|افزایش|کاهش|کارمزد|قسط)[:\s*+-]*([0-9]{4,15})\s*(ریال|تومان|تومن|irr|rial)?""",
            RegexOption.IGNORE_CASE
        )
        val unitAmountRegex = Regex(
            """([0-9]{4,15})\s*(ریال|تومان|تومن|irr|rial)""",
            RegexOption.IGNORE_CASE
        )
        val signedLineAmountRegex = Regex(
            """(?:^|\n)\s*([+-]?[0-9]{4,15})\s*(ریال|تومان|تومن|irr|rial)?\s*(?:$|\n)"""
        )

        var matchedRawAmount: Long? = null
        var matchedUnit = ""
        var hasExplicitMinusSign = false
        var hasExplicitPlusSign = false

        val labeledMatch = explicitLabeledAmountRegex.findAll(textWithoutBalance)
            .firstOrNull { m ->
                val v = m.groupValues[1].toLongOrNull() ?: 0L
                v >= 1_000L && v !in 1390L..1410L
            }

        if (labeledMatch != null) {
            matchedRawAmount = labeledMatch.groupValues[1].toLongOrNull()
            matchedUnit = labeledMatch.groupValues[2]
            hasExplicitMinusSign = labeledMatch.value.contains("-")
            hasExplicitPlusSign = labeledMatch.value.contains("+")
        } else {
            val unitMatch = unitAmountRegex.findAll(textWithoutBalance)
                .firstOrNull { m ->
                    val v = m.groupValues[1].toLongOrNull() ?: 0L
                    v >= 1_000L
                }
            if (unitMatch != null) {
                matchedRawAmount = unitMatch.groupValues[1].toLongOrNull()
                matchedUnit = unitMatch.groupValues[2]
            } else {
                val signedMatch = signedLineAmountRegex.findAll(textWithoutBalance)
                    .firstOrNull { m ->
                        val rawToken = m.groupValues[1]
                        val v = rawToken.trimStart('+', '-').toLongOrNull() ?: 0L
                        v >= 10_000L && rawToken.trimStart('+', '-').length != 16
                    }
                if (signedMatch != null) {
                    val token = signedMatch.groupValues[1]
                    hasExplicitMinusSign = token.startsWith("-")
                    hasExplicitPlusSign = token.startsWith("+")
                    matchedRawAmount = token.trimStart('+', '-').toLongOrNull()
                    matchedUnit = signedMatch.groupValues[2]
                }
            }
        }

        val rawNumber = matchedRawAmount ?: 0L
        val isExplicitToman = matchedUnit == "تومان" || matchedUnit == "تومن" ||
            (matchedUnit.isEmpty() && (normalized.contains("تومان") || normalized.contains("تومن")) && !normalized.contains("ریال"))
        val amountToman = if (rawNumber <= 0L) {
            0L
        } else if (isExplicitToman) {
            rawNumber
        } else {
            rawNumber / 10L
        }
        val rawAmountRial = if (isExplicitToman) rawNumber * 10L else rawNumber

        // 4. Use Real Android SMS Timestamp in Asia/Tehran Timezone (Jalali Calendar)
        val weekdayDate = realJalali.weekdayDate
        val timeStr = realJalali.timeStr

        // 5. Accurate Expense vs Deposit Detection
        val firstExpenseIdx = listOf("برداشت", "خرید", "کسر", "کاهش", "پرداخت", "کارمزد", "قسط")
            .mapNotNull { kw -> normalized.indexOf(kw).takeIf { it >= 0 } }
            .minOrNull() ?: Int.MAX_VALUE

        val firstIncomeIdx = listOf("واریز", "افزایش", "حقوق", "پاداش", "سود")
            .mapNotNull { kw -> normalized.indexOf(kw).takeIf { it >= 0 } }
            .minOrNull() ?: Int.MAX_VALUE

        val isSalaryDeposit = !hasExplicitMinusSign && firstIncomeIdx < firstExpenseIdx && (
            normalized.contains("حقوق") || normalized.contains("پاداش") ||
                (normalized.contains("پایا") && amountToman >= 12_000_000L)
            )

        val isIncome = when {
            hasExplicitMinusSign -> false
            hasExplicitPlusSign && firstExpenseIdx == Int.MAX_VALUE -> true
            isSalaryDeposit -> true
            firstExpenseIdx < firstIncomeIdx -> false
            firstIncomeIdx < firstExpenseIdx -> true
            else -> false
        }

        val isMicroFee = !isIncome &&
            (normalized.contains("کارمزد") || normalized.contains("آبونمان") || normalized.contains("هزینه پیامک")) &&
            amountToman in 1L..5_000L

        val isSelfTransfer = normalized.contains("کارت خود") ||
            normalized.contains("حساب خود") ||
            normalized.contains("انتقال خودی")

        // 6. Store / Purpose detection vs Untagged SMS
        val hasKnownMerchant = normalized.contains("اسنپ") || normalized.contains("دیجی") ||
            normalized.contains("کافه") || normalized.contains("هایپر") ||
            normalized.contains("رستوران") || normalized.contains("افق کوروش") ||
            normalized.contains("شهروند") || normalized.contains("تپسی")

        val isUntaggedPurchase = !isIncome && !isMicroFee && !isSelfTransfer && !hasKnownMerchant

        val detectedStore = when {
            isMicroFee -> "شبکه بانکی شاپرک"
            isSelfTransfer -> "بین کارت‌های خودی ($bankName)"
            isSalaryDeposit -> "واریز حقوق ماهانه ($bankName)"
            normalized.contains("اسنپ") -> "اسنپ‌مارکت"
            normalized.contains("دیجی") -> "دیجی‌کالا"
            normalized.contains("کافه") -> "کافه"
            normalized.contains("افق کوروش") -> "افق کوروش"
            normalized.contains("شهروند") -> "فروشگاه شهروند"
            normalized.contains("هایپر") -> "هایپراستار"
            normalized.contains("تپسی") -> "تپسی"
            isIncome -> "واریز بانکی ($bankName)"
            else -> "خرید کارتی ($bankName)"
        }

        val detectedPurpose = when {
            isMicroFee -> "کارمزد خرد بانکی (نادیده گرفته شده)"
            isSelfTransfer -> "جابه‌جایی داخلی بدون اثر بر تراز"
            isSalaryDeposit -> "شارژ ماه حقوقی جدید"
            hasKnownMerchant -> "خرید روزمره"
            isIncome -> "افزایش موجودی حساب"
            else -> "تکمیل نشده • با لمس میکروفون بگویید"
        }

        val detectedTitle = when {
            isMicroFee -> "کارمزد خرد بانکی (فیلتر شده)"
            isSelfTransfer -> "انتقال بین بانکی خودی ($bankName)"
            isSalaryDeposit -> "واریز حقوق و آغاز ماه حقوقی ($bankName)"
            hasKnownMerchant -> "$detectedStore ($detectedPurpose)"
            isIncome -> "واریز به $cardLabel"
            else -> "برداشت کارتی ($bankName)"
        }

        val isValidFinancialSms = !isOtp && hasFinancialKeyword && amountToman > 0L

        val summaryMessage = when {
            !isValidFinancialSms -> "پیامک غیرمالی یا کد تایید (نادیده گرفته شد)"
            isMicroFee -> "کارمزد ناچیز (${PersianNumberUtils.formatToman(amountToman, true)}) برای حفظ آرامش ذهنی شما نادیده گرفته شد."
            isSelfTransfer -> "انتقال بین کارت‌های خودی تشخیص داده شد و از «خیال راحت امروز» کسر نگردید."
            isSalaryDeposit -> "واریز حقوق (${PersianNumberUtils.formatToman(amountToman, true)}) تشخیص داده شد! چرخه ماه حقوقی و تراز ماه بروزرسانی شد."
            isUntaggedPurchase -> "مبلغ ${PersianNumberUtils.formatToman(rawAmountRial)} ریال به ${PersianNumberUtils.formatToman(amountToman, true)} تبدیل شد."
            else -> "پیامک $bankName به مبلغ ${PersianNumberUtils.formatToman(amountToman, true)} ثبت شد."
        }

        return ParsedSmsResult(
            bankName = bankName,
            cardLast4 = persianCard4,
            cardLabel = cardLabel,
            rawAmountRial = rawAmountRial,
            amountToman = amountToman,
            extractedBalanceToman = extractedBalanceToman,
            isIncome = isIncome,
            isMicroFee = isMicroFee,
            isSelfTransfer = isSelfTransfer,
            isSalaryDeposit = isSalaryDeposit,
            isUntaggedPurchase = isUntaggedPurchase,
            isValidFinancialSms = isValidFinancialSms,
            weekdayDate = weekdayDate,
            jalaliYear = realJalali.year,
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
        val cleaned = PersianNumberUtils.normalizePersianText(text.trim())
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

        // 4. Parse Card (کارت) across all Iranian banks
        val detectedBank = BankSmsParserEngine.detectIranianBankName(cleaned)
        val cardLabel = if (detectedBank != "بانک") {
            "کارت $detectedBank"
        } else {
            "کارت بانکی"
        }

        // 5. Parse Dong Split (سهم دنگ)
        var dongAmount = 0L
        var dongFriend = ""
        if (cleaned.contains("دنگ") || cleaned.contains("دُنگ") || cleaned.contains("سهم")) {
            val friendRegex = Regex("""(?:دنگ|دُنگ|سهم)\s+([^\s]+)""")
            val rawFriend = friendRegex.find(cleaned)?.groupValues?.get(1)
                ?.replace("بود", "")
                ?.replace("شد", "")
                ?.trim()
            dongFriend = if (!rawFriend.isNullOrBlank()) rawFriend else "علی"

            if (cleaned.contains("نصفش") || cleaned.contains("نصف")) {
                dongAmount = totalToman / 2L
            } else {
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
