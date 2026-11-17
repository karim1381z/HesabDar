package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.BankAccountEntity
import com.example.data.local.CommitmentEntity
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
import com.example.ui.theme.OrosiSurfaceHigh
import com.example.ui.theme.PersianLapis
import com.example.ui.theme.PersianLapisDeep
import com.example.ui.theme.PersianLapisGlow
import com.example.ui.theme.PersianLapisLight
import com.example.ui.theme.RubyRose
import com.example.ui.theme.RubyRoseSoft
import com.example.ui.theme.SaffronGoldBright
import com.example.ui.theme.SaffronGoldLight

@Composable
fun DashboardScreen(
    settings: AppSettingsEntity,
    bankAccounts: List<BankAccountEntity>,
    commitments: List<CommitmentEntity>,
    transactions: List<TransactionEntity>,
    isSyncingSms: Boolean = false,
    onToggleHideAmounts: () -> Unit,
    onSetOnlyTankhahMode: (Boolean) -> Unit,
    onToggleAccountTankhah: (String) -> Unit,
    onOpenAccountsTab: () -> Unit,
    onHeroCardClick: () -> Unit,
    onAddVoiceClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onUntaggedMicClick: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalAllAccountsBalance = bankAccounts
        .sumOf { it.balanceToman.coerceAtLeast(0L) }

    val rawTankhahSum = bankAccounts
        .filter { it.isTankhah }
        .sumOf { it.balanceToman.coerceAtLeast(0L) }

    val nonTankhahBalance = bankAccounts
        .filter { !it.isTankhah }
        .sumOf { it.balanceToman.coerceAtLeast(0L) }

    val unpaidCommitmentsToman = commitments
        .filter { !it.isSettled && (it.category == "SAYYAD_ISSUED" || it.category == "LOAN_INSTALLMENT") }
        .sumOf { it.amountToman }

    val tankhahBalance = if (unpaidCommitmentsToman in 1L until rawTankhahSum) {
        (rawTankhahSum - unpaidCommitmentsToman).coerceAtLeast(0L)
    } else {
        rawTankhahSum
    }

    val visibleTransactions = androidx.compose.runtime.remember(transactions) {
        transactions.filter { it.totalAmountToman > 0L }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isSyncingSms) {
            item(key = "sms_sync_banner", contentType = "sync_banner") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(OrosiSurfaceHigh)
                        .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .testTag("sms_sync_loading_card"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        color = NishapurTurquoiseBright,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "در حال بازخوانی و تحلیل پیامک‌های بانکی...",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = AlabasterSilk
                    )
                }
            }
        }

        // 1. Jibeman-Style Financial Status Hero Card ("وضعیت مالی")
        item(key = "hero_card", contentType = "hero_card") {
            JibemanFinancialStatusHeroCard(
                totalAllAccountsBalance = totalAllAccountsBalance,
                unpaidCommitmentsToman = unpaidCommitmentsToman,
                tankhahBalance = tankhahBalance,
                nonTankhahBalance = nonTankhahBalance,
                hideAmounts = settings.hideAmounts,
                tankhahModeEnabled = settings.tankhahModeEnabled,
                onlyTankhahMode = settings.onlyTankhahMode,
                isSyncingSms = isSyncingSms,
                onToggleHideAmounts = onToggleHideAmounts,
                onSetOnlyTankhahMode = onSetOnlyTankhahMode,
                onClick = onHeroCardClick
            )
        }

        // 2. Horizontal "حساب‌ها" (Bank Accounts) Carousel
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حساب‌ها",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AlabasterSilk
                    )
                    Text(
                        text = "مدیریت همه حساب‌ها >",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = NishapurTurquoiseSoft,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenAccountsTab)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("open_accounts_tab_link")
                    )
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_accounts_carousel"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = bankAccounts,
                        key = { it.bankName }
                    ) { account ->
                        BankAccountCarouselCard(
                            account = account,
                            hideAmounts = settings.hideAmounts,
                            showTankhahBadge = settings.tankhahModeEnabled,
                            onCardClick = onOpenAccountsTab,
                            onToggleTankhah = { onToggleAccountTankhah(account.bankName) }
                        )
                    }
                }
            }
        }

        // 3. Section Header "تراکنش‌های اخیر" + Sleek Turquoise Pill Button "+ ثبت صوتی"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تراکنش‌های اخیر",
                    style = MaterialTheme.typography.titleLarge,
                    color = AlabasterSilk
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    NishapurTurquoise,
                                    NishapurTurquoiseDeep
                                )
                            )
                        )
                        .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(50))
                        .clickable(onClick = onAddVoiceClick)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("header_voice_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "+ ثبت صوتی",
                        tint = AlabasterSilk,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ ثبت صوتی",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = AlabasterSilk,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 4. All Recent Transaction Cards (with 2-step swipe-to-delete and cached Jalali dates)
        items(
            items = visibleTransactions,
            key = { tx -> tx.id },
            contentType = { "transaction_card" }
        ) { tx ->
            TwoStepSwipeToDeleteWrapper(
                transactionId = tx.id,
                onConfirmDelete = { onDeleteTransaction(tx) }
            ) {
                CalmTransactionCard(
                    transaction = tx,
                    hideAmounts = settings.hideAmounts,
                    onRowClick = { onTransactionClick(tx) },
                    onUntaggedMicClick = { onUntaggedMicClick(tx) }
                )
            }
        }
    }
}

@Composable
fun TwoStepSwipeToDeleteWrapper(
    transactionId: Long,
    onConfirmDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    var isConfirmOpen by remember(transactionId) { mutableStateOf(false) }
    var dragAccumulated by remember(transactionId) { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(transactionId) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulated = 0f },
                    onDragEnd = {
                        if (kotlin.math.abs(dragAccumulated) > 65f) {
                            isConfirmOpen = !isConfirmOpen
                        }
                        dragAccumulated = 0f
                    },
                    onDragCancel = { dragAccumulated = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulated += dragAmount
                    }
                )
            }
    ) {
        content()

        AnimatedVisibility(visible = isConfirmOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(RubyRose.copy(alpha = 0.24f))
                    .border(1.dp, RubyRoseSoft, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("swipe_delete_confirm_bar_$transactionId"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "حذف این تراکنش؟",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AlabasterSilk,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(RubyRose)
                            .border(1.dp, RubyRoseSoft, RoundedCornerShape(12.dp))
                            .clickable {
                                isConfirmOpen = false
                                onConfirmDelete()
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("confirm_swipe_delete_$transactionId")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف قطعی",
                            tint = AlabasterSilk,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "حذف قطعی",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = AlabasterSilk
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(OrosiSurfaceHigh)
                            .border(1.dp, OrosiHairline, RoundedCornerShape(12.dp))
                            .clickable { isConfirmOpen = false }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("cancel_swipe_delete_$transactionId")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "انصراف",
                            tint = OrosiOnSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "انصراف",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = AlabasterSilk
                        )
                    }
                }
            }
        }
    }
}

private enum class HeroAccountFilter {
    ALL,
    PERSONAL,
    TANKHAH_COMPANY
}

@Composable
private fun JibemanFinancialStatusHeroCard(
    totalAllAccountsBalance: Long,
    unpaidCommitmentsToman: Long,
    tankhahBalance: Long,
    nonTankhahBalance: Long,
    hideAmounts: Boolean,
    tankhahModeEnabled: Boolean,
    onlyTankhahMode: Boolean,
    isSyncingSms: Boolean = false,
    onToggleHideAmounts: () -> Unit,
    onSetOnlyTankhahMode: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    var selectedFilter by androidx.compose.runtime.remember(tankhahModeEnabled) {
        androidx.compose.runtime.mutableStateOf(
            if (!tankhahModeEnabled) HeroAccountFilter.ALL
            else if (onlyTankhahMode) HeroAccountFilter.TANKHAH_COMPANY
            else HeroAccountFilter.PERSONAL
        )
    }

    val effectiveFilter = if (!tankhahModeEnabled) HeroAccountFilter.ALL else selectedFilter

    val selectedTotalBalance = when (effectiveFilter) {
        HeroAccountFilter.ALL -> totalAllAccountsBalance
        HeroAccountFilter.PERSONAL -> nonTankhahBalance
        HeroAccountFilter.TANKHAH_COMPANY -> tankhahBalance
    }

    val freeBalance = (selectedTotalBalance - unpaidCommitmentsToman).coerceAtLeast(0L)

    val animatedFreeBalanceFloat by animateFloatAsState(
        targetValue = freeBalance.toFloat(),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "animatedFreeBalance"
    )
    val animatedTotalBalanceFloat by animateFloatAsState(
        targetValue = selectedTotalBalance.toFloat(),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "animatedTotalBalance"
    )
    val displayedFreeBalance = kotlin.math.round(animatedFreeBalanceFloat.toDouble()).toLong().coerceAtLeast(0L)
    val displayedTotalBalance = kotlin.math.round(animatedTotalBalanceFloat.toDouble()).toLong().coerceAtLeast(0L)

    val denom = (selectedTotalBalance + unpaidCommitmentsToman).coerceAtLeast(1L)
    val progressRatio = if (selectedTotalBalance > 0L) {
        (freeBalance.toFloat() / denom.toFloat()).coerceIn(0.12f, 1f)
    } else {
        0.5f
    }

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
                        PersianLapisLight.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .clickable(onClick = onClick)
            .testTag("dashboard_hero_card")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = NishapurTurquoiseBright.copy(alpha = 0.08f),
                radius = size.height * 0.6f,
                center = Offset(size.width * 0.15f, size.height * 0.2f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Top Row: "وضعیت مالی" on the right, Eye icon (👁) on the left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "وضعیت مالی",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = AlabasterSilk
                )

                IconButton(
                    onClick = onToggleHideAmounts,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .testTag("toggle_hide_amounts_button")
                ) {
                    Icon(
                        imageVector = if (hideAmounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (hideAmounts) "نمایش مبالغ" else "مخفی کردن مبالغ",
                        tint = AlabasterSilk,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // If tankhahModeEnabled == true: show a compact 3-chip filter bar ("همه حساب‌ها" | "شخصی" | "تنخواه / شرکت")
            if (tankhahModeEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.28f))
                        .border(1.dp, PersianLapisLight.copy(alpha = 0.32f), RoundedCornerShape(50))
                        .padding(3.dp)
                        .testTag("hero_mode_switcher"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chips = listOf(
                        Triple(HeroAccountFilter.ALL, "همه حساب‌ها", "mode_all_accounts_button"),
                        Triple(HeroAccountFilter.PERSONAL, "شخصی", "mode_personal_accounts_button"),
                        Triple(HeroAccountFilter.TANKHAH_COMPANY, "تنخواه / شرکت", "mode_tankhah_only_button")
                    )
                    chips.forEach { (filterMode, label, tag) ->
                        val isSelected = effectiveFilter == filterMode
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) NishapurTurquoise else Color.Transparent)
                                .clickable {
                                    selectedFilter = filterMode
                                    when (filterMode) {
                                        HeroAccountFilter.TANKHAH_COMPANY -> onSetOnlyTankhahMode(true)
                                        HeroAccountFilter.PERSONAL -> onSetOnlyTankhahMode(false)
                                        HeroAccountFilter.ALL -> onSetOnlyTankhahMode(false)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag(tag)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = if (isSelected) AlabasterSilk else PersianLapisLight,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Center Subtitle (13.sp): "موجودی آزاد" (or "موجودی شخصی" / "موجودی تنخواه و شرکت")
            // Center Big Number (28.sp bold, single-line): "۵,۳۴۳,۲۰۰ تومان"
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val centerSubtitle = when (effectiveFilter) {
                    HeroAccountFilter.ALL -> "موجودی آزاد"
                    HeroAccountFilter.PERSONAL -> "موجودی شخصی"
                    HeroAccountFilter.TANKHAH_COMPANY -> "موجودی تنخواه و شرکت"
                }

                Text(
                    text = centerSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = PersianLapisLight,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = PersianNumberUtils.formatToman(
                        displayedFreeBalance,
                        showUnit = true,
                        hideAmount = hideAmounts
                    ),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp
                    ),
                    color = AlabasterSilk,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.testTag("hero_peace_of_mind_amount")
                )

                AnimatedVisibility(visible = isSyncingSms) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        LinearProgressIndicator(
                            color = NishapurTurquoiseBright,
                            trackColor = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier
                                .width(140.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "در حال به‌روزرسانی مانده بانک‌ها...",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = NishapurTurquoiseSoft
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal progress bar
            LinearProgressIndicator(
                progress = { progressRatio },
                color = NishapurTurquoiseBright,
                trackColor = Color.White.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Two clean summary rows below the progress bar (matching Jibeman):
            // 1) Green dot (●): "موجودی کل" on the right, amount on the left.
            // 2) Red/Coral dot (●): "تعهدات مالی این ماه" on the right, commitment amount (۰ تومان) on the left.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "موجودی کل",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = PersianLapisLight
                        )
                    }
                    Text(
                        text = PersianNumberUtils.formatToman(
                            displayedTotalBalance,
                            showUnit = true,
                            hideAmount = hideAmounts
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NishapurTurquoiseSoft,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(RubyRoseSoft)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تعهدات مالی این ماه",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = PersianLapisLight
                        )
                    }
                    Text(
                        text = PersianNumberUtils.formatToman(
                            unpaidCommitmentsToman,
                            showUnit = true,
                            hideAmount = hideAmounts
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = RubyRoseSoft,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun BankAccountCarouselCard(
    account: BankAccountEntity,
    hideAmounts: Boolean,
    showTankhahBadge: Boolean,
    onCardClick: () -> Unit,
    onToggleTankhah: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(184.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(OrosiSurfaceHigh)
            .border(
                width = if (showTankhahBadge && account.isTankhah) 1.5.dp else 1.dp,
                color = if (showTankhahBadge && account.isTankhah) NishapurTurquoiseBright.copy(alpha = 0.65f) else OrosiHairline,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onCardClick)
            .padding(14.dp)
            .testTag("carousel_account_${account.bankName}"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                com.example.ui.components.BankLogoAvatar(
                    bankName = account.bankName,
                    size = 44.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = account.bankName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AlabasterSilk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ONLY show the "تنخواه" pill badge when tankhahModeEnabled == true
            if (showTankhahBadge) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (account.isTankhah) NishapurTurquoiseDeep else OrosiSurfaceContainer
                        )
                        .border(
                            1.dp,
                            if (account.isTankhah) NishapurTurquoiseBright else OrosiHairline,
                            RoundedCornerShape(50)
                        )
                        .clickable(onClick = onToggleTankhah)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("carousel_tankhah_badge_${account.bankName}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (account.isTankhah) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NishapurTurquoiseBright,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Text(
                            text = if (account.isTankhah) "تنخواه" else "شخصی",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (account.isTankhah) NishapurTurquoiseSoft else OrosiOnSurfaceVariant
                        )
                    }
                }
            }
        }

        Column {
            Text(
                text = "آخرین مانده:",
                style = MaterialTheme.typography.labelSmall,
                color = OrosiOnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = PersianNumberUtils.formatToman(
                    account.balanceToman,
                    showUnit = true,
                    hideAmount = hideAmounts
                ),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                ),
                color = AlabasterSilk,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun CalmTransactionCard(
    transaction: TransactionEntity,
    hideAmounts: Boolean = false,
    onRowClick: () -> Unit,
    onUntaggedMicClick: () -> Unit
) {
    val amountColor = when (transaction.type) {
        "INCOME" -> NishapurTurquoiseSoft
        "GOLD_SAVE" -> SaffronGoldLight
        "SELF_TRANSFER" -> OrosiOnSurfaceVariant
        else -> RubyRoseSoft
    }

    val shortBank = androidx.compose.runtime.remember(transaction.bankName, transaction.cardInfo) {
        transaction.shortBankName
    }
    val rowTitle = androidx.compose.runtime.remember(
        transaction.isUntagged,
        transaction.title,
        transaction.purpose,
        transaction.storeName,
        shortBank
    ) {
        if (transaction.isUntagged) {
            "خرید کارتی ($shortBank)"
        } else {
            transaction.title.ifBlank { transaction.displayTitle }
        }
    }
    val subtitleText = androidx.compose.runtime.remember(transaction.timestamp, shortBank) {
        transaction.simpleSubtitle
    }
    val formattedSingleLineAmount = androidx.compose.runtime.remember(
        transaction.type,
        transaction.userShareToman,
        transaction.totalAmountToman,
        hideAmounts
    ) {
        when (transaction.type) {
            "INCOME" -> "+${PersianNumberUtils.formatToman(transaction.userShareToman, showUnit = true, hideAmount = hideAmounts)}"
            "SELF_TRANSFER" -> PersianNumberUtils.formatToman(transaction.totalAmountToman, showUnit = true, hideAmount = hideAmounts)
            else -> PersianNumberUtils.formatToman(transaction.userShareToman, showUnit = true, hideAmount = hideAmounts)
        }
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
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("transaction_item_${transaction.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Official 44.dp circular Bank Logo Avatar
        com.example.ui.components.BankLogoAvatar(
            bankName = shortBank,
            size = 44.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rowTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                ),
                color = AlabasterSilk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = OrosiOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("transaction_subtitle_${transaction.id}")
            )
            if (transaction.isUntagged) {
                Spacer(modifier = Modifier.height(6.dp))
                PulsingTurquoiseMicButton(
                    onClick = onUntaggedMicClick,
                    testTag = "untagged_mic_button_${transaction.id}"
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = formattedSingleLineAmount,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
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
