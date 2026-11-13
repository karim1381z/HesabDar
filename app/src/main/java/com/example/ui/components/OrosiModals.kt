package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsEntity
import com.example.domain.ParsedSmsResult
import com.example.domain.PersianNumberUtils
import com.example.ui.theme.AlabasterSilk
import com.example.ui.theme.NishapurTurquoise
import com.example.ui.theme.NishapurTurquoiseBright
import com.example.ui.theme.NishapurTurquoiseSoft
import com.example.ui.theme.OrosiHairline
import com.example.ui.theme.OrosiOnSurfaceVariant
import com.example.ui.theme.OrosiSurfaceContainer
import com.example.ui.theme.OrosiSurfaceHigh
import com.example.ui.theme.OrosiSurfaceLow
import com.example.ui.theme.PersianLapis
import com.example.ui.theme.PersianLapisLight
import com.example.ui.theme.RoyalAmethyst
import com.example.ui.theme.RoyalAmethystSoft
import com.example.ui.theme.RubyRoseSoft
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SaffronGoldBright
import com.example.ui.theme.SaffronGoldLight

data class BankSmsSample(
    val label: String,
    val smsText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSimulatorBottomSheet(
    onPreviewSms: (String) -> ParsedSmsResult,
    onProcessSms: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val samples = remember {
        listOf(
            BankSmsSample(
                label = "بلوبانک (خرید بی‌نام)",
                smsText = "بلوبانک\nبرداشت: ۲,۴۰۰,۰۰۰ ریال\nکارت: ۶۲۱۹\nجمعه ۱ آبان ۱۸:۱۰"
            ),
            BankSmsSample(
                label = "بانک ملت (خرید کارتی)",
                smsText = "بانک ملت\nبرداشت مبلغ ۱,۸۵۰,۰۰۰ ریال\nکارت ۸۴۱۲\nجمعه ۱۴۰۳/۰۸/۰۱-۱۹:۲۰"
            ),
            BankSmsSample(
                label = "بانک ملی (خرید روزمره)",
                smsText = "بانک ملی ایران\nخرید کارتی: ۳,۲۰۰,۰۰۰ ریال\nکارت: ۵۰۴۱\nپنجشنبه ۳۰ مهر ۲۰:۴۵"
            ),
            BankSmsSample(
                label = "بانک سامان (واریز حقوق)",
                smsText = "بانک سامان\nواریز پایا حقوق: ۳۸۰,۰۰۰,۰۰۰ ریال\nکارت: ۷۳۹۰\nجمعه ۱ آبان ۰۸:۳۰"
            ),
            BankSmsSample(
                label = "بانک رسالت (انتقال خودی)",
                smsText = "بانک قرض‌الحسنه رسالت\nانتقال به کارت خود ۶۲۱۹ از کارت ۱۱۸۴\nمبلغ: ۱۵,۰۰۰,۰۰۰ ریال\nجمعه ۱ آبان ۱۷:۱۵"
            ),
            BankSmsSample(
                label = "کارمزد خرد (فیلتر خودکار)",
                smsText = "بلوبانک\nبرداشت کارمزد پیامک: ۱۲,۰۰۰ ریال\nکارت ۶۲۱۹\nجمعه ۱ آبان ۱۲:۰۰"
            )
        )
    }

    var rawSmsText by remember { mutableStateOf(samples.first().smsText) }
    val preview = remember(rawSmsText) { onPreviewSms(rawSmsText) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrosiSurfaceLow,
        contentColor = AlabasterSilk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("sms_simulator_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = null,
                        tint = NishapurTurquoiseBright,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شبیه‌ساز پیامک بانکی (موتور آفلاین)",
                        style = MaterialTheme.typography.titleLarge,
                        color = AlabasterSilk
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = OrosiOnSurfaceVariant
                    )
                }
            }

            Text(
                text = "تبدیل خودکار ریال به تومان (تقسیم بر ۱۰)، استخراج روز هفته و کارت، حذف کارمزد خرد و تشخیص کارت‌به‌کارت خودی",
                style = MaterialTheme.typography.bodySmall,
                color = OrosiOnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Bank Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                samples.forEachIndexed { idx, sample ->
                    val selected = rawSmsText == sample.smsText
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) NishapurTurquoise.copy(alpha = 0.28f)
                                else OrosiSurfaceContainer
                            )
                            .border(
                                1.dp,
                                if (selected) NishapurTurquoiseBright else OrosiHairline,
                                RoundedCornerShape(50)
                            )
                            .clickable { rawSmsText = sample.smsText }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("sms_preset_$idx")
                    ) {
                        Text(
                            text = sample.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = AlabasterSilk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = rawSmsText,
                onValueChange = { rawSmsText = it },
                label = { Text("متن پیامک بانک (بلو، ملت، ملی، سامان، رسالت)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sms_input_field"),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NishapurTurquoiseBright,
                    unfocusedBorderColor = OrosiHairline,
                    focusedTextColor = AlabasterSilk,
                    unfocusedTextColor = AlabasterSilk
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Live Parsed Preview Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrosiSurfaceHigh)
                    .border(1.dp, NishapurTurquoise.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "پیش‌نمایش استخراج آنی:",
                    style = MaterialTheme.typography.labelMedium,
                    color = NishapurTurquoiseSoft
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "مبلغ به تومان:",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrosiOnSurfaceVariant
                    )
                    Text(
                        text = "${PersianNumberUtils.formatToman(preview.amountToman, true)} (از ${PersianNumberUtils.formatToman(preview.rawAmountRial)} ریال)",
                        style = MaterialTheme.typography.titleSmall,
                        color = AlabasterSilk
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "زیرنویس استاندارد:",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrosiOnSurfaceVariant
                    )
                    Text(
                        text = preview.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = NishapurTurquoiseBright
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preview.summaryMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = SaffronGoldLight
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onProcessSms(rawSmsText) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NishapurTurquoise,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("process_sms_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "پردازش پیامک و بروزرسانی خیال راحت امروز",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaydayAndBackupBottomSheet(
    settings: AppSettingsEntity,
    backupJsonText: String,
    onSaveSettings: (Int, String, Boolean, Long, Long) -> Unit,
    onImportBackup: (String) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    var cycleLabel by remember(settings) { mutableStateOf(settings.paydayCycleLabel) }
    var paydayDayStr by remember(settings) {
        mutableStateOf(PersianNumberUtils.toPersianDigits(settings.paydayStartDay.toString()))
    }
    var autoDetectSalary by remember(settings) { mutableStateOf(settings.autoDetectSalary) }
    var goldPriceStr by remember(settings) {
        mutableStateOf(PersianNumberUtils.toPersianDigits(settings.goldPrice18kToman.toString()))
    }
    var dailySafeStr by remember(settings) {
        mutableStateOf(PersianNumberUtils.toPersianDigits(settings.dailySafeBudgetToman.toString()))
    }
    var editableBackupJson by remember(backupJsonText) { mutableStateOf(backupJsonText) }
    var showBackupBox by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrosiSurfaceLow,
        contentColor = AlabasterSilk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("payday_backup_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تنظیمات ماه حقوقی، نرخ طلا و پشتیبان",
                    style = MaterialTheme.typography.titleLarge,
                    color = AlabasterSilk
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = OrosiOnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = cycleLabel,
                onValueChange = { cycleLabel = it },
                label = { Text("بازه ماه حقوقی") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NishapurTurquoiseBright,
                    unfocusedBorderColor = OrosiHairline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = paydayDayStr,
                    onValueChange = { paydayDayStr = PersianNumberUtils.toPersianDigits(it) },
                    label = { Text("روز واریز حقوق") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NishapurTurquoiseBright,
                        unfocusedBorderColor = OrosiHairline
                    )
                )

                OutlinedTextField(
                    value = dailySafeStr,
                    onValueChange = { dailySafeStr = PersianNumberUtils.toPersianDigits(it) },
                    label = { Text("خیال راحت امروز (تومان)") },
                    modifier = Modifier.weight(1.4f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NishapurTurquoiseBright,
                        unfocusedBorderColor = OrosiHairline
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = goldPriceStr,
                onValueChange = { goldPriceStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("نرخ لحظه‌ای هر گرم طلای ۱۸ عیار (تومان)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gold_price_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SaffronGoldBright,
                    unfocusedBorderColor = OrosiHairline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OrosiSurfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تشخیص خودکار آغاز ماه حقوقی با پیامک حقوق",
                        style = MaterialTheme.typography.titleSmall,
                        color = AlabasterSilk
                    )
                    Text(
                        text = "با دریافت پیامک واریز حقوق، تراز ماه به صورت خودکار بازتنظیم می‌شود",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrosiOnSurfaceVariant
                    )
                }
                Switch(
                    checked = autoDetectSalary,
                    onCheckedChange = { autoDetectSalary = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AlabasterSilk,
                        checkedTrackColor = NishapurTurquoise
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val day = PersianNumberUtils.toEnglishDigits(paydayDayStr).toIntOrNull() ?: 1
                    val goldPrice = PersianNumberUtils.toEnglishDigits(goldPriceStr).toLongOrNull() ?: 4_360_000L
                    val dailySafe = PersianNumberUtils.toEnglishDigits(dailySafeStr).toLongOrNull() ?: 680_000L
                    onSaveSettings(day, cycleLabel, autoDetectSalary, goldPrice, dailySafe)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PersianLapis,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PersianLapisLight, RoundedCornerShape(14.dp))
                    .testTag("save_payday_settings_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ذخیره تنظیمات ماه حقوقی و نرخ طلا",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Local Database Backup / Restore Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showBackupBox = !showBackupBox },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_backup_json_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = null,
                        tint = NishapurTurquoiseBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showBackupBox) "بستن پشتیبان" else "خروجی / بازیابی بک‌آپ",
                        color = AlabasterSilk,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onResetDefault,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = SaffronGoldBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "بازنشانی پیش‌فرض",
                        color = SaffronGoldLight,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (showBackupBox) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = editableBackupJson,
                    onValueChange = { editableBackupJson = it },
                    label = { Text("پشتیبان JSON دیتابیس محلی (قابل کپی یا جایگذاری)") },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NishapurTurquoiseBright,
                        unfocusedBorderColor = OrosiHairline
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onImportBackup(editableBackupJson) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NishapurTurquoise,
                        contentColor = AlabasterSilk
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("بازیابی اطلاعات از JSON بالا")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCommitmentBottomSheet(
    onAddCommitment: (String, String, String, Long, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf("SAYYAD_ISSUED") }
    var title by remember { mutableStateOf("چک صیادی خرید تجهیزات") }
    var counterparty by remember { mutableStateOf("در وجه: فروشگاه دیجیتال آریا") }
    var amountStr by remember { mutableStateOf("۱۲,۰۰۰,۰۰۰") }
    var dueDateStr by remember { mutableStateOf("سررسید: ۳۰ آبان ۱۴۰۳") }
    var sayyadId by remember { mutableStateOf("۴۰۳۷۷۸۱۹۲۰۳۸۴۷۵۶") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrosiSurfaceLow,
        contentColor = AlabasterSilk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("add_commitment_sheet")
        ) {
            Text(
                text = "+ ثبت و استعلام چک جدید / تعهد مالی",
                style = MaterialTheme.typography.titleLarge,
                color = AlabasterSilk
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cats = listOf(
                    "SAYYAD_ISSUED" to "چک صادره",
                    "SAYYAD_RECEIVED" to "چک دریافتی",
                    "LOAN_INSTALLMENT" to "قسط وام",
                    "DONG" to "دُنگ"
                )
                cats.forEach { (key, label) ->
                    val selected = category == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) RoyalAmethyst else OrosiSurfaceContainer)
                            .clickable { category = key }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = AlabasterSilk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان چک یا تعهد") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("مبلغ (تومان)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = counterparty,
                onValueChange = { counterparty = it },
                label = { Text("طرف حساب / بانک") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dueDateStr,
                onValueChange = { dueDateStr = it },
                label = { Text("تاریخ سررسید") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sayyadId,
                onValueChange = { sayyadId = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("شناسه ۱۶ رقمی صیادی") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amount = PersianNumberUtils.toEnglishDigits(amountStr).toLongOrNull() ?: 5_000_000L
                    onAddCommitment(category, title, counterparty, amount, dueDateStr, sayyadId)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalAmethyst,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_new_commitment_button")
            ) {
                Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "استعلام وضعیت سفید و ثبت در تعهدات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertSurplusToGoldBottomSheet(
    settings: AppSettingsEntity,
    onConvert: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amountStr by remember { mutableStateOf("۱,۵۰۰,۰۰۰") }
    val amountToman = PersianNumberUtils.toEnglishDigits(amountStr).toLongOrNull() ?: 1_500_000L
    val equivalentGrams = amountToman.toDouble() / settings.goldPrice18kToman.coerceAtLeast(1L).toDouble()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrosiSurfaceLow,
        contentColor = AlabasterSilk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("convert_gold_sheet")
        ) {
            Text(
                text = "تبدیل مازاد تراز به طلای ۱۸ عیار",
                style = MaterialTheme.typography.titleLarge,
                color = AlabasterSilk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "نرخ لحظه‌ای طلای ۱۸ عیار: ${PersianNumberUtils.formatToman(settings.goldPrice18kToman, true)}",
                style = MaterialTheme.typography.bodySmall,
                color = SaffronGoldLight
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("مبلغ تبدیل از مازاد تراز (تومان)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SaffronGoldBright,
                    unfocusedBorderColor = OrosiHairline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "معادل طلای دریافتی: ${PersianNumberUtils.formatDecimal(equivalentGrams, 3)} گرم طلای ۱۸ عیار",
                style = MaterialTheme.typography.titleSmall,
                color = NishapurTurquoiseBright
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onConvert(amountToman) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaffronGold,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_convert_gold_button")
            ) {
                Icon(imageVector = Icons.Default.CurrencyExchange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تایید تبدیل و افزودن به سپر تورمی",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
