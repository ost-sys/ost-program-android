package com.ost.application.presentation.battery
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import com.ost.application.core.battery.BatteryDisplayMode
import com.ost.application.core.battery.BatteryInfoProvider
import com.ost.application.core.battery.BatteryInfoProvider.toChargingSourceText
import com.ost.application.core.battery.BatteryInfoProvider.toDisplayString
import com.ost.application.core.battery.BatteryInfoStrings
import com.ost.application.core.battery.BatteryStatusIcon
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.ost.application.core.settings.formatTemperatureFloat
import com.ost.application.settings.WearTemperatureUnitRepository
import com.ost.application.settings.WearTimingSettingsRepository
import java.util.Locale

data class WearBatteryInfoUiState(
    val levelText: String = "...",
    val iconResId: Int = R.drawable.ic_battery_full_24dp,
    val health: String = "...",
    val status: String = "...",
    val temperature: String = "...",
    val voltage: String = "...",
    val technology: String = "...",
    val capacity: String = "...",
    val isLoadingCapacity: Boolean = true,
    val displayMode: BatteryDisplayMode = BatteryDisplayMode.NORMAL,
    val cycleCount: String = "...",
    val current: String = "...",
    val chargeTimeRemaining: String? = null
)

class WearBatteryInfoViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(WearBatteryInfoUiState())
    val uiState: StateFlow<WearBatteryInfoUiState> = _uiState.asStateFlow()
    private val timingRepository = WearTimingSettingsRepository(application, viewModelScope)
    private val temperatureRepository = WearTemperatureUnitRepository(application, viewModelScope, timingRepository.syncState)
    private var batteryUpdateJob: Job? = null
    private var capacityJob: Job? = null
    private var currentPollJob: Job? = null
    private var latestInfo: com.ost.application.core.battery.BatteryInfo? = null
    private val strings = BatteryInfoStrings(
        charging = getString(R.string.charging),
        chargingAc = getString(R.string.charging_ac),
        chargingUsb = getString(R.string.charging_via_usb),
        chargingWireless = getString(R.string.wireless_charging),
        discharging = getString(R.string.discharging),
        batteryLevel = getString(R.string.battery_level),
        good = getString(R.string.good),
        overheat = getString(R.string.overheat),
        dead = getString(R.string.dead),
        overVoltage = getString(R.string.over_voltage),
        unspecifiedFailure = getString(R.string.fail),
        cold = getString(R.string.cold),
        unknown = getString(R.string.unknown),
        mv = getString(R.string.mv),
        mah = getString(R.string.mah),
        cycleCount = getString(R.string.cycle_count),
        notAvailable = getString(R.string.not_available)
    )
    init {
        loadBatteryCapacity()
        startObservingBatteryUpdates()
        startCurrentPolling()
        viewModelScope.launch {
            temperatureRepository.unit.collect { unit ->
                _uiState.update { state ->
                    val formattedTemp = latestInfo?.temperatureCelsius?.let {
                        formatTemperatureFloat(it, unit)
                    } ?: strings.unknown
                    state.copy(temperature = formattedTemp)
                }
            }
        }
    }
    private fun startCurrentPolling() {
        currentPollJob?.cancel()
        currentPollJob = viewModelScope.launch {
            while (true) {
                latestInfo?.let { info ->
                    val currentMa = BatteryInfoProvider.getCurrentNowMilliAmps(getApplication(), info.isCharging)
                    val currentText = if (currentMa != null) {
                        val watts = info.voltageVolts?.let { volts ->
                            String.format(Locale.getDefault(), " (%.1f W)", kotlin.math.abs(currentMa) / 1000f * volts)
                        } ?: ""
                        "${if (currentMa > 0) "+" else ""}$currentMa ${getString(R.string.ma)}$watts"
                    } else strings.notAvailable
                    val chargeTime = if (info.isCharging) {
                        BatteryInfoProvider.getChargeTimeRemainingMillis(getApplication())?.let { formatDuration(it) }
                    } else null
                    _uiState.update { it.copy(current = currentText, chargeTimeRemaining = chargeTime) }
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }
    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "$hours ${getString(R.string.h)} $minutes ${getString(R.string.min)}"
        else "$minutes ${getString(R.string.min)}"
    }
    private fun startObservingBatteryUpdates() {
        batteryUpdateJob?.cancel()
        batteryUpdateJob = BatteryInfoProvider.observeBatteryInfo(getApplication())
            .onEach { info ->
                latestInfo = info
                val currentUnit = temperatureRepository.unit.value
                _uiState.update {
                    it.copy(
                        levelText = if (info.levelPercent >= 0) {
                            if (info.isCharging) "${info.toChargingSourceText(strings)}: ${info.levelPercent}%"
                            else "${info.levelPercent}%"
                        } else "...",
                        iconResId = info.statusIcon.toResId(),
                        health = info.health.toDisplayString(strings),
                        status = info.toChargingSourceText(strings),
                        temperature = info.temperatureCelsius?.let { temp ->
                            formatTemperatureFloat(temp, currentUnit)
                        } ?: strings.unknown,
                        voltage = info.voltageVolts?.let { volts ->
                            String.format(Locale.getDefault(), "%.2fV", volts)
                        } ?: strings.unknown,
                        technology = info.technology ?: strings.unknown,
                        displayMode = info.displayMode,
                        cycleCount = info.cycleCount?.toString() ?: getString(R.string.unknown),
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    private fun loadBatteryCapacity() {
        capacityJob?.cancel()
        capacityJob = viewModelScope.launch {
            val capacity = BatteryInfoProvider.getBatteryCapacityMah(getApplication())
            _uiState.update {
                it.copy(
                    capacity = if (capacity != null && capacity > 0) {
                        "${Math.round(capacity)} ${strings.mah}"
                    } else strings.unknown,
                    isLoadingCapacity = false
                )
            }
        }
    }
    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
    override fun onCleared() {
        super.onCleared()
        batteryUpdateJob?.cancel()
        capacityJob?.cancel()
        currentPollJob?.cancel()
    }
}
private fun BatteryStatusIcon.toResId(): Int = when (this) {
    BatteryStatusIcon.CHARGER -> R.drawable.ic_charger_24dp
    BatteryStatusIcon.POWER_SAVE -> R.drawable.ic_energy_program_saving_24dp
    BatteryStatusIcon.FULL -> R.drawable.ic_battery_full_alt_24dp
    BatteryStatusIcon.HORIZ_075 -> R.drawable.ic_battery_horiz_075_24dp
    BatteryStatusIcon.HORIZ_050 -> R.drawable.ic_battery_horiz_050_24dp
    BatteryStatusIcon.LOW -> R.drawable.ic_battery_low_24dp
    BatteryStatusIcon.VERY_LOW -> R.drawable.ic_battery_very_low_24dp
    BatteryStatusIcon.HORIZ_000 -> R.drawable.ic_battery_horiz_000_24dp
}
