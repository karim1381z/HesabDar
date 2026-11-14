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
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CommitmentEntity
import com.example.domain.PersianNumberUtils
import com.example.ui.theme.AlabasterSilk
import com.example.ui.theme.NishapurTurquoiseBright
import com.example.ui.theme.NishapurTurquoiseDeep
import com.example.ui.theme.NishapurTurquoiseSoft
import com.example.ui.theme.OrosiHairline
import com.example.ui.theme.OrosiOnSurfaceVariant
import com.example.ui.theme.OrosiSurfaceContainer
import com.example.ui.theme.OrosiSurfaceHigh
import com.example.ui.theme.RoyalAmethyst
import com.example.ui.theme.RoyalAmethystContainer
import com.example.ui.theme.RoyalAmethystDeep
import com.example.ui.theme.RoyalAmethystSoft
import com.example.ui.theme.SaffronGoldBright

@Composable
fun CommitmentsScreen(
    commitments: List<CommitmentEntity>,
    onCommitmentClick: (CommitmentEntity) -> Unit,
    onOpenAddCommitmentModal: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToDashboard)

    val issuedCheque = commitments.firstOrNull { it.category == "SAYYAD_ISSUED" }
    val otherCommitments = commitments.filter { it.id != issuedCheque?.id }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("commitments_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. One Reassuring Green/Turquoise Banner at the Top
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NishapurTurquoiseDeep.copy(alpha = 0.65f))
                    .border(1.dp, NishapurTurquoiseBright.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .testTag("reassuring_commitments_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NishapurTurquoiseBright,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "خیالت راحت! مبلغ تمام تعهدات این ماه از حقوقت کنار گذاشته شده",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = AlabasterSilk
                )
            }
        }

        // Empty state when real SMS wiped fake demo commitments and user hasn't added any yet
        if (commitments.isEmpty()) {
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
                        text = "هیچ چک یا قسط فعالی ثبت نشده است",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AlabasterSilk,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "برای مدیریت چک‌های صیادی، اقساط وام یا دُنگ‌های خود، از دکمه زیر استفاده کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrosiOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 2. Purple Sayyad Cheque Hero Card
        if (issuedCheque != null) {
            item {
                SpaciousPurpleSayyadCard(
                    cheque = issuedCheque,
                    onClick = { onCommitmentClick(issuedCheque) }
                )
            }
        }

        // 3. Clean, Spacious Cards Below It with Zero Clutter
        items(
            items = otherCommitments,
            key = { it.id }
        ) { item ->
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
                onClick = { onCommitmentClick(item) }
            )
        }

        // 4. Clean Action Button: + ثبت و استعلام چک جدید
        item {
            Spacer(modifier = Modifier.height(4.dp))
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
                    text = "+ ثبت و استعلام چک جدید",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SpaciousPurpleSayyadCard(
    cheque: CommitmentEntity,
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
            // Title allowed to wrap up to maxLines = 2 (14.sp) on its own full-width row so it is never cut off with "..."
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

            // Due-date badge on its own dedicated row
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
                text = PersianNumberUtils.formatToman(cheque.amountToman, showUnit = true),
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
                text = PersianNumberUtils.formatToman(commitment.amountToman, showUnit = true),
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

        // Due-date badge placed on its own row so title text is never truncated
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
