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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CommitmentEntity
import com.example.domain.PersianNumberUtils
import com.example.ui.CommitmentTab
import com.example.ui.theme.AlabasterSilk
import com.example.ui.theme.NishapurTurquoise
import com.example.ui.theme.NishapurTurquoiseBright
import com.example.ui.theme.NishapurTurquoiseSoft
import com.example.ui.theme.OrosiHairline
import com.example.ui.theme.OrosiOnSurfaceVariant
import com.example.ui.theme.OrosiSurfaceContainer
import com.example.ui.theme.OrosiSurfaceHigh
import com.example.ui.theme.PersianLapis
import com.example.ui.theme.PersianLapisLight
import com.example.ui.theme.RoyalAmethyst
import com.example.ui.theme.RoyalAmethystBright
import com.example.ui.theme.RoyalAmethystContainer
import com.example.ui.theme.RoyalAmethystDeep
import com.example.ui.theme.RoyalAmethystSoft
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SaffronGoldBright
import com.example.ui.theme.SaffronGoldLight

@Composable
fun CommitmentsScreen(
    selectedTab: CommitmentTab,
    commitments: List<CommitmentEntity>,
    onSelectTab: (CommitmentTab) -> Unit,
    onToggleSettled: (CommitmentEntity) -> Unit,
    onOpenAddCommitmentModal: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToDashboard)

    val issuedCheque = commitments.firstOrNull { it.category == "SAYYAD_ISSUED" }
    val receivedCheque = commitments.firstOrNull { it.category == "SAYYAD_RECEIVED" }
    val loanInstallment = commitments.firstOrNull { it.category == "LOAN_INSTALLMENT" }

    val tabItems = when (selectedTab) {
        CommitmentTab.SAYYAD -> commitments.filter {
            it.category == "SAYYAD_ISSUED" || it.category == "SAYYAD_RECEIVED" || it.category == "LOAN_INSTALLMENT"
        }
        CommitmentTab.INSTALLMENTS -> commitments.filter { it.category == "LOAN_INSTALLMENT" }
        CommitmentTab.DONG -> commitments.filter { it.category == "DONG" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("commitments_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Segmented Tabs: چک صیادی | اقساط وام | دُنگ
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrosiSurfaceContainer)
                    .border(1.dp, OrosiHairline, RoundedCornerShape(16.dp))
                    .padding(4.dp)
                    .testTag("commitments_segmented_tabs"),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CommitmentTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val count = when (tab) {
                        CommitmentTab.SAYYAD -> commitments.count { it.category.startsWith("SAYYAD") }
                        CommitmentTab.INSTALLMENTS -> commitments.count { it.category == "LOAN_INSTALLMENT" }
                        CommitmentTab.DONG -> commitments.count { it.category == "DONG" && !it.isSettled }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) RoyalAmethyst else Color.Transparent
                            )
                            .clickable { onSelectTab(tab) }
                            .padding(vertical = 10.dp)
                            .testTag("commitment_tab_${tab.name}")
                    ) {
                        Text(
                            text = "${tab.title} (${PersianNumberUtils.toPersianDigits(count.toString())})",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) AlabasterSilk else OrosiOnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 2. Purple Sayyad Cheque Hero Card: ۱۵,۰۰۰,۰۰۰ تومان
        if (selectedTab == CommitmentTab.SAYYAD && issuedCheque != null) {
            item {
                PurpleSayyadChequeHeroCard(
                    cheque = issuedCheque,
                    onToggleSettled = { onToggleSettled(issuedCheque) }
                )
            }
        }

        // 3. Received Cheque + Bank Installment Cards (or Filtered Tab List)
        if (selectedTab == CommitmentTab.SAYYAD) {
            if (receivedCheque != null) {
                item {
                    CommitmentVaultCard(
                        commitment = receivedCheque,
                        badgeLabel = "چک دریافتی صیادی",
                        accentColor = NishapurTurquoiseBright,
                        icon = Icons.Default.CallReceived,
                        onToggleSettled = { onToggleSettled(receivedCheque) }
                    )
                }
            }
            if (loanInstallment != null) {
                item {
                    CommitmentVaultCard(
                        commitment = loanInstallment,
                        badgeLabel = "قسط وام بانکی",
                        accentColor = SaffronGoldBright,
                        icon = Icons.Default.AccountBalance,
                        onToggleSettled = { onToggleSettled(loanInstallment) }
                    )
                }
            }
            // Any extra custom Sayyad cheques added by user
            val extraSayyad = commitments.filter {
                it.category.startsWith("SAYYAD") && it.id != issuedCheque?.id && it.id != receivedCheque?.id
            }
            items(extraSayyad, key = { it.id }) { item ->
                CommitmentVaultCard(
                    commitment = item,
                    badgeLabel = if (item.category == "SAYYAD_RECEIVED") "چک دریافتی" else "چک صیادی صادره",
                    accentColor = RoyalAmethystSoft,
                    icon = Icons.Default.VerifiedUser,
                    onToggleSettled = { onToggleSettled(item) }
                )
            }
        } else {
            items(tabItems, key = { it.id }) { item ->
                val (badge, color, icon) = when (item.category) {
                    "LOAN_INSTALLMENT" -> Triple("قسط وام بانکی", SaffronGoldBright, Icons.Default.AccountBalance)
                    "DONG" -> Triple("سهم دُنگ دوستان", NishapurTurquoiseBright, Icons.Default.Group)
                    else -> Triple("چک صیادی", RoyalAmethystSoft, Icons.Default.VerifiedUser)
                }
                CommitmentVaultCard(
                    commitment = item,
                    badgeLabel = badge,
                    accentColor = color,
                    icon = icon,
                    onToggleSettled = { onToggleSettled(item) }
                )
            }
        }

        // 4. Required Single-Plus Button: + ثبت و استعلام چک جدید
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onOpenAddCommitmentModal,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalAmethyst,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RoyalAmethystSoft.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .testTag("add_new_cheque_button")
            ) {
                Text(
                    text = "+ ثبت و استعلام چک جدید",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PurpleSayyadChequeHeroCard(
    cheque: CommitmentEntity,
    onToggleSettled: () -> Unit
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
            .padding(20.dp)
            .testTag("purple_sayyad_cheque_card")
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
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = RoyalAmethystSoft,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "چک صیادی بنفش (صادره)",
                        style = MaterialTheme.typography.labelMedium,
                        color = AlabasterSilk
                    )
                }

                Text(
                    text = cheque.dueDateStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = RoyalAmethystSoft
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Hero Amount: ۱۵,۰۰۰,۰۰۰ تومان
            Text(
                text = PersianNumberUtils.formatToman(cheque.amountToman, showUnit = true),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp
                ),
                color = AlabasterSilk,
                modifier = Modifier.testTag("sayyad_hero_amount")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = cheque.title,
                style = MaterialTheme.typography.titleSmall,
                color = AlabasterSilk
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${cheque.counterparty} • شناسه صیاد: ${cheque.sayyadId}",
                style = MaterialTheme.typography.bodySmall,
                color = RoyalAmethystSoft
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cheque.statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = NishapurTurquoiseSoft,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (cheque.isSettled) NishapurTurquoise.copy(alpha = 0.3f)
                            else Color.White.copy(alpha = 0.14f)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleSettled)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (cheque.isSettled) "پاس شده ✓" else "ثبت پاس شدن",
                        style = MaterialTheme.typography.labelMedium,
                        color = AlabasterSilk
                    )
                }
            }
        }
    }
}

@Composable
private fun CommitmentVaultCard(
    commitment: CommitmentEntity,
    badgeLabel: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onToggleSettled: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(OrosiSurfaceHigh)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("commitment_card_${commitment.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = badgeLabel,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                    Text(
                        text = commitment.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = AlabasterSilk
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = PersianNumberUtils.formatToman(commitment.amountToman, showUnit = true),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
                Text(
                    text = commitment.dueDateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = OrosiOnSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = commitment.counterparty,
                    style = MaterialTheme.typography.bodySmall,
                    color = OrosiOnSurfaceVariant
                )
                Text(
                    text = commitment.statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (commitment.isSettled) NishapurTurquoiseBright else PersianLapisLight
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (commitment.isSettled) NishapurTurquoise.copy(alpha = 0.25f)
                        else accentColor.copy(alpha = 0.16f)
                    )
                    .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleSettled)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("settle_commitment_${commitment.id}")
            ) {
                Text(
                    text = if (commitment.isSettled) "تسویه شد ✓" else "تسویه شد",
                    style = MaterialTheme.typography.labelMedium,
                    color = AlabasterSilk
                )
            }
        }
    }
}
