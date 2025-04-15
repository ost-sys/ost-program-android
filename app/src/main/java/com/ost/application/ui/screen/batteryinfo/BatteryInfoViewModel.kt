package com.ost.application.ui.screen.batteryinfo

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

@Stable
data class BatteryInfoUiState(
    val levelText: String = "...",
    val iconResId: Int = R.drawable.ic_battery_full_24dp,
    val health: String = "...",
    val status: String = "...",
    val temperature: String = "...",
    val voltage: String = "...",
    val technology: String = "...",
    val capacity: String = "...",
    val isLoadingCapacity: Boolean = true
)

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.batteryUpdatesFlow(): Flow<Intent> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent)
        }
    }
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    registerReceiver(receiver, filter)

    awaitClose { unregisterReceiver(receiver) }
}


class BatteryInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BatteryInfoUiState())
    val uiState: StateFlow<BatteryInfoUiState> = _uiState.asStateFlow()

    private var batteryUpdateJob: Job? = null
    private var capacityJob: Job? = null

    init {
        loadBatteryCapacity()
        startObservingBatteryUpdates()
    }

    private fun startObservingBatteryUpdates() {
        batteryUpdateJob?.cancel()
        batteryUpdateJob = getApplication<Application>().batteryUpdatesFlow()
            .onEach { intent -> processBatteryIntent(intent) }
            .launchIn(viewModelScope)
    }

    private fun loadBatteryCapacity() {
        capacityJob?.cancel()
        capacityJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCapacity = true) }
            val capacity = getBatteryCapacity(getApplication())
            _uiState.update {
                it.copy(
                    capacity = if (capacity > 0) "${capacity.roundToInt()} ${getString(R.string.mah)}" else getString(R.string.unknown),
                    isLoadingCapacity = false
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun processBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).roundToInt() else -1

        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000f
        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: getString(R.string.unknown)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)

        val healthString = getHealthString(health)
        val statusPair = getStatusStringAndIcon(status, plugged, batteryPct)
        val statusString = statusPair.first
        val iconRes = statusPair.second
        val levelString = getLevelString(status, plugged, batteryPct)

        _uiState.update {
            it.copy(
                levelText = levelString,
                iconResId = iconRes,
                health = healthString,
                status = statusString,
                temperature = if (temp >= 0) String.format(Locale.getDefault(), "%.1f°C", temp) else getString(R.string.unknown),
                voltage = if (voltage >= 0) String.format(Locale.getDefault(), "%.2fV", voltage) else getString(R.string.unknown),
                technology = technology
            )
        }
    }

    private fun getString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }

    private fun getHealthString(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> getString(R.string.good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> getString(R.string.overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> getString(R.string.dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> getString(R.string.over_voltage)
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> getString(R.string.fail)
            BatteryManager.BATTERY_HEALTH_COLD -> getString(R.string.cold)
            else -> getString(R.string.unknown)
        }
    }

    private fun getStatusStringAndIcon(status: Int, plugged: Int, level: Int): Pair<String, Int> {
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val statusString: String
        var iconRes = R.drawable.ic_battery_unknown_24dp

        if (isCharging) {
            statusString = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> getString(R.string.charging)
                BatteryManager.BATTERY_PLUGGED_USB -> getString(R.string.charging_via_usb)
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> getString(R.string.wireless_charging)
                else -> getString(R.string.charging)
            }
            iconRes = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> R.drawable.ic_charger_24dp
                BatteryManager.BATTERY_PLUGGED_USB -> R.drawable.ic_usb_24dp
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> R.drawable.ic_charging_station_24dp
                else -> R.drawable.ic_charger_24dp
            }
            if (status == BatteryManager.BATTERY_STATUS_FULL) {
            }
        } else {
            statusString = getString(R.string.discharging)
            iconRes = when {
                (level >= 90 && level <= 100) -> R.drawable.ic_battery_full_alt_24dp
                (level >= 75 && level <= 89) -> R.drawable.ic_battery_horiz_075_24dp
                (level >= 50 && level <= 74) -> R.drawable.ic_battery_horiz_050_24dp
                (level >= 25 && level <= 49) -> R.drawable.ic_battery_low_24dp
                (level >= 10 && level <= 24) -> R.drawable.ic_battery_very_low_24dp
                (level >= 0 && level <= 9) -> R.drawable.ic_battery_horiz_000_24dp
                else -> R.drawable.ic_battery_unknown_24dp
            }
        }
        return statusString to iconRes
    }

    private fun getLevelString(status: Int, plugged: Int, level: Int): String {
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return if (isCharging) {
            val chargingType = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> getString(R.string.charging)
                BatteryManager.BATTERY_PLUGGED_USB -> getString(R.string.charging_via_usb)
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> getString(R.string.wireless_charging)
                else -> getString(R.string.charging)
            }
            if (level >= 0) "$chargingType: $level%" else chargingType
        } else {
            if (level >= 0) "$level%" else "..."
        }
    }

    @SuppressLint("PrivateApi")
    private suspend fun getBatteryCapacity(context: Context): Double = withContext(Dispatchers.IO) {
        var batteryCapacity = 0.0
        try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val powerProfile = powerProfileClass.getConstructor(Context::class.java).newInstance(context)
            batteryCapacity = powerProfileClass.getMethod("getBatteryCapacity").invoke(powerProfile) as Double
        } catch (e: Exception) {
            Log.e("BatteryInfoVM", "Failed to get battery capacity via reflection", e)
        }
        batteryCapacity
    }

    override fun onCleared() {
        super.onCleared()
        batteryUpdateJob?.cancel()
        capacityJob?.cancel()
    }
}