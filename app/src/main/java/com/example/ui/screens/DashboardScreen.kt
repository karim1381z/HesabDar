package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.TransactionEntity
import com.example.domain.PersianNumberUtils
import com.example.ui.OrosiScreen
import com.example.ui.theme.AlabasterSilk
import com.example.ui.theme.NishapurTurquoise
import com.example.ui.theme.NishapurTurquoiseBright
import com.example.ui.theme.NishapurTurquoiseDeep
import com.example.ui.theme.NishapurTurquoiseSoft
import com.example.ui.theme.OrosiGlassBorder
import com.example.ui.theme.OrosiHairline
import com.example.ui.theme.OrosiOnSurface
import com.example.ui.theme.OrosiOnSurfaceVariant
import com.example.ui.theme.OrosiSurfaceContainer
import com.example.ui.theme.OrosiSurfaceLow
import com.example.ui.theme.PersianLapis
import com.example.ui.theme.PersianLapisDeep
import com.example.ui.theme.PersianLapisGlow
import com.example.ui.theme.PersianLapisLight
import com.example.ui.theme.RoyalAmethyst
import com.example.ui.theme.RoyalAmethystSoft
import com.example.ui.theme.RubyRose
import com.example.ui.theme.RubyRoseSoft
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SaffronGoldBright
import com.example.ui.theme.SaffronGoldLight

@Composable
fun DashboardScreen(
    settings: AppSettingsEntity,
    transactions: List<TransactionEntity>,
    totalReservedCommitmentsToman: Long,
    onNavigate: (OrosiScreen) -> Unit,
    onOpenSmsSimulator: () -> Unit,
    onUntaggedMicClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredTransactions = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            "UNTAGGED" -> transactions.filter { it.isUntagged }
            "EXPENSE" -> transactions.filter { it.type == "EXPENSE" }
            "INCOME" -> transactions.filter { it.type == "INCOME" || it.type == "GOLD_SAVE" }
            else -> transactions
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // 1. Royal Blue Hero Card: ۶۸۰,۰۰۰ تومان خیال راحت امروز
        item {
            RoyalBlueHeroCard(
                dailySafeBudgetToman = settings.dailySafeBudgetToman,
                monthlyBalanceToman = settings.monthlyBalanceToman,
                reservedCommitmentsToman = totalReservedCommitmentsToman,
                paydayCycleLabel = settings.paydayCycleLabel
            )
        }

        // 2. 4 Circular Quick Actions
        item {
            QuickActionsRow(
                onVoiceClick = { onNavigate(OrosiScreen.VOICE_ENTRY) },
                onSmsClick = onOpenSmsSimulator,
                onCommitmentsClick = { onNavigate(OrosiScreen.COMMITMENTS) },
                onGoldShieldClick = { onNavigate(OrosiScreen.INFLATION_SHIELD) }
            )
        }

        // 3. Connected Frosted Ledger Sheet Header + Filter Chips
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "تراکنش‌های اخیر",
                            style = MaterialTheme.typography.titleLarge,
                            color = AlabasterSilk
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val untaggedCount = transactions.count { it.isUntagged }
                        if (untaggedCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(NishapurTurquoise.copy(alpha = 0.22f))
                                    .border(1.dp, NishapurTurquoise, RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${PersianNumberUtils.toPersianDigits(untaggedCount.toString())} پیامک بی‌نام",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NishapurTurquoiseBright
                                )
                            }
                        }
                    }

                    Text(
                        text = "${PersianNumberUtils.toPersianDigits(filteredTransactions.size.toString())} مورد",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrosiOnSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OrosiFilterChip(
                        label = "همه",
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" }
                    )
                    OrosiFilterChip(
                        label = "نیازمند صدا",
                        selected = selectedFilter == "UNTAGGED",
                        onClick = { selectedFilter = "UNTAGGED" },
                        accentColor = NishapurTurquoise
                    )
                    OrosiFilterChip(
                        label = "خروجی‌ها",
                        selected = selectedFilter == "EXPENSE",
                        onClick = { selectedFilter = "EXPENSE" }
                    )
                    OrosiFilterChip(
                        label = "واریز و طلا",
                        selected = selectedFilter == "INCOME",
                        onClick = { selectedFilter = "INCOME" }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // 4. Nested Orosi Ledger Tray of Transactions
        itemsIndexed(
            items = filteredTransactions,
            key = { _, tx -> tx.id }
        ) { index, tx ->
            val isFirst = index == 0
            val isLast = index == filteredTransactions.lastIndex
            OrosiTransactionLedgerRow(
                transaction = tx,
                isFirst = isFirst,
                isLast = isLast,
                onUntaggedMicClick = { onUntaggedMicClick(tx) }
            )
        }
    }
}

@Composable
private fun RoyalBlueHeroCard(
    dailySafeBudgetToman: Long,
    monthlyBalanceToman: Long,
    reservedCommitmentsToman: Long,
    paydayCycleLabel: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(24.dp))
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
                        Color.White.copy(alpha = 0.28f),
                        PersianLapisLight.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("dashboard_hero_card")
    ) {
        // Subtle Persian Orosi Geometric Lattice Background Accent
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val diamondPath = Path().apply {
                moveTo(w * 0.15f, -h * 0.1f)
                lineTo(w * 0.35f, h * 0.35f)
                lineTo(w * 0.15f, h * 0.8f)
                lineTo(-w * 0.05f, h * 0.35f)
                close()
            }
            drawPath(
                path = diamondPath,
                color = Color.White.copy(alpha = 0.04f),
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = NishapurTurquoiseBright.copy(alpha = 0.08f),
                radius = h * 0.55f,
                center = Offset(w * 0.12f, h * 0.2f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Status Pill inside Hero
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NishapurTurquoiseBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سقف خرج روزانه بدون استرس",
                        style = MaterialTheme.typography.labelMedium,
                        color = AlabasterSilk
                    )
                }

                Text(
                    text = paydayCycleLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = PersianLapisLight
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Hero Amount: ۶۸۰,۰۰۰ تومان خیال راحت امروز
            Text(
                text = "${PersianNumberUtils.formatToman(dailySafeBudgetToman)} تومان خیال راحت امروز",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                ),
                color = AlabasterSilk,
                modifier = Modifier.testTag("hero_peace_of_mind_amount")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "اقساط وام، چک‌های صیادی و پس‌انداز طلا از تراز ماه شما کنار گذاشته شده‌اند.",
                style = MaterialTheme.typography.bodySmall,
                color = PersianLapisLight.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Hairline Sasheh Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom 2-column Ledger Metrics inside Hero Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "تراز آزاد ماه حقوقی",
                        style = MaterialTheme.typography.labelSmall,
                        color = PersianLapisLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = PersianNumberUtils.formatToman(monthlyBalanceToman, showUnit = true),
                        style = MaterialTheme.typography.titleMedium,
                        color = NishapurTurquoiseSoft,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ذخیره تعهدات و چک ماه",
                        style = MaterialTheme.typography.labelSmall,
                        color = PersianLapisLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = PersianNumberUtils.formatToman(reservedCommitmentsToman, showUnit = true),
                        style = MaterialTheme.typography.titleMedium,
                        color = SaffronGoldLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onVoiceClick: () -> Unit,
    onSmsClick: () -> Unit,
    onCommitmentsClick: () -> Unit,
    onGoldShieldClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CircularQuickActionItem(
            title = "ثبت صوتی",
            icon = Icons.Default.Mic,
            containerColor = NishapurTurquoise.copy(alpha = 0.20f),
            borderColor = NishapurTurquoiseBright,
            iconTint = NishapurTurquoiseBright,
            testTag = "quick_action_voice",
            onClick = onVoiceClick
        )
        CircularQuickActionItem(
            title = "پیامک بانک",
            icon = Icons.Default.Sms,
            containerColor = PersianLapisGlow.copy(alpha = 0.35f),
            borderColor = PersianLapisLight,
            iconTint = PersianLapisLight,
            testTag = "quick_action_sms",
            onClick = onSmsClick
        )
        CircularQuickActionItem(
            title = "چک و قسط",
            icon = Icons.Default.EventNote,
            containerColor = RoyalAmethyst.copy(alpha = 0.22f),
            borderColor = RoyalAmethystSoft,
            iconTint = RoyalAmethystSoft,
            testTag = "quick_action_commitments",
            onClick = onCommitmentsClick
        )
        CircularQuickActionItem(
            title = "سپر طلا",
            icon = Icons.Default.Shield,
            containerColor = SaffronGold.copy(alpha = 0.22f),
            borderColor = SaffronGoldBright,
            iconTint = SaffronGoldBright,
            testTag = "quick_action_gold",
            onClick = onGoldShieldClick
        )
    }
}

@Composable
private fun CircularQuickActionItem(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    borderColor: Color,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(containerColor)
                .border(1.dp, borderColor.copy(alpha = 0.65f), CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = OrosiOnSurface
        )
    }
}

@Composable
private fun OrosiFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = RoyalAmethyst
) {
    val bgColor = if (selected) accentColor.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.04f)
    val borderColor = if (selected) accentColor else OrosiHairline
    val textColor = if (selected) AlabasterSilk else OrosiOnSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
private fun OrosiTransactionLedgerRow(
    transaction: TransactionEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onUntaggedMicClick: () -> Unit
) {
    val shape = when {
        isFirst && isLast -> RoundedCornerShape(16.dp)
        isFirst -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        isLast -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(0.dp)
    }

    val (nodeColor, iconVector, amountColor) = when (transaction.type) {
        "INCOME" -> Triple(NishapurTurquoiseBright, Icons.Outlined.ArrowDownward, NishapurTurquoiseSoft)
        "GOLD_SAVE" -> Triple(SaffronGoldBright, Icons.Default.AutoAwesome, SaffronGoldLight)
        "SELF_TRANSFER" -> Triple(PersianLapisLight, Icons.Default.SwapHoriz, OrosiOnSurfaceVariant)
        else -> Triple(RubyRose, Icons.Outlined.ArrowUpward, AlabasterSilk)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(OrosiSurfaceContainer)
            .border(width = 0.5.dp, color = OrosiHairline, shape = shape)
            .testTag("transaction_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category / Status Node Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(nodeColor.copy(alpha = 0.15f))
                    .border(1.dp, nodeColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = if (transaction.isUntagged) Icons.Default.AccountBalanceWallet else iconVector,
                    contentDescription = transaction.title,
                    tint = nodeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Required Subtitle: جمعه ۱ آبان • ۱۸:۱۰ • کارت بلو ۶۲۱۹
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = AlabasterSilk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (transaction.dongAmountToman > 0L) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NishapurTurquoise.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "دنگ ${transaction.dongFriendName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NishapurTurquoiseBright
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Exact required format: جمعه ۱ آبان • ۱۸:۱۰ • کارت بلو ۶۲۱۹
                Text(
                    text = transaction.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OrosiOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("transaction_subtitle_${transaction.id}")
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Small Turquoise Mic Icon on any untagged SMS transaction
            AnimatedVisibility(visible = transaction.isUntagged) {
                Surface(
                    onClick = onUntaggedMicClick,
                    shape = CircleShape,
                    color = NishapurTurquoiseDeep,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(38.dp)
                        .border(1.dp, NishapurTurquoiseBright, CircleShape)
                        .testTag("untagged_mic_button_${transaction.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "تکمیل صوتی فروشگاه و بابت",
                            tint = NishapurTurquoiseBright,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Amount Column
            Column(horizontalAlignment = Alignment.End) {
                val signedText = when (transaction.type) {
                    "INCOME" -> "+${PersianNumberUtils.formatToman(transaction.userShareToman)}"
                    "SELF_TRANSFER" -> PersianNumberUtils.formatToman(transaction.totalAmountToman)
                    else -> "−${PersianNumberUtils.formatToman(transaction.userShareToman)}"
                }
                Text(
                    text = signedText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = amountColor
                )
                Text(
                    text = if (transaction.type == "SELF_TRANSFER") "تومان (خنثی)" else "تومان",
                    style = MaterialTheme.typography.labelSmall,
                    color = OrosiOnSurfaceVariant
                )
            }
        }
    }
}
