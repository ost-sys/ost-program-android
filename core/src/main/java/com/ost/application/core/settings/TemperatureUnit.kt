package com.ost.application.core.settings

import java.util.Locale

enum class TemperatureUnit {
    SYSTEM,
    CELSIUS,
    FAHRENHEIT;
    companion object {
        val DEFAULT = SYSTEM
        fun fromKey(key: String?): TemperatureUnit =
            entries.find { it.name == key } ?: DEFAULT
    }
}
object TemperaturePrefKeys {
    const val TEMPERATURE_UNIT = "temperature_unit"
}
fun TemperatureUnit.resolve(systemCountryCode: String = Locale.getDefault().country): TemperatureUnit = when (this) {
    TemperatureUnit.SYSTEM -> {
        val fahrenheitCountries = setOf("US", "LR", "MM")
        if (systemCountryCode.uppercase() in fahrenheitCountries) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
    }
    else -> this
}
fun Double.celsiusToFahrenheit(): Double = this * 9.0 / 5.0 + 32.0
fun Double.fahrenheitToCelsius(): Double = (this - 32.0) * 5.0 / 9.0
fun formatTemperature(celsius: Double, unit: TemperatureUnit, systemCountryCode: String = Locale.getDefault().country): String {
    val resolved = unit.resolve(systemCountryCode)
    return when (resolved) {
        TemperatureUnit.FAHRENHEIT -> "${celsius.celsiusToFahrenheit().roundToOneDecimalOrInt()}°F"
        else -> "${celsius.roundToOneDecimalOrInt()}°C"
    }
}
fun formatTemperatureFloat(celsius: Float, unit: TemperatureUnit, systemCountryCode: String = Locale.getDefault().country): String {
    return formatTemperature(celsius.toDouble(), unit, systemCountryCode)
}
fun convertTemperature(celsius: Float, unit: TemperatureUnit, systemCountryCode: String = Locale.getDefault().country): Float {
    val resolved = unit.resolve(systemCountryCode)
    return when (resolved) {
        TemperatureUnit.FAHRENHEIT -> celsius.toDouble().celsiusToFahrenheit().toFloat()
        else -> celsius
    }
}
private fun Double.roundToOneDecimalOrInt(): String {
    val rounded = kotlin.math.round(this * 10) / 10
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}
