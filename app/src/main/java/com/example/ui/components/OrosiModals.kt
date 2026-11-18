package com.example.ui.components

import android.Manifest
import android.app.Activity
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CommitmentEntity
import com.example.data.local.TransactionEntity
import com.example.domain.JalaliCalendarUtils
import com.example.domain.OrosiRegressionSuite
import com.example.domain.PersianNumberUtils
import com.example.domain.RegressionSuiteReport
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

private fun formatAmountTextFieldValue(newValue: TextFieldValue): TextFieldValue {
    val formatted = PersianNumberUtils.formatLiveAmountInput(newValue.text)
    return TextFieldValue(
        text = formatted,
        selection = TextRange(formatted.length)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceOverlayBottomSheet(
    voiceState: VoiceUiState,
    onSpeechChange: (String) -> Unit,
    onManualAmountChange: (String) -> Unit = {},
    onPurposeChange: (String) -> Unit = {},
    onUpdateMultiItem: (index: Int, newTitle: String, newAmountStr: String) -> Unit = { _, _, _ -> },
    onSimulateVoiceRecord: () -> Unit,
    onConfirmVoiceEntry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isListening by remember { mutableStateOf(false) }
    var voiceStatusHint by remember { mutableStateOf("برای شروع یا ضبط مجدد صدا روی موج ضربه بزنید") }
    var hasAutoLaunchedSystemDialogOnError by remember { mutableStateOf(false) }

    val currentOnSpeechChange by rememberUpdatedState(onSpeechChange)
    val currentOnSimulateVoiceRecord by rememberUpdatedState(onSimulateVoiceRecord)
    val currentRecognizedText by rememberUpdatedState(voiceState.recognizedText)

    // System Voice Popup Dialog Launcher (works 100% reliably even when a Screen Recorder holds the microphone!)
    val systemVoiceDialogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val bestSpoken = matches?.firstOrNull()?.trim()
            if (!bestSpoken.isNullOrBlank()) {
                currentOnSpeechChange(bestSpoken)
                voiceStatusHint = "متن صوتی دریافت شد ✓"
            }
        }
    }

    fun launchSystemVoicePopup(fallbackToSimIfUnavailable: Boolean = false) {
        try {
            val popupIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "جمله تراکنش را بگویید (مثلاً: ۷۰ تومن نانوایی)"
                )
            }
            systemVoiceDialogLauncher.launch(popupIntent)
        } catch (_: Exception) {
            if (fallbackToSimIfUnavailable) {
                currentOnSimulateVoiceRecord()
            }
        }
    }

    // Retain SpeechRecognizer with remember & destroy in DisposableEffect.onDispose
    val speechRecognizer = remember(context) {
        try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (_: Exception) {
            null
        }
    }

    fun buildSpeechRecognizerIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

    fun startOrRestartRecognition(triggerFallbackIfUnavailable: Boolean = false) {
        if (speechRecognizer != null) {
            try {
                if (isListening) {
                    try {
                        speechRecognizer.stopListening()
                        speechRecognizer.cancel()
                    } catch (_: Exception) {
                    }
                    isListening = false
                }
                isListening = true
                voiceStatusHint = "در حال شنیدن صدای شما... (مثلاً بگویید: ۷۰ تومن نانوایی)"
                speechRecognizer.startListening(buildSpeechRecognizerIntent())
            } catch (_: Exception) {
                isListening = false
                if (triggerFallbackIfUnavailable) {
                    launchSystemVoicePopup(fallbackToSimIfUnavailable = true)
                }
            }
        } else if (triggerFallbackIfUnavailable) {
            launchSystemVoicePopup(fallbackToSimIfUnavailable = true)
        }
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                voiceStatusHint = "در حال شنیدن صدای شما... صحبت کنید"
            }

            override fun onBeginningOfSpeech() {
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                voiceStatusHint = "ضبط پایان یافت • برای ضبط مجدد روی موج ضربه بزنید"
            }

            override fun onError(error: Int) {
                isListening = false
                voiceStatusHint = "برای ضبط مجدد روی موج یا دکمه دیالوگ صوتی سیستم بزنید"
                // Critical fallback: if background SpeechRecognizer fails (e.g. ERROR_AUDIO = 3 when Screen Recorder holds mic,
                // ERROR_CLIENT = 5, ERROR_NO_MATCH = 7, ERROR_INSUFFICIENT_PERMISSIONS = 9), launch native system voice dialog
                if (!hasAutoLaunchedSystemDialogOnError &&
                    currentRecognizedText.isBlank() &&
                    (error == SpeechRecognizer.ERROR_AUDIO ||
                        error == SpeechRecognizer.ERROR_CLIENT ||
                        error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                        error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                        error == SpeechRecognizer.ERROR_NO_MATCH)
                ) {
                    hasAutoLaunchedSystemDialogOnError = true
                    launchSystemVoicePopup(fallbackToSimIfUnavailable = false)
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val firstSpoken = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                if (!firstSpoken.isNullOrBlank()) {
                    currentOnSpeechChange(firstSpoken)
                    voiceStatusHint = "متن صوتی دریافت شد ✓"
                } else {
                    voiceStatusHint = "صدایی تشخیص داده نشد • دوباره امتحان کنید"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                if (!partial.isNullOrBlank()) {
                    currentOnSpeechChange(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)

        onDispose {
            isListening = false
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {
            }
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        launchSystemVoicePopup(fallbackToSimIfUnavailable = false)
    }

    // Immediately and automatically launch the system Google Voice Dialog (ACTION_RECOGNIZE_SPEECH) on open
    LaunchedEffect(Unit) {
        launchSystemVoicePopup(fallbackToSimIfUnavailable = false)
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
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 1. Glowing Turquoise Audio Wave (Tapping launches the system Google Voice Dialog)
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
                        launchSystemVoicePopup(fallbackToSimIfUnavailable = true)
                    }
                    .padding(vertical = 16.dp, horizontal = 18.dp)
                    .testTag("voice_record_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        val barCount = 26
                        val totalWidth = size.width
                        val spacing = totalWidth / (barCount * 1.8f)
                        val barWidth = spacing * 0.85f
                        val centerY = size.height / 2f

                        for (i in 0 until barCount) {
                            val norm = i.toFloat() / barCount.toFloat()
                            val envelope = sin(norm * Math.PI).toFloat()
                            val dynamicWave = if (isListening || voiceState.isRecordingSimulated) {
                                0.38f + 0.62f * kotlin.math.abs(sin(phase * 1.4f + i * 0.45f))
                            } else {
                                0.25f + 0.35f * kotlin.math.abs(sin(phase + i * 0.45f))
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isListening) "در حال شنیدن صدای شما..." else voiceStatusHint,
                        style = MaterialTheme.typography.labelMedium,
                        color = NishapurTurquoiseSoft,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prominent Button: "🎤 شروع مجدد ضبط صوتی" (Launches native system voice dialog whenever tapped)
            Button(
                onClick = { launchSystemVoicePopup(fallbackToSimIfUnavailable = true) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PersianLapis,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(16.dp))
                    .testTag("system_voice_dialog_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = NishapurTurquoiseBright,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🎤 شروع مجدد ضبط صوتی",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = AlabasterSilk,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Single Clean Sentence/Description Field at the top:
            // "جمله یا توضیح تراکنش (مثلاً: ۷۰ تومن نانوایی)"
            OutlinedTextField(
                value = voiceState.recognizedText,
                onValueChange = onSpeechChange,
                label = {
                    Text(
                        if (targetTx != null) {
                            "بابتِ این تراکنش بانکی (مثلاً: پفک، شارژ ایرانسل)"
                        } else {
                            "جمله یا توضیح تراکنش (مثلاً: ۷۰ تومن نانوایی)"
                        }
                    )
                },
                placeholder = {
                    Text(
                        if (targetTx != null) {
                            "مثلاً: پفک، شارژ ایرانسل، نانوایی..."
                        } else {
                            "مثلاً: ۷۰ تومن نانوایی، ۳۹ تومن نونوایی..."
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

            Spacer(modifier = Modifier.height(12.dp))

            val multiItems = voiceState.parsedMultiItems
            val hasMultiItems = multiItems.size > 1
            val isMultiNewMode = targetTx == null && hasMultiItems

            // Live Auto-Extracted Summary Card: Editable Multi-Item List OR Single Item Editable Fields
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrosiSurfaceHigh)
                    .border(1.dp, NishapurTurquoiseBright.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasMultiItems) {
                    Text(
                        text = if (targetTx != null) {
                            "${PersianNumberUtils.toPersianDigits(multiItems.size.toString())} مورد از مبلغ بانک تفکیک شد (قابل ویرایش):"
                        } else {
                            "${PersianNumberUtils.toPersianDigits(multiItems.size.toString())} تراکنش مجزا تشخیص داده شد (قابل ویرایش):"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = NishapurTurquoiseSoft,
                        modifier = Modifier.testTag("recognized_speech_text")
                    )

                    multiItems.forEachIndexed { idx, item ->
                        var itemAmountField by remember(idx, item.amountToman) {
                            val formatted = PersianNumberUtils.formatLiveAmountInput(item.amountToman.toString())
                            mutableStateOf(
                                TextFieldValue(
                                    text = formatted,
                                    selection = TextRange(formatted.length)
                                )
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(OrosiSurfaceLow)
                                .border(1.dp, NishapurTurquoise.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("multi_voice_item_$idx"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(NishapurTurquoiseDeep)
                                            .border(1.dp, NishapurTurquoiseBright, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = PersianNumberUtils.toPersianDigits((idx + 1).toString()),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = AlabasterSilk
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تراکنش شماره ${PersianNumberUtils.toPersianDigits((idx + 1).toString())}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NishapurTurquoiseSoft
                                    )
                                }
                                Text(
                                    text = PersianNumberUtils.formatToman(item.amountToman, showUnit = true),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = SaffronGoldLight
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                OutlinedTextField(
                                    value = item.purposeCategory,
                                    onValueChange = { newTitle ->
                                        onUpdateMultiItem(idx, newTitle, itemAmountField.text)
                                    },
                                    label = {
                                        Text(
                                            text = "عنوان مورد ${PersianNumberUtils.toPersianDigits((idx + 1).toString())}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1.25f)
                                        .testTag("multi_voice_item_title_$idx"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NishapurTurquoiseBright,
                                        unfocusedBorderColor = OrosiHairline
                                    )
                                )

                                OutlinedTextField(
                                    value = itemAmountField,
                                    onValueChange = { newVal ->
                                        val formattedVal = formatAmountTextFieldValue(newVal)
                                        itemAmountField = formattedVal
                                        onUpdateMultiItem(idx, item.purposeCategory, formattedVal.text)
                                    },
                                    label = {
                                        Text(
                                            text = "مبلغ (تومان)",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("multi_voice_item_amount_$idx"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SaffronGoldLight,
                                        unfocusedBorderColor = OrosiHairline
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_manual_amount_input"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (targetTx != null) "عنوان ترکیبی ثبت‌شده:" else "جمع کل مبالغ:",
                            style = MaterialTheme.typography.bodySmall,
                            color = OrosiOnSurfaceVariant
                        )
                        Text(
                            text = if (targetTx != null) {
                                voiceState.purposeCategory
                            } else {
                                PersianNumberUtils.formatToman(voiceState.totalAmountToman, showUnit = true)
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = NishapurTurquoiseSoft
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recognized_speech_text"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "عنوان / بابت:",
                            style = MaterialTheme.typography.bodySmall,
                            color = OrosiOnSurfaceVariant
                        )
                        Text(
                            text = voiceState.purposeCategory.ifBlank { "در انتظار ورود جمله..." },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (voiceState.purposeCategory.isNotBlank()) NishapurTurquoiseSoft else OrosiOnSurfaceVariant
                        )
                    }

                    if (targetTx == null) {
                        var singleAmountField by remember(voiceState.manualAmountText) {
                            val formatted = PersianNumberUtils.formatLiveAmountInput(voiceState.manualAmountText)
                            mutableStateOf(
                                TextFieldValue(
                                    text = formatted,
                                    selection = TextRange(formatted.length)
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = voiceState.purposeCategory,
                                onValueChange = onPurposeChange,
                                label = {
                                    Text(
                                        text = "عنوان / بابت (قابل ویرایش)",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("voice_editable_title_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NishapurTurquoiseBright,
                                    unfocusedBorderColor = OrosiHairline
                                )
                            )

                            OutlinedTextField(
                                value = singleAmountField,
                                onValueChange = { newVal ->
                                    val formattedVal = formatAmountTextFieldValue(newVal)
                                    singleAmountField = formattedVal
                                    onManualAmountChange(formattedVal.text)
                                },
                                label = {
                                    Text(
                                        text = "مبلغ (تومان)",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("voice_manual_amount_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SaffronGoldLight,
                                    unfocusedBorderColor = OrosiHairline
                                )
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_manual_amount_input"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مبلغ (تومان):",
                                style = MaterialTheme.typography.bodySmall,
                                color = OrosiOnSurfaceVariant
                            )
                            Text(
                                text = PersianNumberUtils.formatToman(voiceState.totalAmountToman, showUnit = true),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = SaffronGoldLight
                            )
                        }
                    }
                }
            }

            val hasBankMismatch = targetTx != null && voiceState.hasBankAmountMismatch
            if (hasBankMismatch) {
                val spokenFmt = PersianNumberUtils.formatToman(voiceState.conflictingSpokenAmountToman, showUnit = true)
                val bankFmt = PersianNumberUtils.formatToman(targetTx!!.totalAmountToman, showUnit = true)
                val diffFmt = PersianNumberUtils.formatToman(
                    kotlin.math.abs(voiceState.conflictingSpokenAmountToman - targetTx.totalAmountToman),
                    showUnit = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RubyRose.copy(alpha = 0.22f))
                        .border(1.dp, RubyRoseSoft, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("voice_bank_amount_mismatch_warning")
                ) {
                    Text(
                        text = "⚠️ عدم تطابق مبلغ با پیامک بانک: مبلغ گفته‌شده ($spokenFmt) با مبلغ پیامک بانک ($bankFmt) به اندازه $diffFmt اختلاف دارد. لطفاً مبلغ را اصلاح کنید تا ثبت فعال شود.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = RubyRoseSoft
                    )
                }
            }

            val canConfirm = !hasBankMismatch &&
                (targetTx != null || voiceState.totalAmountToman > 0L) &&
                voiceState.purposeCategory.isNotBlank()
            if (!canConfirm && !hasBankMismatch) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (targetTx == null) {
                        "جمله تراکنش را همراه با مبلغ بگویید یا بنویسید (مثلاً: ۷۰ تومن نانوایی)"
                    } else {
                        "بگویید یا بنویسید این تراکنش بابت چه چیزی بود"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = SaffronGoldLight,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val confirmButtonLabel = if (isMultiNewMode) {
                "تأیید و ثبت (${PersianNumberUtils.toPersianDigits(multiItems.size.toString())} تراکنش)"
            } else {
                "تأیید و ثبت"
            }

            // 3. Confirm Button: "تأیید و ثبت" or "تأیید و ثبت (۲ تراکنش)"
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
                    text = confirmButtonLabel,
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
    var amountField by remember(commitment) {
        val formatted = PersianNumberUtils.formatToman(commitment.amountToman)
        mutableStateOf(TextFieldValue(text = formatted, selection = TextRange(formatted.length)))
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
                    val parsedAmount = PersianNumberUtils.parseAmountInputToLong(amountField.text)
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
                label = { Text("عنوان چک، قسط یا دُنگ", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amountField,
                onValueChange = { amountField = formatAmountTextFieldValue(it) },
                label = { Text("مبلغ (تومان)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dueDateStr,
                onValueChange = { dueDateStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("تاریخ سررسید", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = counterpartyStr,
                onValueChange = { counterpartyStr = it },
                label = { Text("طرف حساب / بانک", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                        val parsedAmount = PersianNumberUtils.parseAmountInputToLong(amountField.text)
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

    var amountField by remember(transaction) {
        val formatted = PersianNumberUtils.formatToman(
            transaction.userShareToman.takeIf { it > 0L } ?: transaction.totalAmountToman
        )
        mutableStateOf(
            TextFieldValue(
                text = formatted,
                selection = TextRange(formatted.length)
            )
        )
    }
    var balanceAfterField by remember(transaction) {
        val formatted = transaction.balanceAfterToman?.let { PersianNumberUtils.formatToman(it) } ?: ""
        mutableStateOf(
            TextFieldValue(
                text = formatted,
                selection = TextRange(formatted.length)
            )
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
    var confirmDeleteClicked by remember(transaction.id) { mutableStateOf(false) }

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
            // Header Row: Title (Right in RTL) + 2-Step Trash Icon & Close Icon (Left in RTL)
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
                    if (!confirmDeleteClicked) {
                        IconButton(
                            onClick = { confirmDeleteClicked = true },
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
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(RubyRose)
                                .border(1.dp, RubyRoseSoft, RoundedCornerShape(50))
                                .clickable { onDeleteTransaction(transaction) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("delete_transaction_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "تأیید حذف",
                                tint = AlabasterSilk,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تأیید حذف؟",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = AlabasterSilk
                            )
                        }
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

            Spacer(modifier = Modifier.height(12.dp))

            // Hero Summary Header Card: Bank Logo Avatar + Direction Badge + Large Amount + Unified Date/Time Pill
            val livePreviewAmount = PersianNumberUtils.parseAmountInputToLong(amountField.text)
                ?: transaction.totalAmountToman
            val formattedHeroAmount = if (isIncome) {
                PersianNumberUtils.formatIncomeToman(livePreviewAmount, showUnit = true)
            } else {
                PersianNumberUtils.formatToman(livePreviewAmount, showUnit = true)
            }
            val bankBadgeLabel = if (transaction.shortBankName == "نقدی / دستی") {
                "نقدی / دستی"
            } else {
                "بانک ${transaction.shortBankName}"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PersianLapis,
                                OrosiSurfaceHigh
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isIncome) NishapurTurquoiseBright.copy(alpha = 0.55f) else PersianLapisLight.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BankLogoAvatar(
                            bankName = transaction.shortBankName,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = bankBadgeLabel,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = AlabasterSilk
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${jalaliInfo.dateWithYear} • ساعت ${transaction.timeStr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OrosiOnSurfaceVariant
                            )
                        }
                    }

                    JibemanDetailBadge(
                        text = if (isIncome) "دریافتی" else "پرداختی",
                        bgColor = if (isIncome) NishapurTurquoiseDeep else RubyRose.copy(alpha = 0.28f),
                        borderColor = if (isIncome) NishapurTurquoiseBright else RubyRoseSoft,
                        textColor = if (isIncome) NishapurTurquoiseSoft else RubyRoseSoft
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OrosiSurfaceLow.copy(alpha = 0.65f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formattedHeroAmount,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = if (isIncome) NishapurTurquoiseSoft else RubyRoseSoft,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 1 Card: اطلاعات مالی تراکنش (Amount & Balance After)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(OrosiSurfaceContainer)
                    .border(1.dp, OrosiHairline, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = NishapurTurquoiseBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "اطلاعات مالی تراکنش",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = NishapurTurquoiseSoft
                    )
                }

                // 1) مبلغ (تومان) - Live 3-digit comma formatting with cursor at end
                OutlinedTextField(
                    value = amountField,
                    onValueChange = { amountField = formatAmountTextFieldValue(it) },
                    label = { Text("مبلغ تراکنش (تومان)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = OrosiSurfaceLow,
                        unfocusedContainerColor = OrosiSurfaceLow,
                        focusedBorderColor = NishapurTurquoiseBright,
                        unfocusedBorderColor = OrosiHairline
                    )
                )

                // 2) مانده حساب پس از تراکنش (تومان) - Live 3-digit comma formatting with cursor at end
                OutlinedTextField(
                    value = balanceAfterField,
                    onValueChange = { balanceAfterField = formatAmountTextFieldValue(it) },
                    label = { Text("مانده حساب پس از تراکنش (تومان)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("مثلاً: ۹۸۴,۴۳۳") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_balance_after_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = OrosiSurfaceLow,
                        unfocusedContainerColor = OrosiSurfaceLow,
                        focusedBorderColor = NishapurTurquoiseBright,
                        unfocusedBorderColor = OrosiHairline
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 2 Card: توضیحات و مستندات پیامک (Purpose + Voice Retag + Collapsible Raw Bank SMS)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(OrosiSurfaceContainer)
                    .border(1.dp, OrosiHairline, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = SaffronGoldLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "توضیحات و بابت تراکنش",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = SaffronGoldLight
                        )
                    }
                    Text(
                        text = "قابل ویرایش متنی یا صوتی",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrosiOnSurfaceVariant
                    )
                }

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
                    singleLine = false,
                    maxLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = OrosiSurfaceLow,
                        unfocusedContainerColor = OrosiSurfaceLow,
                        focusedBorderColor = NishapurTurquoiseBright,
                        unfocusedBorderColor = OrosiHairline
                    )
                )

                // Collapsible "مشاهده متن اصلی پیامک بانک" ONLY if rawSmsBody is present (real bank SMS)
                if (transaction.rawSmsBody.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(OrosiSurfaceLow)
                            .border(1.dp, PersianLapisLight.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRawSms = !showRawSms }
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                                .testTag("toggle_raw_sms_button"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = PersianLapisLight,
                                    modifier = Modifier.size(17.dp)
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 3: Full-Width 3-Segment Pill Selector for Transaction Mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(OrosiSurfaceContainer)
                    .border(1.dp, OrosiHairline, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "دسته‌بندی ماهیت تراکنش:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = AlabasterSilk
                )

                val modeOptions = listOf(
                    "NORMAL" to "خرج عادی",
                    "INSTALLMENT" to "وام و قسط",
                    "DONG" to "بدهی و دُنگ"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OrosiSurfaceLow)
                        .border(1.dp, OrosiHairline, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) activeColor else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.45f) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { specialTagMode = modeKey }
                                .padding(vertical = 9.dp, horizontal = 6.dp)
                                .testTag("tx_mode_toggle_$modeKey"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = modeLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                ),
                                color = if (isSelected) AlabasterSilk else OrosiOnSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val parsedAmount = PersianNumberUtils.parseAmountInputToLong(amountField.text)
                        ?: transaction.totalAmountToman
                    val parsedBalanceAfter = if (balanceAfterField.text.isBlank()) {
                        null
                    } else {
                        PersianNumberUtils.parseAmountInputToLong(balanceAfterField.text)
                            ?: transaction.balanceAfterToman
                    }
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

private data class InboxSmsPickerRow(
    val id: Long,
    val senderAddress: String,
    val body: String,
    val dateStr: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankAccountBottomSheet(
    onAddAccount: (
        bankName: String,
        balanceToman: Long,
        isTankhah: Boolean,
        cardLast4: String,
        customSenderAddresses: List<String>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = کارت بانکی, 1 = منبع نقدی
    var selectedBankName by remember { mutableStateOf("بلو") }
    var bankSearchQuery by remember { mutableStateOf("") }
    var accountCustomTitle by remember { mutableStateOf("") }
    var balanceField by remember { mutableStateOf(TextFieldValue("")) }
    var cardLast4 by remember { mutableStateOf("") }
    var isTankhah by remember { mutableStateOf(false) }
    var attachedSenders by remember { mutableStateOf(listOf<String>()) }
    var showSmsSenderPickerDialog by remember { mutableStateOf(false) }
    var recentInboxMessages by remember { mutableStateOf<List<InboxSmsPickerRow>>(emptyList()) }

    val defaultSendersForSelectedBank = remember(selectedBankName) {
        com.example.domain.JibemanSmsEngine.BANK_PROFILES
            .firstOrNull { it.nameFa == selectedBankName || it.nameFa.replace("\u200C", " ") == selectedBankName.replace("\u200C", " ") }
            ?.senderIds
            ?.take(3)
            .orEmpty()
    }

    val filteredBanks = remember(bankSearchQuery) {
        val q = bankSearchQuery.trim()
        if (q.isEmpty()) {
            JibemanAssetLogoCache.ALL_38_IRANIAN_BANKS
        } else {
            JibemanAssetLogoCache.ALL_38_IRANIAN_BANKS.filter { (fa, key) ->
                fa.contains(q) || key.contains(q, ignoreCase = true)
            }
        }
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
                        text = "ایجاد حساب",
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

            // 2-Tab Selector at top: "کارت بانکی" | "منبع نقدی"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OrosiSurfaceContainer)
                    .border(1.dp, OrosiHairline, RoundedCornerShape(14.dp))
                    .padding(4.dp)
                    .testTag("add_account_tab_bar"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf(0 to "کارت بانکی", 1 to "منبع نقدی")
                tabs.forEach { (idx, title) ->
                    val isSelected = selectedTab == idx
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NishapurTurquoise else Color.Transparent)
                            .clickable { selectedTab = idx }
                            .padding(vertical = 10.dp)
                            .testTag("add_account_tab_$idx")
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 13.5.sp
                            ),
                            color = if (isSelected) AlabasterSilk else OrosiOnSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // Tab 1: کارت بانکی
                Text(
                    text = "انتخاب بانک (۳۸ بانک و مؤسسه رسمی)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = NishapurTurquoiseSoft
                )

                OutlinedTextField(
                    value = bankSearchQuery,
                    onValueChange = { bankSearchQuery = it },
                    label = {
                        Text(
                            text = "جستجوی نام بانک (مثلاً: بلو، سپه، ملت، پاسارگاد...)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NishapurTurquoiseBright,
                        unfocusedBorderColor = OrosiHairline
                    )
                )

                // Horizontal scrollable selector of all 38 Iranian banks with official 192x192 PNG logos
                // Cards are 106.dp wide with 2-line centered text so NO bank name is ever truncated with "..."
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("iranian_banks_selector_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredBanks,
                        key = { it.first }
                    ) { (bankFa, _) ->
                        val isSelected = selectedBankName == bankFa
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .width(106.dp)
                                .height(98.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) PersianLapis else OrosiSurfaceContainer
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NishapurTurquoiseBright else OrosiHairline,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedBankName = bankFa }
                                .padding(vertical = 8.dp, horizontal = 6.dp)
                                .testTag("select_bank_$bankFa")
                        ) {
                            BankLogoAvatar(
                                bankName = bankFa,
                                size = 44.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = bankFa,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                ),
                                color = if (isSelected) AlabasterSilk else OrosiOnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = accountCustomTitle,
                    onValueChange = { accountCustomTitle = it },
                    label = {
                        Text(
                            text = "نام حساب (اختیاری — مثلاً: حساب شخصی من)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    placeholder = { Text("بانک $selectedBankName") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_account_name_input")
                )

                OutlinedTextField(
                    value = balanceField,
                    onValueChange = { balanceField = formatAmountTextFieldValue(it) },
                    label = {
                        Text(
                            text = "مانده فعلی (تومان)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    placeholder = { Text("مثلاً: ۴,۳۴۲,۱۳۹") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_account_balance_input")
                )

                // Jibeman-style SMS sender section: "سرشماره‌های پیامکی" + "+ افزودن سرشماره از پیامک‌ها"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(OrosiSurfaceContainer)
                        .border(1.dp, OrosiHairline, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سرشماره‌های پیامکی ($selectedBankName)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = AlabasterSilk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        val allDisplayedSenders = (defaultSendersForSelectedBank + attachedSenders).distinct()
                        if (allDisplayedSenders.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                allDisplayedSenders.take(2).forEach { sender ->
                                    Text(
                                        text = PersianNumberUtils.formatLtrSender(sender),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            textDirection = TextDirection.Ltr,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = NishapurTurquoiseSoft,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (attachedSenders.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "سرشماره‌های انتخاب‌شده:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = SaffronGoldLight
                            )
                            Text(
                                text = PersianNumberUtils.formatLtrSenderList(attachedSenders, "  "),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDirection = TextDirection.Ltr
                                ),
                                color = SaffronGoldLight
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val list = mutableListOf<InboxSmsPickerRow>()
                            try {
                                val cursor = context.contentResolver.query(
                                    android.provider.Telephony.Sms.Inbox.CONTENT_URI,
                                    arrayOf(
                                        android.provider.Telephony.Sms._ID,
                                        android.provider.Telephony.Sms.ADDRESS,
                                        android.provider.Telephony.Sms.BODY,
                                        android.provider.Telephony.Sms.DATE
                                    ),
                                    null,
                                    null,
                                    "${android.provider.Telephony.Sms.DATE} DESC"
                                )
                                cursor?.use { c ->
                                    val idIdx = c.getColumnIndex(android.provider.Telephony.Sms._ID)
                                    val addrIdx = c.getColumnIndex(android.provider.Telephony.Sms.ADDRESS)
                                    val bodyIdx = c.getColumnIndex(android.provider.Telephony.Sms.BODY)
                                    val dateIdx = c.getColumnIndex(android.provider.Telephony.Sms.DATE)
                                    var count = 0
                                    while (c.moveToNext() && count < 45) {
                                        val addr = if (addrIdx >= 0) c.getString(addrIdx).orEmpty().trim() else ""
                                        val body = if (bodyIdx >= 0) c.getString(bodyIdx).orEmpty().trim() else ""
                                        val ts = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()
                                        if (addr.isNotBlank() && body.isNotBlank()) {
                                            val j = com.example.domain.JalaliCalendarUtils.timestampToJalali(ts)
                                            list.add(
                                                InboxSmsPickerRow(
                                                    id = if (idIdx >= 0) c.getLong(idIdx) else count.toLong(),
                                                    senderAddress = addr,
                                                    body = body,
                                                    dateStr = "${j.dateWithYear} • ${j.timeStr}"
                                                )
                                            )
                                            count++
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                            }
                            recentInboxMessages = list
                            showSmsSenderPickerDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_sender_from_sms_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = NishapurTurquoiseBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ افزودن سرشماره از پیامک‌ها",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = NishapurTurquoiseSoft,
                            maxLines = 1
                        )
                    }
                }
            } else {
                // Tab 2: منبع نقدی
                OutlinedTextField(
                    value = accountCustomTitle,
                    onValueChange = { accountCustomTitle = it },
                    label = {
                        Text(
                            text = "نام حساب (مثلاً: کیف پول نقدی، گاوصندوق)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    placeholder = { Text("کیف پول نقدی") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_cash_account_name_input")
                )

                OutlinedTextField(
                    value = balanceField,
                    onValueChange = { balanceField = formatAmountTextFieldValue(it) },
                    label = {
                        Text(
                            text = "مانده فعلی (تومان)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    placeholder = { Text("مثلاً: ۲,۵۰۰,۰۰۰") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_cash_account_balance_input")
                )
            }

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
                    text = "حساب تنخواه / شرکت",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    color = AlabasterSilk,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                    val finalBankName = if (selectedTab == 0) {
                        selectedBankName.ifBlank { accountCustomTitle.trim().ifEmpty { "بلو" } }
                    } else {
                        accountCustomTitle.trim().ifEmpty { "کیف پول نقدی" }
                    }
                    val parsedBalance = PersianNumberUtils.parseAmountInputToLong(balanceField.text) ?: 0L
                    onAddAccount(finalBankName, parsedBalance, isTankhah, cardLast4, attachedSenders)
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
                    text = "ذخیره حساب",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    if (showSmsSenderPickerDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSmsSenderPickerDialog = false },
            containerColor = OrosiSurfaceLow,
            titleContentColor = AlabasterSilk,
            textContentColor = AlabasterSilk,
            title = {
                Text(
                    text = "انتخاب سرشماره از پیامک‌های اخیر",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                if (recentInboxMessages.isEmpty()) {
                    Text(
                        text = "پیامکی در صندوق ورودی یافت نشد یا دسترسی پیامک هنوز تأیید نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OrosiOnSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentInboxMessages.forEach { smsItem ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(OrosiSurfaceContainer)
                                    .border(1.dp, OrosiHairline, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "سرشماره:",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = NishapurTurquoiseSoft,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = PersianNumberUtils.formatLtrSender(smsItem.senderAddress),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    textDirection = TextDirection.Ltr
                                                ),
                                                color = NishapurTurquoiseSoft,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = smsItem.dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = OrosiOnSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NishapurTurquoise)
                                            .clickable {
                                                if (smsItem.senderAddress !in attachedSenders) {
                                                    attachedSenders = attachedSenders + smsItem.senderAddress
                                                }
                                                showSmsSenderPickerDialog = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "انتخاب",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = AlabasterSilk
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = smsItem.body.replace("\n", " "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AlabasterSilk,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showSmsSenderPickerDialog = false }) {
                    Text("بستن", color = AlabasterSilk)
                }
            }
        )
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
    onSetTankhahModeEnabled: (Boolean) -> Unit = {},
    onSelectFontFamily: (String) -> Unit = {},
    onImportBackup: (String) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    var cycleLabel by remember(settings) { mutableStateOf(settings.paydayCycleLabel) }
    var paydayDayStr by remember(settings) {
        mutableStateOf(PersianNumberUtils.toPersianDigits(settings.paydayStartDay.toString()))
    }
    var autoDetectSalary by remember(settings) { mutableStateOf(settings.autoDetectSalary) }
    var dailySafeField by remember(settings) {
        val formatted = PersianNumberUtils.formatToman(settings.dailySafeBudgetToman)
        mutableStateOf(TextFieldValue(text = formatted, selection = TextRange(formatted.length)))
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

            // Live 5-Font Family Switcher (IRANSansX Default + Dana + IRANYekanX + Peyda + Vazirmatn)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrosiSurfaceContainer)
                    .border(1.dp, OrosiHairline, RoundedCornerShape(16.dp))
                    .padding(14.dp)
                    .testTag("font_family_selector_card"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "فونت برنامه (اعداد فارسی)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = AlabasterSilk
                    )
                    val activeOption = com.example.ui.theme.OrosiFontOption.fromKey(settings.selectedFontFamily)
                    Text(
                        text = "فعال: ${activeOption.persianTitle} ✓",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NishapurTurquoiseSoft
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.ui.theme.OrosiFontOption.entries.forEach { option ->
                        val isSelected = com.example.ui.theme.OrosiFontOption.fromKey(settings.selectedFontFamily) == option
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) NishapurTurquoise else OrosiSurfaceHigh
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NishapurTurquoiseBright else OrosiHairline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectFontFamily(option.key) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("select_font_${option.key}")
                        ) {
                            Text(
                                text = option.persianTitle,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = option.fontFamily,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                color = if (isSelected) AlabasterSilk else OrosiOnSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = cycleLabel,
                onValueChange = { cycleLabel = it },
                label = { Text("بازه ماه حقوقی", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NishapurTurquoiseBright,
                    unfocusedBorderColor = OrosiHairline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Rule 1: Side-by-Side Inputs with single-line labels and Alignment.Bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = paydayDayStr,
                    onValueChange = { paydayDayStr = PersianNumberUtils.toPersianDigits(it) },
                    label = {
                        Text(
                            text = "روز واریز حقوق",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NishapurTurquoiseBright,
                        unfocusedBorderColor = OrosiHairline
                    )
                )

                OutlinedTextField(
                    value = dailySafeField,
                    onValueChange = { dailySafeField = formatAmountTextFieldValue(it) },
                    label = {
                        Text(
                            text = "بودجه روزانه (تومان)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1.35f),
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
                    text = "تفکیک حساب تنخواه",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    color = AlabasterSilk,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = settings.tankhahModeEnabled,
                    onCheckedChange = onSetTankhahModeEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AlabasterSilk,
                        checkedTrackColor = NishapurTurquoise
                    ),
                    modifier = Modifier.testTag("settings_enable_tankhah_mode_switch")
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
                    text = "تشخیص خودکار حقوق",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    color = AlabasterSilk,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                    val dailySafe = PersianNumberUtils.parseAmountInputToLong(dailySafeField.text) ?: 680_000L
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

            Spacer(modifier = Modifier.height(16.dp))

            // In-App Automated Regression & Stress Test Lab (requested in Video batch_9)
            var regressionReport by remember { mutableStateOf<RegressionSuiteReport?>(null) }
            var showRegressionDetails by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(OrosiSurfaceContainer)
                    .border(
                        1.dp,
                        if (regressionReport?.allPassed == true) NishapurTurquoiseBright else OrosiHairline,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(14.dp)
                    .testTag("regression_test_lab_card"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "آزمایشگاه تست خودکار رگرسیون و استرس",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = AlabasterSilk
                        )
                        Text(
                            text = "بررسی خودکار ۳۵ سناریوی صوتی پیچیده، پیامکی، تقویم و بانک‌ها بدون نیاز به تست دستی",
                            style = MaterialTheme.typography.labelSmall,
                            color = OrosiOnSurfaceVariant
                        )
                    }
                    regressionReport?.let { report ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (report.allPassed) NishapurTurquoiseDeep else RubyRose.copy(alpha = 0.35f))
                                .border(
                                    1.dp,
                                    if (report.allPassed) NishapurTurquoiseBright else RubyRoseSoft,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("regression_report_badge")
                        ) {
                            Text(
                                text = "${PersianNumberUtils.toPersianDigits(report.passedCount.toString())}/${PersianNumberUtils.toPersianDigits(report.totalCount.toString())} موفق ✓",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (report.allPassed) NishapurTurquoiseSoft else RubyRoseSoft
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        regressionReport = OrosiRegressionSuite.runFullRegressionAndStressSuite()
                        showRegressionDetails = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NishapurTurquoiseDeep,
                        contentColor = AlabasterSilk
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(12.dp))
                        .testTag("run_regression_suite_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = NishapurTurquoiseBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (regressionReport == null) {
                            "اجرای تست خودکار رگرسیون و استرس (۳۵ سناریو)"
                        } else {
                            "اجرای مجدد تست استرس (${PersianNumberUtils.toPersianDigits(regressionReport!!.totalDurationMs.toString())} میلی‌ثانیه)"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                regressionReport?.let { report ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRegressionDetails = !showRegressionDetails }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نتیجه: ${PersianNumberUtils.toPersianDigits(report.passedCount.toString())} تست موفق از ${PersianNumberUtils.toPersianDigits(report.totalCount.toString())} در ${PersianNumberUtils.toPersianDigits(report.totalDurationMs.toString())} میلی‌ثانیه",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (report.allPassed) NishapurTurquoiseSoft else RubyRoseSoft
                        )
                        Icon(
                            imageVector = if (showRegressionDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = OrosiOnSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showRegressionDetails) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            report.results.forEach { tc ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(OrosiSurfaceHigh)
                                        .border(
                                            1.dp,
                                            if (tc.passed) NishapurTurquoise.copy(alpha = 0.35f) else RubyRoseSoft,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${PersianNumberUtils.toPersianDigits(tc.id.toString())}. ${tc.title}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = AlabasterSilk,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = if (tc.passed) "✓ موفق" else "✗ خطا",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = if (tc.passed) NishapurTurquoiseSoft else RubyRoseSoft
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ورودی: ${tc.inputSummary}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OrosiOnSurfaceVariant
                                    )
                                    Text(
                                        text = "خروجی: ${tc.actualSummary}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = NishapurTurquoiseSoft
                                    )
                                }
                            }
                        }
                    }
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
    var amountField by remember { mutableStateOf(TextFieldValue("")) }
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
                label = { Text("عنوان (مثلاً: چک اجاره، قسط وام، دنگ شام)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amountField,
                onValueChange = { amountField = formatAmountTextFieldValue(it) },
                label = { Text("مبلغ (تومان)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("مثلاً: ۱۲,۰۰۰,۰۰۰") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dueDateStr,
                onValueChange = { dueDateStr = PersianNumberUtils.toPersianDigits(it) },
                label = { Text("تاریخ سررسید (مثلاً: ۳۰ آبان ۱۴۰۵)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("مثلاً: ۳۰ آبان ۱۴۰۵") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = counterparty,
                onValueChange = { counterparty = it },
                label = { Text(if (category == "DONG") "نام دوست / طرف حساب" else "طرف حساب / بانک", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amount = PersianNumberUtils.parseAmountInputToLong(amountField.text) ?: 5_000_000L
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
