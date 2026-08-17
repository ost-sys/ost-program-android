package com.ost.application.settings
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ost.application.core.settings.TemperaturePrefKeys
import com.ost.application.core.settings.TemperatureUnit
import com.ost.application.core.settings.sync.SettingsSyncClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
class PhoneTemperatureUnitRepository(
    context: Context,
    private val scope: CoroutineScope
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    private val syncClient = SettingsSyncClient(context)
    private val _unit = MutableStateFlow(loadUnit())
    val unit: StateFlow<TemperatureUnit> = _unit.asStateFlow()
    init {
        scope.launch { syncClient.pushTemperatureUnit(_unit.value) }
    }
    private fun loadUnit(): TemperatureUnit =
        TemperatureUnit.fromKey(prefs.getString(TemperaturePrefKeys.TEMPERATURE_UNIT, null))
    fun updateUnit(newUnit: TemperatureUnit) {
        _unit.value = newUnit
        prefs.edit { putString(TemperaturePrefKeys.TEMPERATURE_UNIT, newUnit.name) }
        scope.launch { syncClient.pushTemperatureUnit(newUnit) }
    }
}
