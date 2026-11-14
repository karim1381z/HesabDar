@file:Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")

package com.example.domain

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

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
                '،', ',', '٬', '_' -> {}
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun roundDownToCleanThousands(amountToman: Long): Long {
        if (amountToman < 1_000L) return amountToman.coerceAtLeast(0L)
        return (amountToman / 1_000L) * 1_000L
    }

    fun formatToman(
        amount: Long,
        showUnit: Boolean = false,
        showSign: Boolean = false,
        hideAmount: Boolean = false
    ): String {
        if (hideAmount) {
            return if (showUnit) "**** تومان" else "****"
        }
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

    val dateWithYear: String
        get() = "${PersianNumberUtils.toPersianDigits(day.toString())} $monthName ${PersianNumberUtils.toPersianDigits(year.toString())}"

    val fullWeekdayDateWithYear: String
        get() = "$weekdayName ${PersianNumberUtils.toPersianDigits(day.toString())} $monthName ${PersianNumberUtils.toPersianDigits(year.toString())}"

    val monthYearLabel: String
        get() = "$monthName ${PersianNumberUtils.toPersianDigits(year.toString())}"

    val yearMonthKey: Int
        get() = year * 100 + month

    val timeStr: String
        get() = PersianNumberUtils.toPersianDigits(String.format(Locale.US, "%02d:%02d", hour, minute))
}

object JalaliCalendarUtils {
    private val persianMonths = arrayOf(
        "", "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun monthNameOf(month: Int): String = persianMonths.getOrElse(month.coerceIn(1, 12)) { "آبان" }

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

/**
 * Standalone reconstruction of Jibeman's SMS bank-detection and parsing rules.
 * Reconstructed from classes.dex (DEX 039) in the supplied base.zip.
 */
interface BankSmsParser {
    fun parse(rawMessage: String): JibemanSmsEngine.ParsedSms?
}

object JibemanSmsEngine : BankSmsParser {

    data class BankProfile(
        val nameFa: String,
        val nameEn: String,
        val icon: String,
        val senderIds: List<String>
    )

    data class ParsedSms(
        /** "in" for deposit, "out" for withdrawal; null is retained if bytecode permits it. */
        val direction: String?,
        val amount: Long,
        val balance: Long?,
        val date: String?,
        val time: String?
    )

    data class SmsSenderRow(val address: String, val dateMillis: Long)

    data class ParserSms(val address: String, val dateMillis: Long, val body: String)

    data class DetectedBank(val bank: BankProfile, val senderNumbers: List<String>)

    data class BankDetectionResult(
        val inboxRowsRead: Int,
        val banks: List<DetectedBank>
    ) {
        companion object {
            fun unreadable(): BankDetectionResult = BankDetectionResult(0, emptyList())
        }
    }

    private data class SenderHit(val address: String, val lastMillis: Long)

    /** Exact ordered profile list built in DefaultBankProfiles.<clinit>. */
    val BANK_PROFILES: List<BankProfile> = listOf(
        BankProfile("پاسارگاد", "Pasargad", "ic_bank_pasargad", listOf("b.pasargad", "B.Pasargad", "098500019000", "98500019000", "+98500019000")),
        BankProfile("اقتصاد نوین", "Eghtesad Novin", "ic_bank_eghtesad_novin", listOf("ENBank", "Enbank", "+9890004800", "90004800")),
        BankProfile("شهر", "Shahr", "ic_bank_shahr", listOf("+98200035", "20005", "20003502", "+98200085", "700820428285", "9200035", "98200035", "200035")),
        BankProfile("انصار", "Ansar", "ic_bank_ansar", listOf("+98200036", "100036", "98100038")),
        BankProfile("تجارت", "Tejarat", "ic_bank_tejarat", listOf("5000973189", "985000973189", "tejaratbank", "TejaratBank")),
        BankProfile("رفاه کارگران", "Refah", "ic_bank_refah", listOf("Refah", "REFAH", "REFAH BANK", "Refah Bank", "RefahBank")),
        BankProfile("سامان", "Saman", "ic_bank_saman", listOf("+9820000", "Saman Bank", "Saman", "500095", "SamanBank", "9999920000", "2000084080", "99999984080", "099999984080", "9899999984080", "+989999984080", "+9899999984080", "989999920000", "+989999920000")),
        BankProfile("سرمایه", "Sarmayeh", "ic_bank_sarmayeh", listOf("+98300058", "98300058", "7007058", "987007058", "+987007058")),
        BankProfile("سینا", "Sina", "ic_bank_sina", listOf("Sina Bank", "+9850003700798704", "9850003700798704", "50003700798704", "09850004756", "+9850004756", "9850004756", "50004756", "50004751", "+98300028", "500048", "500019", "98500048", "sina bank", "SinaBank", "sinabank")),
        BankProfile("صادرات ایران", "Saderat", "ic_bank_saderat", listOf("+987007851040", "+9830009419", "9830009419", "30009419", "983-000-9419", "+98200060", "+98200040", "+9820004008", "+98700719", "BankSaderat", "700710", "700718", "98700719", "700719", "7007190")),
        BankProfile("سپه", "Sepah", "ic_bank_sepah", listOf("100072419", "SEPAH", "Sepah", "SEPAHBANK", "SEPAH BANK", "SepahBank", "Sepah Bank", "986715001", "+986715001", "6715001", "986715000", "6715000", "+986715000", "+986715000015", "986715000015", "+989122200207", "200015", "6715000015", "+986830068400107", "98715000015", "6715000016")),
        BankProfile("صنعت و معدن", "Industry & Mine", "ic_bank_sanat_madan", listOf("+9820004003", "+98100099", "100099")),
        BankProfile("قرض \u200Cالحسنه رسالت", "Resalat QH", "ic_bank_resalat", listOf("2000474701", "+982000474701", "982000474701", "Resalat", "resalat", "RESALAT", "ResalatBank", "Resalat Bank", "resalatbank", "50001474701", "9850001474701", "+9850001474701", "9850004747", "+9850004747", "989999904747", "9999904747", "50004747", "500014747", "+9820004747", "20004746", "20004747", "+98500014747", "9820004747")),
        BankProfile("قرض \u200Cالحسنه مهر ایران", "Mehr Iran", "ic_bank_mehr_iran", listOf("B.QMEHRIRAN")),
        BankProfile("قوامین", "Ghavamin", "ic_bank_ghavamin", listOf("+981000222", "+9820000222", "+981105151", "2000222", "2000228")),
        BankProfile("مسکن", "Maskan", "ic_bank_maskan", listOf("bank maskan", "+9810002503", "+9850004920", "+98500094", "100025", "98100025", "Bank Maskan", "9850004930", "BANK MASKAN", "BANKMASKAN", "BankMaskan")),
        BankProfile("ملت", "Mellat", "ic_bank_mellat", listOf("+9815560001", "+981000920000", "981000920000", "1000920000", "9815560001", "+9830007505", "+9820003304", "+9820003305", "+9830003304", "Bank Mellat", "30003305", "BankMellat", "500092000")),
        BankProfile("ملی ایران", "Melli Iran", "ic_bank_melli", listOf("+987007170", "98500043087", "300084731", "+989032229936", "+98700717", "+98200044", "+9820004000", "98700717", "700717", "9830009417", "+9830009417", "30009417", "983000941001", "200080", "3000941001", "9830009417", "Bank Melli", "+98700759", "98300094170", "+983000941001")),
        BankProfile("مهر اقتصاد", "Mehr Eghtesad", "ic_bank_mehr_eghtesad", listOf("+98200089", "+98100089", "+982000089", "+981000089")),
        BankProfile("پارسیان", "Parsian", "ic_bank_parsian", listOf("99902318", "99992318", "+98200082", "+98300054", "+98500024", "+9850002318", "+9850001099", "50001099", "300071", "9830007171", "9810005403", "PARSIANBANK", "ParsianBank")),
        BankProfile("پست ایران", "Post", "ic_bank_post", listOf("POSTBANK", "9840400108", "+9840400108", "40400108", "50004940", "+9820004940", "9820004940", "20004940", "+98200029", "+98100029", "50004949", "98700717", "9850004940", "98500009440", "+9850004940")),
        BankProfile("کارآفرین", "Karafarin", "ic_bank_karafarin", listOf("200057780", "B.Karafarin", "98200004321", "+9830004321", "30004321", "+98200004321", "50004858", "50004857", "98200002341", "981000004", "200004321")),
        BankProfile("کشاورزی", "Keshavarzi", "ic_bank_keshavarzi", listOf("+98300081301", "5000181301", "KESHAVARZI", "+989999944444", "9999944444", "989999944444")),
        BankProfile("ایران زمین", "Iran Zamin", "ic_bank_iran_zamin", listOf("IZBANK")),
        BankProfile("گردشگری", "Tourism", "ic_bank_gardeshgari", listOf("TourismBank", "+982000300", "982000309", "982000300")),
        BankProfile("کوثر", "Kosar", "ic_bank_kosar", listOf("+9850002477", "10002477", "9810002477", "6715014005", "98715014005")),
        BankProfile("توسعه تعاون", "Tosee Taavon", "ic_bank_tosee_taavon", listOf("ttbank", "TTBANK", "+9820006438", "+985000257", "5000157", "+985000157", "500158", "30005816", "+989810007000", "9810007000")),
        BankProfile("خاورمیانه", "Middle East", "ic_bank_khavar_mianeh", listOf("9820004861", "+9820004861", "20004861", "20004840", "+9820004860", "9820004860", "20004860")),
        BankProfile("دی", "Dey", "ic_bank_dey", listOf("2000766", "+9820004002", "+9820043", "+9830002726", "Day Bank", "Day", "+98300097500027", "3000766", "500018", "982000766", "DayBank", "98200766", "+982000766\u202C\u200F")),
        BankProfile("حکمت ایرانیان", "Hekmat Iranian", "ic_bank_hekmat", listOf("+9820008955")),
        BankProfile("مؤسسه اعتباری توسعه", "Tosee Credit Inst.", "ic_bank_tosee", listOf("+9830005816")),
        BankProfile("توسعه صادرات", "EDBI", "ic_bank_tosee_saderat", listOf("7000730", "+9830009430", "9830009430", "30009430")),
        BankProfile("مؤسسه اعتباری ملل", "Melal Credit Inst.", "ic_bank_melall", listOf("+98200022222")),
        BankProfile("مؤسسه اعتباری نور", "Noor Credit Inst.", "ic_bank_noor", listOf("9830009480", "30009480", "+9820004009", "7007780", "20004293", "0200080947001", "0200002734006")),
        BankProfile("بلو", "Blu", "ic_bank_blu", listOf("+982187641", "98300087641", "300087641", "989999987641", "9999987641", "+989999987641")),
        BankProfile("ویپاد", "wepod", "ic_bank_wepod", listOf("+981000214", "98500011", "5000114", "+985000114", "985000114", "981000214", "1000214", "9830009017", "30009017")),
        BankProfile("پارسیان", "Parsian", "ic_bank_parsian", listOf("9830007171", "9899902318")),
        BankProfile("بانکینو", "bankino", "ic_bank_bankino", listOf("20004860")),
        BankProfile("ایران ونزوئلا", "iran venezuela", "ic_bank_iran_venezuela", listOf("20004860")),
    )

    private val numberToProfile: Map<String, BankProfile> by lazy {
        LinkedHashMap<String, BankProfile>().apply {
            for (profile in BANK_PROFILES) {
                for (number in profile.senderIds) put(number, profile)
            }
        }
    }

    private val lowercaseNumberToProfile: Map<String, BankProfile> by lazy {
        LinkedHashMap<String, BankProfile>().apply {
            for ((number, profile) in numberToProfile) {
                putIfAbsent(number.lowercase(Locale.ROOT), profile)
            }
        }
    }

    fun hasUssdMark(sender: String): Boolean = sender.indexOf('*') >= 0 || sender.indexOf('#') >= 0

    fun toLatinDigits(sender: String): String {
        val out = StringBuilder(sender.length)
        for (ch in sender) {
            val cp = ch.code
            when {
                cp in 0x06F0..0x06F9 -> out.append((cp - 0x06F0 + '0'.code).toChar())
                cp in 0x0660..0x0669 -> out.append((cp - 0x0660 + '0'.code).toChar())
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    fun normalizeSender(sender: String): String {
        var value = toLatinDigits(sender).lowercase(Locale.ROOT).trim()
            .filter { it.isLetterOrDigit() }
        if (value.startsWith("0098")) {
            value = value.substring(4)
        } else if (value.startsWith("98") && value.length > 8) {
            value = value.substring(2)
        }
        return value
    }

    fun senderMatches(first: String, second: String): Boolean {
        if (hasUssdMark(first) || hasUssdMark(second)) return false
        val a = normalizeSender(first)
        val b = normalizeSender(second)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        val bothNumeric = a.all { it.isDigit() } && b.all { it.isDigit() }
        return bothNumeric && a.length >= 5 && b.length >= 5 &&
            (a.endsWith(b) || b.endsWith(a))
    }

    fun findByNumber(sender: String): BankProfile? =
        numberToProfile[sender] ?: lowercaseNumberToProfile[sender.trim().lowercase(Locale.ROOT)]

    fun resolveBank(sender: String): BankProfile? {
        findByNumber(sender)?.let { return it }
        if (hasUssdMark(sender)) return null
        val normalized = normalizeSender(sender)
        if (normalized.isEmpty()) return null
        val numeric = normalized.all { it.isDigit() }
        for (profile in BANK_PROFILES) {
            for (candidateRaw in profile.senderIds) {
                if (hasUssdMark(candidateRaw)) continue
                val candidate = normalizeSender(candidateRaw)
                if (candidate.isEmpty()) continue
                if (candidate == normalized) return profile
                val candidateNumeric = candidate.all { it.isDigit() }
                if (numeric && candidateNumeric && normalized.length >= 5 && candidate.length >= 5 &&
                    (normalized.endsWith(candidate) || candidate.endsWith(normalized))) {
                    return profile
                }
            }
        }
        return null
    }

    fun detectBanks(rows: List<SmsSenderRow>): BankDetectionResult {
        if (rows.isEmpty()) return BankDetectionResult.unreadable()

        val rowsByNormalizedSender = LinkedHashMap<String, MutableList<SmsSenderRow>>()
        for (row in rows) {
            val normalized = normalizeSender(row.address)
            if (normalized.isEmpty()) continue
            rowsByNormalizedSender.getOrPut(normalized) { mutableListOf() }.add(row)
        }

        val hitsByEnglishName = LinkedHashMap<String, Pair<BankProfile, MutableList<SenderHit>>>()
        for (sameSenderRows in rowsByNormalizedSender.values) {
            var newest = sameSenderRows.first()
            for (candidate in sameSenderRows.drop(1)) {
                if (candidate.dateMillis > newest.dateMillis) newest = candidate
            }
            val bank = resolveBank(newest.address) ?: continue
            val bucket = hitsByEnglishName.getOrPut(bank.nameEn) { bank to mutableListOf() }
            bucket.second += SenderHit(newest.address, newest.dateMillis)
        }

        val detected = hitsByEnglishName.values
            .sortedByDescending { entry -> entry.second.maxOfOrNull { it.lastMillis } ?: Long.MIN_VALUE }
            .map { (bank, hits) ->
                DetectedBank(
                    bank,
                    hits.sortedByDescending { it.lastMillis }.map { it.address }
                )
            }
        return BankDetectionResult(rows.size, detected)
    }

    // ------------------------- GenericSmsParser -------------------------

    private val depositKeywords = listOf(
        "واریز", "دریافت", "بستانکار", "افزایش", "عودت", "برگشت", "نشست", "به حساب شما"
    )
    private val withdrawalKeywords = listOf(
        "برداشت", "خرید", "خریداری", "پرداخت", "بدهکار", "کاهش", "انتقال", "حواله", "کارمزد", "قبض"
    )
    private val depositPositional = listOf("به حساب", "بستانکار")
    private val withdrawPositional = listOf("از حساب", "از سپرده")
    private val contextWithdraw = listOf("کارت اعتباری")
    private val notificationMarkers = listOf("شناسه تایید", "متشکریم", "سپاسگزاریم")
    private val amountLabels = listOf("مبلغ")
    private val balanceLabels = listOf("موجودی حساب", "مانده حساب", "موجودی", "مانده")

    private const val AFTER_LABEL = "[^\\d]{0,12}?([\\d][\\d,]*)"
    private val amountLabelRegexes = amountLabels.map {
        Regex(Pattern.quote(it) + AFTER_LABEL)
    }
    private val balanceLabelRegexes = balanceLabels.map {
        Regex(Pattern.quote(it) + AFTER_LABEL)
    }

    private val signedGroupedRegex = Regex("([+\\-−])?\\s*(\\d{1,3}(?:,\\d{3})+)")
    private val trailingSignRegex = Regex("\\d{1,3}(?:,\\d{3})+\\s*([+\\-−])")
    private val leadingSignRegex = Regex("([+\\-−])\\s*\\d{1,3}(?:,\\d{3})+")
    private val dateRegex = Regex("\\b\\d{2,4}[/-]\\d{1,2}[/-]\\d{1,2}\\b")
    private val timeRegex = Regex("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b")
    private val otpRegex = Regex("(?<![ء-ۿA-Za-z])(?:رمز|پویا|کد\\s*تایید|کد\\s*تأیید|otp|code)")

    fun normalizeMessage(message: String): String {
        val out = StringBuilder(message.length)
        for (ch in message) {
            val cp = ch.code
            when {
                cp == 0x064A || cp == 0x0649 -> out.append('\u06CC')
                cp == 0x0643 -> out.append('\u06A9')
                cp in 0x06F0..0x06F9 -> out.append((cp - 0x06F0 + '0'.code).toChar())
                cp in 0x0660..0x0669 -> out.append((cp - 0x0660 + '0'.code).toChar())
                isBidi(cp) -> Unit
                else -> out.append(ch)
            }
        }
        return out.toString().replace("\r\n", "\n")
    }

    private fun isBidi(codePoint: Int): Boolean =
        codePoint == 8203 || codePoint == 8204 || codePoint == 8205 ||
            codePoint == 8206 || codePoint == 8207 || codePoint == 65279 ||
            codePoint in 8234..8238 || codePoint in 8294..8297

    private fun numberAfterLabel(text: String, patterns: List<Regex>): Long? {
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val raw = match.groupValues.getOrNull(1) ?: continue
            val parsed = parseGrouped(raw)
            if (parsed != null) return parsed
        }
        return null
    }

    private fun parseGrouped(raw: String): Long? {
        val compact = raw.replace(",", "")
        if (compact.length !in 1..12) return null
        val value = compact.toLongOrNull() ?: return null
        return value.takeIf { it >= 0L }
    }

    private fun keywordDirectionNear(text: String, amountStart: Int): String? {
        if (amountStart <= 0) return null
        val startIndex = minOf(amountStart - 1, text.length - 1)
        var bestIndex = -1
        var direction: String? = null
        for (keyword in depositKeywords) {
            val index = text.lastIndexOf(keyword, startIndex, ignoreCase = true)
            if (index >= 0 && index <= startIndex && index > bestIndex) {
                bestIndex = index
                direction = "in"
            }
        }
        for (keyword in withdrawalKeywords) {
            val index = text.lastIndexOf(keyword, startIndex, ignoreCase = true)
            if (index >= 0 && index <= startIndex && index > bestIndex) {
                bestIndex = index
                direction = "out"
            }
        }
        return direction
    }

    private fun adjacentSign(text: String): String {
        trailingSignRegex.find(text)?.groupValues?.getOrNull(1)?.let { if (it.isNotEmpty()) return it }
        leadingSignRegex.find(text)?.groupValues?.getOrNull(1)?.let { if (it.isNotEmpty()) return it }
        return ""
    }

    private fun signChars(ch: Char?): String = when (ch) {
        '+' -> "+"
        '-', '−' -> ch.toString()
        else -> ""
    }

    override fun parse(rawMessage: String): ParsedSms? {
        val text = normalizeMessage(rawMessage)
        if (otpRegex.containsMatchIn(text)) return null

        val balance = numberAfterLabel(text, balanceLabelRegexes)
        if (balance == null && notificationMarkers.any { text.contains(it) }) return null

        var amount: Long? = null
        var amountStart = -1
        var amountFromLabel = false
        var sign = ""

        for (pattern in amountLabelRegexes) {
            val match = pattern.find(text) ?: continue
            val group = match.groups[1] ?: continue
            val parsed = parseGrouped(group.value) ?: continue
            if (parsed <= 0L) continue
            amount = parsed
            amountStart = group.range.first
            val signBefore = signChars(text.getOrNull(group.range.first - 1))
            val signAfter = signChars(text.getOrNull(group.range.last + 1))
            sign = if (signBefore.isNotEmpty()) signBefore else signAfter
            amountFromLabel = true
            break
        }

        if (amount == null) {
            for (match in signedGroupedRegex.findAll(text)) {
                val numberGroup = match.groups[2] ?: continue
                val parsed = parseGrouped(numberGroup.value) ?: continue
                if (parsed <= 0L) continue
                if (balance != null && parsed == balance) continue

                amount = parsed
                amountStart = numberGroup.range.first
                val explicitSign = match.groupValues.getOrNull(1).orEmpty()
                val signAfterNumber = signChars(text.getOrNull(numberGroup.range.last + 1))
                sign = if (explicitSign.isNotEmpty()) explicitSign else signAfterNumber
                break
            }
        }

        val parsedAmount = amount ?: return null
        if (parsedAmount <= 0L) return null

        var direction = keywordDirectionNear(text, amountStart)
        if (direction == null) {
            direction = when {
                depositKeywords.any { text.contains(it) } -> "in"
                withdrawalKeywords.any { text.contains(it) } -> "out"
                else -> null
            }
        }

        if (sign.isEmpty()) sign = adjacentSign(text)
        if (direction == null) {
            direction = when (sign) {
                "+" -> "in"
                "-", "−" -> "out"
                else -> null
            }
        }

        if (direction == null) {
            direction = when {
                depositPositional.any { text.contains(it) } -> "in"
                withdrawPositional.any { text.contains(it) } -> "out"
                contextWithdraw.any { text.contains(it) } -> "out"
                else -> null
            }
        }

        if (balance == null && (!amountFromLabel || direction == null)) return null

        val date = dateRegex.find(text)?.value
        val time = timeRegex.find(text)?.value
        return ParsedSms(direction, parsedAmount, balance, date, time)
    }

    fun dedupPayload(address: String, dateMillis: Long, body: String): String =
        normalizeSender(address) + "|" + dateMillis.toString() + "|" + body

    fun dedupKeyOf(sms: ParserSms): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(dedupPayload(sms.address, sms.dateMillis, sms.body).toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun dedupKey(address: String, dateMillis: Long, body: String): String =
        dedupKeyOf(ParserSms(address, dateMillis, body))

    fun parserCatalog(): Map<String, Any> = linkedMapOf(
        "depositKeywords" to depositKeywords,
        "withdrawalKeywords" to withdrawalKeywords,
        "depositPositional" to depositPositional,
        "withdrawPositional" to withdrawPositional,
        "contextWithdraw" to contextWithdraw,
        "notificationMarkers" to notificationMarkers,
        "amountLabels" to amountLabels,
        "balanceLabels" to balanceLabels,
        "afterLabelRegex" to AFTER_LABEL,
        "signedGroupedRegex" to signedGroupedRegex.pattern,
        "trailingSignRegex" to trailingSignRegex.pattern,
        "leadingSignRegex" to leadingSignRegex.pattern,
        "dateRegex" to dateRegex.pattern,
        "timeRegex" to timeRegex.pattern,
        "otpRegex" to otpRegex.pattern
    )
}

data class ParsedSmsResult(
    val bankName: String,
    val cardLast4: String,
    val cardLabel: String,
    val rawAmountRial: Long,
    val amountToman: Long,
    val extractedBalanceToman: Long?,
    val isIncome: Boolean,
    val isMicroFee: Boolean,
    val isSelfTransfer: Boolean,
    val isSalaryDeposit: Boolean,
    val isUntaggedPurchase: Boolean,
    val isValidFinancialSms: Boolean,
    val dedupKey: String,
    val weekdayDate: String,
    val jalaliYear: Int,
    val timeStr: String,
    val detectedTitle: String,
    val detectedStore: String,
    val detectedPurpose: String,
    val summaryMessage: String
) {
    val subtitle: String
        get() = "$weekdayDate • $timeStr • $cardLabel"
}

object BankSmsParserEngine {

    fun parseWithJibemanEngine(
        senderAddress: String,
        body: String,
        timestampMillis: Long
    ): ParsedSmsResult? {
        val bankProfile = JibemanSmsEngine.resolveBank(senderAddress) ?: return null
        val parsed = JibemanSmsEngine.parse(body) ?: return null

        val amountToman = parsed.amount / 10L
        if (amountToman <= 0L) return null

        val balanceToman = parsed.balance?.div(10L)
        val isDeposit = (parsed.direction == "in")
        val bankName = bankProfile.nameFa
        val uniqueDedupKey = JibemanSmsEngine.dedupKey(senderAddress, timestampMillis, body)

        val realJalali = JalaliCalendarUtils.fromTimestamp(timestampMillis)
        val normalizedBody = JibemanSmsEngine.normalizeMessage(body)

        val maskedCardRegex = Regex("""(?:[0-9]{4,6}[*xX#-]+([0-9]{4}))|(?:([0-9]{4})[*xX#-]+[0-9]{4,6})""")
        val labeledCardRegex = Regex("""(?:کارت|حساب|سپرده)[:\s*#-]*([0-9]{4,16})""")
        val maskedMatch = maskedCardRegex.find(normalizedBody)
        val rawLast4 = when {
            maskedMatch != null -> maskedMatch.groupValues[1].ifEmpty { maskedMatch.groupValues[2] }
            else -> {
                val digits = labeledCardRegex.find(normalizedBody)?.groupValues?.get(1).orEmpty()
                if (digits.length >= 4) digits.takeLast(4) else ""
            }
        }
        val persianCard4 = PersianNumberUtils.toPersianDigits(rawLast4)
        val cardLabel = if (persianCard4.isNotEmpty()) {
            "کارت $bankName $persianCard4"
        } else {
            "کارت $bankName"
        }

        val isMicroFee = !isDeposit &&
            (normalizedBody.contains("کارمزد") || normalizedBody.contains("آبونمان") || normalizedBody.contains("هزینه پیامک")) &&
            amountToman in 1L..5_000L

        val isSelfTransfer = normalizedBody.contains("کارت خود") ||
            normalizedBody.contains("حساب خود") ||
            normalizedBody.contains("انتقال خودی")

        val isSalaryDeposit = isDeposit && (
            normalizedBody.contains("حقوق") ||
                normalizedBody.contains("پاداش") ||
                (normalizedBody.contains("پایا") && amountToman >= 12_000_000L)
            )

        val isUntaggedPurchase = !isDeposit && !isMicroFee && !isSelfTransfer

        val detectedStore = when {
            isMicroFee -> "شبکه بانکی شاپرک"
            isSelfTransfer -> "بین کارت‌های خودی ($bankName)"
            isSalaryDeposit -> "واریز حقوق ماهانه ($bankName)"
            isDeposit -> "واریز بانکی ($bankName)"
            else -> bankName
        }

        val detectedPurpose = when {
            isMicroFee -> "کارمزد خرد بانکی (نادیده گرفته شده)"
            isSelfTransfer -> "جابه‌جایی داخلی بدون اثر بر تراز"
            isSalaryDeposit -> "شارژ ماه حقوقی جدید"
            isDeposit -> "افزایش موجودی حساب"
            else -> ""
        }

        val detectedTitle = when {
            isMicroFee -> "کارمزد خرد بانکی"
            isSelfTransfer -> "انتقال بین بانکی خودی ($bankName)"
            isSalaryDeposit -> "واریز حقوق ($bankName)"
            isDeposit -> "واریز به $cardLabel"
            else -> "برداشت از $cardLabel"
        }

        val summaryMessage = "پیامک بانک $bankName به مبلغ ${PersianNumberUtils.formatToman(amountToman, true)} ثبت شد."

        return ParsedSmsResult(
            bankName = bankName,
            cardLast4 = persianCard4,
            cardLabel = cardLabel,
            rawAmountRial = parsed.amount,
            amountToman = amountToman,
            extractedBalanceToman = balanceToman,
            isIncome = isDeposit,
            isMicroFee = isMicroFee,
            isSelfTransfer = isSelfTransfer,
            isSalaryDeposit = isSalaryDeposit,
            isUntaggedPurchase = isUntaggedPurchase,
            isValidFinancialSms = true,
            dedupKey = uniqueDedupKey,
            weekdayDate = realJalali.weekdayDate,
            jalaliYear = realJalali.year,
            timeStr = realJalali.timeStr,
            detectedTitle = detectedTitle,
            detectedStore = detectedStore,
            detectedPurpose = detectedPurpose,
            summaryMessage = summaryMessage
        )
    }

    fun detectIranianBankName(rawSms: String, senderAddress: String = ""): String {
        JibemanSmsEngine.resolveBank(senderAddress)?.let { return it.nameFa }
        val normalized = JibemanSmsEngine.normalizeMessage(rawSms)
        for (profile in JibemanSmsEngine.BANK_PROFILES) {
            if (normalized.contains(profile.nameFa) ||
                normalized.contains(profile.nameFa.replace("\u200C", " "))
            ) {
                return profile.nameFa
            }
        }
        return when {
            normalized.contains("بلو") -> "بلو"
            normalized.contains("صادرات") -> "صادرات ایران"
            normalized.contains("ملی") -> "ملی ایران"
            normalized.contains("رسالت") -> "قرض‌الحسنه رسالت"
            normalized.contains("مهر ایران") -> "قرض‌الحسنه مهر ایران"
            normalized.contains("رفاه") -> "رفاه کارگران"
            else -> "بانک"
        }
    }

    fun parseBankSms(
        rawSms: String,
        timestampMillis: Long = System.currentTimeMillis(),
        senderAddress: String = ""
    ): ParsedSmsResult {
        if (senderAddress.isNotBlank()) {
            parseWithJibemanEngine(senderAddress, rawSms, timestampMillis)?.let { return it }
        }
        val jibemanParsed = JibemanSmsEngine.parse(rawSms)
        val bankName = detectIranianBankName(rawSms, senderAddress)
        val realJalali = JalaliCalendarUtils.fromTimestamp(timestampMillis)
        val amountRial = jibemanParsed?.amount ?: 0L
        val amountToman = amountRial / 10L
        val balanceToman = jibemanParsed?.balance?.div(10L)
        val isDeposit = jibemanParsed?.direction == "in"
        val dedupKey = JibemanSmsEngine.dedupKey(senderAddress, timestampMillis, rawSms)

        val cardLabel = "کارت $bankName"
        return ParsedSmsResult(
            bankName = bankName,
            cardLast4 = "",
            cardLabel = cardLabel,
            rawAmountRial = amountRial,
            amountToman = amountToman,
            extractedBalanceToman = balanceToman,
            isIncome = isDeposit,
            isMicroFee = false,
            isSelfTransfer = false,
            isSalaryDeposit = isDeposit && rawSms.contains("حقوق"),
            isUntaggedPurchase = !isDeposit,
            isValidFinancialSms = jibemanParsed != null && amountToman > 0L,
            dedupKey = dedupKey,
            weekdayDate = realJalali.weekdayDate,
            jalaliYear = realJalali.year,
            timeStr = realJalali.timeStr,
            detectedTitle = if (isDeposit) "واریز به $cardLabel" else "برداشت از $cardLabel",
            detectedStore = if (isDeposit) "واریز بانکی ($bankName)" else bankName,
            detectedPurpose = if (isDeposit) "افزایش موجودی حساب" else "",
            summaryMessage = "پیامک بانک $bankName به مبلغ ${PersianNumberUtils.formatToman(amountToman, true)} ثبت شد."
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
        "صد تومن" to 100_000L,
        "پنجاه هزار" to 50_000L,
        "پنجاه تومن" to 50_000L
    )

    private val knownBrandsAndStores = listOf(
        "ایرانسل" to "ایرانسل",
        "همراه اول" to "همراه اول",
        "رایتل" to "رایتل",
        "شاتل" to "شاتل",
        "آسیاتک" to "آسیاتک",
        "اسنپ‌مارکت" to "اسنپ‌مارکت",
        "اسنپ مارکت" to "اسنپ‌مارکت",
        "اسنپمارکت" to "اسنپ‌مارکت",
        "اسنپ فود" to "اسنپ‌فود",
        "اسنپ" to "اسنپ",
        "تپسی" to "تپسی",
        "دیجی‌کالا" to "دیجی‌کالا",
        "دیجی کالا" to "دیجی‌کالا",
        "دیجیکالا" to "دیجی‌کالا",
        "افق کوروش" to "افق کوروش",
        "هایپراستار" to "هایپراستار",
        "شهروند" to "فروشگاه شهروند",
        "نانوایی" to "نانوایی",
        "داروخانه" to "داروخانه",
        "سوپرمارکت" to "سوپرمارکت",
        "کافه لمیز" to "کافه لمیز",
        "رستوران نایب" to "رستوران نایب"
    )

    private val conversationalFillers = listOf(
        "پرداخت کردم",
        "کارت کشیدم",
        "واریز کردم",
        "خرید کردم",
        "خریدم",
        "گرفتم",
        "دادم",
        "زدم",
        "شد",
        "بابتِ",
        "بابت"
    )

    /**
     * Exact Voice Command Parser (`parseVoiceCommand`):
     * - Strips conversational filler words (`خریدم`, `گرفتم`, `دادم`, `پرداخت کردم`, `زدم`, `شد`, `بابت`).
     * - If the user does NOT explicitly say `"از [فروشگاه] بابت [توضیح]"`, uses the exact spoken phrase
     *   itself (e.g. `"پفک"`, `"شارژ ایرانسل"`, `"هاست"`, `"نانوایی"`) directly as the transaction's
     *   title and purpose (`بابت`)!
     * - NEVER falls back to `"اسنپ‌مارکت"` or `"خرید روزمره"`! Only sets `storeName` if a brand/store is
     *   actually spoken, otherwise sets `storeName = defaultBankName`.
     */
    fun parseVoiceCommand(
        rawSpeech: String,
        fallbackTotalToman: Long? = null,
        defaultBankName: String = "بلو"
    ): ParsedVoiceExpense {
        val cleaned = PersianNumberUtils.normalizePersianText(rawSpeech.trim())
        val engText = PersianNumberUtils.toEnglishDigits(cleaned)

        // 1. Parse Total Amount in Toman (if not already provided by target transaction)
        var totalToman: Long = fallbackTotalToman ?: 0L
        if (totalToman == 0L && cleaned.isNotBlank()) {
            val matchedWord = wordMultipliers.firstOrNull { cleaned.contains(it.first) }
            if (matchedWord != null) {
                totalToman = matchedWord.second
            } else {
                val numRegex = Regex("""([0-9]+)\s*(میلیون|هزار|تومان|تومن)?""")
                val firstMatch = numRegex.find(engText)
                if (firstMatch != null) {
                    val value = firstMatch.groupValues[1].toLongOrNull() ?: 0L
                    val unit = firstMatch.groupValues[2]
                    totalToman = when {
                        unit == "میلیون" -> value * 1_000_000L
                        unit == "هزار" -> value * 1_000L
                        value in 1L..1999L -> value * 1_000L
                        else -> value
                    }
                }
            }
        }

        // 2. Parse Dong Split (سهم دنگ) if spoken
        var dongAmount = 0L
        var dongFriend = ""
        val speechBeforeDong = if (cleaned.contains("که") && (cleaned.contains("دنگ") || cleaned.contains("دُنگ") || cleaned.contains("سهم"))) {
            cleaned.substringBefore("که").trim()
        } else {
            cleaned
        }

        if (cleaned.contains("دنگ") || cleaned.contains("دُنگ") || cleaned.contains("سهم")) {
            val friendRegex = Regex("""(?:دنگ|دُنگ|سهم)\s+([^\s]+)""")
            val rawFriend = friendRegex.find(cleaned)?.groupValues?.get(1)
                ?.replace("بود", "")
                ?.replace("شد", "")
                ?.trim()
            dongFriend = if (!rawFriend.isNullOrBlank()) rawFriend else "دوست"

            if (cleaned.contains("نصفش") || cleaned.contains("نصف")) {
                dongAmount = totalToman / 2L
            } else {
                val dongNumRegex = Regex("""(?:که\s*)?([0-9]+)\s*(?:هزار\s*)?(?:تومنش|تومانش|تومن|هزار)""")
                val dongMatch = dongNumRegex.find(engText)
                if (dongMatch != null) {
                    val rawDongNum = dongMatch.groupValues[1].toLongOrNull() ?: 0L
                    dongAmount = if (rawDongNum in 1L..1999L) rawDongNum * 1_000L else rawDongNum
                } else {
                    val wordDong = wordMultipliers.firstOrNull {
                        cleaned.substringAfter("که", "").contains(it.first)
                    }
                    dongAmount = wordDong?.second ?: 0L
                }
            }
        }

        // 3. Detect Card / Bank if spoken, else use defaultBankName
        val spokenBank = BankSmsParserEngine.detectIranianBankName(speechBeforeDong)
        val effectiveBankName = if (spokenBank != "بانک") spokenBank else defaultBankName.ifBlank { "بانک" }
        val cardLabel = "کارت $effectiveBankName"

        // 4. Strip amount expressions, card expressions, and conversational fillers to get the exact spoken phrase
        var corePhrase = speechBeforeDong
        wordMultipliers.forEach { (wordAmt, _) ->
            corePhrase = corePhrase.replace(wordAmt, " ")
        }
        corePhrase = corePhrase
            .replace(Regex("""[۰-۹0-9]+\s*(?:میلیون|هزار)?\s*(?:تومان|تومن)"""), " ")
            .replace(Regex("""با\s+کارت\s+[^\s]+"""), " ")
            .replace(Regex("""کارت\s+(?:بلو|ملت|ملی|سپه|پاسارگاد|سامان|صادرات|تجارت|رسالت|ویپاد|مسکن|رفاه)"""), " ")

        // Check if user explicitly said "خرید [X] از [Y]" or "از [Y] بابت [X]"
        val explicitFromPattern = Regex("""(.+?)\s+از\s+(.+)""")
        val explicitMatch = explicitFromPattern.find(corePhrase.trim())

        val storeName: String
        val purposeCategory: String

        if (explicitMatch != null) {
            var leftPart = explicitMatch.groupValues[1].trim()
            var rightStore = explicitMatch.groupValues[2].trim()
            conversationalFillers.forEach { filler ->
                leftPart = leftPart.replace(Regex("""\b$filler\b"""), " ").trim()
                rightStore = rightStore.replace(Regex("""\b$filler\b"""), " ").trim()
            }
            val matchedKnownStore = knownBrandsAndStores.firstOrNull { rightStore.contains(it.first) }?.second
                ?: rightStore.ifBlank { effectiveBankName }
            storeName = matchedKnownStore
            purposeCategory = leftPart.ifBlank { matchedKnownStore }
        } else {
            // Strip conversational filler words ("خریدم", "گرفتم", "دادم", "پرداخت کردم", "زدم", "شد", "بابت")
            var stripped = corePhrase
            for (filler in conversationalFillers) {
                stripped = stripped.replace(filler, " ")
            }
            stripped = stripped.replace(Regex("""\s+"""), " ").trim()

            // Check if a known brand/store was spoken anywhere in the phrase (e.g. "شارژ ایرانسل")
            val detectedBrand = knownBrandsAndStores.firstOrNull { cleaned.contains(it.first) }?.second
            storeName = detectedBrand ?: effectiveBankName
            purposeCategory = stripped.ifBlank { cleaned }
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

    fun parseSpokenText(
        text: String,
        fallbackTotalToman: Long? = null,
        defaultBankName: String = "بلو"
    ): ParsedVoiceExpense = parseVoiceCommand(text, fallbackTotalToman, defaultBankName)
}
