package com.example

import com.example.domain.BankSmsParserEngine
import com.example.domain.PersianNumberUtils
import com.example.domain.PersianVoiceSlangParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testRialToTomanAndSubtitleFormatting() {
        val sms = "بلوبانک\nبرداشت: ۲,۴۰۰,۰۰۰ ریال\nکارت: ۶۲۱۹\nجمعه ۱ آبان ۱۸:۱۰"
        val parsed = BankSmsParserEngine.parseBankSms(sms)
        assertEquals(240_000L, parsed.amountToman)
        assertEquals("جمعه ۱ آبان • ۱۸:۱۰ • کارت بلو ۶۲۱۹", parsed.subtitle)
        assertTrue(parsed.isUntaggedPurchase)
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
}
