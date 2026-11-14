package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CommitmentTab
import com.example.ui.OrosiScreen
import com.example.ui.OrosiUiState
import com.example.ui.OrosiViewModel
import com.example.ui.components.AddBankAccountBottomSheet
import com.example.ui.components.AddCommitmentBottomSheet
import com.example.ui.components.CommitmentActionBottomSheet
import com.example.ui.components.HeroMathBreakdownBottomSheet
import com.example.ui.components.PaydayAndBackupBottomSheet
import com.example.ui.components.SmsSimulatorBottomSheet
import com.example.ui.components.TransactionDetailAndEditBottomSheet
import com.example.ui.components.VoiceOverlayBottomSheet
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.CommitmentsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.AlabasterSilk
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NishapurTurquoiseBright
import com.example.ui.theme.NishapurTurquoiseDeep
import com.example.ui.theme.NishapurTurquoiseSoft
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 1. Automatic Zero-Click SMS Permission Launcher on First Launch
    val autoSmsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.READ_SMS] == true ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        hasSmsPermission = granted
        if (granted) {
            viewModel.autoSyncSmsSilently()
        }
    }

    val statusModalPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.READ_SMS] == true ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        hasSmsPermission = granted
        if (granted) {
            viewModel.autoSyncSmsSilently()
        }
        viewModel.setShowSmsSimulator(true)
    }

    val cleanRescanPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.cleanPurgeAndRescanRealSms()
    }

    val voicePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.openVoiceOverlayNew()
    }

    // Automatically request READ_SMS & RECEIVE_SMS on first launch and immediately sync
    LaunchedEffect(Unit) {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val receiveGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        hasSmsPermission = readGranted
        if (readGranted && receiveGranted) {
            viewModel.autoSyncSmsSilently()
        } else {
            try {
                autoSmsPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS
                    )
                )
            } catch (_: Exception) {
                viewModel.autoSyncSmsSilently()
            }
        }
    }

    // 2. Automatically sync SMS on every Lifecycle.Event.ON_RESUME
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val readGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
                hasSmsPermission = readGranted
                if (readGranted) {
                    viewModel.autoSyncSmsSilently()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 3. Live ContentObserver registered on Telephony.Sms.CONTENT_URI for instant zero-click background sync
    DisposableEffect(context, hasSmsPermission) {
        val smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                viewModel.autoSyncSmsSilently()
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                viewModel.autoSyncSmsSilently()
            }
        }

        var registered = false
        if (hasSmsPermission) {
            try {
                context.contentResolver.registerContentObserver(
                    Telephony.Sms.CONTENT_URI,
                    true,
                    smsObserver
                )
                registered = true
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }

        onDispose {
            if (registered) {
                try {
                    context.contentResolver.unregisterContentObserver(smsObserver)
                } catch (_: Exception) {
                }
            }
        }
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
                onStatusClick = {
                    statusModalPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_SMS
                        )
                    )
                }
            )
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
                    OrosiScreen.HOME -> {
                        DashboardScreen(
                            settings = uiState.settings,
                            bankAccounts = uiState.bankAccounts,
                            commitments = uiState.commitments,
                            transactions = uiState.transactions,
                            onToggleHideAmounts = { viewModel.toggleHideAmounts() },
                            onSetOnlyTankhahMode = { viewModel.setOnlyTankhahMode(it) },
                            onToggleAccountTankhah = { viewModel.toggleAccountTankhah(it) },
                            onOpenAccountsTab = { viewModel.navigateTo(OrosiScreen.ACCOUNTS) },
                            onHeroCardClick = { viewModel.setShowHeroMathSheet(true) },
                            onAddVoiceClick = {
                                voicePermissionLauncher.launch(
                                    arrayOf(Manifest.permission.RECORD_AUDIO)
                                )
                            },
                            onTransactionClick = { tx -> viewModel.openTransactionDetails(tx) },
                            onUntaggedMicClick = { tx ->
                                viewModel.openVoiceForUntaggedTransaction(tx)
                            }
                        )
                    }

                    OrosiScreen.TRANSACTIONS -> {
                        TransactionsScreen(
                            transactions = uiState.transactions,
                            selectedYearMonthKey = uiState.selectedYearMonthKey,
                            hideAmounts = uiState.settings.hideAmounts,
                            onChangeMonth = { delta -> viewModel.changeSelectedMonth(delta) },
                            onTransactionClick = { tx -> viewModel.openTransactionDetails(tx) },
                            onUntaggedMicClick = { tx ->
                                viewModel.openVoiceForUntaggedTransaction(tx)
                            },
                            onBackToHome = { viewModel.navigateTo(OrosiScreen.HOME) }
                        )
                    }

                    OrosiScreen.ACCOUNTS -> {
                        AccountsScreen(
                            bankAccounts = uiState.bankAccounts,
                            hideAmounts = uiState.settings.hideAmounts,
                            onToggleTankhah = { bankName -> viewModel.toggleAccountTankhah(bankName) },
                            onOpenAddAccountModal = { viewModel.setShowAddBankAccountModal(true) },
                            onBackToHome = { viewModel.navigateTo(OrosiScreen.HOME) }
                        )
                    }

                    OrosiScreen.TOOLS -> {
                        CommitmentsScreen(
                            selectedTab = uiState.selectedCommitmentTab,
                            onSelectTab = { viewModel.selectCommitmentTab(it) },
                            commitments = uiState.commitments,
                            transactions = uiState.transactions,
                            hideAmounts = uiState.settings.hideAmounts,
                            onCommitmentClick = { viewModel.openCommitmentActionSheet(it) },
                            onOpenAddCommitmentModal = { viewModel.setShowAddCommitmentModal(true) },
                            onBackToDashboard = { viewModel.navigateTo(OrosiScreen.HOME) }
                        )
                    }
                }
            }
        }
    }

    // Jibeman-Style Transaction Details & Edit Bottom Sheet
    val detailTx = uiState.selectedTransactionForDetail
    if (detailTx != null) {
        TransactionDetailAndEditBottomSheet(
            transaction = detailTx,
            onQuickVoiceRetag = { tx -> viewModel.openVoiceForUntaggedTransaction(tx) },
            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
            onSaveEdits = { amount, balanceAfter, date, time, card, purpose, store, mode ->
                viewModel.saveTransactionEdits(
                    originalTx = detailTx,
                    amountToman = amount,
                    balanceAfterToman = balanceAfter,
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

    // Interactive Commitment Settlement / Edit / Delete Sheet
    val actionCommitment = uiState.selectedCommitmentForAction
    if (actionCommitment != null) {
        CommitmentActionBottomSheet(
            commitment = actionCommitment,
            onSaveCommitment = { title, counterparty, amount, dueDate, isSettled ->
                viewModel.updateCommitmentFromSheet(
                    original = actionCommitment,
                    title = title,
                    counterparty = counterparty,
                    amountToman = amount,
                    dueDateStr = dueDate,
                    isSettled = isSettled
                )
            },
            onDeleteCommitment = { viewModel.deleteCommitmentFromSheet(actionCommitment) },
            onDismiss = { viewModel.dismissCommitmentActionSheet() }
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

    // Hero Card Math & Monthly Balance Report Modal ("ترازنامه ماه حقوقی")
    if (uiState.showHeroMathSheet) {
        HeroMathBreakdownBottomSheet(
            settings = uiState.settings,
            transactions = uiState.transactions,
            reservedCommitmentsToman = reservedCommitmentsToman,
            onDismiss = { viewModel.setShowHeroMathSheet(false) }
        )
    }

    // Clean, Reassuring Automatic SMS Connection Status Modal
    if (uiState.showSmsSimulatorModal) {
        SmsSimulatorBottomSheet(
            onCleanPurgeAndRescanRealSms = {
                cleanRescanPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS
                    )
                )
            },
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
        val defaultCategory = when (uiState.selectedCommitmentTab) {
            CommitmentTab.SAYYAD -> "SAYYAD_ISSUED"
            CommitmentTab.INSTALLMENTS -> "LOAN_INSTALLMENT"
            CommitmentTab.DONG -> "DONG"
            CommitmentTab.REPORTS -> "SAYYAD_ISSUED"
        }
        AddCommitmentBottomSheet(
            defaultCategory = defaultCategory,
            onAddCommitment = { category, title, counterparty, amount, dueDate, sayyadId ->
                viewModel.addNewCommitment(category, title, counterparty, amount, dueDate, sayyadId)
            },
            onDismiss = { viewModel.setShowAddCommitmentModal(false) }
        )
    }

    if (uiState.showAddBankAccountModal) {
        AddBankAccountBottomSheet(
            onAddAccount = { bankName, balanceToman, isTankhah, cardLast4 ->
                viewModel.addOrUpdateBankAccount(bankName, balanceToman, isTankhah, cardLast4)
            },
            onDismiss = { viewModel.setShowAddBankAccountModal(false) }
        )
    }
}

@Composable
private fun OrosiTopHeader(
    currentScreen: OrosiScreen,
    onProfileClick: () -> Unit,
    onStatusClick: () -> Unit
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

        // Left side (in RTL): Clean, reassuring Automatic Bank SMS Status Pill
        Surface(
            onClick = onStatusClick,
            shape = RoundedCornerShape(50),
            color = NishapurTurquoiseDeep.copy(alpha = 0.85f),
            modifier = Modifier
                .border(1.dp, NishapurTurquoiseBright.copy(alpha = 0.7f), RoundedCornerShape(50))
                .testTag("sms_simulator_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "اتصال خودکار به پیامک همه بانک‌ها: فعال ✓",
                    tint = NishapurTurquoiseBright,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "اتصال خودکار بانک‌ها ✓",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = NishapurTurquoiseSoft,
                    maxLines = 1,
                    softWrap = false
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
            Triple(OrosiScreen.HOME, "خانه", Icons.Default.Home),
            Triple(OrosiScreen.TRANSACTIONS, "تراکنش‌ها", Icons.Default.ReceiptLong),
            Triple(OrosiScreen.ACCOUNTS, "حساب‌ها", Icons.Default.AccountBalanceWallet),
            Triple(OrosiScreen.TOOLS, "ابزارها", Icons.Default.Build)
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
