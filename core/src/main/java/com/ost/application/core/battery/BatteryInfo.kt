package com.ost.application.core.battery
enum class BatteryDisplayMode {
    NORMAL,
    CHARGING,
    POWER_SAVE
}
enum class BatteryStatusIcon {
    CHARGER,
    POWER_SAVE,
    FULL,
    HORIZ_075,
    HORIZ_050,
    LOW,
    VERY_LOW,
    HORIZ_000
}
data class BatteryInfo(
    val levelPercent: Int = -1,
    val isCharging: Boolean = false,
    val chargingSource: ChargingSource = ChargingSource.NONE,
    val displayMode: BatteryDisplayMode = BatteryDisplayMode.NORMAL,
    val statusIcon: BatteryStatusIcon = BatteryStatusIcon.HORIZ_000,
    val health: BatteryHealth = BatteryHealth.UNKNOWN,
    val temperatureCelsius: Float? = null,
    val voltageVolts: Float? = null,
    val technology: String? = null,
    val cycleCount: Int? = null
)
enum class ChargingSource { AC, USB, WIRELESS, NONE }
enum class BatteryHealth { GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, UNSPECIFIED_FAILURE, COLD, UNKNOWN }
data class BatteryInfoStrings(
    val charging: String,
    val chargingAc: String,
    val chargingUsb: String,
    val chargingWireless: String,
    val discharging: String,
    val batteryLevel: String,
    val good: String,
    val overheat: String,
    val dead: String,
    val overVoltage: String,
    val unspecifiedFailure: String,
    val cold: String,
    val unknown: String,
    val mv: String,
    val mah: String,
    val cycleCount: String,
    val notAvailable: String
)
