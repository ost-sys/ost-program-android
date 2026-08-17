package com.ost.application.ui.screen.converters.timecalc
enum class DurationUnit(val shortLabel: String) {
    YEAR("y"),
    MONTH("mo"),
    WEEK("w"),
    DAY("d"),
    HOUR("h"),
    MINUTE("min"),
    SECOND("s");
    companion object {
        val ORDERED: List<DurationUnit> = listOf(YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND)
    }
}
data class TimeDuration(
    val values: Map<DurationUnit, Long?> = DurationUnit.ORDERED.associateWith { null },
) {
    fun get(unit: DurationUnit): Long? = values[unit]
    fun set(unit: DurationUnit, value: Long?): TimeDuration =
        copy(values = values + (unit to value))
    val isEmpty: Boolean get() = values.values.all { it == null }
    fun orZero(unit: DurationUnit): Long = values[unit] ?: 0L
    companion object {
        val EMPTY = TimeDuration()
    }
}
enum class Operator(val symbol: String) {
    ADD("+"),
    SUBTRACT("\u2212"),
    MULTIPLY("\u00D7"),
    DIVIDE("\u00F7"),
}
data class ChainTerm(
    val id: Long,
    val duration: TimeDuration,
    val trailingOperator: Operator?,
)
data class TimeCalculatorUiState(
    val history: List<ChainTerm> = emptyList(),
    val current: TimeDuration = TimeDuration.EMPTY,
    val focusedUnit: DurationUnit = DurationUnit.YEAR,
    val result: TimeDuration? = null,
    val errorMessage: String? = null,
)
