package com.ost.application.settings
import android.content.Context
import android.content.SharedPreferences
import com.ost.application.core.settings.TemperaturePrefKeys
import com.ost.application.core.settings.TemperatureUnit
import com.ost.application.core.settings.sync.SettingsSyncClient
import com.ost.application.core.settings.sync.WearSyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
private const val WEAR_PREFS_NAME = "wear_settings_prefs"
class WearTemperatureUnitRepository(
    context: Context,
    private val scope: CoroutineScope,
    syncState: StateFlow<WearSyncState>
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(WEAR_PREFS_NAME, Context.MODE_PRIVATE)
    private val syncClient = SettingsSyncClient(context)
    private val _unit = MutableStateFlow(loadLocalUnit())
    val unit: StateFlow<TemperatureUnit> = _unit.asStateFlow()
    init {
        scope.launch {
            syncState.collect { state ->
                when (state) {
                    WearSyncState.Enabled -> {
                        val phoneUnit = syncClient.getLastSyncedTemperatureUnit()
                        if (phoneUnit != null) _unit.value = phoneUnit
                    }
                    WearSyncState.Disabled, WearSyncState.Unavailable -> {
                        _unit.value = loadLocalUnit()
                    }
                }
            }
        }
        scope.launch {
            syncClient.observeTemperatureUnit().collect { synced ->
                if (syncState.value == WearSyncState.Enabled) _unit.value = synced
            }
        }
    }
    private fun loadLocalUnit(): TemperatureUnit =
        TemperatureUnit.fromKey(prefs.getString(TemperaturePrefKeys.TEMPERATURE_UNIT, null))
    fun updateUnit(newUnit: TemperatureUnit) {
        _unit.value = newUnit
        prefs.edit().putString(TemperaturePrefKeys.TEMPERATURE_UNIT, newUnit.name).apply()
    }
}
