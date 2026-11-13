package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.PersianNumberUtils
import com.example.ui.VoiceUiState
import com.example.ui.theme.AlabasterSilk
import com.example.ui.theme.NishapurTurquoise
import com.example.ui.theme.NishapurTurquoiseBright
import com.example.ui.theme.NishapurTurquoiseDeep
import com.example.ui.theme.NishapurTurquoiseSoft
import com.example.ui.theme.OrosiHairline
import com.example.ui.theme.OrosiOnSurface
import com.example.ui.theme.OrosiOnSurfaceVariant
import com.example.ui.theme.OrosiSurfaceContainer
import com.example.ui.theme.OrosiSurfaceHigh
import com.example.ui.theme.PersianLapis
import com.example.ui.theme.PersianLapisLight
import com.example.ui.theme.RoyalAmethyst
import com.example.ui.theme.RoyalAmethystSoft
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SaffronGoldBright
import com.example.ui.theme.SaffronGoldLight
import kotlin.math.sin

@Composable
fun VoiceEntryScreen(
    voiceState: VoiceUiState,
    onSpeechChange: (String) -> Unit,
    onSimulateVoiceRecord: (String?) -> Unit,
    onUpdateExtractedFields: (String, String, Long, String) -> Unit,
    onClearUntaggedTarget: () -> Unit,
    onConfirmVoiceEntry: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToDashboard)

    var showManualSpeechEditor by remember { mutableStateOf(false) }

    val samplePhrases = remember {
        listOf(
            "پانصد هزار تومان خرید میوه از اسنپ‌مارکت با کارت بلو که ۲۰۰ تومنش دنگ علی بود",
            "هشتصد تومن شام رستوران نایب با کارت ملت که نصفش دنگ سارا بود",
            "دویست و پنجاه تومن قهوه و کیک از کافه لمیز با کارت سامان",
            "یک میلیون و دویست خرید لباس از دیجی‌کالا با کارت ملی که ۴۰۰ تومنش دنگ رضا بود"
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("voice_entry_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner if completing an untagged SMS transaction
        item {
            AnimatedVisibility(visible = voiceState.targetUntaggedTransaction != null) {
                val tx = voiceState.targetUntaggedTransaction
                if (tx != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NishapurTurquoiseDeep.copy(alpha = 0.65f))
                            .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تکمیل صوتی تراکنش پیامکی بی‌نام",
                                style = MaterialTheme.typography.titleSmall,
                                color = NishapurTurquoiseBright
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${PersianNumberUtils.formatToman(tx.totalAmountToman, true)} • ${tx.subtitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AlabasterSilk
                            )
                        }
                        IconButton(onClick = onClearUntaggedTarget) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "لغو تکمیل پیامک",
                                tint = AlabasterSilk
                            )
                        }
                    }
                }
            }
        }

        // 1. Turquoise Voice Waveform Hero Stage + Mic Button
        item {
            TurquoiseVoiceWaveformCard(
                isRecording = voiceState.isRecordingSimulated,
                onMicClick = { onSimulateVoiceRecord(null) }
            )
        }

        // 2. Recognized Persian Speech Card + Sample Voice Presets
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(OrosiSurfaceContainer)
                    .border(1.dp, NishapurTurquoise.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NishapurTurquoiseBright)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "گفتار فارسی شناسایی‌شده (موتور محاوره آفلاین)",
                            style = MaterialTheme.typography.labelLarge,
                            color = NishapurTurquoiseSoft
                        )
                    }

                    Text(
                        text = if (showManualSpeechEditor) "بستن ویرایش" else "ویرایش متن",
                        style = MaterialTheme.typography.labelMedium,
                        color = NishapurTurquoiseBright,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showManualSpeechEditor = !showManualSpeechEditor }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("toggle_speech_edit_button")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (showManualSpeechEditor) {
                    OutlinedTextField(
                        value = voiceState.recognizedText,
                        onValueChange = onSpeechChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_speech_input"),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = AlabasterSilk),
                        label = { Text("جمله محاوره‌ای خود را بنویسید یا ویرایش کنید") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NishapurTurquoiseBright,
                            unfocusedBorderColor = OrosiHairline,
                            focusedLabelColor = NishapurTurquoiseBright
                        )
                    )
                } else {
                    Text(
                        text = "«${voiceState.recognizedText}»",
                        style = MaterialTheme.typography.titleMedium.copy(
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AlabasterSilk,
                        modifier = Modifier.testTag("recognized_speech_text")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "نمونه‌های آماده تست سریع محاوره:",
                    style = MaterialTheme.typography.labelSmall,
                    color = OrosiOnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    samplePhrases.forEachIndexed { idx, phrase ->
                        val shortLabel = when (idx) {
                            0 -> "اسنپ‌مارکت + دنگ علی"
                            1 -> "شام نایب + دنگ سارا"
                            2 -> "کافه لمیز (بدون دنگ)"
                            else -> "دیجی‌کالا + دنگ رضا"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (voiceState.recognizedText == phrase)
                                        NishapurTurquoise.copy(alpha = 0.28f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    1.dp,
                                    if (voiceState.recognizedText == phrase)
                                        NishapurTurquoiseBright
                                    else OrosiHairline,
                                    RoundedCornerShape(50)
                                )
                                .clickable { onSimulateVoiceRecord(phrase) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("voice_preset_$idx")
                        ) {
                            Text(
                                text = shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = AlabasterSilk
                            )
                        }
                    }
                }
            }
        }

        // 3. Three Extracted Cards: فروشگاه (Store), بابت (Category/Purpose), سهم دنگ (Dong Split)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "اطلاعات استخراج‌شده هوشمند",
                    style = MaterialTheme.typography.titleMedium,
                    color = AlabasterSilk
                )

                // Extracted Card 1: فروشگاه (Store)
                ExtractedVoiceFieldCard(
                    label = "فروشگاه",
                    value = voiceState.storeName,
                    subValue = "پرداخت با ${voiceState.cardLabel} • کل مبلغ: ${PersianNumberUtils.formatToman(voiceState.totalAmountToman, true)}",
                    icon = Icons.Default.Storefront,
                    accentColor = NishapurTurquoiseBright,
                    testTag = "extracted_card_store"
                )

                // Extracted Card 2: بابت (Category / Purpose)
                ExtractedVoiceFieldCard(
                    label = "بابت",
                    value = voiceState.purposeCategory,
                    subValue = "دسته‌بندی خودکار خرج روزمره",
                    icon = Icons.Default.Category,
                    accentColor = PersianLapisLight,
                    testTag = "extracted_card_purpose"
                )

                // Extracted Card 3: سهم دنگ (Dong Split)
                val dongText = if (voiceState.dongAmountToman > 0L) {
                    "${PersianNumberUtils.formatToman(voiceState.dongAmountToman, true)} (سهم ${voiceState.dongFriendName})"
                } else {
                    "بدون دنگ (تمام مبلغ سهم شماست)"
                }
                val dongSubText = if (voiceState.dongAmountToman > 0L) {
                    "فقط سهم خالص شما (${PersianNumberUtils.formatToman(voiceState.userShareToman, true)}) از «خیال راحت امروز» کسر می‌شود"
                } else {
                    "مبلغ کسر شده از خیال راحت امروز: ${PersianNumberUtils.formatToman(voiceState.userShareToman, true)}"
                }
                ExtractedVoiceFieldCard(
                    label = "سهم دنگ",
                    value = dongText,
                    subValue = dongSubText,
                    icon = Icons.Default.Group,
                    accentColor = SaffronGoldBright,
                    testTag = "extracted_card_dong"
                )
            }
        }

        // 4. Confirm Button
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onConfirmVoiceEntry,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NishapurTurquoise,
                    contentColor = AlabasterSilk
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(16.dp))
                    .testTag("confirm_voice_entry_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تایید و ثبت هوشمند در پیشخوان",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TurquoiseVoiceWaveformCard(
    isRecording: Boolean,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

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
                NishapurTurquoiseBright.copy(alpha = 0.45f),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isRecording) "در حال گوش دادن به صدای شما..." else "ثبت صوتی بدون استرس و تایپ",
                style = MaterialTheme.typography.titleMedium,
                color = AlabasterSilk
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "فروشگاه، بابت و سهم دنگ دوستان به صورت خودکار جدا می‌شود",
                style = MaterialTheme.typography.bodySmall,
                color = NishapurTurquoiseSoft,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Animated Turquoise Waveform Bars
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                val barCount = 28
                val totalWidth = size.width
                val spacing = totalWidth / (barCount * 1.8f)
                val barWidth = spacing * 0.85f
                val centerY = size.height / 2f

                for (i in 0 until barCount) {
                    val norm = i.toFloat() / barCount.toFloat()
                    val envelope = sin(norm * Math.PI).toFloat()
                    val dynamicWave = if (isRecording) {
                        0.35f + 0.65f * kotlin.math.abs(sin(phase + i * 0.45f))
                    } else {
                        0.25f + 0.45f * kotlin.math.abs(sin(phase * 0.5f + i * 0.4f))
                    }
                    val barHeight = (size.height * 0.85f * envelope * dynamicWave).coerceAtLeast(8f)
                    val x = (totalWidth - (barCount * (barWidth + spacing))) / 2f + i * (barWidth + spacing)

                    drawRoundRect(
                        color = if (i % 3 == 0) NishapurTurquoiseBright else NishapurTurquoiseSoft.copy(alpha = 0.75f),
                        topLeft = Offset(x, centerY - barHeight / 2f),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Glowing Turquoise Microphone Button
            Surface(
                onClick = onMicClick,
                shape = CircleShape,
                color = NishapurTurquoise,
                modifier = Modifier
                    .size(68.dp)
                    .border(2.dp, NishapurTurquoiseBright, CircleShape)
                    .testTag("voice_record_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "ضبط و شبیه‌سازی صدای فارسی",
                        tint = AlabasterSilk,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "برای آزمایش جمله جدید، میکروفون را لمس کنید",
                style = MaterialTheme.typography.labelSmall,
                color = NishapurTurquoiseSoft
            )
        }
    }
}

@Composable
private fun ExtractedVoiceFieldCard(
    label: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrosiSurfaceHigh)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.16f))
                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AlabasterSilk
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = OrosiOnSurfaceVariant
            )
        }
    }
}
