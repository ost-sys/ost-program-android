package com.ost.application.ui.screen.batteryinfo
data class BatterySample(
    val timestampMs: Long,
    val temperatureCelsius: Float,
    val voltageVolts: Float
)
