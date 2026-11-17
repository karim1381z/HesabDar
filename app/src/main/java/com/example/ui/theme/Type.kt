package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val IranSansXFontFamily = FontFamily(
    Font(R.font.iransansx_regular, FontWeight.Normal),
    Font(R.font.iransansx_medium, FontWeight.Medium),
    Font(R.font.iransansx_semibold, FontWeight.SemiBold),
    Font(R.font.iransansx_bold, FontWeight.Bold),
    Font(R.font.iransansx_extrabold, FontWeight.ExtraBold),
    Font(R.font.iransansx_black, FontWeight.Black)
)

val DanaFontFamily = FontFamily(
    Font(R.font.dana_regular, FontWeight.Normal),
    Font(R.font.dana_medium, FontWeight.Medium),
    Font(R.font.dana_semibold, FontWeight.SemiBold),
    Font(R.font.dana_bold, FontWeight.Bold),
    Font(R.font.dana_extrabold, FontWeight.ExtraBold),
    Font(R.font.dana_black, FontWeight.Black)
)

val IranYekanXFontFamily = FontFamily(
    Font(R.font.iranyekanx_regular, FontWeight.Normal),
    Font(R.font.iranyekanx_medium, FontWeight.Medium),
    Font(R.font.iranyekanx_semibold, FontWeight.SemiBold),
    Font(R.font.iranyekanx_bold, FontWeight.Bold),
    Font(R.font.iranyekanx_extrabold, FontWeight.ExtraBold),
    Font(R.font.iranyekanx_black, FontWeight.Black)
)

val PeydaFontFamily = FontFamily(
    Font(R.font.peyda_regular, FontWeight.Normal),
    Font(R.font.peyda_medium, FontWeight.Medium),
    Font(R.font.peyda_semibold, FontWeight.SemiBold),
    Font(R.font.peyda_bold, FontWeight.Bold),
    Font(R.font.peyda_extrabold, FontWeight.ExtraBold),
    Font(R.font.peyda_black, FontWeight.Black)
)

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
    Font(R.font.vazirmatn_black, FontWeight.Black)
)

enum class OrosiFontOption(
    val key: String,
    val persianTitle: String,
    val fontFamily: FontFamily
) {
    IRANSANSX("IRANSANSX", "ایران‌سنس", IranSansXFontFamily),
    DANA("DANA", "دانا", DanaFontFamily),
    IRANYEKANX("IRANYEKANX", "ایران‌یکان", IranYekanXFontFamily),
    PEYDA("PEYDA", "پیدا", PeydaFontFamily),
    VAZIRMATN("VAZIRMATN", "وزیرمتن", VazirmatnFontFamily);

    companion object {
        fun fromKey(key: String?): OrosiFontOption =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: IRANSANSX
    }
}

fun buildOrosiTypography(fontFamilyKey: String = "IRANSANSX"): Typography {
    val activeFamily = OrosiFontOption.fromKey(fontFamilyKey).fontFamily
    return Typography(
        displayLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Black,
            fontSize = 36.sp,
            lineHeight = 46.sp
        ),
        displayMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp,
            lineHeight = 38.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            lineHeight = 34.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 30.sp
        ),
        titleLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        titleSmall = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 23.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        labelMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    )
}

val Typography = buildOrosiTypography("IRANSANSX")
