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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Mic
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
import com.example.data.local.TransactionEntity
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
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SaffronGoldBright
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
                // Fallback to interactive preset simulator if hardware recognizer fails
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
            // 1. Glowing Turquoise Audio Wave (Uses real fa-IR SpeechRecognizer or Preview Fallback)
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
        mutableStateOf(if (base.contains("۱۴۰")) base else "$base ۱۴۰۵")
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

            // Toggle to mark as "قسط/وام", "بدهی/طلب (دُنگ)", or "تبدیل به طلا"
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
                    val cleanWeekdayDate = weekdayDateStr.replace("۱۴۰۵", "").replace("۱۴۰۳", "").trim()
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
fun HeroMathBreakdownBottomSheet(
    settings: AppSettingsEntity,
    reservedCommitmentsToman: Long,
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .testTag("hero_math_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "جزئیات محاسبه سهم خرج امروزت",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = AlabasterSilk
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrosiSurfaceContainer)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "تراز آزاد ماه حقوقی",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OrosiOnSurfaceVariant
                )
                Text(
                    text = PersianNumberUtils.formatToman(settings.monthlyBalanceToman, showUnit = true),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NishapurTurquoiseSoft
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrosiSurfaceContainer)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "کنار گذاشته برای قسط و چک",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OrosiOnSurfaceVariant
                )
                Text(
                    text = PersianNumberUtils.formatToman(reservedCommitmentsToman, showUnit = true),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SaffronGoldLight
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PersianLapis)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "سهم خرجِ امروزت",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PersianLapisLight
                )
                Text(
                    text = PersianNumberUtils.formatToman(settings.dailySafeBudgetToman, showUnit = true),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = AlabasterSilk
                )
            }
        }
    }
}

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
                        text = "دریافت خودکار و شبیه‌ساز پیامک بانکی",
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

            Spacer(modifier = Modifier.height(10.dp))

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrosiSurfaceHigh)
                    .border(1.dp, NishapurTurquoise.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
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
                        text = PersianNumberUtils.formatToman(preview.amountToman, true),
                        style = MaterialTheme.typography.titleSmall,
                        color = AlabasterSilk
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NishapurTurquoise,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 15.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("process_sms_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "پردازش پیامک",
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
