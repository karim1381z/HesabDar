package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsEntity
import com.example.domain.PersianNumberUtils
import com.example.ui.theme.AlabasterSilk
import com.example.ui.theme.NishapurTurquoiseBright
import com.example.ui.theme.NishapurTurquoiseSoft
import com.example.ui.theme.OrosiOnSurfaceVariant
import com.example.ui.theme.OrosiSurfaceContainer
import com.example.ui.theme.OrosiSurfaceHigh
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SaffronGoldBright
import com.example.ui.theme.SaffronGoldContainer
import com.example.ui.theme.SaffronGoldDeep
import com.example.ui.theme.SaffronGoldFixed
import com.example.ui.theme.SaffronGoldLight

@Composable
fun InflationShieldScreen(
    settings: AppSettingsEntity,
    onOpenConvertGoldModal: () -> Unit,
    onOpenGoldPartnerModal: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToDashboard)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("inflation_shield_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Warm Amber-Gold Hero Card: ۴۸,۵۰۰,۰۰۰ تومان
        item {
            AmberGoldHeroCard(settings = settings)
        }

        // 2. Smooth Protective Growth Curve Chart
        item {
            ProtectiveGrowthCurveCard()
        }

        // 3. One Primary Action Button: تبدیل مازاد تراز به طلا
        item {
            Button(
                onClick = onOpenConvertGoldModal,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaffronGold,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SaffronGoldBright, RoundedCornerShape(18.dp))
                    .testTag("convert_surplus_to_gold_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CurrencyExchange,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "تبدیل مازاد تراز به طلا",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // 4. Subtle, High-Trust Partner Card Below the Main CTA
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(OrosiSurfaceHigh)
                    .border(1.dp, SaffronGoldBright.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenGoldPartnerModal)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .testTag("gold_partner_card"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SaffronGold.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = SaffronGoldBright,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "خرید و فروش آنلاین طلای ۱۸ عیار (پلتفرم‌های معتبر دارای مجوز)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = AlabasterSilk
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "کارمزد صفر • تحویل فیزیکی و صندوق‌های طلای بورس • اتصال مستقیم",
                        style = MaterialTheme.typography.bodySmall,
                        color = SaffronGoldLight
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AmberGoldHeroCard(settings: AppSettingsEntity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SaffronGold,
                        SaffronGoldContainer,
                        SaffronGoldDeep
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 650f)
                )
            )
            .border(
                1.dp,
                SaffronGoldBright.copy(alpha = 0.55f),
                RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .testTag("inflation_shield_hero_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ارزش قلک طلای ۱۸ عیار شما",
                style = MaterialTheme.typography.bodyMedium,
                color = SaffronGoldFixed,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Hero Amount: ۴۸,۵۰۰,۰۰۰ تومان
            Text(
                text = PersianNumberUtils.formatToman(settings.goldReserveToman, showUnit = true),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 34.sp
                ),
                color = AlabasterSilk,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.testTag("inflation_shield_hero_amount")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "معادل ${PersianNumberUtils.formatDecimal(settings.goldGrams)} گرم طلای آب‌شده • حفظ کامل قدرت خرید",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = NishapurTurquoiseSoft,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProtectiveGrowthCurveCard() {
    val months = listOf("خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(OrosiSurfaceContainer)
            .border(1.dp, SaffronGoldBright.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "منحنی رشد آرام قلک طلا",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AlabasterSilk
        )

        Spacer(modifier = Modifier.height(18.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .testTag("growth_curve_chart")
        ) {
            val w = size.width
            val h = size.height
            val goldPoints = listOf(0.76f, 0.67f, 0.56f, 0.43f, 0.28f, 0.14f)

            val goldPath = Path()
            val fillPath = Path()
            val coords = goldPoints.mapIndexed { idx, normY ->
                Offset((idx.toFloat() / (goldPoints.size - 1)) * w, normY * h)
            }
            goldPath.moveTo(coords.first().x, coords.first().y)
            fillPath.moveTo(coords.first().x, h)
            fillPath.lineTo(coords.first().x, coords.first().y)

            for (i in 0 until coords.size - 1) {
                val p0 = coords[i]
                val p1 = coords[i + 1]
                val controlX1 = (p0.x + p1.x) / 2f
                val controlY1 = p0.y
                val controlX2 = (p0.x + p1.x) / 2f
                val controlY2 = p1.y
                goldPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }
            fillPath.lineTo(coords.last().x, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SaffronGoldBright.copy(alpha = 0.32f),
                        SaffronGold.copy(alpha = 0.02f)
                    )
                )
            )

            drawPath(
                path = goldPath,
                color = SaffronGoldBright,
                style = Stroke(width = 5.5f, cap = StrokeCap.Round)
            )

            coords.forEach { pt ->
                drawCircle(
                    color = SaffronGoldDeep,
                    radius = 7f,
                    center = pt
                )
                drawCircle(
                    color = SaffronGoldBright,
                    radius = 4.5f,
                    center = pt
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            months.forEach { m ->
                Text(
                    text = m,
                    style = MaterialTheme.typography.labelSmall,
                    color = OrosiOnSurfaceVariant
                )
            }
        }
    }
}
