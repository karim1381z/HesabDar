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
    }
}
