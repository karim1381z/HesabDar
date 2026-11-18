package com.example

import com.example.domain.BankSmsParserEngine
import com.example.domain.JalaliCalendarUtils
import com.example.domain.JibemanSmsEngine
import com.example.domain.PersianNumberUtils
import com.example.domain.PersianVoiceSlangParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testJibemanResolveBankAndRejectPromoSenders() {
        // Non-bank senders (Digikala, Tapsi, Snapp, random promo) must be rejected (null)
        assertNull(JibemanSmsEngine.resolveBank("Digikala"))
        assertNull(JibemanSmsEngine.resolveBank("TAPSI"))
        assertNull(JibemanSmsEngine.resolveBank("Snapp"))
        assertNull(JibemanSmsEngine.resolveBank("10008590"))

        // Real Iranian bank senders must resolve to their exact BankProfile
        assertEquals("صادرات ایران", JibemanSmsEngine.resolveBank("+9830009419")?.nameFa)
        assertEquals("ملت", JibemanSmsEngine.resolveBank("Bank Mellat")?.nameFa)
        assertEquals("ملی ایران", JibemanSmsEngine.resolveBank("+9830009417")?.nameFa)
        assertEquals("بلو", JibemanSmsEngine.resolveBank("+982187641")?.nameFa)
        assertEquals("سپه", JibemanSmsEngine.resolveBank("+986715001")?.nameFa)
        assertEquals("سامان", JibemanSmsEngine.resolveBank("SamanBank")?.nameFa)
        assertEquals("پاسارگاد", JibemanSmsEngine.resolveBank("B.Pasargad")?.nameFa)
        assertEquals("ویپاد", JibemanSmsEngine.resolveBank("30009017")?.nameFa)
    }

    @Test
    fun testJibemanParseAndRialToTomanAndDedupKey() {
        val sender = "+9830009419"
        val sms = """
            بانک صادرات ایران
            برداشت
            مبلغ: ۱۷,۶۷۳,۹۰۰ ریال
            مانده: ۱۴۵,۰۰۰,۰۰۰ ریال
        """.trimIndent()
        val ts = 1730734200000L

        val result = BankSmsParserEngine.parseWithJibemanEngine(
            senderAddress = sender,
            body = sms,
            timestampMillis = ts
        )
        assertNotNull(result)
        result!!
        assertEquals("صادرات ایران", result.bankName)
        assertEquals(1_767_390L, result.amountToman)
        assertEquals(14_500_000L, result.extractedBalanceToman)
        assertEquals(false, result.isIncome)
        assertEquals(JibemanSmsEngine.dedupKey(sender, ts, sms), result.dedupKey)
    }

    @Test
    fun testJibemanRejectsOtpSms() {
        val otpSms = "بانک ملت\nرمز پویا: 123456\nمبلغ: 17,673,900"
        assertNull(JibemanSmsEngine.parse(otpSms))
    }

    @Test
    fun testVoiceTaggingNeverDefaultsToSnappMarketOrDailyPurchase() {
        // 1. User says "پفک خریدم" on a 100,000 Toman Blu transaction
        val pofak = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "پفک خریدم",
            fallbackTotalToman = 100_000L,
            defaultBankName = "بلو"
        )
        assertEquals("پفک", pofak.purposeCategory)
        assertEquals("بلو", pofak.storeName)
        assertFalse(pofak.storeName.contains("اسنپ"))
        assertFalse(pofak.purposeCategory.contains("خرید روزمره"))

        // 2. User says "شارژ ایرانسل" on a 49,500 Toman Sepah transaction -> purpose is "شارژ ایرانسل", bank remains "سپه"
        val irancell = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "شارژ ایرانسل",
            fallbackTotalToman = 49_500L,
            defaultBankName = "سپه"
        )
        assertEquals("شارژ ایرانسل", irancell.purposeCategory)
        assertEquals("سپه", irancell.storeName)
        assertEquals(49_500L, irancell.totalAmountToman)

        // 3. User says "بابت هاست پرداخت کردم"
        val host = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "بابت هاست پرداخت کردم",
            fallbackTotalToman = 320_000L,
            defaultBankName = "پاسارگاد"
        )
        assertEquals("هاست", host.purposeCategory)
        assertEquals("پاسارگاد", host.storeName)
    }

    @Test
    fun testVoiceSlangDongSplit() {
        val phrase = "پانصد هزار تومان خرید میوه از اسنپ‌مارکت با کارت بلو که ۲۰۰ تومنش دنگ علی بود"
        val parsed = PersianVoiceSlangParserEngine.parseSpokenText(phrase)
        assertEquals(500_000L, parsed.totalAmountToman)
        assertEquals(200_000L, parsed.dongAmountToman)
        assertEquals(300_000L, parsed.userShareToman)
        assertEquals("علی", parsed.dongFriendName)
        assertEquals("اسنپ‌مارکت", parsed.storeName)
    }

    @Test
    fun testSpokenPersianNumbersAndTomanMultiplier() {
        // 1-999 followed by تومن / تومان without هزار/میلیون = thousands of Tomans (* 1,000L)
        val p1 = PersianVoiceSlangParserEngine.parseSpokenText("۵۰ تومن نانوایی")
        assertEquals(50_000L, p1.totalAmountToman)
        assertEquals("نانوایی", p1.purposeCategory)
        assertEquals("نقدی / دستی", p1.cardLabel)

        val pNoonvaei = PersianVoiceSlangParserEngine.parseSpokenText("۳۹ تومن نونوایی")
        assertEquals(39_000L, pNoonvaei.totalAmountToman)
        assertEquals("نونوایی", pNoonvaei.purposeCategory)
        assertEquals("نقدی / دستی", pNoonvaei.cardLabel)

        val pNanvaei70 = PersianVoiceSlangParserEngine.parseSpokenText("۷۰ تومن نانوایی")
        assertEquals(70_000L, pNanvaei70.totalAmountToman)
        assertEquals("نانوایی", pNanvaei70.purposeCategory)

        val pNanvaeiSpoken70 = PersianVoiceSlangParserEngine.parseSpokenText("هفتاد تومن نانوایی")
        assertEquals(70_000L, pNanvaeiSpoken70.totalAmountToman)
        assertEquals("نانوایی", pNanvaeiSpoken70.purposeCategory)

        val p2 = PersianVoiceSlangParserEngine.parseSpokenText("۳۰۰ تومن میوه")
        assertEquals(300_000L, p2.totalAmountToman)
        assertEquals("نقدی / دستی", p2.storeName)

        val p3 = PersianVoiceSlangParserEngine.parseSpokenText("پنجاه تومن نانوایی")
        assertEquals(50_000L, p3.totalAmountToman)

        val p4 = PersianVoiceSlangParserEngine.parseSpokenText("یک میلیون و دویست هزار تومان خرید گوشت")
        assertEquals(1_200_000L, p4.totalAmountToman)
    }

    @Test
    fun testExistingBankSmsVoiceTaggingNeverChangesBankAmountOrBankName() {
        // Even if the user mentions a number while tagging an existing bank SMS, its bank amount and bank name stay locked
        val tagged = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "۵۰ تومن پفک و تنقلات",
            fallbackTotalToman = 100_000L,
            defaultBankName = "بلو"
        )
        assertEquals(100_000L, tagged.totalAmountToman)
        assertEquals("بلو", tagged.storeName)
    }

    @Test
    fun testGregorianToJalaliConversion() {
        val (jy, jm, jd) = JalaliCalendarUtils.gregorianToJalali(2024, 11, 4)
        assertEquals(1403, jy)
        assertEquals(8, jm)
        assertEquals(14, jd)

        // 1403 is a leap year (30 Esfand = 2025-03-20)
        val (jyLeap, jmLeap, jdLeap) = JalaliCalendarUtils.gregorianToJalali(2025, 3, 20)
        assertEquals(1403, jyLeap)
        assertEquals(12, jmLeap)
        assertEquals(30, jdLeap)

        // 1405 Nowruz = 2026-03-21
        val (jy1405, jm1405, jd1405) = JalaliCalendarUtils.gregorianToJalali(2026, 3, 21)
        assertEquals(1405, jy1405)
        assertEquals(1, jm1405)
        assertEquals(1, jd1405)

        // 2026-11-14 = 23 Aban 1405
        val (jyAban, jmAban, jdAban) = JalaliCalendarUtils.gregorianToJalali(2026, 11, 14)
        assertEquals(1405, jyAban)
        assertEquals(8, jmAban)
        assertEquals(23, jdAban)
    }

    @Test
    fun testMultiVoiceCommandsAndGreetingStripper() {
        // 1. Compound sentence: "۵۰۰ هزار نانوایی و ۳۰۰ هزار شارژ ایرانسل خریدم"
        val items1 = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
            "۵۰۰ هزار نانوایی و ۳۰۰ هزار شارژ ایرانسل خریدم"
        )
        assertEquals(2, items1.size)
        assertEquals("نانوایی", items1[0].title)
        assertEquals(500_000L, items1[0].amountToman)
        assertEquals("خوراکی و روزمره", items1[0].category)

        assertEquals("شارژ ایرانسل", items1[1].title)
        assertEquals(300_000L, items1[1].amountToman)
        assertEquals("قبض و شارژ", items1[1].category)
        assertEquals("ایرانسل", items1[1].merchant)

        // 2. Compound sentence with greetings and "خرید":
        // "سلام ۵۰۰ هزار تومان خرید نانوایی و ۳۰۰ هزار تومان خرید دمپایی"
        val items2 = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
            "سلام ۵۰۰ هزار تومان خرید نانوایی و ۳۰۰ هزار تومان خرید دمپایی"
        )
        assertEquals(2, items2.size)
        assertEquals("نانوایی", items2[0].title)
        assertEquals(500_000L, items2[0].amountToman)
        assertEquals("خوراکی و روزمره", items2[0].category)

        assertEquals("دمپایی", items2[1].title)
        assertEquals(300_000L, items2[1].amountToman)
        assertEquals("پوشاک و خرید", items2[1].category)

        // 3. Tagging a single existing bank SMS row with a multi-clause sentence
        val taggedCombined = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "۵۰۰ هزار نانوایی و ۳۰۰ هزار شارژ ایرانسل خریدم",
            fallbackTotalToman = 800_000L,
            defaultBankName = "بلو"
        )
        assertEquals("نانوایی (۵۰۰,۰۰۰) + شارژ ایرانسل (۳۰۰,۰۰۰)", taggedCombined.purposeCategory)
        assertEquals(800_000L, taggedCombined.totalAmountToman)
        assertEquals("بلو", taggedCombined.storeName)
    }

    @Test
    fun testItemQuantitiesAndRemainderMathAndSingleTotalMultiItems() {
        // Bug A & C: "یک شارژ ایرانسل و دو تا سیب زمینی خریدم ۵۰ هزار تومان" in "+ ثبت صوتی"
        // Must NOT split into 1,000 Toman and 2,000 Toman items; must keep quantities in title and total = 50,000
        val singleTotalCommand = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "یک شارژ ایرانسل و دو تا سیب زمینی خریدم ۵۰ هزار تومان",
            fallbackTotalToman = null,
            defaultBankName = "نقدی / دستی"
        )
        assertEquals(50_000L, singleTotalCommand.totalAmountToman)
        assertEquals("۱ شارژ ایرانسل و ۲ تا سیب‌زمینی", singleTotalCommand.purposeCategory)

        val multiCheck = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
            rawSpeech = "یک شارژ ایرانسل و دو تا سیب زمینی خریدم ۵۰ هزار تومان"
        )
        // Because only one clause has an explicit currency amount, it is NOT a multi-amount split
        assertEquals(1, multiCheck.size)
        assertEquals(50_000L, multiCheck[0].amountToman)
        assertEquals("۱ شارژ ایرانسل و ۲ تا سیب‌زمینی", multiCheck[0].title)

        // Bug B: Tagging an existing 100,000 Toman bank transaction via "بگو بابت چی؟"
        // User says: "سلام دو تا شارژ خریدم دو تا بستنی ۵۰ هزار تومان"
        // Part 2 ("۲ تا بستنی") = 50,000; Part 1 ("۲ تا شارژ") = 100,000 - 50,000 = 50,000!
        val remainderTagged = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "سلام دو تا شارژ خریدم دو تا بستنی ۵۰ هزار تومان",
            fallbackTotalToman = 100_000L,
            defaultBankName = "بلو"
        )
        assertEquals("۲ تا شارژ (۵۰,۰۰۰) + ۲ تا بستنی (۵۰,۰۰۰)", remainderTagged.purposeCategory)
        assertEquals(100_000L, remainderTagged.totalAmountToman)
        assertEquals("بلو", remainderTagged.storeName)
    }

    @Test
    fun testLiveAmountCommaFormattingAndParsing() {
        assertEquals("۱۰۰,۰۰۰", PersianNumberUtils.formatLiveAmountInput("100000"))
        assertEquals("۱۰,۰۰۰", PersianNumberUtils.formatLiveAmountInput("۱۰,۰۰۰"))
        assertEquals(10_000L, PersianNumberUtils.parseAmountInputToLong("۱۰,۰۰۰"))
        assertEquals(100_000L, PersianNumberUtils.parseAmountInputToLong("۱۰۰,۰۰۰ تومان"))
    }

    @Test
    fun testNaturalSpeechPivotRuleAAndPreserveBabatRuleB() {
        // Rule A: Narrative intro ("رفتم بیرون و اومدم کلاً ۵۰ هزار تومان خرج کردم برای تاکسی و کرایه و بستنی و")
        // -> discard narrative intro before "برای", keep 50,000 Tomans, strip trailing " و" -> "تاکسی و کرایه و بستنی"
        val ruleA = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "رفتم بیرون و اومدم کلاً ۵۰ هزار تومان خرج کردم برای تاکسی و کرایه و بستنی و",
            fallbackTotalToman = null,
            defaultBankName = "نقدی / دستی"
        )
        assertEquals(50_000L, ruleA.totalAmountToman)
        assertEquals("تاکسی و کرایه و بستنی", ruleA.purposeCategory)

        val ruleAMulti = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
            rawSpeech = "رفتم بیرون و اومدم کلاً ۵۰ هزار تومان خرج کردم برای تاکسی و کرایه و بستنی و"
        )
        assertEquals(1, ruleAMulti.size)
        assertEquals(50_000L, ruleAMulti[0].amountToman)
        assertEquals("تاکسی و کرایه و بستنی", ruleAMulti[0].title)

        // Rule B: Preserve "بابت" / "برای" when connecting two nouns ("انتقال پول به احمد بابت عسل")
        val ruleB = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "انتقال پول به احمد بابت عسل",
            fallbackTotalToman = 200_000L,
            defaultBankName = "بلو"
        )
        assertEquals("انتقال پول به احمد بابت عسل", ruleB.purposeCategory)
        assertEquals(200_000L, ruleB.totalAmountToman)
        assertEquals("بلو", ruleB.storeName)
    }

    @Test
    fun testStammeredMentalMathSpeechAndPrepositionStripping() {
        // Real-device test case from video_2 (00:48):
        // "سلام ۵۵ هزار تومان دادم به بستنی و ۳۹ هزار تومان دادم به دو تا سه تا ۵ تومن ۵ تومن نانوایی"
        val items = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
            "سلام ۵۵ هزار تومان دادم به بستنی و ۳۹ هزار تومان دادم به دو تا سه تا ۵ تومن ۵ تومن نانوایی"
        )
        assertEquals(2, items.size)
        assertEquals("بستنی", items[0].title)
        assertEquals(55_000L, items[0].amountToman)
        assertEquals("نانوایی", items[1].title)
        assertEquals(39_000L, items[1].amountToman)
    }

    @Test
    fun testAsyndeticClauseSplittingAndNegationCancellationFilter() {
        // Test case from user's video:
        // "سلام ۳۵ تومن دادم نونوایی ۵۰ تومن دادم بقالی ۵۵ تومن میخواستم بدم نونوایی ولی ندادم"
        val items = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
            "سلام ۳۵ تومن دادم نونوایی ۵۰ تومن دادم بقالی ۵۵ تومن میخواستم بدم نونوایی ولی ندادم"
        )
        assertEquals(2, items.size)
        assertEquals("نونوایی", items[0].title)
        assertEquals(35_000L, items[0].amountToman)
        assertEquals("بقالی", items[1].title)
        assertEquals(50_000L, items[1].amountToman)
    }

    @Test
    fun testRejectBodyTextPromoFalsePositiveBanks() {
        // Promo SMS mentioning "گردونه ملی گلد" from a non-bank sender must NOT be guessed as "ملی ایران"
        assertEquals("بانک", BankSmsParserEngine.detectIranianBankName("گردونه ملی گلد ۱۷,۴۳۰ ریال", "10008590"))
    }

    @Test
    fun testVideoBatch8AndBatch9VoiceScenariosAndIndefiniteRecipient() {
        // Video batch_9: "دخترم ۵۰ تومان خرج کرد"
        val daughter = PersianVoiceSlangParserEngine.parseVoiceCommand("دخترم ۵۰ تومان خرج کرد")
        assertEquals("دخترم خرج کرد", daughter.purposeCategory)
        assertEquals(50_000L, daughter.totalAmountToman)

        // Video batch_9: "۳۵ تومان هزینه قسط پنجم وام دیشب"
        val loan5th = PersianVoiceSlangParserEngine.parseVoiceCommand("۳۵ تومان هزینه قسط پنجم وام دیشب")
        assertEquals("هزینه قسط پنجم وام", loan5th.purposeCategory)
        assertEquals(35_000L, loan5th.totalAmountToman)

        // Video batch_8: "رفته بودم بیرون ۵۰ تومن میخواستم خرج کنم ولی نکردم ۵۰ تومن دادم بستنی ۳۵ تومن دادم به یه فقیر"
        val batch8Items = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
            "رفته بودم بیرون ۵۰ تومن میخواستم خرج کنم ولی نکردم ۵۰ تومن دادم بستنی ۳۵ تومن دادم به یه فقیر"
        )
        assertEquals(2, batch8Items.size)
        assertEquals("بستنی", batch8Items[0].purposeCategory)
        assertEquals(50_000L, batch8Items[0].totalAmountToman)
        assertEquals("فقیر", batch8Items[1].purposeCategory)
        assertEquals(35_000L, batch8Items[1].totalAmountToman)

        // Video batch_8: "بدهی عسل به احمد عمو" on 9,200,000 Toman Blu transaction
        val honeyDebt = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "بدهی عسل به احمد عمو",
            fallbackTotalToman = 9_200_000L,
            defaultBankName = "بلو"
        )
        assertEquals("بدهی عسل به احمد عمو", honeyDebt.purposeCategory)
        assertEquals(9_200_000L, honeyDebt.totalAmountToman)
        assertEquals("بلو", honeyDebt.storeName)
    }

    @Test
    fun testAll38IranianBankLogosIncludingIranVenezuelaAndLtrSenderFormatting() {
        val all38 = com.example.ui.components.JibemanAssetLogoCache.ALL_38_IRANIAN_BANKS
        assertEquals(38, all38.size)
        for ((faName, expectedKey) in all38) {
            val resolved = com.example.ui.components.JibemanAssetLogoCache.resolveBankLogoKey(faName)
            assertEquals("Logo key mismatch for $faName", expectedKey, resolved)
        }
        assertEquals(
            "ic_bank_iran_venezuela",
            com.example.ui.components.JibemanAssetLogoCache.resolveBankLogoKey("ایران ونزوئلا")
        )

        val ltrSender = PersianNumberUtils.formatLtrSender("+989999987641")
        assertEquals("\u2066\u200E+989999987641\u2069", ltrSender)
    }

    @Test
    fun testVideo55BatchAndBatch10StressScenarios() {
        // 1. Video 55batch (00:05): Summary intro total before itemized breakdown
        val summaryBreakdown = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
            "رفتم بیرون و اومدم کلاً ۵۰ هزار تومان خرج کردم ۲۵ تومن بستنی ۲۵ تومن دمپایی"
        )
        assertEquals(2, summaryBreakdown.size)
        assertEquals("بستنی", summaryBreakdown[0].purposeCategory)
        assertEquals(25_000L, summaryBreakdown[0].totalAmountToman)
        assertEquals("دمپایی", summaryBreakdown[1].purposeCategory)
        assertEquals(25_000L, summaryBreakdown[1].totalAmountToman)

        // 2. Video 55batch (00:41): Suffix "تومانش" + sub-thousand 500 Toman remainder on 49,500 Toman SMS
        val subThousand49500 = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "۴۹ تومانش بابت بستنی ۵۰۰ تومانش بابت کاغذ",
            fallbackTotalToman = 49_500L,
            defaultBankName = "بلو"
        )
        assertEquals("بستنی (۴۹,۰۰۰) + کاغذ (۵۰۰)", subThousand49500.purposeCategory)
        assertEquals(49_500L, subThousand49500.totalAmountToman)

        // 3. Video 55batch (00:58): Conflicting spoken amount on bank SMS ("۸۵ تومن بابت بستنی" on 80,000 Toman SMS)
        assertEquals(
            85_000L,
            PersianVoiceSlangParserEngine.detectBankSmsAmountMismatch(
                rawSpeech = "۸۵ تومن بابت بستنی",
                fallbackTotalToman = 80_000L
            )
        )
        assertNull(
            PersianVoiceSlangParserEngine.detectBankSmsAmountMismatch(
                rawSpeech = "بستنی",
                fallbackTotalToman = 80_000L
            )
        )

        // 4. Video 55batch (01:13): Stumbled number correction ("۲۱۷ ۲۱۸ تومن") + 218 Toman fee on 107,218 Toman SMS
        val stumbledFee = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "۱۰۷ تومن بابت انتقال پول و ۲۱۷ ۲۱۸ تومن بابت کارمزد",
            fallbackTotalToman = 107_218L,
            defaultBankName = "بلو"
        )
        assertEquals("انتقال پول (۱۰۷,۰۰۰) + کارمزد (۲۱۸)", stumbledFee.purposeCategory)
        assertEquals(107_218L, stumbledFee.totalAmountToman)

        // 5. Video 55batch_10 (00:08): Preserving item noun across "می‌خواستم ۳۹ تومن بدم ولی ۴۹ تومن دادم"
        val carriedIceCream = PersianVoiceSlangParserEngine.parseVoiceCommand(
            "سلام رفتم بیرون اومدم بعد یادمه که برای دوتا بستنی می‌خواستم ۳۹ تومن بدم ولی ۴۹ تومن دادم"
        )
        assertEquals("۲ تا بستنی", carriedIceCream.purposeCategory)
        assertEquals(49_000L, carriedIceCream.totalAmountToman)

        // 6. Video 55batch_10 (00:35, 00:42, 00:58, 01:20): Self-corrections and "بود" deduplication
        val nahCorrected = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
            "۵۰ تومن نانوایی نه نه نه نه ۳۵ تومن نانوایی"
        )
        assertEquals(1, nahCorrected.size)
        assertEquals("نانوایی", nahCorrected[0].purposeCategory)
        assertEquals(35_000L, nahCorrected[0].totalAmountToman)

        val sameNounCorrected = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(
            "۵۰ تومان نانوایی ۳۵ تومن نانوایی"
        )
        assertEquals(1, sameNounCorrected.size)
        assertEquals("نانوایی", sameNounCorrected[0].purposeCategory)
        assertEquals(35_000L, sameNounCorrected[0].totalAmountToman)

        val boudCorrected = PersianVoiceSlangParserEngine.parseVoiceCommand(
            "۵۰ تومن نانوایی بود نه نه نه نه ۳۵ تومن بود نانوایی"
        )
        assertEquals("نانوایی", boudCorrected.purposeCategory)
        assertEquals(35_000L, boudCorrected.totalAmountToman)

        val boudDeduped = PersianVoiceSlangParserEngine.parseVoiceCommand(
            "۵۰ تومان نانوایی بود نانوایی"
        )
        assertEquals("نانوایی", boudDeduped.purposeCategory)
        assertEquals(50_000L, boudDeduped.totalAmountToman)
    }

    @Test
    fun testVideo66BatchAndBatch11StressScenarios() {
        // 1. Video 66batch_11 (00:05): "خرید سکه پارسیان ۳۵۰ سوت" on 1,951,672 Toman Blu SMS
        val goldCoin = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "خرید سکه پارسیان ۳۵۰ سوت",
            fallbackTotalToman = 1_951_672L,
            defaultBankName = "بلو"
        )
        assertEquals("سکه پارسیان ۳۵۰ سوت", goldCoin.purposeCategory)
        assertEquals(1_951_672L, goldCoin.totalAmountToman)
        assertNull(
            PersianVoiceSlangParserEngine.detectBankSmsAmountMismatch(
                rawSpeech = "خرید سکه پارسیان ۳۵۰ سوت",
                fallbackTotalToman = 1_951_672L
            )
        )

        // 2. Video 66batch_11 (00:46): "خرید کفش سایز ۴۲" without price -> 0 Tomans (does not mistake 42 for 42,000 Tomans)
        val shoeNoPrice = PersianVoiceSlangParserEngine.parseVoiceCommand("خرید کفش سایز ۴۲")
        assertEquals("کفش سایز ۴۲", shoeNoPrice.purposeCategory)
        assertEquals(0L, shoeNoPrice.totalAmountToman)

        // 3. Video 66batch_11 (00:59): "خرید کفش سایز ۴۲ ۷۵۰ هزار تومان" -> title "کفش سایز ۴۲", amount 750,000 Tomans
        val shoeWithPrice = PersianVoiceSlangParserEngine.parseVoiceCommand("خرید کفش سایز ۴۲ ۷۵۰ هزار تومان")
        assertEquals("کفش سایز ۴۲", shoeWithPrice.purposeCategory)
        assertEquals(750_000L, shoeWithPrice.totalAmountToman)

        // 4. Video 66batch (00:25): "انتقال وجه به کارت بلو" on 50,000 Toman Pasargad SMS
        val toBluCard = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = "انتقال وجه به کارت بلو",
            fallbackTotalToman = 50_000L,
            defaultBankName = "پاسارگاد"
        )
        assertEquals("انتقال وجه به کارت بلو", toBluCard.purposeCategory)
        assertEquals(50_000L, toBluCard.totalAmountToman)
        assertEquals("پاسارگاد", toBluCard.storeName)

        // 5. Source vs Destination Card: "۱۰۰ تومن انتقال وجه به کارت بلو با کارت پاسارگاد"
        val destAndSourceCard = PersianVoiceSlangParserEngine.parseVoiceCommand(
            "۱۰۰ تومن انتقال وجه به کارت بلو با کارت پاسارگاد"
        )
        assertEquals("انتقال وجه به کارت بلو", destAndSourceCard.purposeCategory)
        assertEquals(100_000L, destAndSourceCard.totalAmountToman)
        assertEquals("کارت پاسارگاد", destAndSourceCard.cardLabel)
    }

    @Test
    fun testVideo77Batch12And13AndIncomePlusFormatting() {
        // 1. Video 77batch_12: Observation clause ("دیدم") + back-reference ("سی سوتی را خریدم ۲۵ هزار تومان")
        val phrase12 = "رفتم بیرون یک سکه پارسیان ۲۰ سوتی و یک سکه پارسیان ۳۰ سوتی دیدم و سی سوتی را خریدم ۲۵ هزار تومان"
        val items12 = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(phrase12)
        assertEquals(1, items12.size)
        assertEquals("سکه پارسیان ۳۰ سوتی", items12[0].purposeCategory)
        assertEquals(25_000L, items12[0].totalAmountToman)

        // 2. Video 77batch_12 (Google ASR contraction: "سیتی را خریدم ۲۵ هزار تومان")
        val phrase12Asr = "رفتم بیرون یک سکه پارسیان ۲۰ سوتی و یک سکه پارسیان سی سوتی دیدم و سیتی را خریدم ۲۵ هزار تومان"
        val items12Asr = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(phrase12Asr)
        assertEquals(1, items12Asr.size)
        assertEquals("سکه پارسیان ۳۰ سوتی", items12Asr[0].purposeCategory)
        assertEquals(25_000L, items12Asr[0].totalAmountToman)

        // 3. Video 77batch_13: Multi-stage price correction with "-ی" currency suffix ("۲۰ تومنی ... ولی نه ۱۰۰ تومنی خریدم")
        val phrase13 = "من رفتم بیرون و برگشتم ۵۰ هزار تومان دادم یه سکه پارسیان ۲۰ سوتی ولی می‌خواستم ۲۰ تومنی بخرم ولی نه ۱۰۰ تومنی خریدم"
        val items13 = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(phrase13)
        assertEquals(1, items13.size)
        assertEquals("سکه پارسیان ۲۰ سوتی", items13[0].purposeCategory)
        assertEquals(100_000L, items13[0].totalAmountToman)

        val single13 = PersianVoiceSlangParserEngine.parseVoiceCommand(phrase13)
        assertEquals("سکه پارسیان ۲۰ سوتی", single13.purposeCategory)
        assertEquals(100_000L, single13.totalAmountToman)

        // 4. Income "+" sign placed on the left of the number, separated from "تومان"
        assertEquals(
            "\u2066\u200E+۱۳,۰۰۰,۰۰۰\u2069 تومان",
            PersianNumberUtils.formatIncomeToman(13_000_000L, showUnit = true)
        )
    }

    @Test
    fun testVideo88BatchAndBatch14VoiceAndIncomeScenarios() {
        // 1. Video 88batch (00:21): "سلام امروز ۵۰ هزار تومان خرج کردم که ۲۵ تومنش برای موز بود و ۳۵ تومنش برای بستنی"
        val phraseSummaryMismatch = "سلام امروز ۵۰ هزار تومان خرج کردم که ۲۵ تومنش برای موز بود و ۳۵ تومنش برای بستنی"
        val itemsMismatch = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(phraseSummaryMismatch)
        assertEquals(2, itemsMismatch.size)
        assertEquals("موز", itemsMismatch[0].purposeCategory)
        assertEquals(25_000L, itemsMismatch[0].totalAmountToman)
        assertEquals("بستنی", itemsMismatch[1].purposeCategory)
        assertEquals(35_000L, itemsMismatch[1].totalAmountToman)
        assertEquals(
            50_000L,
            PersianVoiceSlangParserEngine.detectSpokenSummaryTotalMismatch(phraseSummaryMismatch, itemsMismatch)
        )

        // 2. Video 88batch (00:43): "سلام ۲۵ تومن می‌خواستم خرج کنم ولی ۲۰ تومن خرج کردم برای یک موز"
        val phraseWantBut = "سلام ۲۵ تومن می‌خواستم خرج کنم ولی ۲۰ تومن خرج کردم برای یک موز"
        val itemsWantBut = PersianVoiceSlangParserEngine.parseMultiVoiceCommands(phraseWantBut)
        assertEquals(1, itemsWantBut.size)
        assertEquals("۱ موز", itemsWantBut[0].purposeCategory)
        assertEquals(20_000L, itemsWantBut[0].totalAmountToman)

        // 3. Video 88batch_14 (00:02): "۵۰ هزار تومان دریافت کردم" -> Income (isIncome = true)
        val received50 = PersianVoiceSlangParserEngine.parseVoiceCommand("۵۰ هزار تومان دریافت کردم")
        assertEquals(true, received50.isIncome)
        assertEquals("دریافت وجه", received50.purposeCategory)
        assertEquals(50_000L, received50.totalAmountToman)
    }

    @Test
    fun testFullOrosiRegressionAndStressSuitePasses100Percent() {
        val report = com.example.domain.OrosiRegressionSuite.runFullRegressionAndStressSuite()
        for (r in report.results) {
            assertEquals(
                "Regression test failed [${r.title}]: expected=${r.expectedSummary}, actual=${r.actualSummary}",
                true,
                r.passed
            )
        }
        assertEquals(true, report.allPassed)
        assertEquals(45, report.totalCount)
        assertEquals(45, report.passedCount)
    }
}




