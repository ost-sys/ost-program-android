package com.ost.application.core.settings
data class TimingSettings(
    val totalDuration: Int = Defaults.TOTAL_DURATION,
    val noiseDuration: Int = Defaults.NOISE_DURATION,
    val blackWhiteNoiseDuration: Int = Defaults.BW_NOISE_DURATION,
    val horizontalDuration: Int = Defaults.HORIZONTAL_DURATION,
    val verticalDuration: Int = Defaults.VERTICAL_DURATION
) {
    object Defaults {
        const val TOTAL_DURATION = 30
        const val NOISE_DURATION = 1
        const val BW_NOISE_DURATION = 1
        const val HORIZONTAL_DURATION = 1
        const val VERTICAL_DURATION = 1
    }
    companion object {
        val DEFAULT = TimingSettings()
    }
}
object TimingPrefKeys {
    const val TOTAL_DURATION = "total_duration"
    const val NOISE_DURATION = "noise_duration"
    const val BLACK_WHITE_NOISE_DURATION = "black_white_noise_duration"
    const val HORIZONTAL_DURATION = "horizontal_duration"
    const val VERTICAL_DURATION = "vertical_duration"
}
interface TimingSettingsRepository {
    val settings: kotlinx.coroutines.flow.StateFlow<TimingSettings>
    fun updateTotalDuration(value: Int)
    fun updateNoiseDuration(value: Int)
    fun updateBlackWhiteNoiseDuration(value: Int)
    fun updateHorizontalDuration(value: Int)
    fun updateVerticalDuration(value: Int)
}
