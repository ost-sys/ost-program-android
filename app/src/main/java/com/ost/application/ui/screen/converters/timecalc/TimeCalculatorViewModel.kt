package com.ost.application.ui.screen.converters.timecalc

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import com.ost.application.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

@Stable
data class TimeCalculatorUiState(
    val firstDateTimeMillis: Long = System.currentTimeMillis(),
    val secondDateTimeMillis: Long = System.currentTimeMillis(),
    val resultText: String? = null
)

class TimeCalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TimeCalculatorUiState())
    val uiState: StateFlow<TimeCalculatorUiState> = _uiState.asStateFlow()

    fun updateFirstDate(year: Int, month: Int, day: Int) {
        val currentMillis = _uiState.value.firstDateTimeMillis
        val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis }
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        _uiState.update { it.copy(firstDateTimeMillis = calendar.timeInMillis) }
    }

    fun updateFirstTime(hour: Int, minute: Int) {
        val currentMillis = _uiState.value.firstDateTimeMillis
        val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis }
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        _uiState.update { it.copy(firstDateTimeMillis = calendar.timeInMillis) }
    }

    fun updateSecondDate(year: Int, month: Int, day: Int) {
        val currentMillis = _uiState.value.secondDateTimeMillis
        val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis }
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        _uiState.update { it.copy(secondDateTimeMillis = calendar.timeInMillis) }
    }

    fun updateSecondTime(hour: Int, minute: Int) {
        val currentMillis = _uiState.value.secondDateTimeMillis
        val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis }
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        _uiState.update { it.copy(secondDateTimeMillis = calendar.timeInMillis) }
    }

    fun calculateTimeDifference() {
        val millis1 = _uiState.value.firstDateTimeMillis
        val millis2 = _uiState.value.secondDateTimeMillis

        val startMillis = minOf(millis1, millis2)
        val endMillis = maxOf(millis1, millis2)

        if (startMillis == endMillis) {
            _uiState.update { it.copy(resultText = getString(R.string.time_difference_zero)) }
            return
        }

        val zoneId = ZoneId.systemDefault()
        val startZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startMillis), zoneId)
        val endZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endMillis), zoneId)

        var tempZdt = startZdt

        val years = ChronoUnit.YEARS.between(tempZdt, endZdt)
        tempZdt = tempZdt.plusYears(years)

        val months = ChronoUnit.MONTHS.between(tempZdt, endZdt)
        tempZdt = tempZdt.plusMonths(months)

        val days = ChronoUnit.DAYS.between(tempZdt, endZdt)
        tempZdt = tempZdt.plusDays(days)

        val hours = ChronoUnit.HOURS.between(tempZdt, endZdt)
        tempZdt = tempZdt.plusHours(hours)

        val minutes = ChronoUnit.MINUTES.between(tempZdt, endZdt)

        val parts = mutableListOf<String>()
        if (years > 0) parts.add(formatYears(years))
        if (months > 0) parts.add("$months ${getString(R.string.months)}")
        if (days > 0) parts.add("$days ${getString(R.string.days)}")
        if (hours > 0) parts.add("$hours ${getString(R.string.hours)}")
        if (minutes > 0) parts.add("$minutes ${getString(R.string.minutes)}")

        val result = if (parts.isEmpty()) {
            val seconds = ChronoUnit.SECONDS.between(startZdt, endZdt)
            "$seconds ${getString(R.string.seconds)}"
        } else {
            parts.joinToString(", ")
        }

        _uiState.update { it.copy(resultText = result) }
    }

    private fun formatYears(diffInYears: Long): String {
        val context = getApplication<Application>()
        return when {
            diffInYears == 0L -> ""
            diffInYears % 100 in 11..14 -> String.format(Locale.getDefault(), context.getString(R.string.diff_years_format), diffInYears, context.getString(R.string.years_five_nine))
            diffInYears % 10 == 1L -> String.format(Locale.getDefault(), context.getString(R.string.diff_years_format), diffInYears, context.getString(R.string.years_one))
            diffInYears % 10 in 2..4 -> String.format(Locale.getDefault(), context.getString(R.string.diff_years_format), diffInYears, context.getString(R.string.years_two_four))
            else -> String.format(Locale.getDefault(), context.getString(R.string.diff_years_format), diffInYears, context.getString(R.string.years_five_nine))
        }
    }

    private fun getString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }
}