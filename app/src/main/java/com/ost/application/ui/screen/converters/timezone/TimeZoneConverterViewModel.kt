package com.ost.application.ui.screen.converters.timezone
import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.concurrent.TimeUnit
@Stable
data class TimeZoneConverterUiState(
    val groupedZones: Map<String, List<String>> = emptyMap(),
    val searchQuery: String = "",
    val sourceTimeZoneId: String = "Etc/UTC",
    val targetTimeZoneId: String = ZoneId.systemDefault().id,
    val selectedTime: LocalTime = LocalTime.now(),
    val resultText: String? = null,
    val offsetDiff: String? = null
)
@OptIn(FlowPreview::class)
class TimeZoneConverterViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(TimeZoneConverterUiState())
    val uiState: StateFlow<TimeZoneConverterUiState> = _uiState.asStateFlow()
    private val allGrouped: Map<String, List<String>> = buildGroupedZones()
    private val _searchQuery = MutableStateFlow("")
    init {
        _uiState.update {
            it.copy(
                groupedZones = allGrouped,
                sourceTimeZoneId = "Etc/UTC",
                targetTimeZoneId = ZoneId.systemDefault().id,
                selectedTime = LocalTime.now(ZoneId.of("Etc/UTC"))
            )
        }
        updateDateTime()
        _searchQuery
            .debounce(200)
            .onEach { query -> applySearch(query) }
            .launchIn(viewModelScope)
    }
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }
    private fun applySearch(query: String) {
        val filtered = if (query.isBlank()) {
            allGrouped
        } else {
            val lower = query.lowercase()
            allGrouped
                .mapValues { (_, zones) -> zones.filter { it.lowercase().contains(lower) } }
                .filterValues { it.isNotEmpty() }
        }
        _uiState.update { it.copy(groupedZones = filtered) }
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
            val srcZone = ZoneId.of(state.sourceTimeZoneId)
            val tgtZone = ZoneId.of(state.targetTimeZoneId)
            val sourceZdt = ZonedDateTime.now(srcZone)
                .withHour(state.selectedTime.hour)
                .withMinute(state.selectedTime.minute)
                .withSecond(0).withNano(0)
            val targetZdt = sourceZdt.withZoneSameInstant(tgtZone)
            val sameDay = sourceZdt.dayOfYear == targetZdt.dayOfYear &&
                    sourceZdt.year == targetZdt.year
            val pattern = if (sameDay) "HH:mm z" else "HH:mm z, dd MMM"
            val formattedResult = targetZdt.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
            val srcOffsetSecs = srcZone.rules.getOffset(sourceZdt.toInstant()).totalSeconds
            val tgtOffsetSecs = tgtZone.rules.getOffset(targetZdt.toInstant()).totalSeconds
            val diffSecs = tgtOffsetSecs - srcOffsetSecs
            val offsetDiff = formatOffsetDiff(diffSecs)
            _uiState.update { it.copy(resultText = formattedResult, offsetDiff = offsetDiff) }
        } catch (e: DateTimeParseException) {
            _uiState.update { it.copy(resultText = getString(R.string.error), offsetDiff = null) }
        } catch (e: Exception) {
            _uiState.update { it.copy(resultText = getString(R.string.error_invalid_zone), offsetDiff = null) }
        }
    }
    private fun formatOffsetDiff(diffSeconds: Int): String {
        if (diffSeconds == 0) return "±0"
        val sign = if (diffSeconds > 0) "+" else "−"
        val abs = Math.abs(diffSeconds)
        val hours = abs / 3600
        val minutes = (abs % 3600) / 60
        return if (minutes == 0) "${sign}${hours}h"
        else "${sign}${hours}h ${minutes}m"
    }
    private fun getString(resId: Int) = getApplication<Application>().getString(resId)
    private fun buildGroupedZones(): Map<String, List<String>> {
        val all = ZoneId.getAvailableZoneIds().sorted()
        val regionOrder = listOf(
            "Africa", "America", "Antarctica", "Arctic",
            "Asia", "Atlantic", "Australia", "Europe",
            "Indian", "Pacific", "Etc", "Other"
        )
        val grouped = LinkedHashMap<String, MutableList<String>>()
        regionOrder.forEach { grouped[it] = mutableListOf() }
        for (zone in all) {
            val region = when {
                zone.startsWith("Africa/")      -> "Africa"
                zone.startsWith("America/")     -> "America"
                zone.startsWith("Antarctica/")  -> "Antarctica"
                zone.startsWith("Arctic/")      -> "Arctic"
                zone.startsWith("Asia/")        -> "Asia"
                zone.startsWith("Atlantic/")    -> "Atlantic"
                zone.startsWith("Australia/")   -> "Australia"
                zone.startsWith("Europe/")      -> "Europe"
                zone.startsWith("Indian/")      -> "Indian"
                zone.startsWith("Pacific/")     -> "Pacific"
                zone.startsWith("Etc/")         -> "Etc"
                else                            -> "Other"
            }
            grouped.getOrPut(region) { mutableListOf() }.add(zone)
        }
        return grouped.filterValues { it.isNotEmpty() }
    }
}
