package com.ost.application.ui.screen.converters.timezone

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import com.ost.application.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Collections
import java.util.Locale

@Stable
data class TimeZoneConverterUiState(
    val timeZones: List<String> = emptyList(),
    val sourceTimeZoneId: String = "Etc/UTC",
    val targetTimeZoneId: String = ZoneId.systemDefault().id,
    val selectedTime: LocalTime = LocalTime.now(),
    val resultText: String? = null
)

class TimeZoneConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TimeZoneConverterUiState())
    val uiState: StateFlow<TimeZoneConverterUiState> = _uiState.asStateFlow()

    init {
        loadTimeZones()
        _uiState.update {
            it.copy(
                sourceTimeZoneId = "Etc/UTC",
                targetTimeZoneId = ZoneId.systemDefault().id,
                selectedTime = LocalTime.now()
            )
        }
        updateDateTime()
    }

    private fun loadTimeZones() {
        val zones = ZoneId.getAvailableZoneIds().toMutableList()
        Collections.sort(zones)
        _uiState.update { it.copy(timeZones = zones) }
    }

    fun setSourceTimeZone(zoneId: String) {
        _uiState.update { it.copy(sourceTimeZoneId = zoneId) }
        updateDateTime()
    }

    fun setTargetTimeZone(zoneId: String) {
        _uiState.update { it.copy(targetTimeZoneId = zoneId) }
        updateDateTime()
    }

    fun setSelectedTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(selectedTime = LocalTime.of(hour, minute)) }
        updateDateTime()
    }

    private fun updateDateTime() {
        val state = _uiState.value
        try {
            val sourceZdt = ZonedDateTime.now(ZoneId.of(state.sourceTimeZoneId))
                .withHour(state.selectedTime.hour)
                .withMinute(state.selectedTime.minute)
                .withSecond(0).withNano(0)

            val targetZdt = sourceZdt.withZoneSameInstant(ZoneId.of(state.targetTimeZoneId))

            val formatter = DateTimeFormatter.ofPattern("HH:mm z", Locale.getDefault())
            val formattedResult = targetZdt.format(formatter)

            _uiState.update { it.copy(resultText = formattedResult) }

        } catch (e: DateTimeParseException) {
            _uiState.update { it.copy(resultText = getString(R.string.error)) }
        } catch (e: Exception) {
            _uiState.update { it.copy(resultText = getString(R.string.error_invalid_zone)) }
        }
    }

    private fun getString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }
}