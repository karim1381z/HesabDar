package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettingsEntity
import com.example.data.local.BankAccountEntity
import com.example.data.local.CommitmentEntity
import com.example.data.local.OrosiDatabase
import com.example.data.local.TransactionEntity
import com.example.data.repository.OrosiFinanceRepository
import com.example.domain.BankSmsParserEngine
import com.example.domain.JalaliCalendarUtils
import com.example.domain.ParsedSmsResult
import com.example.domain.ParsedVoiceExpense
import com.example.domain.PersianNumberUtils
import com.example.domain.PersianVoiceSlangParserEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OrosiScreen(val route: String, val persianTitle: String) {
    HOME("home", "خانه"),
    TRANSACTIONS("transactions", "تراکنش‌ها"),
    ACCOUNTS("accounts", "حساب‌ها"),
    TOOLS("tools", "ابزارها")
}

enum class CommitmentTab(val title: String) {
    SAYYAD("چک صیادی"),
    INSTALLMENTS("وام و قسط"),
    DONG("بدهی و طلب (دُنگ)"),
    REPORTS("گزارش‌گیری دخل و خرج")
}

data class VoiceUiState(
    val isRecordingSimulated: Boolean = false,
    val recognizedText: String = "",
    val storeName: String = "",
    val purposeCategory: String = "",
    val cardLabel: String = "کارت بانکی",
    val totalAmountToman: Long = 0L,
    val dongAmountToman: Long = 0L,
    val dongFriendName: String = "",
    val userShareToman: Long = 0L,
    val targetUntaggedTransaction: TransactionEntity? = null
)

data class OrosiUiState(
    val currentScreen: OrosiScreen = OrosiScreen.HOME,
    val selectedCommitmentTab: CommitmentTab = CommitmentTab.SAYYAD,
    val selectedYearMonthKey: Int = JalaliCalendarUtils.fromTimestamp(System.currentTimeMillis()).yearMonthKey,
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val transactions: List<TransactionEntity> = emptyList(),
    val bankAccounts: List<BankAccountEntity> = emptyList(),
    val commitments: List<CommitmentEntity> = emptyList(),
    val voiceState: VoiceUiState = VoiceUiState(),
    val selectedTransactionForDetail: TransactionEntity? = null,
    val selectedCommitmentForAction: CommitmentEntity? = null,
    val showVoiceOverlaySheet: Boolean = false,
    val showHeroMathSheet: Boolean = false,
    val showSmsSimulatorModal: Boolean = false,
    val showPaydayBackupModal: Boolean = false,
    val showAddCommitmentModal: Boolean = false,
    val showAddBankAccountModal: Boolean = false,
    val backupJsonText: String = "",
    val statusBannerMessage: String? = null
)

class OrosiViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val dao = OrosiDatabase.getInstance(appContext).orosiDao()
    private val repository = OrosiFinanceRepository(dao, appContext)

    private val _currentScreen = MutableStateFlow(OrosiScreen.HOME)
    private val _selectedCommitmentTab = MutableStateFlow(CommitmentTab.SAYYAD)
    private val _selectedYearMonthKey = MutableStateFlow(
        JalaliCalendarUtils.fromTimestamp(System.currentTimeMillis()).yearMonthKey
    )
    private val _voiceState = MutableStateFlow(VoiceUiState())
    private val _selectedTransactionForDetail = MutableStateFlow<TransactionEntity?>(null)
    private val _selectedCommitmentForAction = MutableStateFlow<CommitmentEntity?>(null)
    private val _showVoiceOverlaySheet = MutableStateFlow(false)
    private val _showHeroMathSheet = MutableStateFlow(false)
    private val _showSmsSimulatorModal = MutableStateFlow(false)
    private val _showPaydayBackupModal = MutableStateFlow(false)
    private val _showAddCommitmentModal = MutableStateFlow(false)
    private val _showAddBankAccountModal = MutableStateFlow(false)
    private val _backupJsonText = MutableStateFlow("")
    private val _statusBannerMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    val uiState: StateFlow<OrosiUiState> = combine(
        _currentScreen,
        _selectedCommitmentTab,
        repository.settingsFlow,
        repository.transactionsFlow,
        repository.commitmentsFlow
    ) { screen, commitmentTab, settings, txs, commitments ->
        OrosiUiState(
            currentScreen = screen,
            selectedCommitmentTab = commitmentTab,
            settings = settings ?: AppSettingsEntity(),
            transactions = txs,
            commitments = commitments
        )
    }.combine(
        combine(
            repository.bankAccountsFlow,
            _selectedYearMonthKey
        ) { accounts, ymKey ->
            Pair(accounts, ymKey)
        }
    ) { base, (accounts, ymKey) ->
        base.copy(
            bankAccounts = accounts,
            selectedYearMonthKey = ymKey
        )
    }.combine(
        combine(
            _voiceState,
            _showVoiceOverlaySheet,
            _showHeroMathSheet,
            _showSmsSimulatorModal,
            _showPaydayBackupModal
        ) { voice, voiceSheet, mathSheet, smsModal, paydayModal ->
            Tuple5(voice, voiceSheet, mathSheet, smsModal, paydayModal)
        }
    ) { base, modals ->
        base.copy(
            voiceState = modals.first,
            showVoiceOverlaySheet = modals.second,
            showHeroMathSheet = modals.third,
            showSmsSimulatorModal = modals.fourth,
            showPaydayBackupModal = modals.fifth
        )
    }.combine(
        combine(
            _selectedTransactionForDetail,
            _selectedCommitmentForAction,
            _showAddCommitmentModal,
            _showAddBankAccountModal
        ) { selectedTx, selectedComm, addComm, addAcc ->
            Tuple4(selectedTx, selectedComm, addComm, addAcc)
        }
    ) { state, extra ->
        state.copy(
            selectedTransactionForDetail = extra.first,
            selectedCommitmentForAction = extra.second,
            showAddCommitmentModal = extra.third,
            showAddBankAccountModal = extra.fourth
        )
    }.combine(
        combine(_backupJsonText, _statusBannerMessage) { backup, banner ->
            Pair(backup, banner)
        }
    ) { state, pair ->
        state.copy(
            backupJsonText = pair.first,
            statusBannerMessage = pair.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OrosiUiState()
    )

    fun navigateTo(screen: OrosiScreen) {
        _currentScreen.value = screen
    }

    fun selectCommitmentTab(tab: CommitmentTab) {
        _selectedCommitmentTab.value = tab
    }

    fun changeSelectedMonth(deltaMonths: Int) {
        val current = _selectedYearMonthKey.value
        var year = current / 100
        var month = (current % 100) + deltaMonths
        while (month < 1) {
            month += 12
            year -= 1
        }
        while (month > 12) {
            month -= 12
            year += 1
        }
        _selectedYearMonthKey.value = year * 100 + month
    }

    fun toggleHideAmounts() {
        viewModelScope.launch {
            repository.toggleHideAmounts()
        }
    }

    fun setOnlyTankhahMode(onlyTankhah: Boolean) {
        viewModelScope.launch {
            repository.setOnlyTankhahMode(onlyTankhah)
        }
    }

    fun toggleAccountTankhah(bankName: String) {
        viewModelScope.launch {
            repository.toggleAccountTankhah(bankName)
        }
    }

    fun setShowAddBankAccountModal(show: Boolean) {
        _showAddBankAccountModal.value = show
    }

    fun addOrUpdateBankAccount(
        bankName: String,
        balanceToman: Long,
        isTankhah: Boolean,
        cardLast4: String = ""
    ) {
        viewModelScope.launch {
            repository.addOrUpdateBankAccount(bankName, balanceToman, isTankhah, cardLast4)
            _showAddBankAccountModal.value = false
            showBanner("منبع مالی «$bankName» با موجودی ${PersianNumberUtils.formatToman(balanceToman, true)} ذخیره شد.")
        }
    }

    fun setShowHeroMathSheet(show: Boolean) {
        _showHeroMathSheet.value = show
    }

    fun openTransactionDetails(tx: TransactionEntity) {
        _selectedTransactionForDetail.value = tx
    }

    fun dismissTransactionDetails() {
        _selectedTransactionForDetail.value = null
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx.id)
            _selectedTransactionForDetail.value = null
            showBanner("تراکنش از فهرست حذف شد.")
        }
    }

    fun saveTransactionEdits(
        originalTx: TransactionEntity,
        amountToman: Long,
        balanceAfterToman: Long?,
        weekdayDate: String,
        timeStr: String,
        cardInfo: String,
        purpose: String,
        storeName: String,
        specialTagMode: String
    ) {
        viewModelScope.launch {
            repository.updateTransactionDetails(
                originalTx = originalTx,
                amountToman = amountToman,
                balanceAfterToman = balanceAfterToman,
                weekdayDate = weekdayDate,
                timeStr = timeStr,
                cardInfo = cardInfo,
                purpose = purpose,
                storeName = storeName,
                specialTagMode = specialTagMode
            )
            _selectedTransactionForDetail.value = null
            val displayLabel = purpose.trim().ifEmpty { originalTx.displayTitle }
            showBanner("جزئیات تراکنش «$displayLabel» ذخیره شد ✓")
        }
    }

    fun openCommitmentActionSheet(commitment: CommitmentEntity) {
        _selectedCommitmentForAction.value = commitment
    }

    fun dismissCommitmentActionSheet() {
        _selectedCommitmentForAction.value = null
    }

    fun updateCommitmentFromSheet(
        original: CommitmentEntity,
        title: String,
        counterparty: String,
        amountToman: Long,
        dueDateStr: String,
        isSettled: Boolean
    ) {
        viewModelScope.launch {
            repository.updateCommitmentDetails(
                original = original,
                title = title,
                counterparty = counterparty,
                amountToman = amountToman,
                dueDateStr = dueDateStr,
                isSettled = isSettled
            )
            _selectedCommitmentForAction.value = null
            val statusMsg = if (isSettled) "پاس شد / تسویه شد ✓ و تراز ماه بروزرسانی شد." else "تغییرات تعهد ذخیره شد."
            showBanner("«$title» — $statusMsg")
        }
    }

    fun deleteCommitmentFromSheet(commitment: CommitmentEntity) {
        viewModelScope.launch {
            repository.deleteCommitment(commitment.id)
            _selectedCommitmentForAction.value = null
            showBanner("«${commitment.title}» از فهرست تعهدات حذف شد.")
        }
    }

    /**
     * Opens the Voice Bottom Sheet with an EMPTY transcript (`""`)—never hardcoded "اسنپ‌مارکت"!
     */
    fun openVoiceOverlayNew() {
        _voiceState.value = VoiceUiState(
            isRecordingSimulated = false,
            recognizedText = "",
            storeName = "",
            purposeCategory = "",
            cardLabel = "کارت بانکی",
            totalAmountToman = 0L,
            dongAmountToman = 0L,
            dongFriendName = "",
            userShareToman = 0L,
            targetUntaggedTransaction = null
        )
        _showVoiceOverlaySheet.value = true
    }

    /**
     * Opens the Voice Bottom Sheet for an untagged transaction with an EMPTY transcript (`""`)
     * (or its existing custom purpose if already tagged), NEVER defaulting to "اسنپ‌مارکت" or "خرید روزمره"!
     */
    fun openVoiceForUntaggedTransaction(tx: TransactionEntity) {
        _selectedTransactionForDetail.value = null
        val initialSpeech = if (!tx.isUntagged &&
            tx.purpose.isNotBlank() &&
            tx.purpose != "نیازمند تکمیل صوتی" &&
            tx.purpose != "برداشت کارتی / خرید"
        ) {
            tx.purpose
        } else {
            ""
        }

        _voiceState.value = VoiceUiState(
            isRecordingSimulated = false,
            recognizedText = initialSpeech,
            storeName = tx.shortBankName,
            purposeCategory = initialSpeech,
            cardLabel = tx.cardInfo,
            totalAmountToman = tx.totalAmountToman,
            dongAmountToman = tx.dongAmountToman,
            dongFriendName = tx.dongFriendName,
            userShareToman = tx.userShareToman,
            targetUntaggedTransaction = tx
        )
        if (initialSpeech.isNotBlank()) {
            parseAndUpdateVoiceState(initialSpeech, tx)
        }
        _showVoiceOverlaySheet.value = true
    }

    fun dismissVoiceOverlay() {
        _showVoiceOverlaySheet.value = false
    }

    fun onVoiceSpeechChanged(newSpeech: String) {
        val currentTarget = _voiceState.value.targetUntaggedTransaction
        parseAndUpdateVoiceState(newSpeech, currentTarget)
    }

    private fun parseAndUpdateVoiceState(speech: String, targetTx: TransactionEntity?) {
        val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand(
            rawSpeech = speech,
            fallbackTotalToman = targetTx?.totalAmountToman,
            defaultBankName = targetTx?.shortBankName ?: ""
        )
        _voiceState.value = _voiceState.value.copy(
            recognizedText = speech,
            storeName = parsed.storeName,
            purposeCategory = parsed.purposeCategory,
            cardLabel = targetTx?.cardInfo ?: parsed.cardLabel,
            totalAmountToman = targetTx?.totalAmountToman ?: parsed.totalAmountToman,
            dongAmountToman = parsed.dongAmountToman,
            dongFriendName = parsed.dongFriendName,
            userShareToman = parsed.userShareToman,
            targetUntaggedTransaction = targetTx
        )
    }

    /**
     * Fallback voice simulation when running on a web emulator without a microphone.
     * Uses realistic everyday phrases like "پفک خریدم", "شارژ ایرانسل", "نانوایی سنگک", "کافه قهوه"
     * and NEVER uses "اسنپ‌مارکت"!
     */
    fun triggerVoiceSimulation(samplePhrase: String? = null) {
        viewModelScope.launch {
            _voiceState.value = _voiceState.value.copy(isRecordingSimulated = true)
            delay(500L)
            if (samplePhrase != null) {
                parseAndUpdateVoiceState(samplePhrase, _voiceState.value.targetUntaggedTransaction)
            } else if (_voiceState.value.recognizedText.isBlank()) {
                val targetTx = _voiceState.value.targetUntaggedTransaction
                val phrase = when {
                    targetTx != null && targetTx.totalAmountToman == 49_500L -> "شارژ ایرانسل"
                    targetTx != null -> "پفک خریدم"
                    else -> "صد هزار تومان پفک خریدم"
                }
                parseAndUpdateVoiceState(phrase, targetTx)
            }
            _voiceState.value = _voiceState.value.copy(isRecordingSimulated = false)
        }
    }

    fun confirmVoiceEntry() {
        viewModelScope.launch {
            val v = _voiceState.value
            val parsed = PersianVoiceSlangParserEngine.parseVoiceCommand(
                rawSpeech = v.recognizedText,
                fallbackTotalToman = v.targetUntaggedTransaction?.totalAmountToman ?: v.totalAmountToman.takeIf { it > 0L },
                defaultBankName = v.targetUntaggedTransaction?.shortBankName ?: ""
            )
            if (parsed.purposeCategory.isBlank()) {
                showBanner("لطفاً توضیح تراکنش را بگویید یا بنویسید.")
                return@launch
            }
            repository.confirmVoiceExpense(parsed, v.targetUntaggedTransaction?.id)
            val msg = if (parsed.dongAmountToman > 0L && parsed.dongFriendName.isNotBlank()) {
                "ثبت شد! «${parsed.purposeCategory}» — فقط سهم شما (${PersianNumberUtils.formatToman(parsed.userShareToman, true)}) کسر شد."
            } else {
                "تراکنش با عنوان «${parsed.purposeCategory}» ثبت شد ✓"
            }
            _showVoiceOverlaySheet.value = false
            _voiceState.value = VoiceUiState()
            showBanner(msg)
        }
    }

    fun setShowSmsSimulator(show: Boolean) {
        _showSmsSimulatorModal.value = show
    }

    fun previewBankSms(rawSms: String): ParsedSmsResult {
        return BankSmsParserEngine.parseBankSms(rawSms)
    }

    fun processBankSms(rawSms: String) {
        viewModelScope.launch {
            val result = repository.processIncomingBankSms(rawSms)
            _showSmsSimulatorModal.value = false
            if (result != null) {
                showBanner(result.summaryMessage)
            }
        }
    }

    /**
     * Silent, 100% automatic zero-click background SMS sync triggered on app launch,
     * permission grant, Lifecycle.Event.ON_RESUME, and ContentObserver on Telephony.Sms.CONTENT_URI.
     */
    fun autoSyncSmsSilently() {
        viewModelScope.launch {
            repository.scanDeviceSmsInboxLast30Days(appContext, daysBack = 3650)
        }
    }

    fun scanDeviceSmsInboxLast30Days() {
        viewModelScope.launch {
            val count = repository.scanDeviceSmsInboxLast30Days(appContext, daysBack = 3650)
            _showSmsSimulatorModal.value = false
            if (count > 0) {
                val countStr = PersianNumberUtils.toPersianDigits(count.toString())
                showBanner("$countStr پیامک بانکی جدید به‌صورت خودکار همگام‌سازی شد.")
            }
        }
    }

    /**
     * Triggered when the user taps "پاکسازی و بازخوانی تمیز پیامک‌های واقعی" in the status modal.
     */
    fun cleanPurgeAndRescanRealSms() {
        viewModelScope.launch {
            val count = repository.cleanPurgeAndRescanRealSms(appContext)
            _showSmsSimulatorModal.value = false
            val countStr = PersianNumberUtils.toPersianDigits(count.toString())
            if (count > 0) {
                showBanner("پاکسازی انجام شد و $countStr پیامک واقعی بانک با مانده حساب‌ها بازخوانی شد ✓")
            } else {
                showBanner("پاکسازی و بازخوانی تمیز پیامک‌های بانکی انجام شد ✓")
            }
        }
    }

    fun setShowPaydayBackupModal(show: Boolean) {
        if (show) {
            viewModelScope.launch {
                _backupJsonText.value = repository.exportBackupJson()
                _showPaydayBackupModal.value = true
            }
        } else {
            _showPaydayBackupModal.value = false
        }
    }

    fun savePaydayAndGoldSettings(
        paydayStartDay: Int,
        paydayCycleLabel: String,
        autoDetectSalary: Boolean,
        goldPrice18kToman: Long,
        dailySafeBudgetToman: Long
    ) {
        viewModelScope.launch {
            repository.updatePaydayAndGoldSettings(
                paydayStartDay = paydayStartDay,
                paydayCycleLabel = paydayCycleLabel,
                autoDetectSalary = autoDetectSalary,
                goldPrice18kToman = goldPrice18kToman,
                dailySafeBudgetToman = dailySafeBudgetToman
            )
            _showPaydayBackupModal.value = false
            showBanner("تنظیمات ماه حقوقی ذخیره شد.")
        }
    }

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            val ok = repository.importBackupJson(json)
            if (ok) {
                _showPaydayBackupModal.value = false
                showBanner("پشتیبان دیتابیس آفلاین با موفقیت بازیابی شد.")
            } else {
                showBanner("خطا در ساختار فایل پشتیبان JSON.")
            }
        }
    }

    fun resetDatabaseToDefault() {
        viewModelScope.launch {
            repository.resetToDefaultSeedData()
            _backupJsonText.value = repository.exportBackupJson()
            showBanner("داده‌های برنامه به حالت پیش‌فرض بازنشانی شد.")
        }
    }

    fun setShowAddCommitmentModal(show: Boolean) {
        _showAddCommitmentModal.value = show
    }

    fun addNewCommitment(
        category: String,
        title: String,
        counterparty: String,
        amountToman: Long,
        dueDateStr: String,
        sayyadId: String
    ) {
        viewModelScope.launch {
            repository.addNewCommitment(category, title, counterparty, amountToman, dueDateStr, sayyadId)
            _showAddCommitmentModal.value = false
            showBanner("تعهد جدید «$title» ثبت شد.")
        }
    }

    fun toggleCommitmentSettled(commitment: CommitmentEntity) {
        viewModelScope.launch {
            repository.toggleCommitmentSettled(commitment)
            val msg = if (!commitment.isSettled) {
                "«${commitment.title}» پاس شد / تسویه شد ✓"
            } else {
                "وضعیت «${commitment.title}» به حالت فعال بازگشت."
            }
            showBanner(msg)
        }
    }

    fun dismissBanner() {
        _statusBannerMessage.value = null
    }

    private fun showBanner(message: String) {
        viewModelScope.launch {
            _statusBannerMessage.value = message
            delay(4000L)
            if (_statusBannerMessage.value == message) {
                _statusBannerMessage.value = null
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
