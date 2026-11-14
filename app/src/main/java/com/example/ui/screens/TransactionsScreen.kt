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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.domain.JalaliCalendarUtils
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
import com.example.ui.theme.PersianLapisLight
import com.example.ui.theme.RubyRose
import com.example.ui.theme.RubyRoseSoft
import com.example.ui.theme.SaffronGoldLight

private val jalaliMonthNames = arrayOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)

private data class DayTransactionGroup(
    val dateSortKey: Int,
    val dateHeaderLabel: String,
    val transactions: List<TransactionEntity>
)

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    selectedYearMonthKey: Int,
    hideAmounts: Boolean,
    onChangeMonth: (Int) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onUntaggedMicClick: (TransactionEntity) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    val currentRealYearMonthKey = remember {
        JalaliCalendarUtils.fromTimestamp(System.currentTimeMillis()).yearMonthKey
    }
    val isNextMonthDisabled = selectedYearMonthKey >= currentRealYearMonthKey

    val selectedYear = selectedYearMonthKey / 100
    val selectedMonth = (selectedYearMonthKey % 100).coerceIn(1, 12)
    val monthName = jalaliMonthNames[selectedMonth - 1]
    val monthHeaderLabel = "$monthName ${PersianNumberUtils.toPersianDigits(selectedYear.toString())}"

    // Strictly filter by BOTH jalaliMonth == selectedMonth AND jalaliYear == selectedYear (never fall back to all transactions!)
    val monthTransactions = remember(transactions, selectedYear, selectedMonth) {
        transactions
            .filter { tx ->
                tx.totalAmountToman > 0L && run {
                    val j = JalaliCalendarUtils.fromTimestamp(tx.timestamp)
                    j.year == selectedYear && j.month == selectedMonth
                }
            }
            .sortedWith(
                compareByDescending<TransactionEntity> { it.timestamp }
                    .thenByDescending { it.id }
            )
    }

    val monthlyIncomeToman = remember(monthTransactions) {
        monthTransactions.filter { it.type == "INCOME" }.sumOf { it.userShareToman }
    }
    val monthlyExpenseToman = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.userShareToman }
    }

    // Group by exact Jalali day and sort date groups strictly by timestampMillis / dateSortKey DESC
    val sortedDayGroups = remember(monthTransactions) {
        monthTransactions
            .groupBy { tx ->
                val j = JalaliCalendarUtils.fromTimestamp(tx.timestamp)
                j.year * 10000 + j.month * 100 + j.day
            }
            .entries
            .sortedByDescending { it.key }
            .map { (sortKey, dayTxs) ->
                val sortedDayTxs = dayTxs.sortedWith(
                    compareByDescending<TransactionEntity> { it.timestamp }
                        .thenByDescending { it.id }
                )
                val headerLabel = JalaliCalendarUtils.fromTimestamp(sortedDayTxs.first().timestamp).dateWithYear
                DayTransactionGroup(
                    dateSortKey = sortKey,
                    dateHeaderLabel = headerLabel,
                    transactions = sortedDayTxs
                )
            }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Month Selector Header (< آبان ۱۴۰۵ >)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(OrosiSurfaceHigh)
                    .border(1.dp, OrosiHairline, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("month_selector_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onChangeMonth(-1) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PersianLapis)
                        .testTag("prev_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "ماه قبل",
                        tint = AlabasterSilk
                    )
                }

                Text(
                    text = monthHeaderLabel,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = AlabasterSilk,
                    modifier = Modifier.testTag("selected_month_label")
                )

                IconButton(
                    onClick = { if (!isNextMonthDisabled) onChangeMonth(1) },
                    enabled = !isNextMonthDisabled,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isNextMonthDisabled) OrosiSurfaceContainer else PersianLapis)
                        .alpha(if (isNextMonthDisabled) 0.38f else 1f)
                        .testTag("next_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "ماه بعد",
                        tint = if (isNextMonthDisabled) OrosiOnSurfaceVariant else AlabasterSilk
                    )
                }
            }
        }

        // 2. Two Summary Cards at Top: "دریافتی ماه (سبز)" and "پرداختی ماه (کهربایی/قرمز ملایم)"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // دریافتی ماه (سبز / فیروزه‌ای)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NishapurTurquoiseDeep.copy(alpha = 0.55f))
                        .border(1.dp, NishapurTurquoiseBright.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                        .testTag("monthly_income_summary_card")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDownward,
                            contentDescription = null,
                            tint = NishapurTurquoiseBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "دریافتی ماه",
                            style = MaterialTheme.typography.labelLarge,
                            color = NishapurTurquoiseSoft
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = PersianNumberUtils.formatToman(
                            monthlyIncomeToman,
                            showUnit = true,
                            hideAmount = hideAmounts
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        ),
                        color = AlabasterSilk,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // پرداختی ماه (کهربایی / قرمز ملایم)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(RubyRose.copy(alpha = 0.22f))
                        .border(1.dp, RubyRoseSoft.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                        .testTag("monthly_expense_summary_card")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowUpward,
                            contentDescription = null,
                            tint = RubyRoseSoft,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "پرداختی ماه",
                            style = MaterialTheme.typography.labelLarge,
                            color = RubyRoseSoft
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = PersianNumberUtils.formatToman(
                            monthlyExpenseToman,
                            showUnit = true,
                            hideAmount = hideAmounts
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        ),
                        color = AlabasterSilk,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (sortedDayGroups.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(OrosiSurfaceContainer)
                        .border(1.dp, OrosiHairline, RoundedCornerShape(20.dp))
                        .padding(28.dp)
                        .testTag("empty_month_transactions_card"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "در این ماه تراکنشی ثبت نشده است",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AlabasterSilk,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // 3. Transactions Grouped by Jalali Date Headers sorted newest day first (timestampMillis DESC)
            sortedDayGroups.forEach { dayGroup ->
                item(key = "header_${dayGroup.dateSortKey}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dayGroup.dateHeaderLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = PersianLapisLight
                        )
                        Text(
                            text = "${PersianNumberUtils.toPersianDigits(dayGroup.transactions.size.toString())} تراکنش",
                            style = MaterialTheme.typography.labelSmall,
                            color = OrosiOnSurfaceVariant
                        )
                    }
                }

                items(
                    items = dayGroup.transactions,
                    key = { tx -> "month_tx_${tx.id}" }
                ) { tx ->
                    JibemanGroupedTransactionRow(
                        transaction = tx,
                        hideAmounts = hideAmounts,
                        onRowClick = { onTransactionClick(tx) },
                        onUntaggedMicClick = { onUntaggedMicClick(tx) }
                    )
                }
            }
        }
    }
}

@Composable
private fun JibemanGroupedTransactionRow(
    transaction: TransactionEntity,
    hideAmounts: Boolean,
    onRowClick: () -> Unit,
    onUntaggedMicClick: () -> Unit
) {
    val isIncome = transaction.type == "INCOME"
    val accentColor = if (isIncome) NishapurTurquoiseBright else RubyRoseSoft

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(OrosiSurfaceContainer)
            .border(
                1.dp,
                if (transaction.isUntagged) NishapurTurquoise.copy(alpha = 0.4f) else OrosiHairline,
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onRowClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("grouped_tx_${transaction.id}")
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PersianLapis)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = transaction.shortBankName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NishapurTurquoiseSoft
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = transaction.timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = OrosiOnSurfaceVariant
                )
            }

            Text(
                text = (if (isIncome) "+" else "") + PersianNumberUtils.formatToman(
                    transaction.userShareToman.takeIf { it > 0L } ?: transaction.totalAmountToman,
                    showUnit = true,
                    hideAmount = hideAmounts
                ),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp
                ),
                color = accentColor,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Underneath each row: show user's custom note/voice tag or prompt to tag with voice
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasCustomNote = !transaction.isUntagged &&
                transaction.purpose.isNotBlank() &&
                transaction.purpose != "نیازمند تکمیل صوتی" &&
                transaction.purpose != "برداشت کارتی / خرید"

            Text(
                text = if (hasCustomNote) {
                    "بابت: ${transaction.displayTitle}"
                } else {
                    transaction.title
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (hasCustomNote) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (hasCustomNote) SaffronGoldLight else OrosiOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (transaction.isUntagged) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NishapurTurquoise)
                        .clickable(onClick = onUntaggedMicClick)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "بگو بابت چی؟",
                        tint = AlabasterSilk,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "بگو بابت چی؟",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AlabasterSilk
                    )
                }
            }
        }
    }
}
