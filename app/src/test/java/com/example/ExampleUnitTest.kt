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
}



