package com.ost.application.settings
import android.content.Context
import android.content.SharedPreferences
import com.ost.application.core.settings.TimingPrefKeys
import com.ost.application.core.settings.TimingSettings
import com.ost.application.core.settings.TimingSettingsRepository
import com.ost.application.core.settings.sync.SettingsSyncClient
import com.ost.application.core.settings.sync.WearSyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
private const val WEAR_PREFS_NAME = "wear_settings_prefs"
private const val KEY_SYNC_ENABLED = "sync_with_phone_enabled"
class WearTimingSettingsRepository(
    context: Context,
    private val scope: CoroutineScope
) : TimingSettingsRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences(WEAR_PREFS_NAME, Context.MODE_PRIVATE)
    private val syncClient = SettingsSyncClient(context)
    private val _syncState = MutableStateFlow<WearSyncState>(WearSyncState.Unavailable)
    val syncState: StateFlow<WearSyncState> = _syncState.asStateFlow()
    private val _settings = MutableStateFlow(loadLocalSettings())
    override val settings: StateFlow<TimingSettings> = _settings.asStateFlow()
    init {
        scope.launch { refreshAvailability() }
        scope.launch {
            syncClient.observeTimingSettings().collect { synced ->
                if (prefs.getBoolean(KEY_SYNC_ENABLED, false)) {
                    _syncState.value = WearSyncState.Enabled
                    _settings.value = synced
                }
            }
        }
    }
    private fun loadLocalSettings() = TimingSettings(
        totalDuration = prefs.getInt(TimingPrefKeys.TOTAL_DURATION, TimingSettings.Defaults.TOTAL_DURATION),
        noiseDuration = prefs.getInt(TimingPrefKeys.NOISE_DURATION, TimingSettings.Defaults.NOISE_DURATION),
        blackWhiteNoiseDuration = prefs.getInt(TimingPrefKeys.BLACK_WHITE_NOISE_DURATION, TimingSettings.Defaults.BW_NOISE_DURATION),
        horizontalDuration = prefs.getInt(TimingPrefKeys.HORIZONTAL_DURATION, TimingSettings.Defaults.HORIZONTAL_DURATION),
        verticalDuration = prefs.getInt(TimingPrefKeys.VERTICAL_DURATION, TimingSettings.Defaults.VERTICAL_DURATION)
    )
    private suspend fun refreshAvailability() {
        val phoneSettings = if (syncClient.isCounterpartNodeConnected()) {
            syncClient.getLastSyncedTimingSettings()
        } else null
        val available = phoneSettings != null
        val wantsSync = prefs.getBoolean(KEY_SYNC_ENABLED, false)
        when {
            available && wantsSync -> enableSync(phoneSettings)
            else -> {
                _syncState.value = if (available) WearSyncState.Disabled else WearSyncState.Unavailable
                _settings.value = loadLocalSettings()
            }
        }
    }
    private fun enableSync(initialValue: TimingSettings?) {
        _syncState.value = WearSyncState.Enabled
        if (initialValue != null) _settings.value = initialValue
    }
    fun setSyncEnabled(enabled: Boolean) {
        if (enabled && _syncState.value == WearSyncState.Unavailable) return
        prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply()
        if (enabled) {
            scope.launch {
                val phoneSettings = syncClient.getLastSyncedTimingSettings()
                if (phoneSettings != null) enableSync(phoneSettings)
            }
        } else {
            _syncState.value = WearSyncState.Disabled
            _settings.value = loadLocalSettings()
        }
    }
    private fun updateLocal(update: (TimingSettings) -> TimingSettings) {
        if (_syncState.value == WearSyncState.Enabled) return
        val newSettings = update(_settings.value)
        _settings.value = newSettings
        prefs.edit()
            .putInt(TimingPrefKeys.TOTAL_DURATION, newSettings.totalDuration)
            .putInt(TimingPrefKeys.NOISE_DURATION, newSettings.noiseDuration)
            .putInt(TimingPrefKeys.BLACK_WHITE_NOISE_DURATION, newSettings.blackWhiteNoiseDuration)
            .putInt(TimingPrefKeys.HORIZONTAL_DURATION, newSettings.horizontalDuration)
            .putInt(TimingPrefKeys.VERTICAL_DURATION, newSettings.verticalDuration)
            .apply()
    }
    override fun updateTotalDuration(value: Int) = updateLocal { it.copy(totalDuration = value) }
    override fun updateNoiseDuration(value: Int) = updateLocal { it.copy(noiseDuration = value) }
    override fun updateBlackWhiteNoiseDuration(value: Int) = updateLocal { it.copy(blackWhiteNoiseDuration = value) }
    override fun updateHorizontalDuration(value: Int) = updateLocal { it.copy(horizontalDuration = value) }
    override fun updateVerticalDuration(value: Int) = updateLocal { it.copy(verticalDuration = value) }
}
