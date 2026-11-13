package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CommitmentEntity
import com.example.data.local.OrosiDatabase
import com.example.data.local.TransactionEntity
import com.example.data.repository.OrosiFinanceRepository
import com.example.domain.BankSmsParserEngine
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
    DASHBOARD("dashboard", "امروز من"),
    COMMITMENTS("commitments", "قسط و چک"),
    INFLATION_SHIELD("inflation_shield", "قلک طلا")
}

enum class CommitmentTab(val title: String) {
    SAYYAD("چک صیادی"),
    INSTALLMENTS("اقساط وام"),
    DONG("دُنگ")
}

data class VoiceUiState(
    val isRecordingSimulated: Boolean = false,
    val recognizedText: String = "پانصد هزار تومان خرید میوه از اسنپ‌مارکت با کارت بلو که ۲۰۰ تومنش دنگ علی بود",
    val storeName: String = "اسنپ‌مارکت",
    val purposeCategory: String = "خرید میوه و تره‌بار",
    val cardLabel: String = "کارت بلو ۶۲۱۹",
    val totalAmountToman: Long = 500_000L,
    val dongAmountToman: Long = 200_000L,
    val dongFriendName: String = "علی",
    val userShareToman: Long = 300_000L,
    val targetUntaggedTransaction: TransactionEntity? = null
)

data class OrosiUiState(
    val currentScreen: OrosiScreen = OrosiScreen.DASHBOARD,
    val selectedCommitmentTab: CommitmentTab = CommitmentTab.SAYYAD,
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val transactions: List<TransactionEntity> = emptyList(),
    val commitments: List<CommitmentEntity> = emptyList(),
    val voiceState: VoiceUiState = VoiceUiState(),
    val showVoiceOverlaySheet: Boolean = false,
    val showHeroMathSheet: Boolean = false,
    val showSmsSimulatorModal: Boolean = false,
    val showPaydayBackupModal: Boolean = false,
    val showAddCommitmentModal: Boolean = false,
    val showConvertGoldModal: Boolean = false,
    val backupJsonText: String = "",
    val statusBannerMessage: String? = null
)

class OrosiViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = OrosiDatabase.getInstance(application).orosiDao()
    private val repository = OrosiFinanceRepository(dao)

    private val _currentScreen = MutableStateFlow(OrosiScreen.DASHBOARD)
    private val _selectedCommitmentTab = MutableStateFlow(CommitmentTab.SAYYAD)
    private val _voiceState = MutableStateFlow(VoiceUiState())
    private val _showVoiceOverlaySheet = MutableStateFlow(false)
    private val _showHeroMathSheet = MutableStateFlow(false)
    private val _showSmsSimulatorModal = MutableStateFlow(false)
    private val _showPaydayBackupModal = MutableStateFlow(false)
    private val _showAddCommitmentModal = MutableStateFlow(false)
    private val _showConvertGoldModal = MutableStateFlow(false)
    private val _backupJsonText = MutableStateFlow("")
    private val _statusBannerMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            parseAndUpdateVoiceState(_voiceState.value.recognizedText, null)
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
            _showAddCommitmentModal,
            _showConvertGoldModal,
            _backupJsonText,
            _statusBannerMessage
        ) { addComm, goldModal, backup, banner ->
            Tuple4(addComm, goldModal, backup, banner)
        }
    ) { state, extra ->
        state.copy(
            showAddCommitmentModal = extra.first,
            showConvertGoldModal = extra.second,
            backupJsonText = extra.third,
            statusBannerMessage = extra.fourth
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

    fun setShowHeroMathSheet(show: Boolean) {
        _showHeroMathSheet.value = show
    }

    fun openVoiceOverlayNew() {
        val defaultSpeech = "پانصد هزار تومان خرید میوه از اسنپ‌مارکت با کارت بلو که ۲۰۰ تومنش دنگ علی بود"
        parseAndUpdateVoiceState(defaultSpeech, null)
        _showVoiceOverlaySheet.value = true
    }

    fun openVoiceForUntaggedTransaction(tx: TransactionEntity) {
        val suggestedSpeech = "${PersianNumberUtils.formatToman(tx.totalAmountToman)} تومان خرید میوه از اسنپ‌مارکت با ${tx.cardInfo} که ۱۰۰ تومنش دنگ علی بود"
        val parsed = PersianVoiceSlangParserEngine.parseSpokenText(
            suggestedSpeech,
            fallbackTotalToman = tx.totalAmountToman
        )
        _voiceState.value = VoiceUiState(
            isRecordingSimulated = false,
            recognizedText = suggestedSpeech,
            storeName = parsed.storeName,
            purposeCategory = parsed.purposeCategory,
            cardLabel = tx.cardInfo,
            totalAmountToman = tx.totalAmountToman,
            dongAmountToman = parsed.dongAmountToman,
            dongFriendName = parsed.dongFriendName,
            userShareToman = parsed.userShareToman,
            targetUntaggedTransaction = tx
        )
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
        val parsed = PersianVoiceSlangParserEngine.parseSpokenText(
            speech,
            fallbackTotalToman = targetTx?.totalAmountToman
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

    fun triggerVoiceSimulation(samplePhrase: String? = null) {
        viewModelScope.launch {
            _voiceState.value = _voiceState.value.copy(isRecordingSimulated = true)
            delay(750L)
            val phrases = listOf(
                "پانصد هزار تومان خرید میوه از اسنپ‌مارکت با کارت بلو که ۲۰۰ تومنش دنگ علی بود",
                "هشتصد تومن شام رستوران نایب با کارت ملت که نصفش دنگ سارا بود",
                "دویست و پنجاه تومن قهوه و کیک از کافه لمیز با کارت سامان",
                "یک میلیون و دویست خرید لباس از دیجی‌کالا با کارت ملی که ۴۰۰ تومنش دنگ رضا بود"
            )
            val nextPhrase = samplePhrase ?: phrases.firstOrNull { it != _voiceState.value.recognizedText } ?: phrases.first()
            parseAndUpdateVoiceState(nextPhrase, _voiceState.value.targetUntaggedTransaction)
            _voiceState.value = _voiceState.value.copy(isRecordingSimulated = false)
        }
    }

    fun confirmVoiceEntry() {
        viewModelScope.launch {
            val v = _voiceState.value
            val parsed = ParsedVoiceExpense(
                recognizedSpeech = v.recognizedText,
                totalAmountToman = v.totalAmountToman,
                userShareToman = v.userShareToman,
                dongAmountToman = v.dongAmountToman,
                dongFriendName = v.dongFriendName,
                storeName = v.storeName,
                purposeCategory = v.purposeCategory,
                cardLabel = v.cardLabel
            )
            repository.confirmVoiceExpense(parsed, v.targetUntaggedTransaction?.id)
            val msg = if (v.dongAmountToman > 0L && v.dongFriendName.isNotBlank()) {
                "ثبت شد! فقط سهم شما (${PersianNumberUtils.formatToman(v.userShareToman, true)}) کسر شد و دنگ ${v.dongFriendName} کنار گذاشته شد."
            } else {
                "«${v.storeName}» به مبلغ ${PersianNumberUtils.formatToman(v.userShareToman, true)} ثبت شد."
            }
            _showVoiceOverlaySheet.value = false
            _voiceState.value = _voiceState.value.copy(targetUntaggedTransaction = null)
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
            showBanner(result.summaryMessage)
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
            showBanner("تنظیمات ماه حقوقی و نرخ طلا ذخیره شد.")
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
                "«${commitment.title}» تسویه شد."
            } else {
                "وضعیت «${commitment.title}» به حالت فعال بازگشت."
            }
            showBanner(msg)
        }
    }

    fun setShowConvertGoldModal(show: Boolean) {
        _showConvertGoldModal.value = show
    }

    fun convertSurplusToGold(amountToman: Long) {
        viewModelScope.launch {
            repository.convertSurplusToGold(amountToman)
            _showConvertGoldModal.value = false
            showBanner("مبلغ ${PersianNumberUtils.formatToman(amountToman, true)} به قلک طلای ۱۸ عیار افزوده شد.")
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
