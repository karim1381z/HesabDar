package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.OrosiScreen
import com.example.ui.OrosiUiState
import com.example.ui.OrosiViewModel
import com.example.ui.components.AddCommitmentBottomSheet
import com.example.ui.components.ConvertSurplusToGoldBottomSheet
import com.example.ui.components.HeroMathBreakdownBottomSheet
import com.example.ui.components.PaydayAndBackupBottomSheet
import com.example.ui.components.SmsSimulatorBottomSheet
import com.example.ui.components.TransactionDetailAndEditBottomSheet
import com.example.ui.components.VoiceOverlayBottomSheet
import com.example.ui.screens.CommitmentsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InflationShieldScreen
import com.example.ui.theme.AlabasterSilk
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NishapurTurquoise
import com.example.ui.theme.NishapurTurquoiseBright
import com.example.ui.theme.NishapurTurquoiseDeep
import com.example.ui.theme.OrosiHairline
import com.example.ui.theme.OrosiOnSurfaceVariant
import com.example.ui.theme.OrosiSurface
import com.example.ui.theme.OrosiSurfaceContainer
import com.example.ui.theme.OrosiSurfaceLowest
import com.example.ui.theme.PersianLapis
import com.example.ui.theme.PersianLapisLight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: OrosiViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                OrosiFinanceApp(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun OrosiFinanceApp(
    uiState: OrosiUiState,
    viewModel: OrosiViewModel
) {
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.setShowSmsSimulator(true)
    }

    val reservedCommitmentsToman = uiState.commitments
        .filter { !it.isSettled && (it.category == "SAYYAD_ISSUED" || it.category == "LOAN_INSTALLMENT") }
        .sumOf { it.amountToman }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(OrosiSurface),
        containerColor = OrosiSurface,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            OrosiTopHeader(
                currentScreen = uiState.currentScreen,
                onProfileClick = { viewModel.setShowPaydayBackupModal(true) },
                onBellClick = {
                    smsPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_SMS
                        )
                    )
                }
            )
        },
        floatingActionButton = {
            if (uiState.currentScreen == OrosiScreen.DASHBOARD) {
                FloatingActionButton(
                    onClick = { viewModel.openVoiceOverlayNew() },
                    shape = CircleShape,
                    containerColor = NishapurTurquoise,
                    contentColor = AlabasterSilk,
                    modifier = Modifier
                        .size(60.dp)
                        .border(1.5.dp, NishapurTurquoiseBright, CircleShape)
                        .testTag("floating_voice_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "ثبت صوتی",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        bottomBar = {
            OrosiBottomNavigationBar(
                currentScreen = uiState.currentScreen,
                onSelectScreen = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Calm Notification Banner
            AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                val msg = uiState.statusBannerMessage.orEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NishapurTurquoiseDeep)
                        .border(1.dp, NishapurTurquoiseBright, RoundedCornerShape(14.dp))
                        .clickable { viewModel.dismissBanner() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("status_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = AlabasterSilk,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Crossfade(
                targetState = uiState.currentScreen,
                label = "orosi_screen_transition",
                modifier = Modifier.weight(1f)
            ) { screen ->
                when (screen) {
                    OrosiScreen.DASHBOARD -> {
                        DashboardScreen(
                            settings = uiState.settings,
                            transactions = uiState.transactions,
                            onHeroCardClick = { viewModel.setShowHeroMathSheet(true) },
                            onTransactionClick = { tx -> viewModel.openTransactionDetails(tx) },
                            onUntaggedMicClick = { tx -> viewModel.openVoiceForUntaggedTransaction(tx) }
                        )
                    }

                    OrosiScreen.COMMITMENTS -> {
                        CommitmentsScreen(
                            commitments = uiState.commitments,
                            onToggleSettled = { viewModel.toggleCommitmentSettled(it) },
                            onOpenAddCommitmentModal = { viewModel.setShowAddCommitmentModal(true) },
                            onBackToDashboard = { viewModel.navigateTo(OrosiScreen.DASHBOARD) }
                        )
                    }

                    OrosiScreen.INFLATION_SHIELD -> {
                        InflationShieldScreen(
                            settings = uiState.settings,
                            onOpenConvertGoldModal = { viewModel.setShowConvertGoldModal(true) },
                            onBackToDashboard = { viewModel.navigateTo(OrosiScreen.DASHBOARD) }
                        )
                    }
                }
            }
        }
    }

    // Full Transaction Details & Edit Bottom Sheet (6 Core Fields + Classification Toggle + Voice Re-tag)
    val detailTx = uiState.selectedTransactionForDetail
    if (detailTx != null) {
        TransactionDetailAndEditBottomSheet(
            transaction = detailTx,
            onQuickVoiceRetag = { tx -> viewModel.openVoiceForUntaggedTransaction(tx) },
            onSaveEdits = { amount, date, time, card, purpose, store, mode ->
                viewModel.saveTransactionEdits(
                    originalTx = detailTx,
                    amountToman = amount,
                    weekdayDate = date,
                    timeStr = time,
                    cardInfo = card,
                    purpose = purpose,
                    storeName = store,
                    specialTagMode = mode
                )
            },
            onDismiss = { viewModel.dismissTransactionDetails() }
        )
    }

    // Effortless Voice Overlay Bottom Sheet
    if (uiState.showVoiceOverlaySheet) {
        VoiceOverlayBottomSheet(
            voiceState = uiState.voiceState,
            onSpeechChange = { viewModel.onVoiceSpeechChanged(it) },
            onSimulateVoiceRecord = { viewModel.triggerVoiceSimulation(null) },
            onConfirmVoiceEntry = { viewModel.confirmVoiceEntry() },
            onDismiss = { viewModel.dismissVoiceOverlay() }
        )
    }

    // Optional Hero Card Math Modal
    if (uiState.showHeroMathSheet) {
        HeroMathBreakdownBottomSheet(
            settings = uiState.settings,
            reservedCommitmentsToman = reservedCommitmentsToman,
            onDismiss = { viewModel.setShowHeroMathSheet(false) }
        )
    }

    if (uiState.showSmsSimulatorModal) {
        SmsSimulatorBottomSheet(
            onPreviewSms = { viewModel.previewBankSms(it) },
            onProcessSms = { viewModel.processBankSms(it) },
            onDismiss = { viewModel.setShowSmsSimulator(false) }
        )
    }

    if (uiState.showPaydayBackupModal) {
        PaydayAndBackupBottomSheet(
            settings = uiState.settings,
            backupJsonText = uiState.backupJsonText,
            onSaveSettings = { day, label, autoSalary, goldPrice, dailySafe ->
                viewModel.savePaydayAndGoldSettings(day, label, autoSalary, goldPrice, dailySafe)
            },
            onImportBackup = { viewModel.importBackupJson(it) },
            onResetDefault = { viewModel.resetDatabaseToDefault() },
            onDismiss = { viewModel.setShowPaydayBackupModal(false) }
        )
    }

    if (uiState.showAddCommitmentModal) {
        AddCommitmentBottomSheet(
            onAddCommitment = { category, title, counterparty, amount, dueDate, sayyadId ->
                viewModel.addNewCommitment(category, title, counterparty, amount, dueDate, sayyadId)
            },
            onDismiss = { viewModel.setShowAddCommitmentModal(false) }
        )
    }

    if (uiState.showConvertGoldModal) {
        ConvertSurplusToGoldBottomSheet(
            settings = uiState.settings,
            onConvert = { viewModel.convertSurplusToGold(it) },
            onDismiss = { viewModel.setShowConvertGoldModal(false) }
        )
    }
}

@Composable
private fun OrosiTopHeader(
    currentScreen: OrosiScreen,
    onProfileClick: () -> Unit,
    onBellClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Right side (in RTL): Clean Profile Icon + Screen Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(OrosiSurfaceContainer)
                .border(1.dp, OrosiHairline, RoundedCornerShape(50))
                .clickable(onClick = onProfileClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("profile_settings_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PersianLapis)
                    .border(1.dp, PersianLapisLight.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "تنظیمات ماه حقوقی و پشتیبان‌گیری",
                    tint = AlabasterSilk,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = currentScreen.persianTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AlabasterSilk
            )
        }

        // Left side (in RTL): Unclipped Notification Bell Button with generous margin
        Surface(
            onClick = onBellClick,
            shape = CircleShape,
            color = OrosiSurfaceContainer,
            modifier = Modifier
                .size(48.dp)
                .border(1.dp, NishapurTurquoise.copy(alpha = 0.55f), CircleShape)
                .testTag("sms_simulator_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "شبیه‌ساز پیامک بانکی",
                    tint = NishapurTurquoiseBright,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun OrosiBottomNavigationBar(
    currentScreen: OrosiScreen,
    onSelectScreen: (OrosiScreen) -> Unit
) {
    NavigationBar(
        containerColor = OrosiSurfaceLowest,
        contentColor = AlabasterSilk,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = OrosiHairline)
            .testTag("bottom_navigation_bar")
    ) {
        val navItems = listOf(
            Triple(OrosiScreen.DASHBOARD, "امروز من", Icons.Default.Dashboard),
            Triple(OrosiScreen.COMMITMENTS, "قسط و چک", Icons.Default.EventNote),
            Triple(OrosiScreen.INFLATION_SHIELD, "قلک طلا", Icons.Default.Shield)
        )

        navItems.forEach { (screen, label, icon) ->
            val selected = currentScreen == screen
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectScreen(screen) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NishapurTurquoiseBright,
                    selectedTextColor = AlabasterSilk,
                    indicatorColor = PersianLapis,
                    unselectedIconColor = OrosiOnSurfaceVariant,
                    unselectedTextColor = OrosiOnSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_tab_${screen.route}")
            )
        }
    }
}
