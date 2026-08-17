package com.ost.application.ui.screen.settings
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import com.ost.application.core.locale.LocaleHelper
import com.ost.application.core.locale.SupportedLocalesLoader
import com.ost.application.core.settings.TemperatureUnit
import com.ost.application.core.settings.sync.SettingsSyncClient
import com.ost.application.settings.GithubTokenRepository
import com.ost.application.settings.PhoneTemperatureUnitRepository
import com.ost.application.settings.PhoneTimingSettingsRepository
import com.ost.application.ui.activity.about.AboutActivity
import com.ost.application.util.DeveloperModeManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
data class SettingsUiState(
    val totalDuration: Int = 30,
    val noiseDuration: Int = 1,
    val blackWhiteNoiseDuration: Int = 1,
    val horizontalDuration: Int = 1,
    val verticalDuration: Int = 1,
    val githubToken: String = "",
    val isLanguageDialogVisible: Boolean = false,
    val supportedLocales: List<Locale> = emptyList(),
    val currentAppliedLocale: Locale = LocaleHelper.getCurrentLocale(),
    val selectedLanguageInDialog: Locale? = LocaleHelper.getCurrentLocale(),
    val temperatureUnit: TemperatureUnit = TemperatureUnit.DEFAULT,
    val isDeveloperModeEnabled: Boolean = false,
    val showLogcatDialog: Boolean = false,
    val showDeveloperOptionsDialog: Boolean = false
)
sealed class SettingsAction {
    data class StartActivity(val intent: Intent) : SettingsAction()
    data class ShowToast(val messageResId: Int) : SettingsAction()
}
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val timingRepository = PhoneTimingSettingsRepository(application, viewModelScope)
    private val temperatureRepository = PhoneTemperatureUnitRepository(application, viewModelScope)
    private val syncClient = SettingsSyncClient(application)
    private val tokenRepository = GithubTokenRepository(application, viewModelScope)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val _action = Channel<SettingsAction>(Channel.BUFFERED)
    val action = _action.receiveAsFlow()
    init {
        viewModelScope.launch {
            timingRepository.settings.collect { timing ->
                _uiState.update {
                    it.copy(
                        totalDuration = timing.totalDuration,
                        noiseDuration = timing.noiseDuration,
                        blackWhiteNoiseDuration = timing.blackWhiteNoiseDuration,
                        horizontalDuration = timing.horizontalDuration,
                        verticalDuration = timing.verticalDuration
                    )
                }
            }
        }
        viewModelScope.launch {
            temperatureRepository.unit.collect { unit ->
                _uiState.update { it.copy(temperatureUnit = unit) }
            }
        }
        _uiState.update { it.copy(githubToken = tokenRepository.token.value) }
        tokenRepository.pushPresence()
        loadSupportedLocales()
        refreshDeveloperMode()
    }
    fun refreshDeveloperMode() {
        val enabled = DeveloperModeManager.isDeveloperModeEnabled(getApplication())
        _uiState.update { it.copy(isDeveloperModeEnabled = enabled) }
    }
    fun showDeveloperOptionsDialog() {
        _uiState.update { it.copy(showDeveloperOptionsDialog = true) }
    }
    fun dismissDeveloperOptionsDialog() {
        _uiState.update { it.copy(showDeveloperOptionsDialog = false) }
    }
    fun showLogcatDialog() {
        _uiState.update { it.copy(showLogcatDialog = true, showDeveloperOptionsDialog = false) }
    }
    fun dismissLogcatDialog() {
        _uiState.update { it.copy(showLogcatDialog = false) }
    }
    fun updateTemperatureUnit(unit: TemperatureUnit) = temperatureRepository.updateUnit(unit)
    fun updateTotalDuration(v: Int) = timingRepository.updateTotalDuration(v)
    fun updateNoiseDuration(v: Int) = timingRepository.updateNoiseDuration(v)
    fun updateBlackWhiteNoiseDuration(v: Int) = timingRepository.updateBlackWhiteNoiseDuration(v)
    fun updateHorizontalDuration(v: Int) = timingRepository.updateHorizontalDuration(v)
    fun updateVerticalDuration(v: Int) = timingRepository.updateVerticalDuration(v)
    private fun loadSupportedLocales() {
        val locales = SupportedLocalesLoader.load(getApplication(), R.xml.locales_config)
        _uiState.update { it.copy(supportedLocales = locales) }
    }
    fun updateGithubToken(token: String) {
        _uiState.update { it.copy(githubToken = token) }
    }
    fun saveGithubToken() {
        tokenRepository.setToken(_uiState.value.githubToken)
    }
    fun clearGithubToken() {
        tokenRepository.clearToken()
        _uiState.update { it.copy(githubToken = "") }
    }
    fun onAboutAppClicked() {
        val intent = Intent(getApplication(), AboutActivity::class.java)
        viewModelScope.launch {
            _action.send(SettingsAction.StartActivity(intent))
        }
    }
    fun onLanguagePreferenceClick() {
        _uiState.update {
            it.copy(
                isLanguageDialogVisible = true,
                selectedLanguageInDialog = it.currentAppliedLocale
            )
        }
    }
    fun onLanguageSelectedInDialog(locale: Locale?) {
        _uiState.update { it.copy(selectedLanguageInDialog = locale) }
    }
    fun onLanguageDialogDismiss() {
        _uiState.update { it.copy(isLanguageDialogVisible = false) }
    }
    fun onLanguageDialogConfirm() {
        val selectedLocale = _uiState.value.selectedLanguageInDialog
        LocaleHelper.setLocale(selectedLocale)
        _uiState.update {
            it.copy(
                isLanguageDialogVisible = false,
                currentAppliedLocale = selectedLocale ?: LocaleHelper.getSystemLocale()
            )
        }
        viewModelScope.launch {
            syncClient.pushLanguageTag(LocaleHelper.getCurrentLanguageTag())
        }
    }
}
