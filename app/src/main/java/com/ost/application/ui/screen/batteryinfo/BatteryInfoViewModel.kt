package com.ost.application.ui.screen.batteryinfo
import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import com.ost.application.core.battery.BatteryDisplayMode
import com.ost.application.core.battery.BatteryHealth
import com.ost.application.core.battery.BatteryInfo
import com.ost.application.core.battery.BatteryInfoProvider
import com.ost.application.core.battery.BatteryInfoProvider.toChargingSourceText
import com.ost.application.core.battery.BatteryInfoProvider.toDisplayString
import com.ost.application.core.battery.BatteryInfoStrings
import com.ost.application.core.battery.BatteryStatusIcon
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import com.ost.application.core.settings.TemperatureUnit
import com.ost.application.core.settings.formatTemperatureFloat
import com.ost.application.settings.PhoneTemperatureUnitRepository

private const val GRAPH_HISTORY_SIZE = 30
@Stable
data class BatteryInfoUiState(
    val levelPercent: Int = -1,
    val iconResId: Int = R.drawable.ic_battery_full_24dp,
    val health: String = "...",
    val healthStatus: BatteryHealth = BatteryHealth.UNKNOWN,
    val status: String = "...",
    val temperature: String = "...",
    val temperatureUnit: TemperatureUnit = TemperatureUnit.DEFAULT,
    val voltage: String = "...",
    val technology: String = "...",
    val capacity: String = "...",
    val isLoadingCapacity: Boolean = true,
    val displayMode: BatteryDisplayMode = BatteryDisplayMode.NORMAL,
    val cycleCount: String = "...",
    val current: String = "...",
    val chargeTimeRemaining: String? = null,
    val temperatureHistory: List<BatterySample> = emptyList(),
    val voltageHistory: List<BatterySample> = emptyList(),
    val temperatureMin: Float? = null,
    val temperatureMax: Float? = null,
    val voltageMin: Float? = null,
    val voltageMax: Float? = null
)
class BatteryInfoViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(BatteryInfoUiState())
    val uiState: StateFlow<BatteryInfoUiState> = _uiState.asStateFlow()
    private val temperatureRepository = PhoneTemperatureUnitRepository(application, viewModelScope)
    private var batteryUpdateJob: Job? = null
    private var capacityJob: Job? = null
    private var samplingJob: Job? = null
    private val samplingController = BatterySamplingController(application)
    private var latestBatteryInfo: BatteryInfo? = null
    private val temperatureHistory = ArrayDeque<BatterySample>(GRAPH_HISTORY_SIZE)
    private val voltageHistory = ArrayDeque<BatterySample>(GRAPH_HISTORY_SIZE)
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
        startSampling()
        viewModelScope.launch {
            temperatureRepository.unit.collect { unit ->
                _uiState.update { state ->
                    val formattedTemp = latestBatteryInfo?.temperatureCelsius?.let {
                        formatTemperatureFloat(it, unit)
                    } ?: strings.unknown
                    state.copy(temperatureUnit = unit, temperature = formattedTemp)
                }
            }
        }
    }
    fun setScreenVisible(visible: Boolean) {
        samplingController.setScreenVisible(visible)
    }
    private fun startObservingBatteryUpdates() {
        batteryUpdateJob?.cancel()
        batteryUpdateJob = BatteryInfoProvider.observeBatteryInfo(getApplication())
            .onEach { info ->
                latestBatteryInfo = info
                val iconRes = info.statusIcon.toResId()
                val currentUnit = temperatureRepository.unit.value
                _uiState.update {
                    it.copy(
                        levelPercent = info.levelPercent,
                        iconResId = iconRes,
                        health = info.health.toDisplayString(strings),
                        healthStatus = info.health,
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
    private fun startSampling() {
        samplingJob?.cancel()
        samplingJob = viewModelScope.launch {
            samplingController.samplingContext
                .distinctUntilChanged()
                .collect { context ->
                    while (isActive) {
                        recordSample()
                        kotlinx.coroutines.delay(context.intervalMs)
                    }
                }
        }
    }
    private fun recordSample() {
        val info = latestBatteryInfo ?: return
        val now = System.currentTimeMillis()
        updateCurrentAndChargeTime(info)
        info.temperatureCelsius?.let { temp ->
            if (temperatureHistory.size >= GRAPH_HISTORY_SIZE) temperatureHistory.removeFirst()
            temperatureHistory.addLast(BatterySample(now, temp, info.voltageVolts ?: 0f))
        }
        info.voltageVolts?.let { volt ->
            if (voltageHistory.size >= GRAPH_HISTORY_SIZE) voltageHistory.removeFirst()
            voltageHistory.addLast(BatterySample(now, info.temperatureCelsius ?: 0f, volt))
        }
        val tempValues = temperatureHistory.map { it.temperatureCelsius }
        val voltValues = voltageHistory.map { it.voltageVolts }
        _uiState.update {
            it.copy(
                temperatureHistory = temperatureHistory.toList(),
                voltageHistory = voltageHistory.toList(),
                temperatureMin = tempValues.minOrNull(),
                temperatureMax = tempValues.maxOrNull(),
                voltageMin = voltValues.minOrNull(),
                voltageMax = voltValues.maxOrNull()
            )
        }
    }
    private fun updateCurrentAndChargeTime(info: BatteryInfo) {
        val currentMa = BatteryInfoProvider.getCurrentNowMilliAmps(getApplication(), info.isCharging)
        val currentText = if (currentMa != null) {
            val watts = info.voltageVolts?.let { volts ->
                String.format(Locale.getDefault(), " (%.1f W)", kotlin.math.abs(currentMa) / 1000f * volts)
            } ?: ""
            val sign = if (currentMa > 0) "+" else ""
            "$sign$currentMa ${getString(R.string.ma)}$watts"
        } else {
            strings.notAvailable
        }
        val chargeTimeText = if (info.isCharging) {
            BatteryInfoProvider.getChargeTimeRemainingMillis(getApplication())?.let { formatDuration(it) }
        } else {
            null
        }
        _uiState.update { it.copy(current = currentText, chargeTimeRemaining = chargeTimeText) }
    }
    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "$hours ${getString(R.string.h)} $minutes ${getString(R.string.min)}"
        else "$minutes ${getString(R.string.min)}"
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
        samplingJob?.cancel()
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
