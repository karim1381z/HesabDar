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

    /**
     * Strips commas (`,`, `٬`, `،`, `_`), spaces, and converts Persian/Arabic digits to English
     * before parsing as a Long so formatted inputs like "۱۰,۰۰۰" or "100,000" always save properly.
     */
    fun parseAmountInputToLong(input: String): Long? {
        val digitsOnly = toEnglishDigits(input)
            .filter { it in '0'..'9' }
        if (digitsOnly.isEmpty()) return null
        return digitsOnly.toLongOrNull()
    }

    /**
     * Live 3-digit comma formatter for amount text fields (e.g. "100000" -> "۱۰۰,۰۰۰").
     */
    fun formatLiveAmountInput(input: String): String {
        val digitsOnly = toEnglishDigits(input).filter { it in '0'..'9' }
        if (digitsOnly.isEmpty()) return ""
        val trimmed = digitsOnly.trimStart('0').ifEmpty { "0" }
        val capped = trimmed.take(15)
        val parsed = capped.toLongOrNull() ?: return toPersianDigits(capped)
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatted = DecimalFormat("#,###", symbols).format(parsed)
        return toPersianDigits(formatted)
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
    private val tehranTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Tehran")

    private val persianMonths = arrayOf(
        "", "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun monthNameOf(month: Int): String = persianMonths.getOrElse(month.coerceIn(1, 12)) { "آبان" }

    fun isJalaliLeapYear(jy: Int): Boolean {
        val mod = ((jy + 12) % 33 + 33) % 33
        return mod in setOf(1, 5, 9, 13, 17, 22, 26, 30)
    }

    fun daysInJalaliMonth(jy: Int, jm: Int): Int = when {
        jm in 1..6 -> 31
        jm in 7..11 -> 30
        jm == 12 -> if (isJalaliLeapYear(jy)) 30 else 29
        else -> 30
    }

    fun timestampToJalali(timestampMillis: Long): JalaliDateTime = fromTimestamp(timestampMillis)

    fun fromTimestamp(timestampMillis: Long): JalaliDateTime {
        val effectiveMillis = if (timestampMillis > 946684800000L) {
            timestampMillis
        } else {
            System.currentTimeMillis()
        }
        val cal = Calendar.getInstance(tehranTimeZone, Locale.US).apply {
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

    /**
     * Exact standard mathematical Gregorian-to-Jalali calendar conversion algorithm:
     * - Accounts for exact Gregorian leap years (4, 100, 400) and Jalali 33-year / 4-year cycles.
     * - First 6 months (Farvardin..Shahrivar) = 31 days (186 days total).
     * - Next 5 months (Mehr..Bahman) = 30 days.
     * - Esfand = 29 or 30 days in leap years.
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 +
            (365 * gy) +
            ((gy2 + 3) / 4) -
            ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) +
            gd +
            gDaysInMonth[(gm - 1).coerceIn(0, 11)]

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
        val monthLength = daysInJalaliMonth(jalali.year, jalali.month)
        val currentDay = jalali.day
        val targetDay = paydayStartDay.coerceIn(1, monthLength)
        return when {
            currentDay < targetDay -> (targetDay - currentDay).coerceIn(1, 31)
            currentDay > targetDay -> (monthLength - currentDay + targetDay).coerceIn(1, 31)
            else -> monthLength
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

typealias ParsedVoiceCommand = ParsedVoiceExpense

data class ParsedVoiceExpense(
    val recognizedSpeech: String,
    val totalAmountToman: Long,
    val userShareToman: Long,
    val dongAmountToman: Long,
    val dongFriendName: String,
    val storeName: String,
    val purposeCategory: String,
    val cardLabel: String,
    val category: String = "خوراکی و روزمره",
    val merchant: String = ""
) {
    val title: String
        get() = purposeCategory

    val amountToman: Long
        get() = totalAmountToman
}

object PersianVoiceSlangParserEngine {
    private val exactPhraseAmounts = listOf(
        "یک میلیون و پانصد" to 1_500_000L,
        "یک میلیون و پونصد" to 1_500_000L,
        "یک میلیون و دویست" to 1_200_000L,
        "یک و نیم میلیون" to 1_500_000L,
        "دو و نیم میلیون" to 2_500_000L,
        "سه و نیم میلیون" to 3_500_000L,
        "نیم میلیون" to 500_000L
    )

    private val numberWordTokens = mapOf(
        "یک" to 1L,
        "یه" to 1L,
        "دو" to 2L,
        "سه" to 3L,
        "چهار" to 4L,
        "پنج" to 5L,
        "شش" to 6L,
        "شیش" to 6L,
        "هفت" to 7L,
        "هشت" to 8L,
        "نه" to 9L,
        "ده" to 10L,
        "یازده" to 11L,
        "دوازده" to 12L,
        "سیزده" to 13L,
        "چهارده" to 14L,
        "پانزده" to 15L,
        "پونزده" to 15L,
        "شانزده" to 16L,
        "شونزده" to 16L,
        "هفده" to 17L,
        "هجده" to 18L,
        "هیجده" to 18L,
        "نوزده" to 19L,
        "بیست" to 20L,
        "سی" to 30L,
        "چهل" to 40L,
        "پنجاه" to 50L,
        "شصت" to 60L,
        "هفتاد" to 70L,
        "هشتاد" to 80L,
        "نود" to 90L,
        "صد" to 100L,
        "یکصد" to 100L,
        "دویست" to 200L,
        "سیصد" to 300L,
        "چهارصد" to 400L,
        "چارصد" to 400L,
        "پانصد" to 500L,
        "پونصد" to 500L,
        "ششصد" to 600L,
        "شیشصد" to 600L,
        "هفتصد" to 700L,
        "هشتصد" to 800L,
        "نهصد" to 900L
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
        "نونوایی" to "نونوایی",
        "داروخانه" to "داروخانه",
        "سوپرمارکت" to "سوپرمارکت",
        "کافه لمیز" to "کافه لمیز",
        "رستوران نایب" to "رستوران نایب"
    )

    private val multiWordConversationalFillers = listOf(
        "سلام علیکم",
        "خسته نباشید",
        "فکر کنم",
        "فکر میکنم",
        "فکر می‌کنم",
        "یادم نیست",
        "یادم نمیاد",
        "یادم نمی‌آد",
        "چی بود",
        "پرداخت کردم",
        "کارت کشیدم",
        "واریز کردم",
        "خرید کردم",
        "خرج کردم"
    )

    private val singleWordConversationalFillers = listOf(
        "سلام",
        "علیکم",
        "درود",
        "والا",
        "ولی",
        "برای",
        "بابتِ",
        "بابت",
        "خریدم",
        "گرفتم",
        "دادم",
        "کردم",
        "زدم",
        "شد",
        "هزار",
        "تومان",
        "تومن",
        "میلیون",
        "میلیارد"
    )

    private val countingUnits = listOf(
        "کیلوگرم", "کیلو", "گرم", "دونه", "دانه", "عدد", "بسته",
        "بطری", "جلد", "برگ", "سیخ", "پرس", "قوطی", "پاکت", "شانه", "جفت", "تا"
    )

    private val smallQuantityWords = mapOf(
        "یه" to 1,
        "یک" to 1,
        "دو" to 2,
        "سه" to 3,
        "چهار" to 4,
        "پنج" to 5,
        "شش" to 6,
        "شیش" to 6,
        "هفت" to 7,
        "هشت" to 8,
        "نه" to 9,
        "ده" to 10,
        "یازده" to 11,
        "دوازده" to 12
    )

    private val currencyAndMultiplierWords = setOf(
        "تومان", "تومن", "تومنش", "تومانش", "هزار", "میلیون", "ملیون", "میلیارد", "ریال"
    )

    private val compoundMoneyPrefixWords = setOf(
        "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود",
        "صد", "یکصد", "دویست", "سیصد", "چهارصد", "چارصد", "پانصد", "پونصد",
        "ششصد", "شیشصد", "هفتصد", "هشتصد", "نهصد", "هزار", "میلیون", "ملیون", "میلیارد"
    )

    data class ProtectedQuantitiesText(
        val protectedText: String,
        val replacements: Map<String, String>
    ) {
        fun restore(input: String): String {
            var out = input
            for ((token, replacement) in replacements) {
                out = out.replace(token, replacement)
            }
            return out.replace(Regex("""\s+"""), " ").trim()
        }
    }

    /**
     * Normalizes compound Persian nouns (e.g. "سیب زمینی" -> "سیب‌زمینی") and protects item quantities
     * (like "یک شارژ", "دو تا سیب‌زمینی", "دو تا بستنی", "۳ دونه نان") with placeholders so they are:
     * 1) NEVER mistaken for 1,000 or 2,000 Toman money amounts, and
     * 2) Preserved in the final item title with clean Persian digits ("۱ شارژ ایرانسل", "۲ تا سیب‌زمینی").
     */
    fun protectItemQuantities(rawText: String): ProtectedQuantitiesText {
        var text = PersianNumberUtils.normalizePersianText(rawText)
            .replace(Regex("""سیب\s+زمینی"""), "سیب‌زمینی")
            .replace(Regex("""تخم\s+مرغ"""), "تخم‌مرغ")
            .replace(Regex("""گوجه\s+فرنگی"""), "گوجه‌فرنگی")

        val replacements = linkedMapOf<String, String>()
        var counter = 0

        // Pattern A: <number word or 1..2 digit number> + <counting unit (تا/دونه/عدد/بسته/کیلو/...)> + <non-currency noun>
        val unitsPattern = countingUnits.joinToString("|")
        val qtyWordsPattern = smallQuantityWords.keys.sortedByDescending { it.length }.joinToString("|")
        val withUnitRegex = Regex(
            """(?<=^|\s)($qtyWordsPattern|[۰-۹0-9]{1,2})\s+($unitsPattern)(?=\s+([^\s]+))"""
        )

        text = withUnitRegex.replace(text) { match ->
            val rawNumToken = match.groupValues[1]
            val unitToken = match.groupValues[2]
            val followingWord = match.groupValues[3]
                .replace("،", "")
                .replace(".", "")
                .trim()

            if (followingWord in currencyAndMultiplierWords || followingWord in numberWordTokens) {
                match.value
            } else {
                val numericVal = smallQuantityWords[rawNumToken]
                    ?: PersianNumberUtils.toEnglishDigits(rawNumToken).toIntOrNull()
                if (numericVal != null && numericVal in 1..99) {
                    val key = "⟪QTY${('A' + counter++)}⟫"
                    val persianNum = PersianNumberUtils.toPersianDigits(numericVal.toString())
                    replacements[key] = "$persianNum $unitToken"
                    key
                } else {
                    match.value
                }
            }
        }

        // Pattern B: Small number word (یه/یک/دو/سه/.../نه) directly preceding an item noun WITHOUT a counting unit or currency word
        val directNounWordsPattern = listOf("یه", "یک", "دو", "سه", "چهار", "پنج", "شش", "شیش", "هفت", "هشت", "نه")
            .joinToString("|")
        val directNounRegex = Regex("""(?<=^|\s)($directNounWordsPattern)(?=\s+([^\s]+))""")

        text = directNounRegex.replace(text) { match ->
            val wordToken = match.groupValues[1]
            val followingWord = match.groupValues[2]
                .replace("،", "")
                .replace(".", "")
                .trim()

            // Check if preceded by "<tens/hundreds> و " (e.g., "سی و نه تومن" or "بیست و پنج هزار")
            val prefixBeforeMatch = text.substring(0, match.range.first).trimEnd()
            val isPartOfCompoundMoney = if (prefixBeforeMatch.endsWith("و")) {
                val beforeVav = prefixBeforeMatch.removeSuffix("و").trimEnd()
                val prevWord = beforeVav.split(Regex("""\s+""")).lastOrNull().orEmpty()
                prevWord in compoundMoneyPrefixWords || prevWord in numberWordTokens
            } else {
                false
            }

            val isInvalidFollowing = followingWord in currencyAndMultiplierWords ||
                followingWord in numberWordTokens ||
                followingWord in singleWordConversationalFillers ||
                followingWord in countingUnits ||
                followingWord == "و" ||
                followingWord == "نیم" ||
                followingWord == "که" ||
                followingWord == "دنگ" ||
                followingWord == "سهم" ||
                followingWord.startsWith("⟪QTY")

            if (isPartOfCompoundMoney || isInvalidFollowing) {
                match.value
            } else {
                val numericVal = smallQuantityWords[wordToken] ?: 1
                val key = "⟪QTY${('A' + counter++)}⟫"
                val persianNum = PersianNumberUtils.toPersianDigits(numericVal.toString())
                replacements[key] = persianNum
                key
            }
        }

        return ProtectedQuantitiesText(text, replacements)
    }

    fun inferExpenseCategory(title: String): String {
        val t = PersianNumberUtils.normalizePersianText(title)
        return when {
            listOf("شارژ", "ایرانسل", "همراه اول", "رایتل", "اینترنت", "قبض", "برق", "آب", "گاز", "تلفن", "شاتل", "آسیاتک", "هاست").any { t.contains(it) } ->
                "قبض و شارژ"
            listOf("نانوایی", "نونوایی", "نان", "نون", "میوه", "سیب‌زمینی", "سیب زمینی", "بستنی", "گوشت", "مرغ", "پفک", "تنقلات", "سوپرمارکت", "ماست", "شیر", "خوراکی", "غذا", "رستوران", "کافه", "شام", "ناهار", "صبحانه").any { t.contains(it) } ->
                "خوراکی و روزمره"
            listOf("دمپایی", "کفش", "لباس", "مانتو", "شلوار", "پوشاک", "پیراهن").any { t.contains(it) } ->
                "پوشاک و خرید"
            listOf("اسنپ", "تپسی", "تاکسی", "بنزین", "مترو", "اتوبوس").any { t.contains(it) } ->
                "حمل و نقل"
            listOf("داروخانه", "دارو", "دکتر", "آزمایش", "پزشک").any { t.contains(it) } ->
                "درمان و سلامت"
            else -> "خوراکی و روزمره"
        }
    }

    fun stripGreetingsAndFillers(rawText: String): String {
        var result = PersianNumberUtils.normalizePersianText(rawText)
        for (phrase in multiWordConversationalFillers) {
            result = result.replace(Regex("""(?<=^|\s)$phrase(?=\s|$)"""), " ")
        }
        for (word in singleWordConversationalFillers) {
            result = result.replace(Regex("""(?<=^|\s)$word(?=\s|$)"""), " ")
        }
        result = result.replace(Regex("""\s+"""), " ").trim()
        // Strip leading/trailing孤立 "و"
        result = result.removePrefix("و ").removeSuffix(" و").trim()
        // Strip leading "خرید " when followed by an item noun (e.g. "خرید نانوایی" -> "نانوایی", "خرید دمپایی" -> "دمپایی")
        if (result.startsWith("خرید ") && result.removePrefix("خرید ").trim().isNotBlank()) {
            result = result.removePrefix("خرید ").trim()
        }
        return result
    }

    /**
     * Parses spoken Persian numbers and digits into Tomans while ignoring item quantities:
     * - Item quantities like "یک شارژ ایرانسل" or "دو تا سیب‌زمینی" or "دو تا بستنی" are protected first
     *   so they are NEVER mistaken for 1,000 or 2,000 Tomans.
     * - Supports Persian number words and digits with هزار/میلیون/تومان/تومن, as well as bare price numbers.
     */
    fun parseSpokenPersianAmountToman(rawText: String): Long {
        val cleaned = PersianNumberUtils.normalizePersianText(rawText.trim())
        if (cleaned.isBlank()) return 0L

        for ((phrase, amt) in exactPhraseAmounts) {
            if (cleaned.contains(phrase)) return amt
        }

        val protectedText = protectItemQuantities(cleaned).protectedText
        val textWithoutQtyPlaceholders = protectedText.replace(Regex("""⟪QTY[A-Z]+⟫"""), " ")
        val engWithoutPlaceholders = PersianNumberUtils.toEnglishDigits(textWithoutQtyPlaceholders)

        // 1. Check numeric digits first: e.g. "50 هزار تومان", "70 تومن", "300 هزار تومان"
        val digitRegex = Regex("""([0-9]+(?:\.[0-9]+)?)\s*(میلیارد|میلیون|ملیون|هزار\s*تومان|هزار\s*تومن|هزار|تومان|تومن|ریال)?""")
        val digitMatch = digitRegex.find(engWithoutPlaceholders)
        if (digitMatch != null) {
            val numVal = digitMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val unit = digitMatch.groupValues[2].trim()
            if (numVal > 0.0) {
                return when {
                    unit == "میلیارد" -> (numVal * 1_000_000_000.0).toLong()
                    unit == "میلیون" || unit == "ملیون" -> (numVal * 1_000_000.0).toLong()
                    unit == "ریال" -> (numVal / 10.0).toLong()
                    unit.startsWith("هزار") -> (numVal * 1_000.0).toLong()
                    numVal in 1.0..999.0 -> (numVal * 1_000.0).toLong()
                    else -> numVal.toLong()
                }
            }
        }

        // 2. Parse spoken Persian number words: e.g. "پانصد هزار تومان", "پنجاه تومن", "هفتاد تومن", "سی و نه تومن"
        val tokens = textWithoutQtyPlaceholders
            .replace("،", " ")
            .replace(".", " ")
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }

        var totalAccumulated = 0L
        var currentGroup = 0L
        var matchedAnyNumberWord = false
        var hasExplicitMultiplier = false

        for (rawToken in tokens) {
            val token = rawToken.trim()
            if (token == "و") continue

            val cleanWord = token
                .removeSuffix("تومنش")
                .removeSuffix("تومانش")
                .removeSuffix("تومان")
                .removeSuffix("تومن")
                .trim()

            val wordVal = numberWordTokens[cleanWord] ?: numberWordTokens[token]
            if (wordVal != null) {
                currentGroup += wordVal
                matchedAnyNumberWord = true
                continue
            }

            when (token) {
                "میلیارد" -> {
                    val base = if (currentGroup > 0L) currentGroup else 1L
                    totalAccumulated += base * 1_000_000_000L
                    currentGroup = 0L
                    matchedAnyNumberWord = true
                    hasExplicitMultiplier = true
                }
                "میلیون", "ملیون" -> {
                    val base = if (currentGroup > 0L) currentGroup else 1L
                    totalAccumulated += base * 1_000_000L
                    currentGroup = 0L
                    matchedAnyNumberWord = true
                    hasExplicitMultiplier = true
                }
                "هزار" -> {
                    val base = if (currentGroup > 0L) currentGroup else 1L
                    totalAccumulated += base * 1_000L
                    currentGroup = 0L
                    matchedAnyNumberWord = true
                    hasExplicitMultiplier = true
                }
                "تومان", "تومن", "تومنش", "تومانش" -> {
                    if (matchedAnyNumberWord) {
                        break
                    }
                }
            }
        }

        if (!matchedAnyNumberWord) return 0L

        if (currentGroup > 0L) {
            totalAccumulated += if (!hasExplicitMultiplier && currentGroup in 1L..999L) {
                currentGroup * 1_000L
            } else if (hasExplicitMultiplier && totalAccumulated >= 1_000_000L && currentGroup in 1L..999L) {
                currentGroup * 1_000L
            } else {
                currentGroup
            }
        }

        return totalAccumulated
    }

    /**
     * Formats multiple parsed voice commands into a single combined note for an existing bank SMS row:
     * e.g. "۲ تا شارژ (۵۰,۰۰۰) + ۲ تا بستنی (۵۰,۰۰۰)" or "نانوایی (۵۰۰,۰۰۰) + شارژ ایرانسل (۳۰۰,۰۰۰)"
     */
    fun formatCombinedMultiVoiceNote(items: List<ParsedVoiceExpense>): String {
        return items.joinToString(" + ") { item ->
            "${item.purposeCategory} (${PersianNumberUtils.formatToman(item.totalAmountToman, showUnit = false)})"
        }
    }

    /**
     * Splits a spoken phrase into candidate clauses when separated by ` و ` or a mid-sentence action verb
     * (like "سلام دو تا شارژ خریدم دو تا بستنی ۵۰ هزار تومان").
     */
    private fun extractCandidateSubClauses(cleaned: String): List<String> {
        val byVav = cleaned.split(Regex("""\s+و\s+""")).map { it.trim() }.filter { it.isNotBlank() }
        if (byVav.size >= 2) {
            // Check if any part itself contains a mid-sentence verb splitting two items
            val expanded = mutableListOf<String>()
            for (part in byVav) {
                val verbSplit = splitOnMidSentenceVerb(part)
                expanded.addAll(verbSplit)
            }
            return expanded
        }
        return splitOnMidSentenceVerb(cleaned)
    }

    private fun splitOnMidSentenceVerb(clause: String): List<String> {
        val midVerbRegex = Regex("""^(.+?)\s+(?:خریدم|گرفتم|دادم|زدم)\s+(.+)$""")
        val match = midVerbRegex.find(clause.trim()) ?: return listOf(clause.trim())
        val left = match.groupValues[1].trim()
        val right = match.groupValues[2].trim()
        // Only split on the verb if the right side isn't JUST a price for the left side!
        // e.g. in "یک شارژ ایرانسل و دو تا سیب زمینی خریدم ۵۰ هزار تومان", right is "۵۰ هزار تومان" (no item title!)
        // whereas in "سلام دو تا شارژ خریدم دو تا بستنی ۵۰ هزار تومان", right is "دو تا بستنی ۵۰ هزار تومان" (has item title "۲ تا بستنی"!)
        val rightParsed = parseSingleVoiceClause(right, null, "نقدی / دستی")
        val leftParsed = parseSingleVoiceClause(left, null, "نقدی / دستی")
        return if (leftParsed.purposeCategory.isNotBlank() && rightParsed.purposeCategory.isNotBlank()) {
            listOf(left, right)
        } else {
            listOf(clause.trim())
        }
    }

    /**
     * Smart Multi-Clause Splitter (`parseMultiVoiceCommands`):
     * 1) When a user speaks a compound sentence where multiple clauses have their own stated amounts
     *    (e.g., "۵۰۰ هزار نانوایی و ۳۰۰ هزار شارژ ایرانسل خریدم"), splits into separate `ParsedVoiceExpense` items.
     * 2) When tagging an existing bank transaction (`fallbackTotalToman != null`, e.g. 100,000 Tomans) and the user
     *    mentions 2 items where only one item has a stated price (e.g. "سلام دو تا شارژ خریدم دو تا بستنی ۵۰ هزار تومان"),
     *    automatically computes the unpriced item's price as `fallbackTotalToman - statedPrice` (50,000 Tomans)!
     * 3) When in "+ ثبت صوتی" (`fallbackTotalToman == null`) and only one total price is stated at the end
     *    (e.g. "یک شارژ ایرانسل و دو تا سیب زمینی خریدم ۵۰ هزار تومان"), keeps it as one clean transaction
     *    (`title = "۱ شارژ ایرانسل و ۲ تا سیب‌زمینی"`, `amount = 50,000` Tomans) without inventing fake 1,000/2,000 Toman items.
     */
    fun parseMultiVoiceCommands(
        rawSpeech: String,
        defaultBankName: String = "نقدی / دستی",
        fallbackTotalToman: Long? = null
    ): List<ParsedVoiceExpense> {
        val cleaned = PersianNumberUtils.normalizePersianText(rawSpeech.trim())
        if (cleaned.isBlank()) return emptyList()

        // If sentence has a Dong split ("که ... دنگ"), treat as single command
        if (cleaned.contains("که") && (cleaned.contains("دنگ") || cleaned.contains("دُنگ") || cleaned.contains("سهم"))) {
            return listOf(parseSingleVoiceClause(cleaned, fallbackTotalToman, defaultBankName))
        }

        val candidateParts = extractCandidateSubClauses(cleaned)
        if (candidateParts.size <= 1) {
            return listOf(parseSingleVoiceClause(cleaned, fallbackTotalToman, defaultBankName))
        }

        // Case A: Tagging an existing bank transaction with known total (e.g. 100,000 Tomans)
        // Check if we have 2 item clauses where 1 has a stated price < fallbackTotalToman and 1 has no stated price (0L)
        if (fallbackTotalToman != null && fallbackTotalToman > 0L && candidateParts.size == 2) {
            val firstParsed = parseSingleVoiceClause(candidateParts[0], null, defaultBankName)
            val secondParsed = parseSingleVoiceClause(candidateParts[1], null, defaultBankName)

            if (firstParsed.purposeCategory.isNotBlank() && secondParsed.purposeCategory.isNotBlank()) {
                if (firstParsed.totalAmountToman == 0L &&
                    secondParsed.totalAmountToman in 1L until fallbackTotalToman
                ) {
                    val remainder = (fallbackTotalToman - secondParsed.totalAmountToman).coerceAtLeast(0L)
                    return listOf(
                        firstParsed.copy(totalAmountToman = remainder, userShareToman = remainder),
                        secondParsed
                    )
                } else if (secondParsed.totalAmountToman == 0L &&
                    firstParsed.totalAmountToman in 1L until fallbackTotalToman
                ) {
                    val remainder = (fallbackTotalToman - firstParsed.totalAmountToman).coerceAtLeast(0L)
                    return listOf(
                        firstParsed,
                        secondParsed.copy(totalAmountToman = remainder, userShareToman = remainder)
                    )
                } else if (firstParsed.totalAmountToman > 0L && secondParsed.totalAmountToman > 0L) {
                    return listOf(firstParsed, secondParsed)
                }
            }
        }

        // Case B: Multi-clause sentence where both sides have explicit stated amounts > 0L
        val mergedClauses = mutableListOf<String>()
        var currentClause = candidateParts.first()

        for (nextPart in candidateParts.drop(1)) {
            val currentParsed = parseSingleVoiceClause(currentClause, null, defaultBankName)
            val nextAmount = parseSpokenPersianAmountToman(nextPart)

            if (currentParsed.totalAmountToman > 0L &&
                currentParsed.purposeCategory.isNotBlank() &&
                nextAmount > 0L
            ) {
                mergedClauses.add(currentClause)
                currentClause = nextPart
            } else {
                currentClause = "$currentClause و $nextPart"
            }
        }
        mergedClauses.add(currentClause)

        if (mergedClauses.size >= 2) {
            val parsedItems = mergedClauses.map { clause ->
                parseSingleVoiceClause(clause, null, defaultBankName)
            }.filter { it.totalAmountToman > 0L && it.purposeCategory.isNotBlank() }

            if (parsedItems.size >= 2) {
                return parsedItems
            }
        }

        return listOf(parseSingleVoiceClause(cleaned, fallbackTotalToman, defaultBankName))
    }

    /**
     * Exact Voice Command Parser (`parseVoiceCommand`):
     * - Distinguishes between tagging an existing bank SMS (`fallbackTotalToman != null`) vs
     *   creating a new manual cash/voice transaction (`fallbackTotalToman == null`).
     * - When tagging an existing bank SMS with a multi-clause sentence (including Smart Remainder Math
     *   like "سلام دو تا شارژ خریدم دو تا بستنی ۵۰ هزار تومان" on a 100,000 Toman SMS),
     *   formats the combined note as "۲ تا شارژ (۵۰,۰۰۰) + ۲ تا بستنی (۵۰,۰۰۰)".
     */
    fun parseVoiceCommand(
        rawSpeech: String,
        fallbackTotalToman: Long? = null,
        defaultBankName: String = "نقدی / دستی"
    ): ParsedVoiceExpense {
        val cleaned = PersianNumberUtils.normalizePersianText(rawSpeech.trim())
        val isTaggingExistingSms = fallbackTotalToman != null && fallbackTotalToman > 0L

        val multiItems = parseMultiVoiceCommands(cleaned, defaultBankName, fallbackTotalToman)
        if (multiItems.size > 1) {
            if (isTaggingExistingSms) {
                val combinedNote = formatCombinedMultiVoiceNote(multiItems)
                val effectiveBank = defaultBankName.ifBlank { "بلو" }
                return ParsedVoiceExpense(
                    recognizedSpeech = cleaned,
                    totalAmountToman = fallbackTotalToman!!,
                    userShareToman = fallbackTotalToman,
                    dongAmountToman = 0L,
                    dongFriendName = "",
                    storeName = effectiveBank,
                    purposeCategory = combinedNote,
                    cardLabel = "کارت $effectiveBank",
                    category = multiItems.first().category,
                    merchant = multiItems.first().merchant
                )
            } else {
                val sumAmount = multiItems.sumOf { it.totalAmountToman }
                val combinedNote = formatCombinedMultiVoiceNote(multiItems)
                return ParsedVoiceExpense(
                    recognizedSpeech = cleaned,
                    totalAmountToman = sumAmount,
                    userShareToman = sumAmount,
                    dongAmountToman = 0L,
                    dongFriendName = "",
                    storeName = defaultBankName.ifBlank { "نقدی / دستی" },
                    purposeCategory = combinedNote,
                    cardLabel = "نقدی / دستی",
                    category = multiItems.first().category,
                    merchant = multiItems.first().merchant
                )
            }
        }

        return parseSingleVoiceClause(cleaned, fallbackTotalToman, defaultBankName)
    }

    private fun parseSingleVoiceClause(
        rawSpeech: String,
        fallbackTotalToman: Long? = null,
        defaultBankName: String = "نقدی / دستی"
    ): ParsedVoiceExpense {
        val cleaned = PersianNumberUtils.normalizePersianText(rawSpeech.trim())
        val isTaggingExistingSms = fallbackTotalToman != null && fallbackTotalToman > 0L

        val speechBeforeDong = if (cleaned.contains("که") && (cleaned.contains("دنگ") || cleaned.contains("دُنگ") || cleaned.contains("سهم"))) {
            cleaned.substringBefore("که").trim()
        } else {
            cleaned
        }

        // 1. Parse Total Amount in Toman
        // If tagging an existing bank SMS, NEVER change its bank amount!
        val totalToman: Long = if (isTaggingExistingSms) {
            fallbackTotalToman!!
        } else {
            parseSpokenPersianAmountToman(speechBeforeDong)
        }

        // 2. Parse Dong Split (سهم دنگ) if spoken
        var dongAmount = 0L
        var dongFriend = ""
        if (cleaned.contains("دنگ") || cleaned.contains("دُنگ") || cleaned.contains("سهم")) {
            val friendRegex = Regex("""(?:دنگ|دُنگ|سهم)\s+([^\s]+)""")
            val rawFriend = friendRegex.find(cleaned)?.groupValues?.get(1)
                ?.replace("بود", "")
                ?.replace("شد", "")
                ?.trim()
            dongFriend = if (!rawFriend.isNullOrBlank()) rawFriend else "دوست"

            if (cleaned.contains("نصفش") || cleaned.contains("نصف")) {
                dongAmount = totalToman / 2L
            } else if (cleaned.contains("که")) {
                val afterKeh = cleaned.substringAfter("که")
                val parsedDong = parseSpokenPersianAmountToman(afterKeh)
                if (parsedDong > 0L) {
                    dongAmount = parsedDong
                } else {
                    val dongNumRegex = Regex("""([0-9]+)\s*(?:هزار\s*)?(?:تومنش|تومانش|تومن|هزار)""")
                    val dongMatch = dongNumRegex.find(PersianNumberUtils.toEnglishDigits(afterKeh))
                    if (dongMatch != null) {
                        val rawDongNum = dongMatch.groupValues[1].toLongOrNull() ?: 0L
                        dongAmount = if (rawDongNum in 1L..999L) rawDongNum * 1_000L else rawDongNum
                    }
                }
            }
        }

        // 3. Resolve Bank / Source Label
        val effectiveBankName = if (isTaggingExistingSms) {
            defaultBankName.ifBlank { "بلو" }
        } else {
            val spokenBank = BankSmsParserEngine.detectIranianBankName(speechBeforeDong)
            if (spokenBank != "بانک") spokenBank else defaultBankName.takeIf { it.isNotBlank() && it != "بانک" } ?: "نقدی / دستی"
        }
        val cardLabel = if (effectiveBankName == "نقدی / دستی") "نقدی / دستی" else "کارت $effectiveBankName"

        // 4. Protect item quantities (like "یک شارژ" -> "__QTY_0__ شارژ", "دو تا سیب‌زمینی" -> "__QTY_1__ سیب‌زمینی")
        // BEFORE stripping money expressions so quantities are preserved in the title!
        val protectedQuantities = protectItemQuantities(speechBeforeDong)
        var corePhrase = protectedQuantities.protectedText

        exactPhraseAmounts.forEach { (phrase, _) ->
            corePhrase = corePhrase.replace(phrase, " ")
        }
        // Strip numeric digits (outside __QTY_n__ placeholders) + optional commas + optional هزار/میلیون + optional تومان/تومن
        corePhrase = corePhrase.replace(
            Regex("""(?<!__QTY_)[۰-۹0-9][۰-۹0-9,٬،]*(?!__)\s*(?:میلیارد|میلیون|ملیون|هزار)?\s*(?:تومان|تومن|ریال)?"""),
            " "
        )
        // Strip spoken number words followed by هزار/میلیون/تومن/تومان
        val numberWordsPattern = numberWordTokens.keys.sortedByDescending { it.length }.joinToString("|")
        corePhrase = corePhrase.replace(
            Regex("""(?<=^|\s)(?:(?:$numberWordsPattern)(?:\s+و\s+(?:$numberWordsPattern))*)\s*(?:میلیارد|میلیون|ملیون|هزار)?\s*(?:تومان|تومن|ریال)(?=\s|$)"""),
            " "
        )
        corePhrase = corePhrase.replace(
            Regex("""(?<=^|\s)(?:(?:$numberWordsPattern)(?:\s+و\s+(?:$numberWordsPattern))*)\s+(?:میلیارد|میلیون|ملیون|هزار)(?=\s|$)"""),
            " "
        )

        corePhrase = corePhrase
            .replace(Regex("""با\s+کارت\s+[^\s]+"""), " ")
            .replace(Regex("""کارت\s+(?:بلو|ملت|ملی|سپه|پاسارگاد|سامان|صادرات|تجارت|رسالت|ویپاد|مسکن|رفاه)"""), " ")

        val explicitFromPattern = Regex("""(.+?)\s+از\s+(.+)""")
        val explicitMatch = explicitFromPattern.find(corePhrase.trim())

        val storeName: String
        val purposeCategory: String
        val detectedBrand = knownBrandsAndStores.firstOrNull { cleaned.contains(it.first) }?.second.orEmpty()

        if (explicitMatch != null) {
            val leftPart = protectedQuantities.restore(stripGreetingsAndFillers(explicitMatch.groupValues[1]))
            val rightStore = protectedQuantities.restore(stripGreetingsAndFillers(explicitMatch.groupValues[2]))
            val matchedKnownStore = knownBrandsAndStores.firstOrNull { rightStore.contains(it.first) }?.second
                ?: rightStore.ifBlank { effectiveBankName }
            storeName = if (isTaggingExistingSms) effectiveBankName else matchedKnownStore
            purposeCategory = if (isTaggingExistingSms && matchedKnownStore.isNotBlank() && leftPart != matchedKnownStore) {
                "${leftPart.ifBlank { matchedKnownStore }} از $matchedKnownStore"
            } else {
                leftPart.ifBlank { matchedKnownStore }
            }
        } else {
            val stripped = protectedQuantities.restore(stripGreetingsAndFillers(corePhrase))
            storeName = if (isTaggingExistingSms) {
                effectiveBankName
            } else {
                detectedBrand.ifBlank { effectiveBankName }
            }
            purposeCategory = if (stripped.isNotBlank()) {
                stripped
            } else if (isTaggingExistingSms) {
                protectedQuantities.restore(stripGreetingsAndFillers(protectedQuantities.protectedText))
            } else {
                ""
            }
        }

        val safeDongAmount = dongAmount.coerceIn(0L, totalToman)
        val userShare = (totalToman - safeDongAmount).coerceAtLeast(0L)
        val category = inferExpenseCategory(purposeCategory)

        return ParsedVoiceExpense(
            recognizedSpeech = cleaned,
            totalAmountToman = totalToman,
            userShareToman = userShare,
            dongAmountToman = safeDongAmount,
            dongFriendName = dongFriend,
            storeName = storeName,
            purposeCategory = purposeCategory,
            cardLabel = cardLabel,
            category = category,
            merchant = detectedBrand
        )
    }

    fun parseSpokenText(
        text: String,
        fallbackTotalToman: Long? = null,
        defaultBankName: String = "نقدی / دستی"
    ): ParsedVoiceExpense = parseVoiceCommand(text, fallbackTotalToman, defaultBankName)
}


