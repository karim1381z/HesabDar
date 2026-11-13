package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsEntity
import com.example.domain.PersianNumberUtils
import com.example.ui.theme.AlabasterSilk
import com.example.ui.theme.NishapurTurquoise
import com.example.ui.theme.NishapurTurquoiseBright
import com.example.ui.theme.NishapurTurquoiseSoft
import com.example.ui.theme.OrosiHairline
import com.example.ui.theme.OrosiOnSurfaceVariant
import com.example.ui.theme.OrosiSurfaceContainer
import com.example.ui.theme.OrosiSurfaceHigh
import com.example.ui.theme.RubyRoseSoft
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
    onToggleDailyAutoSave: (Boolean) -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToDashboard)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("inflation_shield_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Warm Amber-Gold Hero Card: ۴۸,۵۰۰,۰۰۰ تومان
        item {
            AmberGoldHeroCard(settings = settings)
        }

        // 2. Smooth Protective Growth Curve Chart
        item {
            ProtectiveGrowthCurveCard(goldReserveToman = settings.goldReserveToman)
        }

        // 3. تبدیل مازاد تراز به طلا Button
        item {
            Button(
                onClick = onOpenConvertGoldModal,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaffronGold,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SaffronGoldBright, RoundedCornerShape(16.dp))
                    .testTag("convert_surplus_to_gold_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CurrencyExchange,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تبدیل مازاد تراز به طلا",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // 4. Daily Auto-Save Toggle Card
        item {
            DailyAutoSaveToggleCard(
                enabled = settings.dailyAutoSaveEnabled,
                onToggle = onToggleDailyAutoSave
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AmberGoldHeroCard(settings: AppSettingsEntity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
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
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
            .testTag("inflation_shield_hero_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SaffronGoldFixed,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سپر تورمی طلای ۱۸ عیار",
                        style = MaterialTheme.typography.labelMedium,
                        color = AlabasterSilk
                    )
                }

                Text(
                    text = "+۱۸٫۴٪ حفظ قدرت خرید",
                    style = MaterialTheme.typography.labelMedium,
                    color = NishapurTurquoiseSoft
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Hero Amount: ۴۸,۵۰۰,۰۰۰ تومان
            Text(
                text = PersianNumberUtils.formatToman(settings.goldReserveToman, showUnit = true),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp
                ),
                color = AlabasterSilk,
                modifier = Modifier.testTag("inflation_shield_hero_amount")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "معادل ${PersianNumberUtils.formatDecimal(settings.goldGrams)} گرم طلای ۱۸ عیار • نرخ گرم: ${PersianNumberUtils.formatToman(settings.goldPrice18kToman, true)}",
                style = MaterialTheme.typography.bodySmall,
                color = SaffronGoldFixed
            )
        }
    }
}

@Composable
private fun ProtectiveGrowthCurveCard(goldReserveToman: Long) {
    val months = listOf("خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(OrosiSurfaceContainer)
            .border(1.dp, SaffronGoldBright.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = SaffronGoldBright,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "منحنی محافظت از ارزش دارایی (۶ ماه اخیر)",
                    style = MaterialTheme.typography.titleSmall,
                    color = AlabasterSilk
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SaffronGoldBright)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "سپر طلای ۱۸ عیار",
                    style = MaterialTheme.typography.labelSmall,
                    color = SaffronGoldLight
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RubyRoseSoft.copy(alpha = 0.65f))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "پول نقد راکد (کاهش قدرت خرید)",
                    style = MaterialTheme.typography.labelSmall,
                    color = OrosiOnSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Smooth Cubic Bezier Protective Growth Curve Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .testTag("growth_curve_chart")
        ) {
            val w = size.width
            val h = size.height
            val goldPoints = listOf(0.75f, 0.66f, 0.58f, 0.44f, 0.30f, 0.14f)
            val cashPoints = listOf(0.75f, 0.78f, 0.82f, 0.86f, 0.90f, 0.94f)

            // Horizontal subtle grid lines
            for (row in 1..3) {
                val y = h * (row / 4f)
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            // Cash erosion dashed path
            val cashPath = Path()
            cashPoints.forEachIndexed { idx, normY ->
                val x = (idx.toFloat() / (cashPoints.size - 1)) * w
                val y = normY * h
                if (idx == 0) cashPath.moveTo(x, y) else cashPath.lineTo(x, y)
            }
            drawPath(
                path = cashPath,
                color = RubyRoseSoft.copy(alpha = 0.45f),
                style = Stroke(
                    width = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )

            // Smooth Protective Gold Growth Cubic Path
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
                        SaffronGoldBright.copy(alpha = 0.34f),
                        SaffronGold.copy(alpha = 0.02f)
                    )
                )
            )

            drawPath(
                path = goldPath,
                color = SaffronGoldBright,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
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

        Spacer(modifier = Modifier.height(8.dp))

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

@Composable
private fun DailyAutoSaveToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(OrosiSurfaceHigh)
            .border(1.dp, OrosiHairline, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoGraph,
                    contentDescription = null,
                    tint = SaffronGoldBright,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "پس‌انداز خودکار روزانه در طلا",
                    style = MaterialTheme.typography.titleSmall,
                    color = AlabasterSilk
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "تبدیل خودکار باقی‌مانده «خیال راحت امروز» در پایان شب به طلای ۱۸ عیار",
                style = MaterialTheme.typography.bodySmall,
                color = OrosiOnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AlabasterSilk,
                checkedTrackColor = SaffronGold,
                uncheckedThumbColor = OrosiOnSurfaceVariant,
                uncheckedTrackColor = OrosiSurfaceContainer
            ),
            modifier = Modifier.testTag("daily_auto_save_toggle")
        )
    }
}
