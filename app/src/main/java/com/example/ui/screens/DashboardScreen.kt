package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.TransactionEntity
import com.example.domain.PersianNumberUtils
import com.example.ui.theme.AlabasterSilk
import com.example.ui.theme.NishapurTurquoise
import com.example.ui.theme.NishapurTurquoiseBright
import com.example.ui.theme.NishapurTurquoiseDeep
import com.example.ui.theme.NishapurTurquoiseSoft
import com.example.ui.theme.OrosiHairline
import com.example.ui.theme.OrosiOnSurfaceVariant
import com.example.ui.theme.OrosiSurfaceContainer
import com.example.ui.theme.PersianLapis
import com.example.ui.theme.PersianLapisDeep
import com.example.ui.theme.PersianLapisGlow
import com.example.ui.theme.PersianLapisLight
import com.example.ui.theme.RubyRose
import com.example.ui.theme.SaffronGoldBright
import com.example.ui.theme.SaffronGoldLight

@Composable
fun DashboardScreen(
    settings: AppSettingsEntity,
    transactions: List<TransactionEntity>,
    onHeroCardClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onUntaggedMicClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Radically Decluttered Royal Blue Hero Card
        item {
            CalmRoyalBlueHeroCard(
                dailySafeBudgetToman = settings.dailySafeBudgetToman,
                onClick = onHeroCardClick
            )
        }

        // 2. Calm Section Header (no filter chips, no count badges)
        item {
            Text(
                text = "تراکنش‌های اخیر",
                style = MaterialTheme.typography.titleLarge,
                color = AlabasterSilk,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
        }

        // 3. Spacious, Breathable Transaction Cards (16.dp vertical spacing)
        items(
            items = transactions,
            key = { tx -> tx.id }
        ) { tx ->
            CalmTransactionCard(
                transaction = tx,
                onRowClick = { onTransactionClick(tx) },
                onUntaggedMicClick = { onUntaggedMicClick(tx) }
            )
        }
    }
}

@Composable
private fun CalmRoyalBlueHeroCard(
    dailySafeBudgetToman: Long,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        PersianLapisGlow,
                        PersianLapis,
                        PersianLapisDeep
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 700f)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.26f),
                        PersianLapisLight.copy(alpha = 0.10f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .clickable(onClick = onClick)
            .testTag("dashboard_hero_card")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = NishapurTurquoiseBright.copy(alpha = 0.07f),
                radius = size.height * 0.6f,
                center = Offset(size.width * 0.15f, size.height * 0.2f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1) Small calm text at top
            Text(
                text = "سهم خرجِ امروزت (تا حقوق بعدی ۱۴ روز مونده)",
                style = MaterialTheme.typography.bodyMedium,
                color = PersianLapisLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2) Huge, breathable number in the center: "۶۸۰,۰۰۰ تومان"
            Text(
                text = PersianNumberUtils.formatToman(dailySafeBudgetToman, showUnit = true),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 36.sp,
                    lineHeight = 44.sp
                ),
                color = AlabasterSilk,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.testTag("hero_peace_of_mind_amount")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3) One reassuring human sentence at the bottom in soft mint
            Text(
                text = "✓ پولِ تمام قسط‌ها و چک‌های این ماهت امن کنار گذاشته شده.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = NishapurTurquoiseSoft,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalmTransactionCard(
    transaction: TransactionEntity,
    onRowClick: () -> Unit,
    onUntaggedMicClick: () -> Unit
) {
    val (nodeColor, iconVector, amountColor) = when (transaction.type) {
        "INCOME" -> Triple(NishapurTurquoiseBright, Icons.Outlined.ArrowDownward, NishapurTurquoiseSoft)
        "GOLD_SAVE" -> Triple(SaffronGoldBright, Icons.Default.AutoAwesome, SaffronGoldLight)
        "SELF_TRANSFER" -> Triple(PersianLapisLight, Icons.Default.SwapHoriz, OrosiOnSurfaceVariant)
        else -> Triple(RubyRose, Icons.Outlined.ArrowUpward, AlabasterSilk)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(OrosiSurfaceContainer)
            .border(
                width = 1.dp,
                color = if (transaction.isUntagged) NishapurTurquoise.copy(alpha = 0.45f) else OrosiHairline,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onRowClick)
            .padding(horizontal = 14.dp, vertical = 16.dp)
            .testTag("transaction_item_${transaction.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Right side (in RTL, start of Row):
        // If untagged, show the single-line Turquoise pill button "بگو بابت چی؟"
        if (transaction.isUntagged) {
            PulsingTurquoiseMicButton(
                onClick = onUntaggedMicClick,
                testTag = "untagged_mic_button_${transaction.id}"
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(nodeColor.copy(alpha = 0.14f))
                    .border(1.dp, nodeColor.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = transaction.title,
                    tint = nodeColor,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Middle Column (Title + Simple Subtitle) with weight(1f) so neither side wraps
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (transaction.isUntagged) "خرید کارتی بی‌نام" else transaction.storeName.ifBlank { transaction.title },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = AlabasterSilk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = transaction.simpleSubtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = OrosiOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("transaction_subtitle_${transaction.id}")
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Left side (in RTL, end of Row): Clean single-line amount ("۲۵۰,۰۰۰ تومان" without wrapping "تومان" to a new line)
        val formattedSingleLineAmount = when (transaction.type) {
            "INCOME" -> "+${PersianNumberUtils.formatToman(transaction.userShareToman, showUnit = true)}"
            "SELF_TRANSFER" -> PersianNumberUtils.formatToman(transaction.totalAmountToman, showUnit = true)
            else -> PersianNumberUtils.formatToman(transaction.userShareToman, showUnit = true)
        }

        Text(
            text = formattedSingleLineAmount,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = amountColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun PulsingTurquoiseMicButton(
    onClick: () -> Unit,
    testTag: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentWidth(unbounded = false)
            .scale(pulseScale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        NishapurTurquoise,
                        NishapurTurquoiseDeep
                    )
                )
            )
            .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "بگو بابت چی؟",
            tint = AlabasterSilk,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "بگو بابت چی؟",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
            color = AlabasterSilk,
            maxLines = 1,
            softWrap = false
        )
    }
}
