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
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.theme.SaffronGoldLight
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceOverlayBottomSheet(
    voiceState: VoiceUiState,
    onSpeechChange: (String) -> Unit,
    onManualAmountChange: (String) -> Unit = {},
    onSimulateVoiceRecord: () -> Unit,
    onConfirmVoiceEntry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLiveHardwareListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                null
            }
        } catch (_: Exception) {
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
            }
            override fun onResults(results: Bundle?) {
                isLiveHardwareListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val firstSpoken = matches?.firstOrNull()
                if (!firstSpoken.isNullOrBlank()) {
                    onSpeechChange(firstSpoken)
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

    fun startRealOrFallbackRecognition(triggerSimFallbackIfUnavailable: Boolean = false) {
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
                if (triggerSimFallbackIfUnavailable) onSimulateVoiceRecord()
            }
        } else if (triggerSimFallbackIfUnavailable) {
            onSimulateVoiceRecord()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRealOrFallbackRecognition(triggerSimFallbackIfUnavailable = false)
        }
    }

    // Immediately trigger SpeechRecognizer.startListening when opened
    LaunchedEffect(Unit) {
        val hasAudioPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasAudioPerm) {
            startRealOrFallbackRecognition(triggerSimFallbackIfUnavailable = false)
        } else {
            try {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } catch (_: Exception) {
            }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .testTag("voice_overlay_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val targetTx = voiceState.targetUntaggedTransaction
            if (targetTx != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OrosiSurfaceContainer)
                        .border(1.dp, OrosiHairline, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تراکنش بانک ${targetTx.shortBankName}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = NishapurTurquoiseSoft
                        )
                        Text(
                            text = PersianNumberUtils.formatToman(targetTx.totalAmountToman, showUnit = true),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = AlabasterSilk
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

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
                            startRealOrFallbackRecognition(triggerSimFallbackIfUnavailable = true)
                        } else {
                            try {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } catch (_: Exception) {
                                onSimulateVoiceRecord()
                            }
                        }
                    }
                    .padding(vertical = 20.dp, horizontal = 20.dp)
                    .testTag("voice_record_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
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
                                0.28f + 0.55f * kotlin.math.abs(sin(phase + i * 0.45f))
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isLiveHardwareListening) "در حال شنیدن صدای شما... (بگویید بابت چه کاری بود)" else "برای ضبط مجدد صدا روی موج ضربه بزنید",
                        style = MaterialTheme.typography.labelMedium,
                        color = NishapurTurquoiseSoft
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Clean Live Text Input Box so the user can see/edit the recognized speech live before tapping "تأیید و ثبت"
            OutlinedTextField(
                value = voiceState.recognizedText,
                onValueChange = onSpeechChange,
                label = {
                    Text(
                        if (targetTx != null) {
                            "بابتِ این تراکنش بانکی (قابل ویرایش)"
                        } else {
                            "متن صوتی یا توضیح تراکنش نقدی/دستی"
                        }
                    )
                },
                placeholder = {
                    Text(
                        if (targetTx != null) {
                            "مثلاً: پفک، شارژ ایرانسل، نانوایی..."
                        } else {
                            "مثلاً: پنجاه تومن نانوایی، ۳۰۰ تومن میوه..."
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_live_text_input"),
                singleLine = false,
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NishapurTurquoiseBright,
                    unfocusedBorderColor = OrosiHairline
                )
            )

            // When creating a new manual cash/voice transaction (targetTx == null), show editable "مبلغ (تومان)" field
            if (targetTx == null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = voiceState.manualAmountText,
                    onValueChange = onManualAmountChange,
                    label = { Text("مبلغ (تومان) - الزامی برای ثبت دستی") },
                    placeholder = { Text("مثلاً: ۵۰,۰۰۰ یا ۳۰۰,۰۰۰") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_manual_amount_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NishapurTurquoiseBright,
                        unfocusedBorderColor = OrosiHairline
                    )
                )
            }

            if (voiceState.purposeCategory.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OrosiSurfaceHigh)
                        .border(1.dp, NishapurTurquoiseBright.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("recognized_speech_text"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (targetTx != null) "عنوان ثبت‌شونده:" else "عنوان (نقدی / دستی):",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrosiOnSurfaceVariant
                    )
                    Text(
                        text = voiceState.purposeCategory,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NishapurTurquoiseSoft
                    )
                }
            } else {
                // Keep testTag("recognized_speech_text") present even when empty
                Text(
                    text = if (targetTx != null) {
                        "بگویید یا بنویسید این تراکنش بابت چه چیزی بود"
                    } else {
                        "مبلغ و بابت تراکنش نقدی را بگویید یا وارد کنید (مثلاً: ۵۰ تومن نانوایی)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = OrosiOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag("recognized_speech_text")
                )
            }

            val canConfirm = targetTx != null || voiceState.totalAmountToman > 0L
            if (!canConfirm) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "برای ثبت تراکنش جدید، مبلغ باید بیشتر از ۰ تومان باشد.",
                    style = MaterialTheme.typography.labelSmall,
                    color = SaffronGoldLight,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Confirm Button: "تأیید و ثبت"
            Button(
                onClick = onConfirmVoiceEntry,
                enabled = canConfirm,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NishapurTurquoise,
                    contentColor = AlabasterSilk,
                    disabledContainerColor = OrosiSurfaceContainer,
                    disabledContentColor = OrosiOnSurfaceVariant
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (canConfirm) NishapurTurquoiseBright else OrosiHairline,
                        RoundedCornerShape(18.dp)
                    )
                    .testTag("confirm_voice_entry_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تأیید و ثبت",
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
    }
    val totalExpensesToman = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }.sumOf { it.userShareToman }
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
                text = "ترازنامه ماه حقوقی و وضعیت مالی",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = AlabasterSilk
            )

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
                        text = "سهم خرجِ روزانه پیشنهادی",
                        style = MaterialTheme.typography.titleSmall,
                        color = PersianLapisLight
                    )
                    Text(
                        text = "موجودی کل حساب‌ها: ${PersianNumberUtils.formatToman(settings.monthlyBalanceToman, showUnit = true)}",
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

/**
 * 4. Jibeman-Style Transaction Details Modal (`TransactionDetailAndEditBottomSheet`):
 * - Top 4 badges: Direction (`دریافتی` / `پرداختی`), Bank (`بلو`, `سپه`, etc.), Jalali Date (`۲۲ آبان ۱۴۰۵`), and Time (`۱۲:۲۱`) + a top-left Trash icon to delete the transaction.
 * - `مبلغ (تومان)` AND `مانده حساب پس از تراکنش (تومان)`.
 * - `توضیحات / بابت` field with a direct Microphone button right inside the field + collapsible "مشاهده متن اصلی پیامک بانک".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailAndEditBottomSheet(
    transaction: TransactionEntity,
    onQuickVoiceRetag: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onSaveEdits: (
        amountToman: Long,
        balanceAfterToman: Long?,
        weekdayDate: String,
        timeStr: String,
        cardInfo: String,
        purpose: String,
        storeName: String,
        specialTagMode: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val jalaliInfo = remember(transaction.timestamp) {
        JalaliCalendarUtils.fromTimestamp(transaction.timestamp)
    }

    var amountStr by remember(transaction) {
        mutableStateOf(
            PersianNumberUtils.formatToman(
                transaction.userShareToman.takeIf { it > 0L } ?: transaction.totalAmountToman
            )
        )
    }
    var balanceAfterStr by remember(transaction) {
        mutableStateOf(
            transaction.balanceAfterToman?.let { PersianNumberUtils.formatToman(it) } ?: ""
        )
    }
    var purposeStr by remember(transaction) {
        val initialPurpose = if (transaction.isUntagged ||
            transaction.purpose == "نیازمند تکمیل صوتی" ||
            transaction.purpose == "برداشت کارتی / خرید" ||
            transaction.purpose == "واریز به حساب"
        ) {
            ""
        } else {
            transaction.purpose
        }
        mutableStateOf(initialPurpose)
    }
    var storeStr by remember(transaction) {
        mutableStateOf(
            if (transaction.isUntagged || transaction.storeName == "نامشخص") {
                transaction.shortBankName
            } else {
                transaction.storeName
            }
        )
    }
    var specialTagMode by remember(transaction) {
        mutableStateOf("NORMAL")
    }
    var showRawSms by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isIncome = transaction.type == "INCOME"

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
            // Header Row: Title (Right in RTL) + Trash Icon & Close Icon (Left in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "جزئیات تراکنش",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = AlabasterSilk
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onDeleteTransaction(transaction) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RubyRose.copy(alpha = 0.16f))
                            .testTag("delete_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف تراکنش",
                            tint = RubyRoseSoft,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = OrosiOnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Top 4 Jibeman Badges: Direction (دریافتی / پرداختی), Bank (بلو, سپه...), Jalali Date (۲۲ آبان ۱۴۰۵), Time (۱۲:۲۱)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge 1: Direction
                JibemanDetailBadge(
                    text = if (isIncome) "دریافتی" else "پرداختی",
                    bgColor = if (isIncome) NishapurTurquoiseDeep else RubyRose.copy(alpha = 0.25f),
                    borderColor = if (isIncome) NishapurTurquoiseBright else RubyRoseSoft,
                    textColor = if (isIncome) NishapurTurquoiseSoft else RubyRoseSoft
                )
                // Badge 2: Bank or Manual/Cash ("نقدی / دستی")
                val bankBadgeLabel = if (transaction.shortBankName == "نقدی / دستی") {
                    "نقدی / دستی"
                } else {
                    "بانک ${transaction.shortBankName}"
                }
                JibemanDetailBadge(
                    text = bankBadgeLabel,
                    bgColor = PersianLapis,
                    borderColor = PersianLapisLight.copy(alpha = 0.5f),
                    textColor = AlabasterSilk
                )
                // Badge 3: Jalali Date
                JibemanDetailBadge(
                    text = jalaliInfo.dateWithYear,
                    bgColor = OrosiSurfaceHigh,
                    borderColor = OrosiHairline,
                    textColor = AlabasterSilk
                )
                // Badge 4: Time
                JibemanDetailBadge(
                    text = transaction.timeStr,
                    bgColor = OrosiSurfaceHigh,
                    borderColor = OrosiHairline,
                    textColor = SaffronGoldLight
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1) مبلغ (تومان)
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("مبلغ (تومان)") },
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

            // 2) مانده حساب پس از تراکنش (تومان)
            OutlinedTextField(
                value = balanceAfterStr,
                onValueChange = { balanceAfterStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("مانده حساب پس از تراکنش (تومان)") },
                placeholder = { Text("مثلاً: ۹۸۴,۴۳۳") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_balance_after_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NishapurTurquoiseBright,
                    unfocusedBorderColor = OrosiHairline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3) توضیحات / بابت field with a direct Microphone button right inside the field
            OutlinedTextField(
                value = purposeStr,
                onValueChange = { purposeStr = it },
                label = { Text("توضیحات / بابت") },
                placeholder = { Text("مثلاً: پفک، شارژ ایرانسل، نانوایی...") },
                trailingIcon = {
                    IconButton(
                        onClick = { onQuickVoiceRetag(transaction) },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(CircleShape)
                            .background(NishapurTurquoise)
                            .size(36.dp)
                            .testTag("sheet_voice_retag_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "ثبت صوتی بابت",
                            tint = AlabasterSilk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_purpose_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NishapurTurquoiseBright,
                    unfocusedBorderColor = OrosiHairline
                )
            )

            // Collapsible "مشاهده متن اصلی پیامک بانک" ONLY if rawSmsBody is present (real bank SMS)
            if (transaction.rawSmsBody.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OrosiSurfaceContainer)
                        .border(1.dp, OrosiHairline, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRawSms = !showRawSms }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("toggle_raw_sms_button"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = PersianLapisLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مشاهده متن اصلی پیامک بانک",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = PersianLapisLight
                            )
                        }
                        Icon(
                            imageVector = if (showRawSms) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = OrosiOnSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showRawSms) {
                        Text(
                            text = transaction.rawSmsBody,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                            color = AlabasterSilk,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .padding(bottom = 12.dp)
                                .testTag("raw_sms_body_text")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "نشانه‌گذاری نوع تراکنش:",
                style = MaterialTheme.typography.labelLarge,
                color = OrosiOnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val modeOptions = listOf(
                "NORMAL" to "خرج عادی",
                "INSTALLMENT" to "وام و قسط",
                "DONG" to "بدهی و طلب (دُنگ)"
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
                    val parsedBalanceAfter = PersianNumberUtils.toEnglishDigits(balanceAfterStr)
                        .toLongOrNull()
                        ?: transaction.balanceAfterToman
                    onSaveEdits(
                        parsedAmount,
                        parsedBalanceAfter,
                        transaction.weekdayDate,
                        transaction.timeStr,
                        transaction.cardInfo,
                        purposeStr.trim(),
                        storeStr.trim().ifEmpty { transaction.shortBankName },
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

@Composable
private fun JibemanDetailBadge(
    text: String,
    bgColor: Color,
    borderColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankAccountBottomSheet(
    onAddAccount: (bankName: String, balanceToman: Long, isTankhah: Boolean, cardLast4: String) -> Unit,
    onDismiss: () -> Unit
) {
    var bankName by remember { mutableStateOf("") }
    var balanceStr by remember { mutableStateOf("") }
    var cardLast4 by remember { mutableStateOf("") }
    var isTankhah by remember { mutableStateOf(true) }

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
                .testTag("add_bank_account_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = NishapurTurquoiseBright
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "افزودن منبع مالی / حساب جدید",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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

            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("نام بانک یا منبع مالی (مثلاً: ملت، ویپاد، کیف پول)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_account_name_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = balanceStr,
                onValueChange = { balanceStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("موجودی فعلی (تومان)") },
                placeholder = { Text("مثلاً: ۲,۵۰۰,۰۰۰") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_account_balance_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = cardLast4,
                onValueChange = { cardLast4 = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("۴ رقم آخر کارت (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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
                    text = "حساب تنخواه (خرج روزمره)",
                    style = MaterialTheme.typography.titleSmall,
                    color = AlabasterSilk
                )
                Switch(
                    checked = isTankhah,
                    onCheckedChange = { isTankhah = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AlabasterSilk,
                        checkedTrackColor = NishapurTurquoise
                    )
                )
            }

            Button(
                onClick = {
                    val cleanBank = bankName.trim().ifEmpty { "حساب جدید" }
                    val parsedBalance = PersianNumberUtils.toEnglishDigits(balanceStr).toLongOrNull() ?: 0L
                    onAddAccount(cleanBank, parsedBalance, isTankhah, cardLast4)
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
                    .testTag("submit_new_account_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ذخیره منبع مالی",
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
                        text = "اتصال خودکار به پیامک همه بانک‌ها: فعال ✓",
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
                    text = "✓ اسکن کامل صندوق پیامک و شناسایی تمام بانک‌های شما",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NishapurTurquoiseSoft
                )
                Text(
                    text = "پشتیبانی خودکار از تمام بانک‌های ایران: بلوبانک، سپه، پاسارگاد، ملت، ویپاد، ملی، صادرات، تجارت، سامان، رسالت، مسکن، رفاه، آینده، پارسیان، اقتصاد نوین، مهر ایران، کشاورزی، سینا، شهر، گردشگری و آبانک.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OrosiOnSurfaceVariant
                )
                Text(
                    text = "• استخراج آخرین مانده هر بانک حتی از پیامک‌های ماه‌های گذشته و نادیده گرفتن ۱۰۰٪ پیامک‌های تبلیغاتی (دیجی‌کالا، اسنپ، تپسی)",
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
                    text = "پاکسازی و بازخوانی تمیز پیامک‌های واقعی",
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
                    text = "تنظیمات ماه حقوقی و پشتیبان آفلاین",
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
                    label = { Text("سهم خرج روزانه (تومان)") },
                    modifier = Modifier.weight(1.4f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NishapurTurquoiseBright,
                        unfocusedBorderColor = OrosiHairline
                    )
                )
            }

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
                    val dailySafe = PersianNumberUtils.toEnglishDigits(dailySafeStr).toLongOrNull() ?: 680_000L
                    onSaveSettings(day, cycleLabel, autoDetectSalary, settings.goldPrice18kToman, dailySafe)
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
    defaultCategory: String = "SAYYAD_ISSUED",
    onAddCommitment: (String, String, String, Long, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember(defaultCategory) { mutableStateOf(defaultCategory) }
    var isReceivableDong by remember { mutableStateOf(true) } // true = طلب دارم (به من بدهکار است), false = بدهی دارم (من بدهکارم)
    var title by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var dueDateStr by remember { mutableStateOf("") }
    var sayyadId by remember { mutableStateOf("") }

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
                text = "ثبت تعهد جدید (چک، قسط یا دُنگ)",
                style = MaterialTheme.typography.titleLarge,
                color = AlabasterSilk
            )
            Spacer(modifier = Modifier.height(12.dp))

            val categoryOptions = listOf(
                "SAYYAD_ISSUED" to "چک صیادی",
                "LOAN_INSTALLMENT" to "وام و قسط",
                "DONG" to "بدهی و طلب (دُنگ)"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryOptions.forEach { (catKey, catLabel) ->
                    val selected = category == catKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) RoyalAmethyst else OrosiSurfaceContainer)
                            .border(
                                1.dp,
                                if (selected) RoyalAmethystSoft else OrosiHairline,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { category = catKey }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = catLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AlabasterSilk
                        )
                    }
                }
            }

            // When "بدهی و طلب (دُنگ)" is selected, show a clear 2-way selector:
            // "طلب دارم (به من بدهکار است)" vs "بدهی دارم (من بدهکارم)"
            if (category == "DONG") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dong_direction_selector"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isReceivableDong) NishapurTurquoiseDeep else OrosiSurfaceContainer)
                            .border(
                                1.dp,
                                if (isReceivableDong) NishapurTurquoiseBright else OrosiHairline,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { isReceivableDong = true }
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                            .testTag("dong_type_receivable"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "طلب دارم (به من بدهکار است)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isReceivableDong) NishapurTurquoiseSoft else OrosiOnSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isReceivableDong) RubyRose.copy(alpha = 0.28f) else OrosiSurfaceContainer)
                            .border(
                                1.dp,
                                if (!isReceivableDong) RubyRoseSoft else OrosiHairline,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { isReceivableDong = false }
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                            .testTag("dong_type_payable"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "بدهی دارم (من بدهکارم)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (!isReceivableDong) RubyRoseSoft else OrosiOnSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان (مثلاً: چک اجاره، قسط وام، دنگ شام)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("مبلغ (تومان)") },
                placeholder = { Text("مثلاً: ۱۲,۰۰۰,۰۰۰") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dueDateStr,
                onValueChange = { dueDateStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("تاریخ سررسید (مثلاً: ۳۰ آبان ۱۴۰۵)") },
                placeholder = { Text("مثلاً: ۳۰ آبان ۱۴۰۵") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = counterparty,
                onValueChange = { counterparty = it },
                label = { Text(if (category == "DONG") "نام دوست / طرف حساب" else "طرف حساب / بانک") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amount = PersianNumberUtils.toEnglishDigits(amountStr).toLongOrNull() ?: 5_000_000L
                    val baseTitle = title.trim().ifEmpty {
                        when (category) {
                            "DONG" -> if (isReceivableDong) "طلب دُنگ" else "بدهی دُنگ"
                            "LOAN_INSTALLMENT" -> "قسط وام"
                            else -> "چک صیادی"
                        }
                    }
                    val cleanCounterparty = if (category == "DONG") {
                        val prefix = if (isReceivableDong) "طلب از" else "بدهی به"
                        val person = counterparty.trim().ifEmpty { "دوست" }
                        if (person.startsWith("طلب") || person.startsWith("بدهی")) person else "$prefix $person"
                    } else {
                        counterparty.trim()
                    }
                    val cleanDate = dueDateStr.trim().ifEmpty { "۳۰ آبان ۱۴۰۵" }
                    onAddCommitment(category, baseTitle, cleanCounterparty, amount, cleanDate, sayyadId)
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
                    text = "تأیید و ثبت",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
