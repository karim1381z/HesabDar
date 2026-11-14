package com.example

import com.example.domain.BankSmsParserEngine
import com.example.domain.JalaliCalendarUtils
import com.example.domain.PersianNumberUtils
import com.example.domain.PersianVoiceSlangParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testSeparateAmountFromMandehBalanceAndRealTimestamp() {
        // Real Iranian bank SMS containing both transaction amount (17,673,900 Rial = 1,767,390 Toman)
        // and account balance (مانده: 145,000,000 Rial = 14,500,000 Toman)
        val sms = """
            بانک صادرات ایران
            کارت ۶۰۳۷*۱۲۳۴
            مانده: ۱۴۵,۰۰۰,۰۰۰ ریال
            برداشت مبلغ ۱۷,۶۷۳,۹۰۰ ریال
        """.trimIndent()
        val sampleTimestamp = 1730734200000L // Nov 4, 2024 18:40 Asia/Tehran -> دوشنبه ۱۴ آبان ۱۴۰۳
        val parsed = BankSmsParserEngine.parseBankSms(
            rawSms = sms,
            timestampMillis = sampleTimestamp,
            senderAddress = "983000"
        )
        assertTrue(parsed.isValidFinancialSms)
        assertEquals("صادرات", parsed.bankName)
        assertEquals(1_767_390L, parsed.amountToman)
        assertEquals(14_500_000L, parsed.extractedBalanceToman)
        assertEquals("دوشنبه ۱۴ آبان", parsed.weekdayDate)
        assertEquals(1403, parsed.jalaliYear)
    }

    @Test
    fun testRejectOtpDynamicPasswordSms() {
        val otpSms = "بانک ملت\nرمز پویا خرید اینترنتی: 482910\nمبلغ: 17,673,900 ریال"
        val parsed = BankSmsParserEngine.parseBankSms(otpSms)
        assertFalse(parsed.isValidFinancialSms)
    }

    @Test
    fun testUnknownBankDefaultsToBankNotBlu() {
        val genericSms = "موسسه مالی\nبرداشت: ۲,۴۰۰,۰۰۰ ریال\nمانده: ۵۰,۰۰۰,۰۰۰ ریال"
        val parsed = BankSmsParserEngine.parseBankSms(genericSms)
        assertEquals("بانک", parsed.bankName)
        assertEquals(240_000L, parsed.amountToman)
        assertEquals(5_000_000L, parsed.extractedBalanceToman)
    }

    @Test
    fun testAllIranianBanksDetection() {
        assertEquals("ملت", BankSmsParserEngine.detectIranianBankName("بانک ملت\nبرداشت 100000 ریال"))
        assertEquals("ملی", BankSmsParserEngine.detectIranianBankName("بانک ملی ایران\nبرداشت"))
        assertEquals("تجارت", BankSmsParserEngine.detectIranianBankName("بانک تجارت\nواریز"))
        assertEquals("پاسارگاد", BankSmsParserEngine.detectIranianBankName("بانک پاسارگاد\nبرداشت"))
        assertEquals("سپه", BankSmsParserEngine.detectIranianBankName("بانک سپه\nبرداشت"))
        assertEquals("مسکن", BankSmsParserEngine.detectIranianBankName("بانک مسکن\nبرداشت"))
        assertEquals("رفاه", BankSmsParserEngine.detectIranianBankName("بانک رفاه کارگران"))
        assertEquals("آینده", BankSmsParserEngine.detectIranianBankName("بانک آینده"))
        assertEquals("پارسیان", BankSmsParserEngine.detectIranianBankName("بانک پارسیان"))
        assertEquals("اقتصاد نوین", BankSmsParserEngine.detectIranianBankName("بانک اقتصاد نوین"))
        assertEquals("مهر ایران", BankSmsParserEngine.detectIranianBankName("بانک قرض الحسنه مهر ایران"))
        assertEquals("کشاورزی", BankSmsParserEngine.detectIranianBankName("بانک کشاورزی"))
        assertEquals("سینا", BankSmsParserEngine.detectIranianBankName("بانک سینا"))
        assertEquals("شهر", BankSmsParserEngine.detectIranianBankName("بانک شهر"))
        assertEquals("گردشگری", BankSmsParserEngine.detectIranianBankName("بانک گردشگری"))
        assertEquals("ویپاد", BankSmsParserEngine.detectIranianBankName("ویپاد تراکنش"))
        assertEquals("آبانک", BankSmsParserEngine.detectIranianBankName("آبانک برداشت"))
        assertEquals("بلو", BankSmsParserEngine.detectIranianBankName("بلوبانک برداشت"))
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
        assertEquals("۶۸۰,۰۰۰", PersianNumberUtils.formatToman(680_000L))
    }

    @Test
    fun testGregorianToJalaliConversion() {
        val (jy, jm, jd) = JalaliCalendarUtils.gregorianToJalali(2024, 11, 4)
        assertEquals(1403, jy)
        assertEquals(8, jm)
        assertEquals(14, jd)
    }
}
