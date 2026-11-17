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

    /**
     * Wraps a phone number or SMS sender address in Unicode Left-to-Right Isolate + LTR Mark
     * (`\u2066\u200E...\u2069`) so leading `+` signs (e.g. `+982187641`, `+989999987641`)
     * ALWAYS render on the LEFT side even inside RTL Persian text or comma-separated lists.
     */
    fun formatLtrSender(raw: String): String {
        val cleaned = raw.trim()
            .removePrefix("\u2066")
            .removePrefix("\u200E")
            .removePrefix("\u202A")
            .removeSuffix("\u2069")
            .removeSuffix("\u202C")
            .removeSuffix("\u200F")
            .trim()
        if (cleaned.isEmpty()) return ""
        return "\u2066\u200E$cleaned\u2069"
    }

    fun formatLtrSenderList(senders: List<String>, separator: String = " ، "): String =
        senders.mapNotNull { s -> formatLtrSender(s).takeIf { it.isNotEmpty() } }
            .joinToString(separator)
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
    private val persianDayStr: String = PersianNumberUtils.toPersianDigits(day.toString())
    private val persianYearStr: String = PersianNumberUtils.toPersianDigits(year.toString())

    val weekdayDate: String = "$weekdayName $persianDayStr $monthName"

    val dateWithYear: String = "$persianDayStr $monthName $persianYearStr"

    val fullWeekdayDateWithYear: String = "$weekdayName $persianDayStr $monthName $persianYearStr"

    val monthYearLabel: String = "$monthName $persianYearStr"

    val yearMonthKey: Int = year * 100 + month

    val timeStr: String = PersianNumberUtils.toPersianDigits(String.format(Locale.US, "%02d:%02d", hour, minute))
}

object JalaliCalendarUtils {
    private val tehranTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Tehran")
    private val minuteCache = java.util.concurrent.ConcurrentHashMap<Long, JalaliDateTime>(512)

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
        val minuteKey = effectiveMillis / 60_000L
        minuteCache[minuteKey]?.let { return it }

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

        val computed = JalaliDateTime(
            year = jy,
            month = jm,
            day = jd,
            monthName = monthName,
            weekdayName = weekdayName,
            hour = hour,
            minute = minute
        )
        if (minuteCache.size > 4096) {
            minuteCache.clear()
        }
        minuteCache[minuteKey] = computed
        return computed
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

    private val customSenderToProfile = java.util.concurrent.ConcurrentHashMap<String, BankProfile>()

    fun registerCustomSenderForBank(senderAddress: String, bankNameFa: String) {
        val cleanSender = senderAddress.trim()
        if (cleanSender.isEmpty()) return
        val profile = BANK_PROFILES.firstOrNull {
            it.nameFa == bankNameFa ||
                it.nameFa.replace("\u200C", " ") == bankNameFa.replace("\u200C", " ") ||
                it.nameFa.contains(bankNameFa) ||
                bankNameFa.contains(it.nameFa)
        } ?: BankProfile(
            nameFa = bankNameFa,
            nameEn = bankNameFa,
            icon = "ic_bank_blu",
            senderIds = listOf(cleanSender)
        )
        customSenderToProfile[cleanSender] = profile
        customSenderToProfile[cleanSender.lowercase(Locale.ROOT)] = profile
        val norm = normalizeSender(cleanSender)
        if (norm.isNotEmpty()) {
            customSenderToProfile[norm] = profile
        }
    }

    fun findByNumber(sender: String): BankProfile? =
        customSenderToProfile[sender]
            ?: customSenderToProfile[sender.trim().lowercase(Locale.ROOT)]
            ?: numberToProfile[sender]
            ?: lowercaseNumberToProfile[sender.trim().lowercase(Locale.ROOT)]

    fun resolveBank(sender: String): BankProfile? {
        findByNumber(sender)?.let { return it }
        if (hasUssdMark(sender)) return null
        val normalized = normalizeSender(sender)
        if (normalized.isEmpty()) return null
        customSenderToProfile[normalized]?.let { return it }
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

        val realJalali = JalaliCalendarUtils.timestampToJalali(timestampMillis)
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
            weekdayDate = realJalali.dateWithYear,
            jalaliYear = realJalali.year,
            timeStr = realJalali.timeStr,
            detectedTitle = detectedTitle,
            detectedStore = detectedStore,
            detectedPurpose = detectedPurpose,
            summaryMessage = summaryMessage
        )
    }

    /**
     * Identifies banks ONLY via `JibemanSmsEngine.resolveBank(senderAddress)` (plus custom sender numbers
     * explicitly linked by the user). NEVER guesses bank names from SMS body text so promo SMS like
     * "گردونه ملی گلد" are 100% rejected.
     */
    fun detectIranianBankName(rawSms: String, senderAddress: String = ""): String {
        if (senderAddress.isNotBlank()) {
            JibemanSmsEngine.resolveBank(senderAddress)?.let { return it.nameFa }
        }
        return "بانک"
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
        val realJalali = JalaliCalendarUtils.timestampToJalali(timestampMillis)
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
            weekdayDate = realJalali.dateWithYear,
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
        "یادم هست",
        "یادمه که",
        "بعد یادمه",
        "چی بود",
        "پرداخت کردم",
        "کارت کشیدم",
        "واریز کردم",
        "خرید کردم",
        "خرج کردم",
        "پول دادم",
        "حساب کردم",
        "رفتم بیرون",
        "بیرون رفتم",
        "اشتباه شد"
    )

    // Note: "برای", "بابت", "بابتِ", "واسه" are intentionally NOT in singleWordConversationalFillers
    // so they are preserved when connecting two meaningful nouns (Rule B: "انتقال پول به احمد بابت عسل")
    // and handled via pivot rule when preceded by narrative intros or at the start of a phrase (Rule A).
    private val singleWordConversationalFillers = listOf(
        "سلام",
        "علیکم",
        "درود",
        "والا",
        "ولی",
        "اما",
        "رفتم",
        "بیرون",
        "اومدم",
        "آمدم",
        "برگشتم",
        "امروز",
        "دیروز",
        "دیشب",
        "پریروز",
        "بعد",
        "یادمه",
        "کلاً",
        "کلا",
        "جمعاً",
        "جمعا",
        "مجموعاً",
        "مجموعا",
        "سرجمع",
        "خریدم",
        "گرفتم",
        "دادم",
        "کردم",
        "زدم",
        "شد",
        "بود",
        "بودش",
        "هست",
        "نبود",
        "ببخشید",
        "یعنی",
        "منظورم",
        "هزارش",
        "هزار",
        "تومانش",
        "تومنش",
        "تومان",
        "تومن",
        "ریالش",
        "ریال",
        "میلیون",
        "میلیارد"
    )

    private val narrativeIntroTriggerWords = listOf(
        "سلام",
        "رفتم",
        "بیرون",
        "اومدم",
        "آمدم",
        "برگشتم",
        "امروز",
        "دیروز",
        "دیشب",
        "پریروز",
        "بعد",
        "یادمه",
        "یادم هست",
        "کلاً",
        "کلا",
        "جمعاً",
        "جمعا",
        "مجموعاً",
        "مجموعا",
        "سرجمع",
        "خرج کردم",
        "پول دادم",
        "کارت کشیدم",
        "پرداخت کردم",
        "خرید کردم"
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
        "تومان", "تومن", "تومنش", "تومانش", "هزار", "هزارش", "میلیون", "ملیون", "میلیارد", "ریال", "ریالش"
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
     * Strips bare stumbled numbers immediately preceding `<number> <currency>`
     * (e.g. "۲۱۷ ۲۱۸ تومن بابت کارمزد" -> "۲۱۸ تومن بابت کارمزد").
     */
    fun stripStumbledNumbers(rawText: String): String {
        val normalized = PersianNumberUtils.normalizePersianText(rawText)
        val stumbleRegex = Regex(
            """(?<=^|\s)[۰-۹0-9,٬،]+(?:\s+[۰-۹0-9,٬،]+)*(?=\s+[۰-۹0-9,٬،]+\s*(?:میلیارد|میلیون|ملیون|هزار\s*تومانش|هزار\s*تومنش|هزار\s*تومان|هزار\s*تومن|هزارش|هزار|تومانش|تومنش|تومان|تومن|ریالش|ریال)(?=\s|$))"""
        )
        return stumbleRegex.replace(normalized, " ").replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Strips standalone negation/correction "نه" ("no") while preserving "نه" when used as the number 9
     * (e.g., "سی و نه تومن", "نه هزار تومان", "نه تا بستنی").
     */
    private fun stripNonNumericNahTokens(rawText: String): String {
        var text = PersianNumberUtils.normalizePersianText(rawText)
        // Repeated "نه نه..." is always "no, no...", never the number 9
        text = text.replace(Regex("""(?<=^|\s)نه(?:\s+نه)+(?=\s|$)"""), " ")

        val unitsAndCurrency = (currencyAndMultiplierWords + countingUnits).joinToString("|")
        val prefixTensPattern = compoundMoneyPrefixWords.joinToString("|")
        val standaloneNahRegex = Regex("""(?<=^|\s)نه(?=\s|$)""")

        return standaloneNahRegex.replace(text) { match ->
            val before = text.substring(0, match.range.first).trimEnd()
            val after = text.substring(match.range.last + 1).trimStart()
            val isCompoundNine = Regex("""(?:$prefixTensPattern)\s+و$""").containsMatchIn(before)
            val isFollowedByUnitOrCurrency = Regex("""^(?:$unitsAndCurrency)(?=\s|$)""").containsMatchIn(after)
            if (isCompoundNine || isFollowedByUnitOrCurrency) {
                match.value
            } else {
                " "
            }
        }.replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Resolves mid-speech self-corrections where the user states an amount, says a correction marker
     * (`نه نه...`, `نبود`, `بود نه`, `اشتباه شد`, `ببخشید`, `یعنی`, `منظورم`, or `نه` before a new number),
     * and then states the corrected amount (e.g. "۵۰ تومن نانوایی بود نه نه نه نه ۳۵ تومن بود نانوایی" -> "۳۵ تومن نانوایی").
     */
    fun resolveMidSpeechSelfCorrections(rawText: String): String {
        val normalized = stripStumbledNumbers(PersianNumberUtils.normalizePersianText(rawText))
        val correctionRegex = Regex(
            """^(.*?)(?<=^|\s)(?:(?:نه\s+)+نه|بود\s+نه(?:\s+نه)*|نبود(?:\s+نه)*|اشتباه\s+شد|ببخشید|منظورم|یعنی|نه(?=\s+[۰-۹0-9]))(?=\s|$)\s*(.+)$"""
        )
        val match = correctionRegex.find(normalized) ?: return stripNonNumericNahTokens(normalized)
        val leftPart = match.groupValues[1].trim()
        val rightPart = resolveMidSpeechSelfCorrections(match.groupValues[2].trim())

        val leftAmount = parseSpokenPersianAmountToman(leftPart)
        val rightAmount = parseSpokenPersianAmountToman(rightPart)
        if (leftAmount > 0L && rightAmount > 0L) {
            val leftParsed = parseSingleVoiceClause(leftPart, null, "نقدی / دستی")
            val rightParsed = parseSingleVoiceClause(rightPart, null, "نقدی / دستی")
            return if (rightParsed.purposeCategory.isNotBlank()) {
                stripNonNumericNahTokens(rightPart)
            } else if (leftParsed.purposeCategory.isNotBlank()) {
                stripNonNumericNahTokens("$rightPart ${leftParsed.purposeCategory}")
            } else {
                stripNonNumericNahTokens(rightPart)
            }
        }
        return stripNonNumericNahTokens(normalized)
    }

    /**
     * Strips stammered / mental-math counting and sub-price chains before the final item noun
     * (e.g., "دو تا سه تا ۵ تومن ۵ تومن نانوایی" -> "نانوایی").
     * Preserves single legitimate item quantities like "دو تا بستنی" or "یک شارژ ایرانسل".
     */
    fun stripMentalMathChains(rawText: String): String {
        val text = stripStumbledNumbers(PersianNumberUtils.normalizePersianText(rawText))
        val unitsPattern = countingUnits.joinToString("|")
        val qtyWordsPattern = smallQuantityWords.keys.sortedByDescending { it.length }.joinToString("|")
        // Matches a single small count or sub-price phrase (e.g. "دو تا", "سه تا", "۵ تومن", "۱۰ تومن")
        val singleSmallCountOrSubPrice =
            """(?:(?:$qtyWordsPattern|[۰-۹0-9]{1,2})\s*(?:$unitsPattern|تومانش|تومنش|تومان|تومن))"""
        val stammerChainRegex = Regex(
            """(?<=^|\s)$singleSmallCountOrSubPrice(?:\s+$singleSmallCountOrSubPrice)+(?=\s|$)"""
        )
        return stammerChainRegex.replace(text, " ").replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Normalizes compound Persian nouns (e.g. "سیب زمینی" -> "سیب‌زمینی") and protects item quantities
     * (like "یک شارژ", "دو تا سیب‌زمینی", "دو تا بستنی", "۳ دونه نان") with placeholders so they are:
     * 1) NEVER mistaken for 1,000 or 2,000 Toman money amounts, and
     * 2) Preserved in the final item title with clean Persian digits ("۱ شارژ ایرانسل", "۲ تا سیب‌زمینی").
     * First strips any back-to-back stammered mental-math chains ("دو تا سه تا ۵ تومن ۵ تومن").
     */
    fun protectItemQuantities(rawText: String): ProtectedQuantitiesText {
        var text = stripNonNumericNahTokens(stripMentalMathChains(rawText))
            .replace(Regex("""سیب\s+زمینی"""), "سیب‌زمینی")
            .replace(Regex("""تخم\s+مرغ"""), "تخم‌مرغ")
            .replace(Regex("""گوجه\s+فرنگی"""), "گوجه‌فرنگی")

        val replacements = linkedMapOf<String, String>()
        var counter = 0

        // Pattern A: <number word or 1..2 digit number> + <counting unit (تا/دونه/عدد/بسته/کیلو/...)> + <non-currency noun>
        val unitsPattern = countingUnits.joinToString("|")
        val qtyWordsPattern = smallQuantityWords.keys.sortedByDescending { it.length }.joinToString("|")
        val withUnitRegex = Regex(
            """(?<=^|\s)($qtyWordsPattern|[۰-۹0-9]{1,2})\s*($unitsPattern)(?=\s+([^\s]+))"""
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
        val indefiniteRecipientNouns = setOf(
            "فقیر", "نیازمند", "بنده", "گدا", "مسکین", "خیریه", "دوست", "رفیق", "نفر", "بچه", "کودک"
        )

        text = directNounRegex.replace(text) { match ->
            val wordToken = match.groupValues[1]
            val followingWord = match.groupValues[2]
                .replace("،", "")
                .replace(".", "")
                .trim()

            // Check if preceded by "<tens/hundreds> و " (e.g., "سی و نه تومن" or "بیست و پنج هزار")
            val prefixBeforeMatch = text.substring(0, match.range.first).trimEnd()
            val prevWordBeforeMatch = prefixBeforeMatch.split(Regex("""\s+""")).lastOrNull().orEmpty()
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
                val isIndefiniteRecipient = (wordToken == "یه" || wordToken == "یک") &&
                    (prevWordBeforeMatch == "به" || followingWord in indefiniteRecipientNouns)
                if (isIndefiniteRecipient) {
                    replacements[key] = ""
                } else {
                    val persianNum = PersianNumberUtils.toPersianDigits(numericVal.toString())
                    replacements[key] = persianNum
                }
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

    private val leadingPrepositionsToStrip = listOf(
        "به", "از", "برای", "بابتِ", "بابت", "تو", "توی", "در", "واسه", "که"
    )

    private fun deduplicateConsecutiveWords(input: String): String {
        val tokens = input.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (tokens.size <= 1) return tokens.joinToString(" ")
        val deduped = mutableListOf<String>()
        for (token in tokens) {
            if (deduped.isEmpty() || deduped.last() != token) {
                deduped.add(token)
            }
        }
        return deduped.joinToString(" ")
    }

    private fun trimLeadingPrepositionsAndVav(input: String): String {
        var s = deduplicateConsecutiveWords(input.replace(Regex("""\s+"""), " ").trim())
        var changed = true
        while (changed) {
            changed = false
            while (s.startsWith("و ")) {
                s = s.removePrefix("و ").trim()
                changed = true
            }
            while (s.endsWith(" و")) {
                s = s.removeSuffix(" و").trim()
                changed = true
            }
            while (s.startsWith("ش ")) {
                s = s.removePrefix("ش ").trim()
                changed = true
            }
            for (prep in leadingPrepositionsToStrip) {
                if (s.startsWith("$prep ")) {
                    s = s.removePrefix("$prep ").trim()
                    changed = true
                }
                if (s.endsWith(" $prep")) {
                    s = s.removeSuffix(" $prep").trim()
                    changed = true
                }
            }
            if (s.startsWith("خرید ") && s.removePrefix("خرید ").trim().isNotBlank()) {
                s = s.removePrefix("خرید ").trim()
                changed = true
            }
        }
        s = deduplicateConsecutiveWords(s)
        if (s == "و" || s == "ش" || s in leadingPrepositionsToStrip) return ""
        return s
    }

    private fun stripBasicFillersOnly(rawText: String): String {
        var result = stripNonNumericNahTokens(PersianNumberUtils.normalizePersianText(rawText))
        for (phrase in multiWordConversationalFillers) {
            result = result.replace(Regex("""(?<=^|\s)$phrase(?=\s|$)"""), " ")
        }
        for (word in singleWordConversationalFillers) {
            result = result.replace(Regex("""(?<=^|\s)$word(?=\s|$)"""), " ")
        }
        return trimLeadingPrepositionsAndVav(result)
    }

    /**
     * Applies Rule A and Rule B for Persian voice NLU:
     * - Strip leading prepositions (`به `, `از `, `برای `, `بابت `, `تو `, `توی `, `در `, `واسه `)
     *   so `"دادم به بستنی"` ALWAYS becomes `"بستنی"`.
     * - Pivot on `"برای"` / `"بابت"` / `"واسه"` after narrative intros (`رفتم`, `بیرون`, `اومدم`, `خرج کردم`, ...).
     * - Preserve `"بابت"` or `"برای"` ONLY when it sits *between* two meaningful noun phrases
     *   (e.g., `"انتقال پول به احمد بابت عسل"`).
     */
    fun stripGreetingsAndFillers(rawText: String): String {
        val normalized = stripMentalMathChains(rawText)
        val pivotRegex = Regex("""^(.*?)(?<=^|\s)(برای|بابتِ|بابت|واسه)(?=\s|$)(.*)$""")
        val match = pivotRegex.find(normalized)

        val result = if (match != null) {
            val leftRaw = match.groupValues[1].trim()
            val prep = if (match.groupValues[2] == "بابتِ") "بابت" else match.groupValues[2].trim()
            val rightRaw = match.groupValues[3].trim()

            val hasNarrativeIntroOnLeft = narrativeIntroTriggerWords.any { marker ->
                Regex("""(?<=^|\s)$marker(?=\s|$)""").containsMatchIn(leftRaw)
            }
            val leftClean = stripBasicFillersOnly(leftRaw)
            val rightClean = stripBasicFillersOnly(rightRaw)

            when {
                hasNarrativeIntroOnLeft && rightClean.isNotBlank() -> rightClean
                leftClean.isBlank() -> rightClean
                rightClean.isBlank() -> leftClean
                else -> "$leftClean $prep $rightClean"
            }
        } else {
            stripBasicFillersOnly(normalized)
        }

        return trimLeadingPrepositionsAndVav(result)
    }

    private fun pivotNarrativeIntroKeepingAmount(rawText: String): String {
        val cleaned = PersianNumberUtils.normalizePersianText(rawText.trim())
        val pivotRegex = Regex("""^(.*?)(?<=^|\s)(?:برای|بابتِ|بابت|واسه)(?=\s|$)\s*(.+)$""")
        val match = pivotRegex.find(cleaned) ?: return trimLeadingPrepositionsAndVav(cleaned)

        val leftRaw = match.groupValues[1].trim()
        val rightRaw = trimLeadingPrepositionsAndVav(match.groupValues[2].trim())
        if (rightRaw.isBlank()) return trimLeadingPrepositionsAndVav(cleaned)

        val hasNarrativeIntroOnLeft = narrativeIntroTriggerWords.any { marker ->
            Regex("""(?<=^|\s)$marker(?=\s|$)""").containsMatchIn(leftRaw)
        }
        if (!hasNarrativeIntroOnLeft) {
            return trimLeadingPrepositionsAndVav(cleaned)
        }

        val leftAmount = parseSpokenPersianAmountToman(leftRaw)
        val rightAmount = parseSpokenPersianAmountToman(rightRaw)
        return if (leftAmount > 0L && rightAmount == 0L) {
            "${leftAmount / 1000L} هزار تومان $rightRaw"
        } else {
            rightRaw
        }
    }

    /**
     * Parses spoken Persian numbers and digits into Tomans while ignoring item quantities and mental-math sub-prices:
     * - Strips bare stumbled numbers before `<number> <currency>` (e.g. "۲۱۷ ۲۱۸ تومن" -> "۲۱۸ تومن").
     * - When a clause contains multiple currency amounts (e.g. "۳۹ هزار تومان دادم به دو تا سه تا ۵ تومن ۵ تومن نانوایی"),
     *   takes the LARGEST explicit currency amount in that clause (`39,000` Tomans), ignoring smaller sub-amounts (`۵ تومن`).
     */
    fun parseSpokenPersianAmountToman(rawText: String): Long {
        val cleaned = stripStumbledNumbers(PersianNumberUtils.normalizePersianText(rawText.trim()))
        if (cleaned.isBlank()) return 0L

        for ((phrase, amt) in exactPhraseAmounts) {
            if (cleaned.contains(phrase)) return amt
        }

        val protectedText = protectItemQuantities(cleaned).protectedText
        val textWithoutQtyPlaceholders = protectedText.replace(Regex("""⟪QTY[A-Z]+⟫"""), " ")
        val engWithoutPlaceholders = PersianNumberUtils.toEnglishDigits(textWithoutQtyPlaceholders)

        // 1. Check ALL numeric digit amounts in the clause and take the largest explicit currency amount
        val digitRegex = Regex(
            """([0-9]+(?:\.[0-9]+)?)\s*(میلیارد|میلیون|ملیون|هزار\s*تومانش|هزار\s*تومنش|هزار\s*تومان|هزار\s*تومن|هزارش|هزار|تومانش|تومنش|تومان|تومن|ریالش|ریال)?"""
        )
        var maxDigitAmount = 0L
        for (digitMatch in digitRegex.findAll(engWithoutPlaceholders)) {
            val numVal = digitMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val unit = digitMatch.groupValues[2].trim()
            if (numVal > 0.0) {
                val candidate = when {
                    unit == "میلیارد" -> (numVal * 1_000_000_000.0).toLong()
                    unit == "میلیون" || unit == "ملیون" -> (numVal * 1_000_000.0).toLong()
                    unit == "ریال" || unit == "ریالش" -> (numVal / 10.0).toLong()
                    unit.startsWith("هزار") -> (numVal * 1_000.0).toLong()
                    numVal in 1.0..999.0 -> (numVal * 1_000.0).toLong()
                    else -> numVal.toLong()
                }
                if (candidate > maxDigitAmount) {
                    maxDigitAmount = candidate
                }
            }
        }
        if (maxDigitAmount > 0L) {
            return maxDigitAmount
        }

        // 2. Parse spoken Persian number words: e.g. "پانصد هزار تومان", "پنجاه تومن", "هفتاد تومن", "سی و نه تومن"
        val tokens = textWithoutQtyPlaceholders
            .replace("،", " ")
            .replace(".", " ")
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }

        var maxSpokenAmount = 0L
        var totalAccumulated = 0L
        var currentGroup = 0L
        var matchedAnyNumberWord = false
        var hasExplicitMultiplier = false

        fun flushCurrentPhrase() {
            if (!matchedAnyNumberWord) return
            var phraseVal = totalAccumulated
            if (currentGroup > 0L) {
                phraseVal += if (!hasExplicitMultiplier && currentGroup in 1L..999L) {
                    currentGroup * 1_000L
                } else if (hasExplicitMultiplier && totalAccumulated >= 1_000_000L && currentGroup in 1L..999L) {
                    currentGroup * 1_000L
                } else {
                    currentGroup
                }
            }
            if (phraseVal > maxSpokenAmount) {
                maxSpokenAmount = phraseVal
            }
            totalAccumulated = 0L
            currentGroup = 0L
            matchedAnyNumberWord = false
            hasExplicitMultiplier = false
        }

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
                if (token.endsWith("تومان") || token.endsWith("تومن") || token.endsWith("تومانش") || token.endsWith("تومنش")) {
                    flushCurrentPhrase()
                }
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
                "هزار", "هزارش" -> {
                    val base = if (currentGroup > 0L) currentGroup else 1L
                    totalAccumulated += base * 1_000L
                    currentGroup = 0L
                    matchedAnyNumberWord = true
                    hasExplicitMultiplier = true
                }
                "تومان", "تومن", "تومنش", "تومانش" -> {
                    if (matchedAnyNumberWord) {
                        flushCurrentPhrase()
                    }
                }
            }
        }

        flushCurrentPhrase()
        return maxSpokenAmount
    }

    /**
     * Formats multiple parsed voice commands into a single combined note for an existing bank SMS row:
     * e.g. "۲ تا شارژ (۵۰,۰۰۰) + ۲ تا بستنی (۵۰,۰۰۰)" or "بستنی (۴۹,۰۰۰) + کاغذ (۵۰۰)"
     */
    fun formatCombinedMultiVoiceNote(items: List<ParsedVoiceExpense>): String {
        return items.joinToString(" + ") { item ->
            "${item.purposeCategory} (${PersianNumberUtils.formatToman(item.totalAmountToman, showUnit = false)})"
        }
    }

    private val negationAndCancellationMarkers = listOf(
        "ندادم",
        "نخریدم",
        "نگرفتم",
        "نکردم",
        "نزدم",
        "نشد",
        "کنسل",
        "کنسلش",
        "لغو",
        "پشیمون",
        "پشیمان",
        "منصرف",
        "نخواستم",
        "نمیخوام",
        "نمی‌خوام",
        "نمیخواستم",
        "نمی‌خواستم"
    )

    private fun hasHardNegationMarker(clause: String): Boolean {
        val norm = PersianNumberUtils.normalizePersianText(clause.trim())
        if (norm.isBlank()) return false
        return negationAndCancellationMarkers.any { marker ->
            Regex("""(?<=^|\s)$marker(?=\s|$)""").containsMatchIn(norm)
        }
    }

    /**
     * Rule B (Negation & Cancellation Filter):
     * Returns true if the clause contains a negation or cancellation marker such as
     * `ندادم`, `نخریدم`, `نگرفتم`, `نکردم`, `نزدم`, `نشد`, `کنسل`, `لغو`, `پشیمون`, `منصرف`, `نخواستم`,
     * or `میخواستم / می‌خواستم ... ولی / اما`.
     */
    fun isNegatedOrCancelledClause(clause: String): Boolean {
        val norm = PersianNumberUtils.normalizePersianText(clause.trim())
        if (norm.isBlank()) return false
        if (hasHardNegationMarker(norm)) return true
        val hasWantedTo = Regex("""(?<=^|\s)(?:میخواستم|می‌خواستم|میخواهم|میخوام)(?=\s|$)""").containsMatchIn(norm)
        val hasBut = Regex("""(?<=^|\s)(?:ولی|اما)(?=\s|$)""").containsMatchIn(norm)
        return hasWantedTo && hasBut
    }

    /**
     * Extracts the intended item subject from a soft-cancelled "می‌خواستم X بدم ولی" clause
     * so that if the immediately following clause ("۴۹ تومن دادم") omits the noun, the subject ("۲ تا بستنی") is preserved.
     */
    private fun extractSubjectFromSoftCancelledClause(cancelledClause: String): String {
        val stripped = PersianNumberUtils.normalizePersianText(cancelledClause)
            .replace(Regex("""(?<=^|\s)(?:میخواستم|می‌خواستم|میخواهم|میخوام|بدم|بخرم|بگیرم|ولی|اما)(?=\s|$)"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (stripped.isBlank()) return ""
        return parseSingleVoiceClause(stripped, null, "نقدی / دستی").purposeCategory
    }

    /**
     * Rule A (Asyndetic Clause Splitting):
     * Splits consecutive clauses even when the user does NOT say " و " between them, whenever a new
     * `<number> + <currency: تومن/تومان/تومانش/تومنش/هزار/میلیون>` pattern begins after a noun or payment verb
     * (`دادم`, `خریدم`, `گرفتم`, `زدم`, `کردم`).
     */
    private fun splitAsyndeticAmountClauses(clause: String): List<String> {
        val trimmed = stripStumbledNumbers(clause.trim())
        if (trimmed.isBlank()) return emptyList()

        val numberWordsPattern = numberWordTokens.keys.sortedByDescending { it.length }.joinToString("|")
        val amountStartRegex = Regex(
            """(?<=^|\s)(?:[۰-۹0-9][۰-۹0-9,٬،]*(?:\.[۰-۹0-9]+)?|(?:$numberWordsPattern)(?:\s+و\s+(?:$numberWordsPattern))*)\s+(?:میلیارد|میلیون|ملیون|هزار\s*تومانش|هزار\s*تومنش|هزار\s*تومان|هزار\s*تومن|هزارش|هزار|تومانش|تومنش|تومان|تومن|ریالش|ریال)(?=\s|$)"""
        )

        val matches = amountStartRegex.findAll(trimmed).toList()
        if (matches.size <= 1) return listOf(trimmed)

        val result = mutableListOf<String>()
        var currentStart = 0

        for (i in 1 until matches.size) {
            val match = matches[i]
            val candidateSplitIdx = match.range.first
            val precedingSegment = trimmed.substring(currentStart, candidateSplitIdx).trim()

            // Do not split if preceding segment is part of a "که ... دنگ" phrase
            if (precedingSegment.endsWith("که") || precedingSegment.endsWith("و")) {
                continue
            }

            val precedingParsed = parseSingleVoiceClause(precedingSegment, null, "نقدی / دستی")
            val hasExplicitVerb = Regex("""(?<=^|\s)(?:دادم|خریدم|گرفتم|زدم|کردم|بدم|بود)(?=\s|$)""")
                .containsMatchIn(precedingSegment)

            if (precedingParsed.totalAmountToman > 0L &&
                (precedingParsed.purposeCategory.isNotBlank() || hasExplicitVerb)
            ) {
                result.add(precedingSegment)
                currentStart = candidateSplitIdx
            }
        }

        val tail = trimmed.substring(currentStart).trim()
        if (tail.isNotBlank()) {
            result.add(tail)
        }
        return result
    }

    private fun extractCandidateSubClauses(cleaned: String): List<String> {
        val selfCorrected = resolveMidSpeechSelfCorrections(cleaned)
        val normalized = stripMentalMathChains(selfCorrected)

        // Protect " و " inside compound Persian numbers (e.g. "سی و نه", "بیست و پنج", "یک میلیون و دویست")
        val prefixPattern = compoundMoneyPrefixWords.sortedByDescending { it.length }.joinToString("|")
        val numWordsPattern = (numberWordTokens.keys + setOf("نیم")).sortedByDescending { it.length }.joinToString("|")
        val compoundVavRegex = Regex("""(?<=^|\s)($prefixPattern)\s+و\s+(?=($numWordsPattern)(?=\s|$))""")
        var protectedCompoundNums = normalized
        while (compoundVavRegex.containsMatchIn(protectedCompoundNums)) {
            protectedCompoundNums = compoundVavRegex.replace(protectedCompoundNums) { m ->
                "${m.groupValues[1]}⟪NUMVAV⟫"
            }
        }

        val byConjOrComma = protectedCompoundNums
            .split(Regex("""(?:\s+و\s+|،)"""))
            .map { resolveMidSpeechSelfCorrections(it.replace("⟪NUMVAV⟫", " و ").trim()) }
            .filter { it.isNotBlank() }

        val afterAsyndetic = mutableListOf<String>()
        for (part in byConjOrComma) {
            afterAsyndetic.addAll(splitAsyndeticAmountClauses(part))
        }

        val expanded = mutableListOf<String>()
        for (part in afterAsyndetic) {
            expanded.addAll(splitOnMidSentenceVerb(part))
        }
        return expanded
    }

    private fun splitOnMidSentenceVerb(clause: String): List<String> {
        val midVerbRegex = Regex("""^(.+?)\s+(?:خریدم|گرفتم|دادم|زدم)\s+(.+)$""")
        val match = midVerbRegex.find(clause.trim()) ?: return listOf(clause.trim())
        val left = match.groupValues[1].trim()
        val right = match.groupValues[2].trim()
        if (isNegatedOrCancelledClause(left) || isNegatedOrCancelledClause(right)) {
            return listOf(clause.trim())
        }
        val rightParsed = parseSingleVoiceClause(right, null, "نقدی / دستی")
        val leftParsed = parseSingleVoiceClause(left, null, "نقدی / دستی")
        return if (leftParsed.purposeCategory.isNotBlank() && rightParsed.purposeCategory.isNotBlank()) {
            listOf(left, right)
        } else {
            listOf(clause.trim())
        }
    }

    private fun normalizeComparableNoun(title: String): String {
        return PersianNumberUtils.normalizePersianText(title)
            .replace("نونوایی", "نانوایی")
            .replace("نون", "نان")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Reconciles sub-thousand Toman amounts (e.g. "۵۰۰ تومانش بابت کاغذ" on a 49,500 SMS, or "۲۱۸ تومن بابت کارمزد" on a 107,218 SMS)
     * where the user spoke a number in 1..999 without "هزار" that represents literal Tomans rather than thousands of Tomans.
     */
    private fun reconcileSubThousandMultiItems(
        items: List<ParsedVoiceExpense>,
        clauses: List<String>,
        fallbackTotalToman: Long?
    ): List<ParsedVoiceExpense> {
        if (items.size < 2) return items

        val canDemoteToSubThousand = items.mapIndexed { idx, item ->
            val clause = clauses.getOrNull(idx).orEmpty()
            val hasExplicitHezar = Regex("""(?<=^|\s)(?:هزارش|هزار|میلیون|ملیون|میلیارد)(?=\s|$)""").containsMatchIn(clause)
            !hasExplicitHezar && item.totalAmountToman in 1_000L..999_000L && (item.totalAmountToman % 1_000L == 0L)
        }

        // 1. If we have a known target total (from bank SMS or summary intro header), check if demoting any candidate items
        //    from N*1000 to literal N makes the total match (or fixes an item that alone exceeds fallbackTotalToman).
        if (fallbackTotalToman != null && fallbackTotalToman > 0L) {
            val currentSum = items.sumOf { it.totalAmountToman }
            if (currentSum != fallbackTotalToman) {
                // Try demoting individual items or any combination of demotable items to match fallbackTotalToman
                for (i in items.indices) {
                    if (canDemoteToSubThousand[i]) {
                        val literalToman = items[i].totalAmountToman / 1_000L
                        val candidateSum = currentSum - items[i].totalAmountToman + literalToman
                        if (candidateSum == fallbackTotalToman ||
                            (items[i].totalAmountToman > fallbackTotalToman && literalToman < fallbackTotalToman)
                        ) {
                            return items.mapIndexed { idx, it ->
                                if (idx == i) it.copy(totalAmountToman = literalToman, userShareToman = literalToman) else it
                            }
                        }
                    }
                }
            }
        }

        // 2. Even without a bank SMS fallback, if an item is a bank transfer fee ("کارمزد") with a raw number 100..999
        //    (without "هزار") alongside a main transfer item >= 10,000 Tomans, treat the fee as literal Tomans (e.g. 218 Tomans).
        val hasMajorTransferItem = items.any { it.totalAmountToman >= 10_000L && !it.purposeCategory.contains("کارمزد") }
        if (hasMajorTransferItem) {
            return items.mapIndexed { idx, item ->
                val literalToman = item.totalAmountToman / 1_000L
                if (canDemoteToSubThousand[idx] && item.purposeCategory.contains("کارمزد") && literalToman in 100L..999L) {
                    item.copy(totalAmountToman = literalToman, userShareToman = literalToman)
                } else {
                    item
                }
            }
        }

        return items
    }

    /**
     * Collapses consecutive items that have the exact same normalized item title when spoken as a self-correction
     * (e.g. "۵۰ تومان نانوایی ۳۵ تومن نانوایی" -> keeps only the second item: "نانوایی" with 35,000 Tomans).
     */
    private fun collapseConsecutiveSameNounCorrections(items: List<ParsedVoiceExpense>): List<ParsedVoiceExpense> {
        if (items.size <= 1) return items
        val collapsed = mutableListOf<ParsedVoiceExpense>()
        for (item in items) {
            val prev = collapsed.lastOrNull()
            if (prev != null &&
                prev.purposeCategory.isNotBlank() &&
                normalizeComparableNoun(prev.purposeCategory) == normalizeComparableNoun(item.purposeCategory)
            ) {
                collapsed[collapsed.lastIndex] = item
            } else {
                collapsed.add(item)
            }
        }
        return collapsed
    }

    /**
     * Detects if the user explicitly spoke an amount (or multiple item amounts) that conflicts with the
     * target bank SMS amount (`fallbackTotalToman`).
     * Returns the conflicting spoken amount in Tomans if there is a mismatch, or `null` if it matches or no amount was spoken.
     */
    fun detectBankSmsAmountMismatch(
        rawSpeech: String,
        fallbackTotalToman: Long,
        multiItems: List<ParsedVoiceExpense> = emptyList()
    ): Long? {
        if (fallbackTotalToman <= 0L) return null
        val cleaned = PersianNumberUtils.normalizePersianText(rawSpeech.trim())
        if (cleaned.isBlank()) return null

        val itemsToCheck = if (multiItems.isNotEmpty()) {
            multiItems
        } else {
            parseMultiVoiceCommands(cleaned, "نقدی / دستی", fallbackTotalToman)
        }

        if (itemsToCheck.size > 1) {
            val sumSpoken = itemsToCheck.sumOf { it.totalAmountToman }
            return if (sumSpoken > 0L && sumSpoken != fallbackTotalToman) sumSpoken else null
        }

        // Single-item bank SMS tagging: check if the user explicitly spoke an amount in rawSpeech
        val freeParsedItems = parseMultiVoiceCommands(cleaned, "نقدی / دستی", fallbackTotalToman = null)
        val spokenAmount = freeParsedItems.sumOf { it.totalAmountToman }
        if (spokenAmount <= 0L) return null

        // Allow exact match or sub-thousand match (e.g. 500 spoken as 500 on a 500 Toman SMS)
        if (spokenAmount == fallbackTotalToman) return null
        if (spokenAmount in 1_000L..999_000L && (spokenAmount / 1_000L) == fallbackTotalToman) return null

        return spokenAmount
    }

    fun parseMultiVoiceCommands(
        rawSpeech: String,
        defaultBankName: String = "نقدی / دستی",
        fallbackTotalToman: Long? = null
    ): List<ParsedVoiceExpense> {
        val rawCleaned = stripStumbledNumbers(PersianNumberUtils.normalizePersianText(rawSpeech.trim()))
        if (rawCleaned.isBlank()) return emptyList()

        if (rawCleaned.contains("که") && (rawCleaned.contains("دنگ") || rawCleaned.contains("دُنگ") || rawCleaned.contains("سهم"))) {
            if (isNegatedOrCancelledClause(rawCleaned)) return emptyList()
            return listOf(parseSingleVoiceClause(rawCleaned, fallbackTotalToman, defaultBankName))
        }

        val cleaned = pivotNarrativeIntroKeepingAmount(rawCleaned)
        val rawCandidateParts = extractCandidateSubClauses(cleaned)

        // Rule B: Drop any negated or cancelled clause (e.g. "۵۵ تومن میخواستم بدم نونوایی ولی ندادم"),
        // while carrying forward the item noun from a soft-cancelled "می‌خواستم X بدم ولی" clause if the next clause omits the noun!
        val candidateParts = mutableListOf<String>()
        var pendingSubjectFromSoftCancelled = ""

        for (rawPart in rawCandidateParts) {
            if (isNegatedOrCancelledClause(rawPart)) {
                if (!hasHardNegationMarker(rawPart)) {
                    val subject = extractSubjectFromSoftCancelledClause(rawPart)
                    if (subject.isNotBlank()) {
                        pendingSubjectFromSoftCancelled = subject
                    }
                } else {
                    pendingSubjectFromSoftCancelled = ""
                }
            } else {
                var effectivePart = rawPart
                if (pendingSubjectFromSoftCancelled.isNotBlank()) {
                    val probe = parseSingleVoiceClause(rawPart, null, defaultBankName)
                    if (probe.purposeCategory.isBlank() && probe.totalAmountToman > 0L) {
                        effectivePart = "$pendingSubjectFromSoftCancelled $rawPart"
                    }
                    pendingSubjectFromSoftCancelled = ""
                }
                candidateParts.add(effectivePart)
            }
        }

        val nonEmptyCandidateParts = candidateParts.filter { part ->
            val p = parseSingleVoiceClause(part, null, defaultBankName)
            p.totalAmountToman > 0L || p.purposeCategory.isNotBlank()
        }

        if (nonEmptyCandidateParts.isEmpty()) {
            return emptyList()
        }

        // Check if the first clause is a Summary Intro Header (e.g. "اومدم کلاً ۵۰ هزار تومان خرج کردم")
        // followed by itemized sub-clauses ("۲۵ تومن بستنی", "۲۵ تومن دمپایی").
        // Must contain an explicit summary/spending marker so compound numbers like "یک میلیون و دویست هزار تومان خرید گوشت" are preserved!
        val summaryHeaderRegex = Regex(
            """(?<=^|\s)(?:کلاً|کلا|جمعاً|جمعا|مجموعاً|مجموعا|سرجمع|خرج\s+کردم|خرید\s+کردم|پول\s+دادم|کارت\s+کشیدم|پرداخت\s+کردم)(?=\s|$)"""
        )
        var summaryHeaderAmount: Long? = null
        val activeCandidateParts = if (nonEmptyCandidateParts.size >= 2) {
            val firstRaw = nonEmptyCandidateParts.first()
            val firstProbe = parseSingleVoiceClause(firstRaw, null, defaultBankName)
            val isSummaryHeaderClause = summaryHeaderRegex.containsMatchIn(firstRaw)
            val restHaveItemWithAmount = nonEmptyCandidateParts.drop(1).any { part ->
                val p = parseSingleVoiceClause(part, null, defaultBankName)
                p.totalAmountToman > 0L && p.purposeCategory.isNotBlank()
            }
            if (isSummaryHeaderClause &&
                firstProbe.totalAmountToman > 0L &&
                firstProbe.purposeCategory.isBlank() &&
                restHaveItemWithAmount
            ) {
                summaryHeaderAmount = firstProbe.totalAmountToman
                nonEmptyCandidateParts.drop(1)
            } else {
                nonEmptyCandidateParts
            }
        } else {
            nonEmptyCandidateParts
        }

        val effectiveFallbackTotal = fallbackTotalToman ?: summaryHeaderAmount

        if (activeCandidateParts.size == 1) {
            return listOf(parseSingleVoiceClause(activeCandidateParts.first(), fallbackTotalToman, defaultBankName))
        }

        // Case A: Tagging an existing bank transaction (or summary total) with known total (e.g. 100,000 or 49,500 Tomans)
        if (effectiveFallbackTotal != null && effectiveFallbackTotal > 0L && activeCandidateParts.size == 2) {
            val firstParsed = parseSingleVoiceClause(activeCandidateParts[0], null, defaultBankName)
            val secondParsed = parseSingleVoiceClause(activeCandidateParts[1], null, defaultBankName)

            if (firstParsed.purposeCategory.isNotBlank() && secondParsed.purposeCategory.isNotBlank()) {
                if (firstParsed.totalAmountToman == 0L &&
                    secondParsed.totalAmountToman in 1L until effectiveFallbackTotal
                ) {
                    val remainder = (effectiveFallbackTotal - secondParsed.totalAmountToman).coerceAtLeast(0L)
                    return collapseConsecutiveSameNounCorrections(
                        listOf(
                            firstParsed.copy(totalAmountToman = remainder, userShareToman = remainder),
                            secondParsed
                        )
                    )
                } else if (secondParsed.totalAmountToman == 0L &&
                    firstParsed.totalAmountToman in 1L until effectiveFallbackTotal
                ) {
                    val remainder = (effectiveFallbackTotal - firstParsed.totalAmountToman).coerceAtLeast(0L)
                    return collapseConsecutiveSameNounCorrections(
                        listOf(
                            firstParsed,
                            secondParsed.copy(totalAmountToman = remainder, userShareToman = remainder)
                        )
                    )
                } else if (firstParsed.totalAmountToman > 0L && secondParsed.totalAmountToman > 0L) {
                    val reconciled = reconcileSubThousandMultiItems(
                        items = listOf(firstParsed, secondParsed),
                        clauses = activeCandidateParts,
                        fallbackTotalToman = effectiveFallbackTotal
                    )
                    return collapseConsecutiveSameNounCorrections(reconciled)
                }
            }
        }

        // Case B: Multi-clause sentence where sub-clauses have explicit stated amounts > 0L
        val mergedClauses = mutableListOf<String>()
        var currentClause = activeCandidateParts.first()

        for (nextPart in activeCandidateParts.drop(1)) {
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
            val validClauses = mutableListOf<String>()
            val parsedItems = mutableListOf<ParsedVoiceExpense>()
            for (clause in mergedClauses) {
                val parsed = parseSingleVoiceClause(clause, null, defaultBankName)
                if (parsed.totalAmountToman > 0L && parsed.purposeCategory.isNotBlank()) {
                    validClauses.add(clause)
                    parsedItems.add(parsed)
                }
            }

            if (parsedItems.size >= 2) {
                val reconciled = reconcileSubThousandMultiItems(
                    items = parsedItems,
                    clauses = validClauses,
                    fallbackTotalToman = effectiveFallbackTotal
                )
                return collapseConsecutiveSameNounCorrections(reconciled)
            }
        }

        val combinedKept = activeCandidateParts.joinToString(" و ")
        return listOf(parseSingleVoiceClause(combinedKept, fallbackTotalToman, defaultBankName))
    }

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

        if (multiItems.size == 1) {
            val single = multiItems.first()
            return if (isTaggingExistingSms) {
                val effectiveBank = defaultBankName.ifBlank { "بلو" }
                single.copy(
                    totalAmountToman = fallbackTotalToman!!,
                    userShareToman = (fallbackTotalToman - single.dongAmountToman).coerceAtLeast(0L),
                    storeName = effectiveBank,
                    cardLabel = "کارت $effectiveBank"
                )
            } else {
                single
            }
        }

        if (isNegatedOrCancelledClause(cleaned)) {
            return ParsedVoiceExpense(
                recognizedSpeech = cleaned,
                totalAmountToman = 0L,
                userShareToman = 0L,
                dongAmountToman = 0L,
                dongFriendName = "",
                storeName = defaultBankName,
                purposeCategory = "",
                cardLabel = "نقدی / دستی"
            )
        }

        return parseSingleVoiceClause(cleaned, fallbackTotalToman, defaultBankName)
    }

    private fun detectSpokenCardBankInVoice(speech: String): String {
        val cardBankRegex = Regex("""کارت\s+(بلو|ملت|ملی|سپه|پاسارگاد|سامان|صادرات|تجارت|رسالت|ویپاد|مسکن|رفاه|پارسیان|کشاورزی|گردشگری|شهر|سینا|آینده|دی|کارآفرین)""")
        val match = cardBankRegex.find(speech) ?: return "بانک"
        return when (val raw = match.groupValues[1]) {
            "ملی" -> "ملی ایران"
            "صادرات" -> "صادرات ایران"
            "رسالت" -> "قرض‌الحسنه رسالت"
            "رفاه" -> "رفاه کارگران"
            else -> raw
        }
    }

    private fun parseSingleVoiceClause(
        rawSpeech: String,
        fallbackTotalToman: Long? = null,
        defaultBankName: String = "نقدی / دستی"
    ): ParsedVoiceExpense {
        val cleaned = stripStumbledNumbers(PersianNumberUtils.normalizePersianText(rawSpeech.trim()))
        val isTaggingExistingSms = fallbackTotalToman != null && fallbackTotalToman > 0L

        val speechBeforeDong = if (cleaned.contains("که") && (cleaned.contains("دنگ") || cleaned.contains("دُنگ") || cleaned.contains("سهم"))) {
            cleaned.substringBefore("که").trim()
        } else {
            cleaned
        }

        // 1. Parse Total Amount in Toman (takes largest explicit currency amount in the clause)
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
            val spokenBank = detectSpokenCardBankInVoice(speechBeforeDong)
            if (spokenBank != "بانک") spokenBank else defaultBankName.takeIf { it.isNotBlank() && it != "بانک" } ?: "نقدی / دستی"
        }
        val cardLabel = if (effectiveBankName == "نقدی / دستی") "نقدی / دستی" else "کارت $effectiveBankName"

        // 4. Protect item quantities (after stripping stammered mental-math chains)
        val protectedQuantities = protectItemQuantities(speechBeforeDong)
        var corePhrase = protectedQuantities.protectedText

        exactPhraseAmounts.forEach { (phrase, _) ->
            corePhrase = corePhrase.replace(phrase, " ")
        }
        // Strip numeric digits (outside ⟪QTY...⟫ placeholders) + optional commas + optional هزار/میلیون + optional تومان/تومن/تومانش/تومنش
        corePhrase = corePhrase.replace(
            Regex("""[۰-۹0-9][۰-۹0-9,٬،]*\s*(?:میلیارد|میلیون|ملیون|هزارش|هزار)?\s*(?:تومانش|تومنش|تومان|تومن|ریالش|ریال)?"""),
            " "
        )
        // Strip spoken number words followed by هزار/میلیون/تومن/تومان/تومانش/تومنش
        val numberWordsPattern = numberWordTokens.keys.sortedByDescending { it.length }.joinToString("|")
        corePhrase = corePhrase.replace(
            Regex("""(?<=^|\s)(?:(?:$numberWordsPattern)(?:\s+و\s+(?:$numberWordsPattern))*)\s*(?:میلیارد|میلیون|ملیون|هزارش|هزار)?\s*(?:تومانش|تومنش|تومان|تومن|ریالش|ریال)(?=\s|$)"""),
            " "
        )
        corePhrase = corePhrase.replace(
            Regex("""(?<=^|\s)(?:(?:$numberWordsPattern)(?:\s+و\s+(?:$numberWordsPattern))*)\s+(?:میلیارد|میلیون|ملیون|هزارش|هزار)(?=\s|$)"""),
            " "
        )

        corePhrase = corePhrase
            .replace(Regex("""با\s+کارت\s+[^\s]+"""), " ")
            .replace(Regex("""کارت\s+(?:بلو|ملت|ملی|سپه|پاسارگاد|سامان|صادرات|تجارت|رسالت|ویپاد|مسکن|رفاه)"""), " ")

        val explicitFromPattern = Regex("""(.+?)\s+از\s+(.+)""")
        val explicitMatch = explicitFromPattern.find(corePhrase.trim())

        val storeName: String
        val rawPurposeCategory: String
        val detectedBrand = knownBrandsAndStores.firstOrNull { cleaned.contains(it.first) }?.second.orEmpty()

        if (explicitMatch != null) {
            val leftPart = trimLeadingPrepositionsAndVav(
                protectedQuantities.restore(stripGreetingsAndFillers(explicitMatch.groupValues[1]))
            )
            val rightStore = trimLeadingPrepositionsAndVav(
                protectedQuantities.restore(stripGreetingsAndFillers(explicitMatch.groupValues[2]))
            )
            val matchedKnownStore = knownBrandsAndStores.firstOrNull { rightStore.contains(it.first) }?.second
                ?: rightStore.ifBlank { effectiveBankName }
            storeName = if (isTaggingExistingSms) effectiveBankName else matchedKnownStore
            rawPurposeCategory = if (isTaggingExistingSms && matchedKnownStore.isNotBlank() && leftPart != matchedKnownStore) {
                "${leftPart.ifBlank { matchedKnownStore }} از $matchedKnownStore"
            } else {
                leftPart.ifBlank { matchedKnownStore }
            }
        } else {
            val stripped = trimLeadingPrepositionsAndVav(
                protectedQuantities.restore(stripGreetingsAndFillers(corePhrase))
            )
            storeName = if (isTaggingExistingSms) {
                effectiveBankName
            } else {
                detectedBrand.ifBlank { effectiveBankName }
            }
            rawPurposeCategory = if (stripped.isNotBlank()) {
                stripped
            } else if (isTaggingExistingSms) {
                trimLeadingPrepositionsAndVav(
                    protectedQuantities.restore(stripGreetingsAndFillers(protectedQuantities.protectedText))
                )
            } else {
                ""
            }
        }

        val purposeCategory = trimLeadingPrepositionsAndVav(rawPurposeCategory)
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


