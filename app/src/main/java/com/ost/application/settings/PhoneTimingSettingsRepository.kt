package com.ost.application.settings
import android.content.Context
import android.content.SharedPreferences
import com.ost.application.core.settings.TimingPrefKeys
import com.ost.application.core.settings.TimingSettings
import com.ost.application.core.settings.TimingSettingsRepository
import com.ost.application.core.settings.sync.SettingsSyncClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.content.edit
class PhoneTimingSettingsRepository(
    context: Context,
    private val scope: CoroutineScope
) : TimingSettingsRepository {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    private val syncClient = SettingsSyncClient(context)
    private val _settings = MutableStateFlow(loadSettings())
    override val settings: StateFlow<TimingSettings> = _settings.asStateFlow()
    init {
        scope.launch { syncClient.pushTimingSettings(_settings.value) }
    }
    private fun loadSettings() = TimingSettings(
        totalDuration = prefs.getInt(TimingPrefKeys.TOTAL_DURATION, TimingSettings.Defaults.TOTAL_DURATION),
        noiseDuration = prefs.getInt(TimingPrefKeys.NOISE_DURATION, TimingSettings.Defaults.NOISE_DURATION),
        blackWhiteNoiseDuration = prefs.getInt(TimingPrefKeys.BLACK_WHITE_NOISE_DURATION, TimingSettings.Defaults.BW_NOISE_DURATION),
        horizontalDuration = prefs.getInt(TimingPrefKeys.HORIZONTAL_DURATION, TimingSettings.Defaults.HORIZONTAL_DURATION),
        verticalDuration = prefs.getInt(TimingPrefKeys.VERTICAL_DURATION, TimingSettings.Defaults.VERTICAL_DURATION)
    )
    private fun persistAndSync(newSettings: TimingSettings) {
        _settings.value = newSettings
        prefs.edit {
            putInt(TimingPrefKeys.TOTAL_DURATION, newSettings.totalDuration)
                .putInt(TimingPrefKeys.NOISE_DURATION, newSettings.noiseDuration)
                .putInt(
                    TimingPrefKeys.BLACK_WHITE_NOISE_DURATION,
                    newSettings.blackWhiteNoiseDuration
                )
                .putInt(TimingPrefKeys.HORIZONTAL_DURATION, newSettings.horizontalDuration)
                .putInt(TimingPrefKeys.VERTICAL_DURATION, newSettings.verticalDuration)
        }
        scope.launch { syncClient.pushTimingSettings(newSettings) }
    }
    override fun updateTotalDuration(value: Int) = persistAndSync(_settings.value.copy(totalDuration = value))
    override fun updateNoiseDuration(value: Int) = persistAndSync(_settings.value.copy(noiseDuration = value))
    override fun updateBlackWhiteNoiseDuration(value: Int) = persistAndSync(_settings.value.copy(blackWhiteNoiseDuration = value))
    override fun updateHorizontalDuration(value: Int) = persistAndSync(_settings.value.copy(horizontalDuration = value))
    override fun updateVerticalDuration(value: Int) = persistAndSync(_settings.value.copy(verticalDuration = value))
}
