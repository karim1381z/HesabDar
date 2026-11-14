package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CommitmentEntity
import com.example.data.local.TransactionEntity
import com.example.domain.PersianNumberUtils
import com.example.ui.CommitmentTab
import com.example.ui.theme.AlabasterSilk
import com.example.ui.theme.NishapurTurquoise
import com.example.ui.theme.NishapurTurquoiseBright
import com.example.ui.theme.NishapurTurquoiseDeep
import com.example.ui.theme.NishapurTurquoiseSoft
import com.example.ui.theme.OrosiHairline
import com.example.ui.theme.OrosiOnSurfaceVariant
import com.example.ui.theme.OrosiSurfaceContainer
import com.example.ui.theme.OrosiSurfaceHigh
import com.example.ui.theme.PersianLapis
import com.example.ui.theme.PersianLapisLight
import com.example.ui.theme.RoyalAmethyst
import com.example.ui.theme.RoyalAmethystContainer
import com.example.ui.theme.RoyalAmethystDeep
import com.example.ui.theme.RoyalAmethystSoft
import com.example.ui.theme.RubyRoseSoft
import com.example.ui.theme.SaffronGoldBright
import com.example.ui.theme.SaffronGoldLight

@Composable
fun CommitmentsScreen(
    selectedTab: CommitmentTab,
    onSelectTab: (CommitmentTab) -> Unit,
    commitments: List<CommitmentEntity>,
    transactions: List<TransactionEntity>,
    hideAmounts: Boolean,
    onCommitmentClick: (CommitmentEntity) -> Unit,
    onOpenAddCommitmentModal: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToDashboard)

    val filteredCommitments = remember(commitments, selectedTab) {
        when (selectedTab) {
            CommitmentTab.SAYYAD -> commitments.filter {
                it.category == "SAYYAD_ISSUED" || it.category == "SAYYAD_RECEIVED"
            }
            CommitmentTab.INSTALLMENTS -> commitments.filter {
                it.category == "LOAN_INSTALLMENT"
            }
            CommitmentTab.DONG -> commitments.filter {
                it.category == "DONG"
            }
            CommitmentTab.REPORTS -> emptyList()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("commitments_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Clean 2x2 Jibeman Tools Grid Selector:
        // ۱) چک صیادی  ۲) وام و قسط  ۳) بدهی و طلب (دُنگ)  ۴) گزارش‌گیری دخل و خرج
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tools_grid_selector"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolGridCard(
                        title = "۱) چک صیادی",
                        icon = Icons.Default.VerifiedUser,
                        isSelected = selectedTab == CommitmentTab.SAYYAD,
                        accentColor = RoyalAmethystSoft,
                        onClick = { onSelectTab(CommitmentTab.SAYYAD) },
                        testTag = "tool_tab_sayyad",
                        modifier = Modifier.weight(1f)
                    )
                    ToolGridCard(
                        title = "۲) وام و قسط",
                        icon = Icons.Default.AccountBalance,
                        isSelected = selectedTab == CommitmentTab.INSTALLMENTS,
                        accentColor = SaffronGoldBright,
                        onClick = { onSelectTab(CommitmentTab.INSTALLMENTS) },
                        testTag = "tool_tab_installments",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolGridCard(
                        title = "۳) بدهی و طلب (دُنگ)",
                        icon = Icons.Default.Group,
                        isSelected = selectedTab == CommitmentTab.DONG,
                        accentColor = NishapurTurquoiseBright,
                        onClick = { onSelectTab(CommitmentTab.DONG) },
                        testTag = "tool_tab_dong",
                        modifier = Modifier.weight(1f)
                    )
                    ToolGridCard(
                        title = "۴) گزارش‌گیری دخل و خرج",
                        icon = Icons.Default.Assessment,
                        isSelected = selectedTab == CommitmentTab.REPORTS,
                        accentColor = PersianLapisLight,
                        onClick = { onSelectTab(CommitmentTab.REPORTS) },
                        testTag = "tool_tab_reports",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (selectedTab == CommitmentTab.REPORTS) {
            item {
                IncomeExpenseReportSection(
                    transactions = transactions,
                    commitments = commitments,
                    hideAmounts = hideAmounts
                )
            }
        } else {
            // Reassuring Banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(NishapurTurquoiseDeep.copy(alpha = 0.65f))
                        .border(1.dp, NishapurTurquoiseBright.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                        .testTag("reassuring_commitments_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NishapurTurquoiseBright,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "مبلغ تعهدات فعال این بخش به‌صورت خودکار از موجودی آزاد کسر شده است.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AlabasterSilk
                    )
                }
            }

            if (filteredCommitments.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(OrosiSurfaceContainer)
                            .border(1.dp, OrosiHairline, RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "موردی در بخش «${selectedTab.title}» ثبت نشده است",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AlabasterSilk,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "برای افزودن مورد جدید از دکمه پایین استفاده کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OrosiOnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(
                items = filteredCommitments,
                key = { it.id }
            ) { item ->
                if (item.category == "SAYYAD_ISSUED") {
                    SpaciousPurpleSayyadCard(
                        cheque = item,
                        hideAmounts = hideAmounts,
                        onClick = { onCommitmentClick(item) }
                    )
                } else {
                    val (accentColor, icon) = when (item.category) {
                        "SAYYAD_RECEIVED" -> Pair(NishapurTurquoiseBright, Icons.Default.CallReceived)
                        "LOAN_INSTALLMENT" -> Pair(SaffronGoldBright, Icons.Default.AccountBalance)
                        "DONG" -> Pair(NishapurTurquoiseSoft, Icons.Default.Group)
                        else -> Pair(RoyalAmethystSoft, Icons.Default.VerifiedUser)
                    }
                    SpaciousCommitmentCard(
                        commitment = item,
                        accentColor = accentColor,
                        icon = icon,
                        hideAmounts = hideAmounts,
                        onClick = { onCommitmentClick(item) }
                    )
                }
            }

            // Action Button to add new Commitment
            item {
                Spacer(modifier = Modifier.height(4.dp))
                val buttonLabel = when (selectedTab) {
                    CommitmentTab.SAYYAD -> "+ ثبت چک صیادی جدید"
                    CommitmentTab.INSTALLMENTS -> "+ ثبت وام یا قسط جدید"
                    CommitmentTab.DONG -> "+ ثبت بدهی یا طلب (دُنگ) جدید"
                    CommitmentTab.REPORTS -> "+ ثبت تعهد جدید"
                }
                Button(
                    onClick = onOpenAddCommitmentModal,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalAmethyst,
                        contentColor = AlabasterSilk
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, RoyalAmethystSoft.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                        .testTag("add_new_cheque_button")
                ) {
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ToolGridCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) PersianLapis else OrosiSurfaceHigh)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else OrosiHairline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 12.5.sp
            ),
            color = AlabasterSilk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IncomeExpenseReportSection(
    transactions: List<TransactionEntity>,
    commitments: List<CommitmentEntity>,
    hideAmounts: Boolean
) {
    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.userShareToman }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.userShareToman }
    val activeCommitments = commitments.filter { !it.isSettled }.sumOf { it.amountToman }

    val expensesByBank = transactions
        .filter { it.type == "EXPENSE" }
        .groupBy { it.shortBankName }
        .mapValues { (_, list) -> list.sumOf { it.userShareToman } }
        .entries
        .sortedByDescending { it.value }

    val expensesByPurpose = transactions
        .filter { it.type == "EXPENSE" }
        .groupBy { it.displayTitle }
        .mapValues { (_, list) -> list.sumOf { it.userShareToman } }
        .entries
        .sortedByDescending { it.value }
        .take(8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reports_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(OrosiSurfaceHigh)
                .border(1.dp, OrosiHairline, RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "خلاصه گزارش دخل و خرج",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = AlabasterSilk
            )

            ReportStatRow(
                label = "کل دریافتی‌ها (واریز):",
                value = PersianNumberUtils.formatToman(totalIncome, showUnit = true, hideAmount = hideAmounts),
                color = NishapurTurquoiseSoft
            )
            ReportStatRow(
                label = "کل پرداختی‌ها (برداشت):",
                value = PersianNumberUtils.formatToman(totalExpense, showUnit = true, hideAmount = hideAmounts),
                color = RubyRoseSoft
            )
            ReportStatRow(
                label = "تعهدات باز (چک، قسط و دُنگ):",
                value = PersianNumberUtils.formatToman(activeCommitments, showUnit = true, hideAmount = hideAmounts),
                color = SaffronGoldLight
            )
        }

        // Breakdown by Bank
        if (expensesByBank.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(OrosiSurfaceContainer)
                    .border(1.dp, OrosiHairline, RoundedCornerShape(22.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "تفکیک مخارج بر اساس بانک",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PersianLapisLight
                )
                expensesByBank.forEach { (bank, amount) ->
                    val ratio = if (totalExpense > 0L) {
                        (amount.toFloat() / totalExpense.toFloat()).coerceIn(0.05f, 1f)
                    } else 0.1f

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "بانک $bank",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AlabasterSilk
                            )
                            Text(
                                text = PersianNumberUtils.formatToman(amount, showUnit = true, hideAmount = hideAmounts),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = NishapurTurquoiseSoft
                            )
                        }
                        LinearProgressIndicator(
                            progress = { ratio },
                            color = NishapurTurquoise,
                            trackColor = OrosiHairline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
            }
        }

        // Breakdown by Purpose / Voice Tag
        if (expensesByPurpose.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(OrosiSurfaceContainer)
                    .border(1.dp, OrosiHairline, RoundedCornerShape(22.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "بیشترین مخارج بر اساس عنوان / بابت",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = SaffronGoldLight
                )
                expensesByPurpose.forEach { (purposeLabel, amount) ->
                    ReportStatRow(
                        label = purposeLabel,
                        value = PersianNumberUtils.formatToman(amount, showUnit = true, hideAmount = hideAmounts),
                        color = AlabasterSilk
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportStatRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OrosiOnSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun SpaciousPurpleSayyadCard(
    cheque: CommitmentEntity,
    hideAmounts: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        RoyalAmethyst,
                        RoyalAmethystContainer,
                        RoyalAmethystDeep
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(800f, 600f)
                )
            )
            .border(
                1.dp,
                RoyalAmethystSoft.copy(alpha = 0.45f),
                RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(22.dp)
            .testTag("purple_sayyad_cheque_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = cheque.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                ),
                color = AlabasterSilk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = cheque.dueDateStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = RoyalAmethystSoft,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = PersianNumberUtils.formatToman(cheque.amountToman, showUnit = true, hideAmount = hideAmounts),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                ),
                color = AlabasterSilk,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.testTag("sayyad_hero_amount")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (cheque.isSettled) "پاس شد / تسویه شد ✓" else cheque.counterparty,
                style = MaterialTheme.typography.bodySmall,
                color = if (cheque.isSettled) NishapurTurquoiseSoft else RoyalAmethystSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpaciousCommitmentCard(
    commitment: CommitmentEntity,
    accentColor: Color,
    icon: ImageVector,
    hideAmounts: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(OrosiSurfaceHigh)
            .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag("commitment_card_${commitment.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = commitment.title,
                        tint = accentColor,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = commitment.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    ),
                    color = AlabasterSilk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = PersianNumberUtils.formatToman(commitment.amountToman, showUnit = true, hideAmount = hideAmounts),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = accentColor,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accentColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (commitment.isSettled) "پاس شد / تسویه شد ✓" else commitment.dueDateStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (commitment.isSettled) NishapurTurquoiseBright else accentColor,
                    maxLines = 1,
                    softWrap = false
                )
            }

            if (commitment.counterparty.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = commitment.counterparty,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = OrosiOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
