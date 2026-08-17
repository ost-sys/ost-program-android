package com.ost.application.core.battery
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
object BatteryInfoProvider {
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun observeBatteryInfo(context: Context): Flow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                trySend(parseBatteryIntent(context, intent))
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        context.registerReceiver(null, filter)?.let { sticky ->
            trySend(parseBatteryIntent(context, sticky))
        }
        awaitClose { context.unregisterReceiver(receiver) }
    }
    fun parseBatteryIntent(context: Context, intent: Intent): BatteryInfo {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val levelPercent = if (level != -1 && scale != -1) {
            ((level * 100) / scale.toFloat()).let { kotlin.math.round(it).toInt() }
        } else -1
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val chargingSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargingSource.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargingSource.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingSource.WIRELESS
            else -> ChargingSource.NONE
        }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSaveMode = powerManager?.isPowerSaveMode == true
        val displayMode = when {
            isCharging -> BatteryDisplayMode.CHARGING
            isPowerSaveMode -> BatteryDisplayMode.POWER_SAVE
            else -> BatteryDisplayMode.NORMAL
        }
        val statusIcon = when {
            isCharging -> BatteryStatusIcon.CHARGER
            isPowerSaveMode -> BatteryStatusIcon.POWER_SAVE
            levelPercent >= 90 -> BatteryStatusIcon.FULL
            levelPercent >= 75 -> BatteryStatusIcon.HORIZ_075
            levelPercent >= 50 -> BatteryStatusIcon.HORIZ_050
            levelPercent >= 25 -> BatteryStatusIcon.LOW
            levelPercent >= 10 -> BatteryStatusIcon.VERY_LOW
            else -> BatteryStatusIcon.HORIZ_000
        }
        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNSPECIFIED_FAILURE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            else -> BatteryHealth.UNKNOWN
        }
        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        val cycleCount = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1).takeIf { it >= 0 }
        } else null
        return BatteryInfo(
            levelPercent = levelPercent,
            isCharging = isCharging,
            chargingSource = chargingSource,
            displayMode = displayMode,
            statusIcon = statusIcon,
            health = health,
            temperatureCelsius = if (temp >= 0) temp / 10f else null,
            voltageVolts = if (voltage >= 0) voltage / 1000f else null,
            technology = technology,
            cycleCount = cycleCount
        )
    }

    fun getCurrentNowMilliAmps(context: Context, isCharging: Boolean): Long? {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            ?: return null
        val raw = batteryManager.getLongProperty(android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        if (raw == Long.MIN_VALUE || raw == 0L) return raw.takeIf { it == 0L }
        val milliAmps = if (kotlin.math.abs(raw) > 100_000) raw / 1000 else raw
        return if (isCharging) kotlin.math.abs(milliAmps) else -kotlin.math.abs(milliAmps)
    }

    fun getChargeTimeRemainingMillis(context: Context): Long? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) return null
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            ?: return null
        return try {
            batteryManager.computeChargeTimeRemaining().takeIf { it > 0 }
        } catch (e: Exception) {
            null
        }
    }
    @SuppressLint("PrivateApi")
    suspend fun getBatteryCapacityMah(context: Context): Double? = withContext(Dispatchers.IO) {
        try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val powerProfile = powerProfileClass.getConstructor(Context::class.java).newInstance(context)
            powerProfileClass.getMethod("getBatteryCapacity").invoke(powerProfile) as Double
        } catch (e: Exception) {
            Log.e("BatteryInfoProvider", "Failed to get battery capacity via reflection", e)
            null
        }
    }
    fun BatteryInfo.toStatusText(strings: BatteryInfoStrings): String {
        if (levelPercent < 0) return "..."
        return if (isCharging) {
            val source = when (chargingSource) {
                ChargingSource.AC -> strings.chargingAc
                ChargingSource.USB -> strings.chargingUsb
                ChargingSource.WIRELESS -> strings.chargingWireless
                ChargingSource.NONE -> strings.charging
            }
            "$source: $levelPercent%"
        } else {
            "$levelPercent%"
        }
    }
    fun BatteryHealth.toDisplayString(strings: BatteryInfoStrings): String = when (this) {
        BatteryHealth.GOOD -> strings.good
        BatteryHealth.OVERHEAT -> strings.overheat
        BatteryHealth.DEAD -> strings.dead
        BatteryHealth.OVER_VOLTAGE -> strings.overVoltage
        BatteryHealth.UNSPECIFIED_FAILURE -> strings.unspecifiedFailure
        BatteryHealth.COLD -> strings.cold
        BatteryHealth.UNKNOWN -> strings.unknown
    }
    fun BatteryInfo.toChargingSourceText(strings: BatteryInfoStrings): String = when (chargingSource) {
        ChargingSource.AC -> strings.chargingAc
        ChargingSource.USB -> strings.chargingUsb
        ChargingSource.WIRELESS -> strings.chargingWireless
        ChargingSource.NONE -> strings.discharging
    }
}
