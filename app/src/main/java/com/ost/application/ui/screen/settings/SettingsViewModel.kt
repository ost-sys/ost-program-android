package com.ost.application.ui.screen.settings

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.ui.screen.about.AboutActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object PrefKeys {
    const val TOTAL_DURATION = "total_duration"
    const val NOISE_DURATION = "noise_duration"
    const val BLACK_WHITE_NOISE_DURATION = "black_white_noise_duration"
    const val HORIZONTAL_DURATION = "horizontal_duration"
    const val VERTICAL_DURATION = "vertical_duration"
}

@Stable
data class SettingsUiState(
    val totalDuration: Int = 30,
    val noiseDuration: Int = 1,
    val blackWhiteNoiseDuration: Int = 1,
    val horizontalDuration: Int = 1,
    val verticalDuration: Int = 1
)

sealed class SettingsAction {
    data class StartActivity(val intent: Intent) : SettingsAction()
    data class ShowToast(val messageResId: Int) : SettingsAction()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _action = Channel<SettingsAction>(Channel.BUFFERED)
    val action = _action.receiveAsFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {

        _uiState.update { currentState ->
            currentState.copy(
                totalDuration = prefs.getInt(PrefKeys.TOTAL_DURATION, 30),
                noiseDuration = prefs.getInt(PrefKeys.NOISE_DURATION, 1),
                blackWhiteNoiseDuration = prefs.getInt(PrefKeys.BLACK_WHITE_NOISE_DURATION, 1),
                horizontalDuration = prefs.getInt(PrefKeys.HORIZONTAL_DURATION, 1),
                verticalDuration = prefs.getInt(PrefKeys.VERTICAL_DURATION, 1)
            )
        }
    }

    fun updateTotalDuration(newValue: Int) {
        prefs.edit().putInt(PrefKeys.TOTAL_DURATION, newValue).apply()
        _uiState.update { it.copy(totalDuration = newValue) }
    }
    fun updateNoiseDuration(newValue: Int) {
        prefs.edit().putInt(PrefKeys.NOISE_DURATION, newValue).apply()
        _uiState.update { it.copy(noiseDuration = newValue) }
    }
    fun updateBlackWhiteNoiseDuration(newValue: Int) {
        prefs.edit().putInt(PrefKeys.BLACK_WHITE_NOISE_DURATION, newValue).apply()
        _uiState.update { it.copy(blackWhiteNoiseDuration = newValue) }
    }
    fun updateHorizontalDuration(newValue: Int) {
        prefs.edit().putInt(PrefKeys.HORIZONTAL_DURATION, newValue).apply()
        _uiState.update { it.copy(horizontalDuration = newValue) }
    }
    fun updateVerticalDuration(newValue: Int) {
        prefs.edit().putInt(PrefKeys.VERTICAL_DURATION, newValue).apply()
        _uiState.update { it.copy(verticalDuration = newValue) }
    }

    fun onAboutAppClicked() {
        val intent = Intent(getApplication(), AboutActivity::class.java)
        viewModelScope.launch {
            _action.send(SettingsAction.StartActivity(intent))
        }
    }
}