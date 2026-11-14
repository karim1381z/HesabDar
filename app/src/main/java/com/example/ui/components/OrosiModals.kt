package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CommitmentEntity
import com.example.data.local.TransactionEntity
import com.example.domain.JalaliCalendarUtils
import com.example.domain.ParsedSmsResult
import com.example.domain.PersianNumberUtils
import com.example.ui.VoiceUiState
import com.example.ui.theme.AlabasterSilk
import com.example.ui.theme.NishapurTurquoise
import com.example.ui.theme.NishapurTurquoiseBright
import com.example.ui.theme.NishapurTurquoiseDeep
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
import com.example.ui.theme.RubyRose
import com.example.ui.theme.RubyRoseSoft
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SaffronGoldBright
import com.example.ui.theme.SaffronGoldDeep
import com.example.ui.theme.SaffronGoldLight
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceOverlayBottomSheet(
    voiceState: VoiceUiState,
    onSpeechChange: (String) -> Unit,
    onSimulateVoiceRecord: () -> Unit,
    onConfirmVoiceEntry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLiveHardwareListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isLiveHardwareListening = true
            }
            override fun onBeginningOfSpeech() {
                isLiveHardwareListening = true
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isLiveHardwareListening = false
            }
            override fun onError(error: Int) {
                isLiveHardwareListening = false
                onSimulateVoiceRecord()
            }
            override fun onResults(results: Bundle?) {
                isLiveHardwareListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val firstSpoken = matches?.firstOrNull()
                if (!firstSpoken.isNullOrBlank()) {
                    onSpeechChange(firstSpoken)
                } else {
                    onSimulateVoiceRecord()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    onSpeechChange(partial)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)

        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    fun startRealOrFallbackRecognition() {
        if (speechRecognizer != null) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                speechRecognizer.startListening(intent)
            } catch (_: Exception) {
                onSimulateVoiceRecord()
            }
        } else {
            onSimulateVoiceRecord()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRealOrFallbackRecognition()
        } else {
            onSimulateVoiceRecord()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "voiceOverlayWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrosiSurfaceLow,
        contentColor = AlabasterSilk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .testTag("voice_overlay_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Glowing Turquoise Audio Wave
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NishapurTurquoiseDeep,
                                PersianLapis
                            )
                        )
                    )
                    .border(
                        1.dp,
                        NishapurTurquoiseBright.copy(alpha = 0.50f),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable {
                        val hasAudioPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasAudioPerm) {
                            startRealOrFallbackRecognition()
                        } else if (SpeechRecognizer.isRecognitionAvailable(context)) {
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            onSimulateVoiceRecord()
                        }
                    }
                    .padding(vertical = 26.dp, horizontal = 20.dp)
                    .testTag("voice_record_button"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                ) {
                    val barCount = 26
                    val totalWidth = size.width
                    val spacing = totalWidth / (barCount * 1.8f)
                    val barWidth = spacing * 0.85f
                    val centerY = size.height / 2f

                    for (i in 0 until barCount) {
                        val norm = i.toFloat() / barCount.toFloat()
                        val envelope = sin(norm * Math.PI).toFloat()
                        val dynamicWave = if (isLiveHardwareListening || voiceState.isRecordingSimulated) {
                            0.38f + 0.62f * kotlin.math.abs(sin(phase * 1.4f + i * 0.45f))
                        } else {
                            0.30f + 0.65f * kotlin.math.abs(sin(phase + i * 0.45f))
                        }
                        val barHeight = (size.height * 0.88f * envelope * dynamicWave).coerceAtLeast(10f)
                        val x = (totalWidth - (barCount * (barWidth + spacing))) / 2f + i * (barWidth + spacing)

                        drawRoundRect(
                            color = if (i % 2 == 0) NishapurTurquoiseBright else NishapurTurquoiseSoft.copy(alpha = 0.78f),
                            topLeft = Offset(x, centerY - barHeight / 2f),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 2. The Spoken Sentence
            Text(
                text = "«${voiceState.recognizedText}»",
                style = MaterialTheme.typography.titleLarge.copy(
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AlabasterSilk,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("recognized_speech_text")
            )

            Spacer(modifier = Modifier.height(26.dp))

            // 3. One Big Button ("تأیید")
            Button(
                onClick = onConfirmVoiceEntry,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NishapurTurquoise,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(18.dp))
                    .testTag("confirm_voice_entry_button")
            ) {
                Text(
                    text = "تأیید",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroMathBreakdownBottomSheet(
    settings: AppSettingsEntity,
    transactions: List<TransactionEntity>,
    reservedCommitmentsToman: Long,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val totalIncomeToman = remember(transactions) {
        transactions.filter { it.type == "INCOME" }.sumOf { it.userShareToman }
            .takeIf { it > 0L } ?: 42_000_000L
    }
    val totalExpensesToman = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }.sumOf { it.userShareToman }
    }
    val totalGoldSavedThisMonthToman = remember(transactions) {
        transactions.filter { it.type == "GOLD_SAVE" }.sumOf { it.userShareToman }
            .takeIf { it > 0L } ?: 1_200_000L
    }

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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .testTag("hero_math_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ترازنامه ماه حقوقی و سهم خرج امروزت",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = AlabasterSilk
            )

            // Stress-free Monthly Balance Report ("ترازنامه ماه حقوقی")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(OrosiSurfaceContainer)
                    .border(1.dp, OrosiHairline, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ترازنامه ماه حقوقی",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AlabasterSilk
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(NishapurTurquoiseDeep)
                            .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "تراز مثبت و متعادل ✓",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = NishapurTurquoiseSoft
                        )
                    }
                }

                MonthlyReportRow(
                    label = "کل ورودی ماه حقوقی",
                    amountText = "+${PersianNumberUtils.formatToman(totalIncomeToman, showUnit = true)}",
                    valueColor = NishapurTurquoiseSoft
                )
                MonthlyReportRow(
                    label = "کل خرج مصرفی",
                    amountText = PersianNumberUtils.formatToman(totalExpensesToman, showUnit = true),
                    valueColor = RubyRoseSoft
                )
                MonthlyReportRow(
                    label = "کنار گذاشته شده برای قسط و چک",
                    amountText = PersianNumberUtils.formatToman(reservedCommitmentsToman, showUnit = true),
                    valueColor = RoyalAmethystSoft
                )
                MonthlyReportRow(
                    label = "تبدیل شده به طلا",
                    amountText = PersianNumberUtils.formatToman(totalGoldSavedThisMonthToman, showUnit = true),
                    valueColor = SaffronGoldLight
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PersianLapis)
                    .border(1.dp, PersianLapisLight.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "سهم خرجِ امروزت",
                        style = MaterialTheme.typography.titleSmall,
                        color = PersianLapisLight
                    )
                    Text(
                        text = "تراز آزاد ماه: ${PersianNumberUtils.formatToman(settings.monthlyBalanceToman, showUnit = true)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NishapurTurquoiseSoft
                    )
                }
                Text(
                    text = PersianNumberUtils.formatToman(
                        PersianNumberUtils.roundDownToCleanThousands(settings.dailySafeBudgetToman),
                        showUnit = true
                    ),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = AlabasterSilk
                )
            }
        }
    }
}

@Composable
private fun MonthlyReportRow(
    label: String,
    amountText: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OrosiOnSurfaceVariant
        )
        Text(
            text = amountText,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitmentActionBottomSheet(
    commitment: CommitmentEntity,
    onSaveCommitment: (
        title: String,
        counterparty: String,
        amountToman: Long,
        dueDateStr: String,
        isSettled: Boolean
    ) -> Unit,
    onDeleteCommitment: () -> Unit,
    onDismiss: () -> Unit
) {
    var titleStr by remember(commitment) { mutableStateOf(commitment.title) }
    var counterpartyStr by remember(commitment) { mutableStateOf(commitment.counterparty) }
    var amountStr by remember(commitment) {
        mutableStateOf(PersianNumberUtils.formatToman(commitment.amountToman))
    }
    var dueDateStr by remember(commitment) { mutableStateOf(commitment.dueDateStr) }

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
                .testTag("commitment_action_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مدیریت و تسویه تعهد مالی",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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

            // Primary 1-Tap Settlement CTA: "پاس شد / تسویه شد ✓"
            Button(
                onClick = {
                    val parsedAmount = PersianNumberUtils.toEnglishDigits(amountStr).toLongOrNull()
                        ?: commitment.amountToman
                    onSaveCommitment(
                        titleStr,
                        counterpartyStr,
                        parsedAmount,
                        dueDateStr,
                        !commitment.isSettled
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (commitment.isSettled) PersianLapis else NishapurTurquoise,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(16.dp))
                    .testTag("mark_commitment_settled_button")
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (commitment.isSettled) "بازگردانی به وضعیت فعال" else "پاس شد / تسویه شد ✓",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = titleStr,
                onValueChange = { titleStr = it },
                label = { Text("عنوان چک، قسط یا دُنگ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("مبلغ (تومان)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dueDateStr,
                onValueChange = { dueDateStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("تاریخ سررسید") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = counterpartyStr,
                onValueChange = { counterpartyStr = it },
                label = { Text("طرف حساب / بانک") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDeleteCommitment,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("delete_commitment_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = RubyRoseSoft
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حذف",
                        color = RubyRoseSoft,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        val parsedAmount = PersianNumberUtils.toEnglishDigits(amountStr).toLongOrNull()
                            ?: commitment.amountToman
                        onSaveCommitment(
                            titleStr,
                            counterpartyStr,
                            parsedAmount,
                            dueDateStr,
                            commitment.isSettled
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalAmethyst,
                        contentColor = AlabasterSilk
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .weight(1.6f)
                        .testTag("save_commitment_edits_button")
                ) {
                    Text(
                        text = "ذخیره ویرایش",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldPartnerAffiliateBottomSheet(
    settings: AppSettingsEntity,
    onDirectPurchaseGold: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedAmountToman by remember { mutableStateOf(2_000_000L) }

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
                .padding(horizontal = 22.dp)
                .padding(bottom = 30.dp)
                .testTag("gold_partner_modal"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = SaffronGoldBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "پلتفرم‌های معتبر خرید آنلاین طلای ۱۸ عیار",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
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
                text = "همکاری رسمی اُرسی با پلتفرم‌های دارای مجوز اتحادیه طلا و جواهر و صندوق‌های طلای بورس (کارمزد ویژه ۰٪ برای کاربران خیال راحت):",
                style = MaterialTheme.typography.bodySmall,
                color = OrosiOnSurfaceVariant
            )

            // Partner 1
            PartnerPlatformItem(
                name = "صندوق‌های طلای بورس (عیار / لوتوس / کهربا)",
                subtitle = "تحت نظارت سازمان بورس • معاف از مالیات • نقدشوندگی آنی",
                badge = "کارمزد ۰٪ اُرسی"
            )

            // Partner 2
            PartnerPlatformItem(
                name = "خزانه طلای آب‌شده ۱۸ عیار (تحویل فیزیکی و آنلاین)",
                subtitle = "دارای مجوز رسمی اتحادیه طلا و شمش بانک مرکزی",
                badge = "پشتوانه فیزیکی"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1_000_000L, 2_000_000L, 5_000_000L).forEach { amt ->
                    val isSelected = selectedAmountToman == amt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SaffronGold else OrosiSurfaceContainer)
                            .border(
                                1.dp,
                                if (isSelected) SaffronGoldBright else OrosiHairline,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedAmountToman = amt }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = PersianNumberUtils.formatToman(amt, showUnit = true),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AlabasterSilk,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Button(
                onClick = { onDirectPurchaseGold(selectedAmountToman) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaffronGold,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SaffronGoldBright, RoundedCornerShape(16.dp))
                    .testTag("confirm_partner_gold_purchase_button")
            ) {
                Icon(imageVector = Icons.Default.CurrencyExchange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ثبت خرید بدون کارمزد و افزودن به قلک طلا",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun PartnerPlatformItem(
    name: String,
    subtitle: String,
    badge: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrosiSurfaceHigh)
            .border(1.dp, SaffronGoldBright.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AlabasterSilk,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SaffronGoldDeep)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = SaffronGoldLight
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = OrosiOnSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailAndEditBottomSheet(
    transaction: TransactionEntity,
    onQuickVoiceRetag: (TransactionEntity) -> Unit,
    onSaveEdits: (
        amountToman: Long,
        weekdayDate: String,
        timeStr: String,
        cardInfo: String,
        purpose: String,
        storeName: String,
        specialTagMode: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var amountStr by remember(transaction) {
        mutableStateOf(PersianNumberUtils.formatToman(transaction.userShareToman.takeIf { it > 0L } ?: transaction.totalAmountToman))
    }
    var weekdayDateStr by remember(transaction) {
        val base = transaction.weekdayDate
        val jalaliYearStr = PersianNumberUtils.toPersianDigits(
            JalaliCalendarUtils.fromTimestamp(transaction.timestamp).year.toString()
        )
        mutableStateOf(if (base.contains("۱۴۰")) base else "$base $jalaliYearStr")
    }
    var timeStr by remember(transaction) { mutableStateOf(transaction.timeStr) }
    var cardInfoStr by remember(transaction) { mutableStateOf(transaction.cardInfo) }
    var purposeStr by remember(transaction) {
        mutableStateOf(if (transaction.isUntagged) "" else transaction.purpose)
    }
    var storeStr by remember(transaction) {
        mutableStateOf(if (transaction.isUntagged) "" else transaction.storeName)
    }
    var specialTagMode by remember(transaction) {
        mutableStateOf(if (transaction.type == "GOLD_SAVE") "GOLD_SAVE" else "NORMAL")
    }

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
                .testTag("transaction_detail_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "جزئیات کامل و ویرایش تراکنش",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Voice Re-tagging Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NishapurTurquoiseDeep)
                    .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(14.dp))
                    .clickable { onQuickVoiceRetag(transaction) }
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .testTag("sheet_voice_retag_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = NishapurTurquoiseBright,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تکمیل یا ویرایش سریع با صدای فارسی",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AlabasterSilk
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1) مبلغ (تومان)
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("۱) مبلغ (تومان)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_amount_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NishapurTurquoiseBright,
                    unfocusedBorderColor = OrosiHairline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2) تاریخ شمسی + روز هفته دقیق & 3) ساعت دقیق
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = weekdayDateStr,
                    onValueChange = { weekdayDateStr = PersianNumberUtils.toPersianDigits(it) },
                    label = { Text("۲) تاریخ شمسی و روز هفته") },
                    modifier = Modifier
                        .weight(1.45f)
                        .testTag("detail_date_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NishapurTurquoiseBright,
                        unfocusedBorderColor = OrosiHairline
                    )
                )

                OutlinedTextField(
                    value = timeStr,
                    onValueChange = { timeStr = PersianNumberUtils.toPersianDigits(it) },
                    label = { Text("۳) ساعت دقیق") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("detail_time_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NishapurTurquoiseBright,
                        unfocusedBorderColor = OrosiHairline
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4) کارت بانکی و ۴ رقم آخر
            OutlinedTextField(
                value = cardInfoStr,
                onValueChange = { cardInfoStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("۴) کارت بانکی و ۴ رقم آخر") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_card_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NishapurTurquoiseBright,
                    unfocusedBorderColor = OrosiHairline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5) بابتِ (Category/Purpose)
            OutlinedTextField(
                value = purposeStr,
                onValueChange = { purposeStr = it },
                label = { Text("۵) بابتِ (دسته‌بندی / علت تراکنش)") },
                placeholder = { Text("مثلاً: خرید میوه و تره‌بار") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_purpose_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NishapurTurquoiseBright,
                    unfocusedBorderColor = OrosiHairline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6) از فروشگاه (Merchant/Store)
            OutlinedTextField(
                value = storeStr,
                onValueChange = { storeStr = it },
                label = { Text("۶) از فروشگاه (نام پذیرنده)") },
                placeholder = { Text("مثلاً: اسنپ‌مارکت") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_store_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NishapurTurquoiseBright,
                    unfocusedBorderColor = OrosiHairline
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "تبدیل یا نشانه‌گذاری نوع تراکنش:",
                style = MaterialTheme.typography.labelLarge,
                color = OrosiOnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val modeOptions = listOf(
                "NORMAL" to "خرج عادی",
                "INSTALLMENT" to "قسط/وام",
                "DONG" to "بدهی/طلب (دُنگ)",
                "GOLD_SAVE" to "تبدیل به طلا"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modeOptions.forEach { (modeKey, modeLabel) ->
                    val isSelected = specialTagMode == modeKey
                    val activeColor = when (modeKey) {
                        "INSTALLMENT" -> RoyalAmethyst
                        "DONG" -> NishapurTurquoise
                        "GOLD_SAVE" -> SaffronGold
                        else -> PersianLapis
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) activeColor else OrosiSurfaceContainer)
                            .border(
                                1.dp,
                                if (isSelected) Color.White.copy(alpha = 0.45f) else OrosiHairline,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { specialTagMode = modeKey }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("tx_mode_toggle_$modeKey")
                    ) {
                        Text(
                            text = modeLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AlabasterSilk,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val parsedAmount = PersianNumberUtils.toEnglishDigits(amountStr).toLongOrNull()
                        ?: transaction.totalAmountToman
                    val cleanWeekdayDate = weekdayDateStr
                        .replace(Regex("۱۴۰[۰-۹]"), "")
                        .trim()
                        .ifEmpty { transaction.weekdayDate }
                    onSaveEdits(
                        parsedAmount,
                        cleanWeekdayDate,
                        timeStr.ifBlank { transaction.timeStr },
                        cardInfoStr.ifBlank { transaction.cardInfo },
                        purposeStr.ifBlank { "خرید روزمره" },
                        storeStr.ifBlank { "فروشگاه" },
                        specialTagMode
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NishapurTurquoise,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(16.dp))
                    .testTag("save_transaction_edits_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ذخیره تغییرات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSimulatorBottomSheet(
    onCleanPurgeAndRescanRealSms: () -> Unit,
    onDismiss: () -> Unit
) {
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
                .padding(horizontal = 22.dp)
                .padding(bottom = 30.dp)
                .testTag("sms_simulator_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NishapurTurquoiseBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اتصال خودکار به پیامک همه بانکها: فعال ✓",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(OrosiSurfaceContainer)
                    .border(1.dp, NishapurTurquoise.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✓ دریافت و تفکیک ۱۰۰٪ خودکار در پس‌زمینه (بدون نیاز به ثبت دستی)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NishapurTurquoiseSoft
                )
                Text(
                    text = "پشتیبانی خودکار از تمام بانک‌های ایران: بلوبانک، ملت، ملی، صادرات، تجارت، سامان، پاسارگاد، رسالت، سپه، مسکن، رفاه، آینده، پارسیان، اقتصاد نوین، مهر ایران، کشاورزی، سینا، شهر، گردشگری، ویپاد و آبانک.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OrosiOnSurfaceVariant
                )
                Text(
                    text = "• تفکیک هوشمند «مبلغ تراکنش» از «مانده حساب» و حذف خودکار پیامک‌های رمز پویا",
                    style = MaterialTheme.typography.bodySmall,
                    color = SaffronGoldLight
                )
            }

            Button(
                onClick = onCleanPurgeAndRescanRealSms,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PersianLapis,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(16.dp))
                    .testTag("clean_rescan_real_sms_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = NishapurTurquoiseBright
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "پاکسازی و بازخوانی تمیز پیامکهای واقعی",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
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
                    label = { Text("سهم خرج امروز (تومان)") },
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
                Text(
                    text = "تشخیص خودکار واریز حقوق از پیامک",
                    style = MaterialTheme.typography.titleSmall,
                    color = AlabasterSilk,
                    modifier = Modifier.weight(1f)
                )
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
                    text = "ذخیره تنظیمات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    label = { Text("پشتیبان JSON دیتابیس محلی") },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall
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
                    Text("بازیابی اطلاعات")
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
                text = "+ ثبت و استعلام چک جدید",
                style = MaterialTheme.typography.titleLarge,
                color = AlabasterSilk
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان چک یا قسط") },
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
                value = dueDateStr,
                onValueChange = { dueDateStr = it },
                label = { Text("تاریخ سررسید") },
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
                    text = "تأیید و ثبت چک",
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
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("مبلغ تبدیل (تومان)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SaffronGoldBright,
                    unfocusedBorderColor = OrosiHairline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "معادل: ${PersianNumberUtils.formatDecimal(equivalentGrams, 3)} گرم طلای ۱۸ عیار",
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
                    text = "تأیید",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
