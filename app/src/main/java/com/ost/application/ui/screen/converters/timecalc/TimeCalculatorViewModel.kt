package com.ost.application.ui.screen.converters.timecalc
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Duration
import java.time.LocalDateTime
import java.time.Period
import java.util.concurrent.atomic.AtomicLong
private data class Fields(
    val years: Long = 0,
    val months: Long = 0,
    val weeks: Long = 0,
    val days: Long = 0,
    val hours: Long = 0,
    val minutes: Long = 0,
    val seconds: Long = 0,
)
private fun TimeDuration.toFields(): Fields = Fields(
    years = orZero(DurationUnit.YEAR),
    months = orZero(DurationUnit.MONTH),
    weeks = orZero(DurationUnit.WEEK),
    days = orZero(DurationUnit.DAY),
    hours = orZero(DurationUnit.HOUR),
    minutes = orZero(DurationUnit.MINUTE),
    seconds = orZero(DurationUnit.SECOND),
)
private fun Fields.toTimeDuration(): TimeDuration = TimeDuration()
    .set(DurationUnit.YEAR, years)
    .set(DurationUnit.MONTH, months)
    .set(DurationUnit.WEEK, weeks)
    .set(DurationUnit.DAY, days)
    .set(DurationUnit.HOUR, hours)
    .set(DurationUnit.MINUTE, minutes)
    .set(DurationUnit.SECOND, seconds)
private fun normalize(f: Fields): Fields {
    var carry = Math.floorDiv(f.seconds, 60L)
    val seconds = Math.floorMod(f.seconds, 60L)
    val totalMinutes = f.minutes + carry
    carry = Math.floorDiv(totalMinutes, 60L)
    val minutes = Math.floorMod(totalMinutes, 60L)
    val totalHours = f.hours + carry
    carry = Math.floorDiv(totalHours, 24L)
    val hours = Math.floorMod(totalHours, 24L)
    val totalDays = f.days + carry
    carry = Math.floorDiv(totalDays, 7L)
    val days = Math.floorMod(totalDays, 7L)
    val weeks = f.weeks + carry
    carry = Math.floorDiv(f.months, 12L)
    val months = Math.floorMod(f.months, 12L)
    val years = f.years + carry
    return Fields(years, months, weeks, days, hours, minutes, seconds)
}
private fun add(a: Fields, b: Fields): Fields = normalize(
    Fields(
        a.years + b.years, a.months + b.months, a.weeks + b.weeks, a.days + b.days,
        a.hours + b.hours, a.minutes + b.minutes, a.seconds + b.seconds,
    )
)
private fun subtract(a: Fields, b: Fields): Fields = normalize(
    Fields(
        a.years - b.years, a.months - b.months, a.weeks - b.weeks, a.days - b.days,
        a.hours - b.hours, a.minutes - b.minutes, a.seconds - b.seconds,
    )
)
private fun Fields.toTotalSeconds(anchor: LocalDateTime): Long {
    val target = anchor
        .plusYears(years).plusMonths(months).plusWeeks(weeks).plusDays(days)
        .plusHours(hours).plusMinutes(minutes).plusSeconds(seconds)
    return Duration.between(anchor, target).seconds
}
private fun totalSecondsToFields(totalSeconds: Long, anchor: LocalDateTime): Fields {
    val sign = if (totalSeconds < 0) -1L else 1L
    val absSeconds = kotlin.math.abs(totalSeconds)
    val target = anchor.plusSeconds(absSeconds)
    var period = Period.between(anchor.toLocalDate(), target.toLocalDate())
    var timeOfDayDeltaSeconds =
        target.toLocalTime().toSecondOfDay() - anchor.toLocalTime().toSecondOfDay()
    if (timeOfDayDeltaSeconds < 0) {
        val adjustedEndDate = target.toLocalDate().minusDays(1)
        period = Period.between(anchor.toLocalDate(), adjustedEndDate)
        timeOfDayDeltaSeconds += 86_400
    }
    val extraDays = period.days.toLong()
    val weeks = extraDays / 7
    val days = extraDays % 7
    val hours = timeOfDayDeltaSeconds / 3600L
    val minutes = (timeOfDayDeltaSeconds % 3600L) / 60L
    val seconds = timeOfDayDeltaSeconds % 60L
    return Fields(
        years = period.years.toLong() * sign,
        months = period.months.toLong() * sign,
        weeks = weeks * sign,
        days = days * sign,
        hours = hours * sign,
        minutes = minutes * sign,
        seconds = seconds * sign,
    )
}
private fun multiply(a: Fields, b: Fields, anchor: LocalDateTime): Fields {
    val secondsA = a.toTotalSeconds(anchor)
    val secondsB = b.toTotalSeconds(anchor)
    val result = try {
        Math.multiplyExact(secondsA, secondsB)
    } catch (_: ArithmeticException) {
        if ((secondsA >= 0) == (secondsB >= 0)) Long.MAX_VALUE else Long.MIN_VALUE
    }
    return totalSecondsToFields(result, anchor)
}
private fun divide(a: Fields, b: Fields, anchor: LocalDateTime): Fields? {
    val secondsA = a.toTotalSeconds(anchor)
    val secondsB = b.toTotalSeconds(anchor)
    if (secondsB == 0L) return null
    return totalSecondsToFields(secondsA / secondsB, anchor)
}
private fun evaluateChain(
    terms: List<ChainTerm>,
    anchor: LocalDateTime = LocalDateTime.now()
): Fields? {
    if (terms.isEmpty()) return Fields()
    data class Node(var value: Fields, var op: Operator?)
    val nodes = terms.map { Node(it.duration.toFields(), it.trailingOperator) }.toMutableList()
    var i = 0
    while (i < nodes.size - 1) {
        val op = nodes[i].op
        if (op == Operator.MULTIPLY || op == Operator.DIVIDE) {
            val a = nodes[i].value
            val b = nodes[i + 1].value
            val combined =
                if (op == Operator.MULTIPLY) multiply(a, b, anchor) else divide(a, b, anchor)
                    ?: return null
            nodes[i] = Node(combined, nodes[i + 1].op)
            nodes.removeAt(i + 1)
        } else {
            i++
        }
    }
    var acc = nodes[0].value
    for (idx in 1 until nodes.size) {
        acc = when (nodes[idx - 1].op) {
            Operator.ADD -> add(acc, nodes[idx].value)
            Operator.SUBTRACT -> subtract(acc, nodes[idx].value)
            else -> acc
        }
    }
    return acc
}
class TimeCalculatorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TimeCalculatorUiState())
    val uiState: StateFlow<TimeCalculatorUiState> = _uiState.asStateFlow()
    private val idGenerator = AtomicLong(0)
    fun onUnitFocused(unit: DurationUnit) {
        _uiState.update { it.copy(focusedUnit = unit, errorMessage = null) }
    }
    fun onDigit(digit: Int) {
        require(digit in 0..9)
        _uiState.update { state ->
            val unit = state.focusedUnit
            val existing = state.current.get(unit)
            val newValue =
                if (existing == null) digit.toLong() else (existing * 10 + digit).coerceAtMost(
                    999_999L
                )
            state.copy(current = state.current.set(unit, newValue), errorMessage = null)
        }
    }
    fun onBackspace() {
        _uiState.update { state ->
            val unit = state.focusedUnit
            val existing = state.current.get(unit)
            when {
                existing == null -> {
                    val order = DurationUnit.ORDERED
                    val idx = order.indexOf(unit)
                    if (idx > 0) state.copy(focusedUnit = order[idx - 1]) else state
                }
                existing < 10 -> state.copy(current = state.current.set(unit, null))
                else -> state.copy(current = state.current.set(unit, existing / 10))
            }
        }
    }
    fun onOperator(operator: Operator) {
        val newId = idGenerator.getAndIncrement()
        _uiState.update { state ->
            if (state.current.isEmpty && state.history.isNotEmpty()) {
                val updated = state.history.toMutableList()
                val last = updated.removeAt(updated.lastIndex)
                updated.add(last.copy(trailingOperator = operator))
                return@update state.copy(history = updated, errorMessage = null)
            }
            val committed =
                ChainTerm(id = newId, duration = state.current, trailingOperator = operator)
            state.copy(
                history = state.history + committed,
                current = TimeDuration.EMPTY,
                focusedUnit = DurationUnit.YEAR,
                result = null,
                errorMessage = null,
            )
        }
    }
    fun onEquals() {
        val newId = idGenerator.getAndIncrement()
        _uiState.update { state ->
            if (state.history.isEmpty() && state.current.isEmpty) return@update state
            val allTerms = if (state.current.isEmpty) {
                state.history
            } else {
                state.history + ChainTerm(
                    id = newId,
                    duration = state.current,
                    trailingOperator = null
                )
            }
            val evaluated = evaluateChain(allTerms)
            if (evaluated == null) {
                state.copy(errorMessage = "Деление на ноль")
            } else {
                val resultDuration = evaluated.toTimeDuration()
                state.copy(
                    history = emptyList(),
                    current = resultDuration,
                    focusedUnit = DurationUnit.YEAR,
                    result = resultDuration,
                    errorMessage = null,
                )
            }
        }
    }
    fun onAllClear() {
        _uiState.update { TimeCalculatorUiState() }
    }
}
