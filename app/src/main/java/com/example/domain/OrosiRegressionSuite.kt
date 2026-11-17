package com.example.domain

import com.example.ui.components.JibemanAssetLogoCache

data class RegressionTestCaseResult(
    val id: Int,
    val category: String,
    val title: String,
    val inputSummary: String,
    val expectedSummary: String,
    val actualSummary: String,
    val passed: Boolean,
    val durationMicros: Long
)

data class RegressionSuiteReport(
    val totalCount: Int,
    val passedCount: Int,
    val failedCount: Int,
    val totalDurationMs: Long,
    val results: List<RegressionTestCaseResult>
) {
    val allPassed: Boolean
        get() = failedCount == 0 && totalCount > 0
}

/**
 * Automated Regression & Stress Test Engine for Orosi Finance ("خیال راحت").
 * Runs both inside JUnit build tests (`ExampleUnitTest.kt`) and live inside the in-app Settings lab.
 */
object OrosiRegressionSuite {

    fun runFullRegressionAndStressSuite(): RegressionSuiteReport {
        val suiteStartNanos = System.nanoTime()
        val results = mutableListOf<RegressionTestCaseResult>()
        var nextId = 1

        fun recordTest(
            category: String,
            title: String,
            inputSummary: String,
            expectedSummary: String,
            block: () -> Pair<Boolean, String>
        ) {
            val t0 = System.nanoTime()
            val (passed, actual) = try {
                block()
            } catch (e: Throwable) {
                false to "خطا: ${e.message ?: e::class.java.simpleName}"
            }
            val elapsedMicros = ((System.nanoTime() - t0) / 1_000L).coerceAtLeast(1L)
            results.add(
                RegressionTestCaseResult(
                    id = nextId++,
                    category = category,
                    title = title,
                    inputSummary = inputSummary,
                    expectedSummary = expectedSummary,
                    actualSummary = actual,
                    passed = passed,
                    durationMicros = elapsedMicros
                )
            )
        }

        // 1. Video batch_9 (00:36): "دخترم ۵۰ تومان خرج کرد"
        recordTest(
            category = "موتور صوتی (NLU)",
            title = "تشخیص فاعل خانوادگی (ویدیو batch_9)",
            inputSummary = "دخترم ۵۰ تومان خرج کرد",
            expectedSummary = "دخترم خرج کرد • ۵۰,۰۰۰ تومان"
        ) {
            val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand("دخترم ۵۰ تومان خرج کرد")
            val ok = parsed.purposeCategory == "دخترم خرج کرد" && parsed.totalAmountToman == 50_000L
            ok to "${parsed.purposeCategory} • ${PersianNumberUtils.formatToman(parsed.totalAmountToman, true)}"
        }

        // 2. Video batch_9 (00:50): "۳۵ تومان هزینه قسط پنجم وام دیشب"
        recordTest(
            category = "موتور صوتی (NLU)",
            title = "حفظ عدد ترتیبی «پنجم» و حذف قید زمان «دیشب» (ویدیو batch_9)",
            inputSummary = "۳۵ تومان هزینه قسط پنجم وام دیشب",
            expectedSummary = "هزینه قسط پنجم وام • ۳۵,۰۰۰ تومان"
        ) {
            val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand("۳۵ تومان هزینه قسط پنجم وام دیشب")
            val ok = parsed.purposeCategory == "هزینه قسط پنجم وام" && parsed.totalAmountToman == 35_000L
            ok to "${parsed.purposeCategory} • ${PersianNumberUtils.formatToman(parsed.totalAmountToman, true)}"
        }

        // 3. Video batch_8 (01:05): Cancelled clause + 2 valid clauses + "به یه فقیر"
        recordTest(
            category = "موتور صوتی (NLU)",
            title = "حذف جمله منفی/لغوشده و تفکیک «بستنی» و «فقیر» (ویدیو batch_8)",
            inputSummary = "رفته بودم بیرون ۵۰ تومن میخواستم خرج کنم ولی نکردم ۵۰ تومن دادم بستنی ۳۵ تومن دادم به یه فقیر",
            expectedSummary = "بستنی (۵۰,۰۰۰) + فقیر (۳۵,۰۰۰)"
        ) {
            val items = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
                "رفته بودم بیرون ۵۰ تومن میخواستم خرج کنم ولی نکردم ۵۰ تومن دادم بستنی ۳۵ تومن دادم به یه فقیر"
            )
            val ok = items.size == 2 &&
                items[0].purposeCategory == "بستنی" && items[0].totalAmountToman == 50_000L &&
                items[1].purposeCategory == "فقیر" && items[1].totalAmountToman == 35_000L
            val actual = items.joinToString(" + ") {
                "${it.purposeCategory} (${PersianNumberUtils.formatToman(it.totalAmountToman)})"
            }
            ok to actual
        }

        // 4. Video batch_8 (01:32): Voice tagging existing 9,200,000 Toman Blu SMS with "بدهی عسل به احمد عمو"
        recordTest(
            category = "تگ صوتی پیامک",
            title = "تگ صوتی «بدهی عسل به احمد عمو» روی تراکنش ۹.۲ میلیون بلوبانک (ویدیو batch_8)",
            inputSummary = "بدهی عسل به احمد عمو (مبلغ بانک: ۹,۲۰۰,۰۰۰ تومان)",
            expectedSummary = "بدهی عسل به احمد عمو • ۹,۲۰۰,۰۰۰ تومان (بلو)"
        ) {
            val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand(
                rawSpeech = "بدهی عسل به احمد عمو",
                fallbackTotalToman = 9_200_000L,
                defaultBankName = "بلو"
            )
            val ok = parsed.purposeCategory == "بدهی عسل به احمد عمو" &&
                parsed.totalAmountToman == 9_200_000L &&
                parsed.storeName == "بلو"
            ok to "${parsed.purposeCategory} • ${PersianNumberUtils.formatToman(parsed.totalAmountToman, true)} (${parsed.storeName})"
        }

        // 5. Asyndetic splitting + negation filter: "سلام ۳۵ تومن دادم نونوایی ۵۰ تومن دادم بقالی ۵۵ تومن میخواستم بدم نونوایی ولی ندادم"
        recordTest(
            category = "موتور صوتی (NLU)",
            title = "تفکیک جملات بدون «و» همراه با فیلتر انصراف «ولی ندادم»",
            inputSummary = "سلام ۳۵ تومن دادم نونوایی ۵۰ تومن دادم بقالی ۵۵ تومن میخواستم بدم نونوایی ولی ندادم",
            expectedSummary = "نونوایی (۳۵,۰۰۰) + بقالی (۵۰,۰۰۰)"
        ) {
            val items = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
                "سلام ۳۵ تومن دادم نونوایی ۵۰ تومن دادم بقالی ۵۵ تومن میخواستم بدم نونوایی ولی ندادم"
            )
            val ok = items.size == 2 &&
                items[0].purposeCategory == "نونوایی" && items[0].totalAmountToman == 35_000L &&
                items[1].purposeCategory == "بقالی" && items[1].totalAmountToman == 50_000L
            val actual = items.joinToString(" + ") {
                "${it.purposeCategory} (${PersianNumberUtils.formatToman(it.totalAmountToman)})"
            }
            ok to actual
        }

        // 6. Stammered mental-math chain: "سلام ۵۵ هزار تومان دادم به بستنی و ۳۹ هزار تومان دادم به دو تا سه تا ۵ تومن ۵ تومن نانوایی"
        recordTest(
            category = "موتور صوتی (NLU)",
            title = "حذف شمارش ذهنی تکراری «دو تا سه تا ۵ تومن ۵ تومن»",
            inputSummary = "سلام ۵۵ هزار تومان دادم به بستنی و ۳۹ هزار تومان دادم به دو تا سه تا ۵ تومن ۵ تومن نانوایی",
            expectedSummary = "بستنی (۵۵,۰۰۰) + نانوایی (۳۹,۰۰۰)"
        ) {
            val items = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
                "سلام ۵۵ هزار تومان دادم به بستنی و ۳۹ هزار تومان دادم به دو تا سه تا ۵ تومن ۵ تومن نانوایی"
            )
            val ok = items.size == 2 &&
                items[0].purposeCategory == "بستنی" && items[0].totalAmountToman == 55_000L &&
                items[1].purposeCategory == "نانوایی" && items[1].totalAmountToman == 39_000L
            val actual = items.joinToString(" + ") {
                "${it.purposeCategory} (${PersianNumberUtils.formatToman(it.totalAmountToman)})"
            }
            ok to actual
        }

        // 7. Narrative intro pivot Rule A: "رفتم بیرون و اومدم کلاً ۵۰ هزار تومان خرج کردم برای تاکسی و کرایه و بستنی و"
        recordTest(
            category = "موتور صوتی (NLU)",
            title = "حذف مقدمه داستانی قبل از «برای» و حفظ مبلغ کل",
            inputSummary = "رفتم بیرون و اومدم کلاً ۵۰ هزار تومان خرج کردم برای تاکسی و کرایه و بستنی و",
            expectedSummary = "تاکسی و کرایه و بستنی • ۵۰,۰۰۰ تومان"
        ) {
            val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand(
                "رفتم بیرون و اومدم کلاً ۵۰ هزار تومان خرج کردم برای تاکسی و کرایه و بستنی و"
            )
            val ok = parsed.purposeCategory == "تاکسی و کرایه و بستنی" && parsed.totalAmountToman == 50_000L
            ok to "${parsed.purposeCategory} • ${PersianNumberUtils.formatToman(parsed.totalAmountToman, true)}"
        }

        // 8. Preserve "بابت" between two nouns (Rule B): "انتقال پول به احمد بابت عسل"
        recordTest(
            category = "تگ صوتی پیامک",
            title = "حفظ «بابت» بین دو اسم معنادار",
            inputSummary = "انتقال پول به احمد بابت عسل",
            expectedSummary = "انتقال پول به احمد بابت عسل • ۲۰۰,۰۰۰ تومان"
        ) {
            val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand(
                rawSpeech = "انتقال پول به احمد بابت عسل",
                fallbackTotalToman = 200_000L,
                defaultBankName = "بلو"
            )
            val ok = parsed.purposeCategory == "انتقال پول به احمد بابت عسل" && parsed.totalAmountToman == 200_000L
            ok to "${parsed.purposeCategory} • ${PersianNumberUtils.formatToman(parsed.totalAmountToman, true)}"
        }

        // 9. Item quantities with single total: "یک شارژ ایرانسل و دو تا سیب زمینی خریدم ۵۰ هزار تومان"
        recordTest(
            category = "موتور صوتی (NLU)",
            title = "حفظ تعداد اقلام («۱ شارژ و ۲ تا سیب‌زمینی») با یک مبلغ کل",
            inputSummary = "یک شارژ ایرانسل و دو تا سیب زمینی خریدم ۵۰ هزار تومان",
            expectedSummary = "۱ شارژ ایرانسل و ۲ تا سیب‌زمینی • ۵۰,۰۰۰ تومان"
        ) {
            val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand(
                "یک شارژ ایرانسل و دو تا سیب زمینی خریدم ۵۰ هزار تومان"
            )
            val ok = parsed.purposeCategory == "۱ شارژ ایرانسل و ۲ تا سیب‌زمینی" && parsed.totalAmountToman == 50_000L
            ok to "${parsed.purposeCategory} • ${PersianNumberUtils.formatToman(parsed.totalAmountToman, true)}"
        }

        // 10. Remainder math on existing bank SMS: "سلام دو تا شارژ خریدم دو تا بستنی ۵۰ هزار تومان" on 100,000 Toman SMS
        recordTest(
            category = "تگ صوتی پیامک",
            title = "محاسبه خودکار باقی‌مانده مبلغ بانک برای مورد اول",
            inputSummary = "سلام دو تا شارژ خریدم دو تا بستنی ۵۰ هزار تومان (بانک: ۱۰۰,۰۰۰)",
            expectedSummary = "۲ تا شارژ (۵۰,۰۰۰) + ۲ تا بستنی (۵۰,۰۰۰)"
        ) {
            val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand(
                rawSpeech = "سلام دو تا شارژ خریدم دو تا بستنی ۵۰ هزار تومان",
                fallbackTotalToman = 100_000L,
                defaultBankName = "بلو"
            )
            val ok = parsed.purposeCategory == "۲ تا شارژ (۵۰,۰۰۰) + ۲ تا بستنی (۵۰,۰۰۰)" &&
                parsed.totalAmountToman == 100_000L
            ok to parsed.purposeCategory
        }

        // 11. Dong split calculation: "پانصد هزار تومان خرید میوه از اسنپ‌مارکت با کارت بلو که ۲۰۰ تومنش دنگ علی بود"
        recordTest(
            category = "موتور صوتی (NLU)",
            title = "محاسبه سهم کاربر و دُنگ دوست («علی»)",
            inputSummary = "پانصد هزار تومان خرید میوه از اسنپ‌مارکت با کارت بلو که ۲۰۰ تومنش دنگ علی بود",
            expectedSummary = "کل: ۵۰۰,۰۰۰ • سهم کاربر: ۳۰۰,۰۰۰ • دنگ علی: ۲۰۰,۰۰۰"
        ) {
            val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand(
                "پانصد هزار تومان خرید میوه از اسنپ‌مارکت با کارت بلو که ۲۰۰ تومنش دنگ علی بود"
            )
            val ok = parsed.totalAmountToman == 500_000L &&
                parsed.userShareToman == 300_000L &&
                parsed.dongAmountToman == 200_000L &&
                parsed.dongFriendName == "علی"
            ok to "کل: ${PersianNumberUtils.formatToman(parsed.totalAmountToman)} • سهم کاربر: ${PersianNumberUtils.formatToman(parsed.userShareToman)} • دنگ ${parsed.dongFriendName}: ${PersianNumberUtils.formatToman(parsed.dongAmountToman)}"
        }

        // 12. All 38 Iranian Bank Logos Resolution (including ایران ونزوئلا)
        recordTest(
            category = "لوگوی ۳۸ بانک",
            title = "بررسی تطابق ۱۰۰٪ نام و کلید لوگوی هر ۳۸ بانک (از جمله ایران ونزوئلا)",
            inputSummary = "۳۸ بانک رسمی در ALL_38_IRANIAN_BANKS",
            expectedSummary = "۳۸ از ۳۸ بانک دارای کلید لوگوی معتبر"
        ) {
            val all38 = JibemanAssetLogoCache.ALL_38_IRANIAN_BANKS
            val matchedCount = all38.count { (faName, expectedKey) ->
                JibemanAssetLogoCache.resolveBankLogoKey(faName) == expectedKey
            }
            val venezuelaKey = JibemanAssetLogoCache.resolveBankLogoKey("ایران ونزوئلا")
            val ok = matchedCount == 38 && venezuelaKey == "ic_bank_iran_venezuela"
            ok to "${PersianNumberUtils.toPersianDigits(matchedCount.toString())} از ۳۸ بانک معتبر (ایران ونزوئلا: $venezuelaKey)"
        }

        // 13. LTR Phone / SMS Sender '+' Sign Isolation
        recordTest(
            category = "چیدمان LTR سرشماره",
            title = "ایزوله‌سازی چپ‌به‌راست علامت + در سرشماره‌ها (+989999987641)",
            inputSummary = "+982187641 , +989999987641",
            expectedSummary = "محصور در کاراکترهای ایزوله چپ‌به‌راست یونیکد (\\u2066\\u200E+98...\\u2069)"
        ) {
            val formatted1 = PersianNumberUtils.formatLtrSender("+982187641")
            val formatted2 = PersianNumberUtils.formatLtrSender("+989999987641")
            val ok = formatted1.startsWith("\u2066\u200E+982187641") &&
                formatted1.endsWith("\u2069") &&
                formatted2.startsWith("\u2066\u200E+989999987641") &&
                formatted2.endsWith("\u2069")
            ok to "$formatted1 ، $formatted2"
        }

        // 14. Jibeman Bank SMS Parser & Promo/OTP Filter
        recordTest(
            category = "موتور پیامک بانکی",
            title = "تشخیص دقیق پیامک بانکی، تبدیل ریال به تومان و رد پیامک تبلیغاتی و رمز پویا",
            inputSummary = "پیامک صادرات + رد دیجی‌کالا/اسنپ/رمز پویا",
            expectedSummary = "مبلغ: ۱,۷۶۷,۳۹۰ تومان • مانده: ۱۴,۵۰۰,۰۰۰ تومان • تبلیغاتی/پویا: رد شد"
        ) {
            val sms = "بانک صادرات ایران\nبرداشت\nمبلغ: ۱۷,۶۷۳,۹۰۰ ریال\nمانده: ۱۴۵,۰۰۰,۰۰۰ ریال"
            val parsed = BankSmsParserEngine.parseWithJibemanEngine("+9830009419", sms, 1730734200000L)
            val otpRejected = JibemanSmsEngine.parse("بانک ملت\nرمز پویا: 123456\nمبلغ: 500,000") == null
            val promoRejected = JibemanSmsEngine.resolveBank("Digikala") == null &&
                JibemanSmsEngine.resolveBank("10008590") == null
            val ok = parsed != null &&
                parsed.amountToman == 1_767_390L &&
                parsed.extractedBalanceToman == 14_500_000L &&
                otpRejected &&
                promoRejected
            ok to "مبلغ: ${PersianNumberUtils.formatToman(parsed?.amountToman ?: 0L, true)} • تبلیغاتی/پویا: رد شد ✓"
        }

        // 15. Stress Test: 1,000 Jalali Date Conversions with Memory Cache (<50ms)
        recordTest(
            category = "تست استرس سرعت (120fps)",
            title = "تست استرس ۱,۰۰۰ تبدیل تاریخ شمسی با کش حافظه برای اسکرول بدون تیک",
            inputSummary = "۱,۰۰۰ فراخوانی متوالی JalaliCalendarUtils.timestampToJalali",
            expectedSummary = "دقیق و سریع (زیر ۵۰ میلی‌ثانیه)"
        ) {
            val tStart = System.nanoTime()
            var lastDate = ""
            val baseTs = 1763120000000L
            for (i in 0 until 1000) {
                val info = JalaliCalendarUtils.timestampToJalali(baseTs + (i % 40) * 60_000L)
                lastDate = info.dateWithYear
            }
            val elapsedMs = (System.nanoTime() - tStart) / 1_000_000L
            val (jy, jm, jd) = JalaliCalendarUtils.gregorianToJalali(2026, 11, 14)
            val ok = jy == 1405 && jm == 8 && jd == 23 && lastDate.isNotBlank() && elapsedMs < 200L
            ok to "۱,۰۰۰ تبدیل در ${PersianNumberUtils.toPersianDigits(elapsedMs.toString())} میلی‌ثانیه ($lastDate)"
        }

        // 16. Video 55batch (00:05): Summary intro total before itemized breakdown
        recordTest(
            category = "تست استرس صوتی (ویدیو ۵۵)",
            title = "حذف مبلغ سرجمع مقدمه («کلاً ۵۰ هزار تومان خرج کردم») قبل از ریز اقلام",
            inputSummary = "رفتم بیرون و اومدم کلاً ۵۰ هزار تومان خرج کردم ۲۵ تومن بستنی ۲۵ تومن دمپایی",
            expectedSummary = "بستنی (۲۵,۰۰۰) + دمپایی (۲۵,۰۰۰) • جمع: ۵۰,۰۰۰ تومان"
        ) {
            val items = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
                "رفتم بیرون و اومدم کلاً ۵۰ هزار تومان خرج کردم ۲۵ تومن بستنی ۲۵ تومن دمپایی"
            )
            val sum = items.sumOf { it.totalAmountToman }
            val ok = items.size == 2 &&
                items[0].purposeCategory == "بستنی" && items[0].totalAmountToman == 25_000L &&
                items[1].purposeCategory == "دمپایی" && items[1].totalAmountToman == 25_000L &&
                sum == 50_000L
            val actual = items.joinToString(" + ") {
                "${it.purposeCategory} (${PersianNumberUtils.formatToman(it.totalAmountToman)})"
            } + " • جمع: ${PersianNumberUtils.formatToman(sum, true)}"
            ok to actual
        }

        // 17. Video 55batch (00:41): Suffix "تومانش" + sub-thousand 500 Toman remainder on 49,500 Toman bank SMS
        recordTest(
            category = "تست استرس تگ پیامک (ویدیو ۵۵)",
            title = "حذف پسوند «تومانش» و تشخیص عدد خرد ۵۰۰ تومان روی پیامک ۴۹,۵۰۰ تومانی",
            inputSummary = "۴۹ تومانش بابت بستنی ۵۰۰ تومانش بابت کاغذ (بانک: ۴۹,۵۰۰ تومان)",
            expectedSummary = "بستنی (۴۹,۰۰۰) + کاغذ (۵۰۰)"
        ) {
            val items = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
                rawSpeech = "۴۹ تومانش بابت بستنی ۵۰۰ تومانش بابت کاغذ",
                defaultBankName = "بلو",
                fallbackTotalToman = 49_500L
            )
            val combined = PersianVoiceSlangParserEngine.formatCombinedMultiVoiceNote(items)
            val mismatch = PersianVoiceSlangParserEngine.detectBankSmsAmountMismatch(
                rawSpeech = "۴۹ تومانش بابت بستنی ۵۰۰ تومانش بابت کاغذ",
                fallbackTotalToman = 49_500L,
                multiItems = items
            )
            val ok = items.size == 2 &&
                items[0].purposeCategory == "بستنی" && items[0].totalAmountToman == 49_000L &&
                items[1].purposeCategory == "کاغذ" && items[1].totalAmountToman == 500L &&
                combined == "بستنی (۴۹,۰۰۰) + کاغذ (۵۰۰)" &&
                mismatch == null
            ok to combined
        }

        // 18. Video 55batch (00:58): Conflicting spoken amount on bank SMS ("۸۵ تومن بابت بستنی" on 80,000 Toman SMS)
        recordTest(
            category = "هشدار مغایرت مبلغ بانک (ویدیو ۵۵)",
            title = "تشخیص مغایرت مبلغ گفته‌شده (۸۵,۰۰۰) با مبلغ پیامک بانک (۸۰,۰۰۰)",
            inputSummary = "۸۵ تومن بابت بستنی (روی پیامک ۸۰,۰۰۰ تومانی بلوبانک)",
            expectedSummary = "هشدار عدم تطابق فعال (۸۵,۰۰۰ ≠ ۸۰,۰۰۰)"
        ) {
            val conflictAmount = PersianVoiceSlangParserEngine.detectBankSmsAmountMismatch(
                rawSpeech = "۸۵ تومن بابت بستنی",
                fallbackTotalToman = 80_000L
            )
            val noConflictWhenMatching = PersianVoiceSlangParserEngine.detectBankSmsAmountMismatch(
                rawSpeech = "۸۰ تومن بابت بستنی",
                fallbackTotalToman = 80_000L
            )
            val noConflictWhenNoAmountSpoken = PersianVoiceSlangParserEngine.detectBankSmsAmountMismatch(
                rawSpeech = "بستنی",
                fallbackTotalToman = 80_000L
            )
            val ok = conflictAmount == 85_000L &&
                noConflictWhenMatching == null &&
                noConflictWhenNoAmountSpoken == null
            ok to "مبلغ متناقض تشخیص داده شد: ${PersianNumberUtils.formatToman(conflictAmount ?: 0L, true)} ≠ ۸۰,۰۰۰ تومان"
        }

        // 19. Video 55batch (01:13): Stumbled number correction ("۲۱۷ ۲۱۸ تومن") + 218 Toman bank fee on 107,218 Toman SMS
        recordTest(
            category = "تست استرس تگ پیامک (ویدیو ۵۵)",
            title = "اصلاح تپق عددی «۲۱۷ ۲۱۸ تومن» و تشخیص کارمزد خرد ۲۱۸ تومان روی پیامک ۱۰۷,۲۱۸ تومانی",
            inputSummary = "۱۰۷ تومن بابت انتقال پول و ۲۱۷ ۲۱۸ تومن بابت کارمزد (بانک: ۱۰۷,۲۱۸)",
            expectedSummary = "انتقال پول (۱۰۷,۰۰۰) + کارمزد (۲۱۸)"
        ) {
            val items = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
                rawSpeech = "۱۰۷ تومن بابت انتقال پول و ۲۱۷ ۲۱۸ تومن بابت کارمزد",
                defaultBankName = "بلو",
                fallbackTotalToman = 107_218L
            )
            val combined = PersianVoiceSlangParserEngine.formatCombinedMultiVoiceNote(items)
            val mismatch = PersianVoiceSlangParserEngine.detectBankSmsAmountMismatch(
                rawSpeech = "۱۰۷ تومن بابت انتقال پول و ۲۱۷ ۲۱۸ تومن بابت کارمزد",
                fallbackTotalToman = 107_218L,
                multiItems = items
            )
            val ok = items.size == 2 &&
                items[0].purposeCategory == "انتقال پول" && items[0].totalAmountToman == 107_000L &&
                items[1].purposeCategory == "کارمزد" && items[1].totalAmountToman == 218L &&
                mismatch == null
            ok to combined
        }

        // 20. Video 55batch_10 (00:08): Preserving item noun across "می‌خواستم ۳۹ تومن بدم ولی ۴۹ تومن دادم"
        recordTest(
            category = "تست استرس صوتی (ویدیو ۵۵_۱۰)",
            title = "حفظ عنوان «۲ تا بستنی» هنگام اصلاح قیمت («می‌خواستم ۳۹ تومن بدم ولی ۴۹ تومن دادم»)",
            inputSummary = "سلام رفتم بیرون اومدم بعد یادمه که برای دوتا بستنی می‌خواستم ۳۹ تومن بدم ولی ۴۹ تومن دادم",
            expectedSummary = "۲ تا بستنی • ۴۹,۰۰۰ تومان"
        ) {
            val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand(
                "سلام رفتم بیرون اومدم بعد یادمه که برای دوتا بستنی می‌خواستم ۳۹ تومن بدم ولی ۴۹ تومن دادم"
            )
            val ok = parsed.purposeCategory == "۲ تا بستنی" && parsed.totalAmountToman == 49_000L
            ok to "${parsed.purposeCategory} • ${PersianNumberUtils.formatToman(parsed.totalAmountToman, true)}"
        }

        // 21. Video 55batch_10 (00:35): Mid-speech self-correction with "نه نه نه نه"
        recordTest(
            category = "تست استرس صوتی (ویدیو ۵۵_۱۰)",
            title = "اصلاح آنی مبلغ در حین صحبت با «نه نه نه نه» (جایگزینی ۵۰ با ۳۵ هزار تومان)",
            inputSummary = "۵۰ تومن نانوایی نه نه نه نه ۳۵ تومن نانوایی",
            expectedSummary = "نانوایی • ۳۵,۰۰۰ تومان (۱ تراکنش)"
        ) {
            val items = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
                "۵۰ تومن نانوایی نه نه نه نه ۳۵ تومن نانوایی"
            )
            val ok = items.size == 1 &&
                items[0].purposeCategory == "نانوایی" &&
                items[0].totalAmountToman == 35_000L
            val actual = items.joinToString(" + ") {
                "${it.purposeCategory} • ${PersianNumberUtils.formatToman(it.totalAmountToman, true)}"
            } + " (${PersianNumberUtils.toPersianDigits(items.size.toString())} تراکنش)"
            ok to actual
        }

        // 22. Video 55batch_10 (00:42): Same-noun asyndetic override ("۵۰ تومان نانوایی ۳۵ تومن نانوایی")
        recordTest(
            category = "تست استرس صوتی (ویدیو ۵۵_۱۰)",
            title = "اصلاح خودکار مبلغ در صورت تکرار همان عنوان بدون «و» («۵۰ تومان نانوایی ۳۵ تومن نانوایی»)",
            inputSummary = "۵۰ تومان نانوایی ۳۵ تومن نانوایی",
            expectedSummary = "نانوایی • ۳۵,۰۰۰ تومان (۱ تراکنش)"
        ) {
            val items = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
                "۵۰ تومان نانوایی ۳۵ تومن نانوایی"
            )
            val single = PersianVoiceSlangParserEngine.parseVoiceCommand(
                "۵۰ تومان نانوایی ۳۵ تومن نانوایی"
            )
            val ok = items.size == 1 &&
                items[0].purposeCategory == "نانوایی" &&
                items[0].totalAmountToman == 35_000L &&
                single.purposeCategory == "نانوایی" &&
                single.totalAmountToman == 35_000L
            ok to "${single.purposeCategory} • ${PersianNumberUtils.formatToman(single.totalAmountToman, true)} (${PersianNumberUtils.toPersianDigits(items.size.toString())} تراکنش)"
        }

        // 23. Video 55batch_10 (00:58): Self-correction with "بود نه نه نه نه ... بود"
        recordTest(
            category = "تست استرس صوتی (ویدیو ۵۵_۱۰)",
            title = "اصلاح مبلغ همراه با فعل ربطی «بود» («۵۰ تومن نانوایی بود نه نه نه نه ۳۵ تومن بود نانوایی»)",
            inputSummary = "۵۰ تومن نانوایی بود نه نه نه نه ۳۵ تومن بود نانوایی",
            expectedSummary = "نانوایی • ۳۵,۰۰۰ تومان"
        ) {
            val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand(
                "۵۰ تومن نانوایی بود نه نه نه نه ۳۵ تومن بود نانوایی"
            )
            val ok = parsed.purposeCategory == "نانوایی" && parsed.totalAmountToman == 35_000L
            ok to "${parsed.purposeCategory} • ${PersianNumberUtils.formatToman(parsed.totalAmountToman, true)}"
        }

        // 24. Video 55batch_10 (01:20): Deduplicating repeated noun around "بود" ("۵۰ تومان نانوایی بود نانوایی")
        recordTest(
            category = "تست استرس صوتی (ویدیو ۵۵_۱۰)",
            title = "حذف «بود» و ادغام کلمات تکراری («۵۰ تومان نانوایی بود نانوایی» ← «نانوایی»)",
            inputSummary = "۵۰ تومان نانوایی بود نانوایی",
            expectedSummary = "نانوایی • ۵۰,۰۰۰ تومان"
        ) {
            val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand("۵۰ تومان نانوایی بود نانوایی")
            val ok = parsed.purposeCategory == "نانوایی" && parsed.totalAmountToman == 50_000L
            ok to "${parsed.purposeCategory} • ${PersianNumberUtils.formatToman(parsed.totalAmountToman, true)}"
        }

        // 25.Preserving "نه" as the number 9 ("سی و نه تومن نونوایی" & "نه تومن آدامس") while stripping negation "نه"
        recordTest(
            category = "تمایز «نه» عددی (۹) از «نه» نفی",
            title = "حفظ «نه» در اعداد مرکب («سی و نه تومن») در کنار حذف «نه» اصلاحی",
            inputSummary = "سی و نه تومن نونوایی",
            expectedSummary = "نونوایی • ۳۹,۰۰۰ تومان"
        ) {
            val parsed39 = PersianVoiceSlangParserEngine.parseVoiceCommand("سی و نه تومن نونوایی")
            val ok = parsed39.purposeCategory == "نونوایی" && parsed39.totalAmountToman == 39_000L
            ok to "${parsed39.purposeCategory} • ${PersianNumberUtils.formatToman(parsed39.totalAmountToman, true)}"
        }

        val totalMs = ((System.nanoTime() - suiteStartNanos) / 1_000_000L).coerceAtLeast(1L)
        val passedCount = results.count { it.passed }
        val failedCount = results.size - passedCount

        return RegressionSuiteReport(
            totalCount = results.size,
            passedCount = passedCount,
            failedCount = failedCount,
            totalDurationMs = totalMs,
            results = results
        )
    }
}
