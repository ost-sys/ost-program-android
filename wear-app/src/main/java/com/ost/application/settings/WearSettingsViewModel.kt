package com.ost.application.settings
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import com.ost.application.core.locale.LocaleHelper
import com.ost.application.core.locale.SupportedLocalesLoader
import com.ost.application.core.settings.TemperatureUnit
import com.ost.application.core.settings.TimingSettings
import com.ost.application.core.settings.sync.SettingsSyncClient
import com.ost.application.core.settings.sync.WearSyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
data class WearSettingsUiState(
    val timing: TimingSettings = TimingSettings.DEFAULT,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.DEFAULT,
    val syncState: WearSyncState = WearSyncState.Unavailable,
    val currentLocale: Locale = LocaleHelper.getCurrentLocale(),
    val followsSystemLocale: Boolean = LocaleHelper.isFollowingSystem(),
    val githubTokenFound: Boolean = false,
    val phoneConnected: Boolean = false
)
class WearSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WearTimingSettingsRepository(application, viewModelScope)
    private val temperatureRepository =
        WearTemperatureUnitRepository(application, viewModelScope, repository.syncState)
    private val syncClient = SettingsSyncClient(application)
    private val _currentLocale = MutableStateFlow(LocaleHelper.getCurrentLocale())
    private val _followsSystem = MutableStateFlow(LocaleHelper.isFollowingSystem())
    private val _githubTokenFound = MutableStateFlow(false)
    private val _phoneConnected = MutableStateFlow(false)
    private val supportedLanguageTags: Set<String> =
        SupportedLocalesLoader.load(application, R.xml.locales_config)
            .map { it.toLanguageTag() }
            .toSet()
    val uiState = combine(
        repository.settings,
        temperatureRepository.unit,
        repository.syncState,
        _currentLocale,
        _followsSystem,
        _githubTokenFound,
        _phoneConnected
    ) { values ->
        WearSettingsUiState(
            timing = values[0] as TimingSettings,
            temperatureUnit = values[1] as TemperatureUnit,
            syncState = values[2] as WearSyncState,
            currentLocale = values[3] as Locale,
            followsSystemLocale = values[4] as Boolean,
            githubTokenFound = values[5] as Boolean,
            phoneConnected = values[6] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearSettingsUiState())
    init {
        viewModelScope.launch { applyInitialLanguage() }
        viewModelScope.launch {
            syncClient.observeLanguageTag().collect { tag -> applyLanguageTag(tag) }
        }
        viewModelScope.launch { refreshPhoneConnectionState() }
    }
    private suspend fun refreshPhoneConnectionState() {
        _phoneConnected.value = syncClient.isCounterpartNodeConnected()
        if (_phoneConnected.value) {
            _githubTokenFound.value = syncClient.getLastSyncedGithubTokenPresence()
        }
    }
    private suspend fun applyInitialLanguage() {
        val tag = syncClient.getLastSyncedLanguageTag()
        applyLanguageTag(tag)
    }
    private fun applyLanguageTag(tag: String?) {
        val localeToApply: Locale? = when {
            tag.isNullOrEmpty() -> null
            tag !in supportedLanguageTags -> null
            else -> Locale.forLanguageTag(tag)
        }
        LocaleHelper.setLocale(localeToApply)
        _currentLocale.value = localeToApply ?: LocaleHelper.getSystemLocale()
        _followsSystem.value = localeToApply == null
    }
    fun onSyncToggle(enabled: Boolean) = repository.setSyncEnabled(enabled)
    fun onTotalDurationChange(v: Int) = repository.updateTotalDuration(v)
    fun onNoiseDurationChange(v: Int) = repository.updateNoiseDuration(v)
    fun onBWNoiseDurationChange(v: Int) = repository.updateBlackWhiteNoiseDuration(v)
    fun onHorizontalDurationChange(v: Int) = repository.updateHorizontalDuration(v)
    fun onVerticalDurationChange(v: Int) = repository.updateVerticalDuration(v)
    fun onTemperatureUnitChange(unit: TemperatureUnit) = temperatureRepository.updateUnit(unit)
    fun requestOpenSettingsOnPhone() {
        viewModelScope.launch { syncClient.requestOpenSettingsOnPhone() }
    }
}
