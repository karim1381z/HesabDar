package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BankAccountEntity
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
import com.example.ui.theme.PersianLapisDeep
import com.example.ui.theme.PersianLapisGlow
import com.example.ui.theme.PersianLapisLight
import com.example.ui.theme.SaffronGoldLight

@Composable
fun AccountsScreen(
    bankAccounts: List<BankAccountEntity>,
    hideAmounts: Boolean,
    tankhahModeEnabled: Boolean = false,
    onSetTankhahModeEnabled: (Boolean) -> Unit = {},
    onToggleTankhah: (String) -> Unit,
    onOpenAddAccountModal: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    val totalAllAccountsToman = bankAccounts
        .sumOf { it.balanceToman.coerceAtLeast(0L) }
    val tankhahAccountsToman = bankAccounts
        .filter { it.isTankhah }
        .sumOf { it.balanceToman.coerceAtLeast(0L) }
    val nonTankhahAccountsToman = bankAccounts
        .filter { !it.isTankhah }
        .sumOf { it.balanceToman.coerceAtLeast(0L) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("accounts_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Summary Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PersianLapisGlow,
                                PersianLapis,
                                PersianLapisDeep
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(800f, 600f)
                        )
                    )
                    .border(1.dp, PersianLapisLight.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .testTag("accounts_summary_card"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (tankhahModeEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NishapurTurquoiseDeep.copy(alpha = 0.55f))
                            .border(1.dp, NishapurTurquoiseBright.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "موجودی تنخواه (خرج روزمره):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NishapurTurquoiseSoft
                        )
                        Text(
                            text = PersianNumberUtils.formatToman(
                                tankhahAccountsToman,
                                showUnit = true,
                                hideAmount = hideAmounts
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = AlabasterSilk,
                            modifier = Modifier.testTag("tankhah_accounts_balance_text")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PersianLapis.copy(alpha = 0.65f))
                            .border(1.dp, PersianLapisLight.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سایر حساب‌ها (پس‌انداز):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = PersianLapisLight
                        )
                        Text(
                            text = PersianNumberUtils.formatToman(
                                nonTankhahAccountsToman,
                                showUnit = true,
                                hideAmount = hideAmounts
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = SaffronGoldLight,
                            modifier = Modifier.testTag("total_accounts_balance_text")
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NishapurTurquoiseDeep.copy(alpha = 0.55f))
                            .border(1.dp, NishapurTurquoiseBright.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "موجودی کل حساب‌ها:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NishapurTurquoiseSoft
                        )
                        Text(
                            text = PersianNumberUtils.formatToman(
                                totalAllAccountsToman,
                                showUnit = true,
                                hideAmount = hideAmounts
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = AlabasterSilk,
                            modifier = Modifier.testTag("total_accounts_balance_text")
                        )
                    }
                }
            }
        }

        // 2. Clean Switch: "فعالسازی حالت حساب تنخواه (تفکیک خرج روزمره از پس‌انداز)"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(OrosiSurfaceHigh)
                    .border(
                        1.dp,
                        if (tankhahModeEnabled) NishapurTurquoiseBright.copy(alpha = 0.55f) else OrosiHairline,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("enable_tankhah_mode_card"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "حساب تنخواه (تفکیک خرج روزمره)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = AlabasterSilk,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (tankhahModeEnabled) {
                            "فعال: کارت‌های خرج روزمره از پس‌انداز (سایر) تفکیک شده‌اند"
                        } else {
                            "غیرفعال (پیش‌فرض): مجموع کل حساب‌ها یکجا محاسبه می‌شود"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = OrosiOnSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = tankhahModeEnabled,
                    onCheckedChange = onSetTankhahModeEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AlabasterSilk,
                        checkedTrackColor = NishapurTurquoise
                    ),
                    modifier = Modifier.testTag("enable_tankhah_mode_switch")
                )
            }
        }

        // 3. Section header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "منابع مالی و حساب‌های بانکی",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AlabasterSilk
                )
                Text(
                    text = "${PersianNumberUtils.toPersianDigits(bankAccounts.size.toString())} حساب فعال",
                    style = MaterialTheme.typography.labelMedium,
                    color = OrosiOnSurfaceVariant
                )
            }
        }

        // 4. List of all bank accounts detected from SMS
        items(
            items = bankAccounts,
            key = { it.bankName }
        ) { account ->
            BankAccountRowCard(
                account = account,
                hideAmounts = hideAmounts,
                tankhahModeEnabled = tankhahModeEnabled,
                onToggleTankhah = { onToggleTankhah(account.bankName) }
            )
        }

        // 5. Button: "افزودن منبع مالی"
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onOpenAddAccountModal,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NishapurTurquoise,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(18.dp))
                    .testTag("add_financial_source_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "افزودن منبع مالی",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BankAccountRowCard(
    account: BankAccountEntity,
    hideAmounts: Boolean,
    tankhahModeEnabled: Boolean,
    onToggleTankhah: () -> Unit
) {
    val lastUpdatedJalali = JalaliCalendarUtils.fromTimestamp(account.lastUpdatedTimestamp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(OrosiSurfaceHigh)
            .border(
                1.dp,
                if (tankhahModeEnabled && account.isTankhah) NishapurTurquoiseBright.copy(alpha = 0.45f) else OrosiHairline,
                RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
            .testTag("account_card_${account.bankName}"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PersianLapis)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = account.bankName,
                        tint = NishapurTurquoiseSoft,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "بانک ${account.bankName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AlabasterSilk
                    )
                    Text(
                        text = if (account.isManual) {
                            "منبع مالی دستی • ${lastUpdatedJalali.dateWithYear}"
                        } else {
                            "شناسایی خودکار از پیامک • ${lastUpdatedJalali.dateWithYear}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = OrosiOnSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = PersianNumberUtils.formatToman(
                        account.balanceToman,
                        showUnit = true,
                        hideAmount = hideAmounts
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp
                    ),
                    color = if (account.balanceToman > 0L) NishapurTurquoiseSoft else SaffronGoldLight
                )
                Text(
                    text = "مانده حساب",
                    style = MaterialTheme.typography.labelSmall,
                    color = OrosiOnSurfaceVariant
                )
            }
        }

        // Toggle row: "حساب تنخواه (خرج روزمره)" ONLY shown when tankhahModeEnabled == true
        if (tankhahModeEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OrosiSurfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "حساب تنخواه (خرج روزمره)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = AlabasterSilk
                )
                Switch(
                    checked = account.isTankhah,
                    onCheckedChange = { onToggleTankhah() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AlabasterSilk,
                        checkedTrackColor = NishapurTurquoise
                    ),
                    modifier = Modifier.testTag("account_tankhah_switch_${account.bankName}")
                )
            }
        }
    }
}
