package com.ost.application.ui.activity.setup

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import com.ost.application.R
import com.ost.application.ui.screen.settings.PrefKeys
import com.ost.application.ui.screen.settings.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val githubPrefs: SharedPreferences = application.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadSupportedLocales()
    }

    private fun loadSettings() {
        _uiState.update { currentState ->
            currentState.copy(
                totalDuration = prefs.getInt(PrefKeys.TOTAL_DURATION, 30),
                noiseDuration = prefs.getInt(PrefKeys.NOISE_DURATION, 1),
                blackWhiteNoiseDuration = prefs.getInt(PrefKeys.BLACK_WHITE_NOISE_DURATION, 1),
                horizontalDuration = prefs.getInt(PrefKeys.HORIZONTAL_DURATION, 1),
                verticalDuration = prefs.getInt(PrefKeys.VERTICAL_DURATION, 1),
                githubToken = githubPrefs.getString("token", "") ?: ""
            )
        }
    }

    private fun loadSupportedLocales() {
        val context = getApplication<Application>()
        val locales = mutableListOf<Locale>()
        try {
            val parser = context.resources.getXml(R.xml.locales_config)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    val langTag = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                    if (langTag != null) {
                        locales.add(Locale.forLanguageTag(langTag))
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _uiState.update { it.copy(supportedLocales = locales) }
    }

    fun updateTotalDuration(newValue: Int) {
        prefs.edit { putInt(PrefKeys.TOTAL_DURATION, newValue) }
        _uiState.update { it.copy(totalDuration = newValue) }
    }

    fun updateNoiseDuration(newValue: Int) {
        prefs.edit { putInt(PrefKeys.NOISE_DURATION, newValue) }
        _uiState.update { it.copy(noiseDuration = newValue) }
    }

    fun updateBlackWhiteNoiseDuration(newValue: Int) {
        prefs.edit { putInt(PrefKeys.BLACK_WHITE_NOISE_DURATION, newValue) }
        _uiState.update { it.copy(blackWhiteNoiseDuration = newValue) }
    }

    fun updateHorizontalDuration(newValue: Int) {
        prefs.edit { putInt(PrefKeys.HORIZONTAL_DURATION, newValue) }
        _uiState.update { it.copy(horizontalDuration = newValue) }
    }

    fun updateVerticalDuration(newValue: Int) {
        prefs.edit { putInt(PrefKeys.VERTICAL_DURATION, newValue) }
        _uiState.update { it.copy(verticalDuration = newValue) }
    }

    fun updateGithubToken(token: String) {
        _uiState.update { it.copy(githubToken = token) }
    }

    fun saveAllSettings() {
        val state = _uiState.value
        prefs.edit {
            putInt(PrefKeys.TOTAL_DURATION, state.totalDuration)
                .putInt(PrefKeys.NOISE_DURATION, state.noiseDuration)
                .putInt(PrefKeys.BLACK_WHITE_NOISE_DURATION, state.blackWhiteNoiseDuration)
                .putInt(PrefKeys.HORIZONTAL_DURATION, state.horizontalDuration)
                .putInt(PrefKeys.VERTICAL_DURATION, state.verticalDuration)
        }

        githubPrefs.edit { putString("token", state.githubToken) }
    }
}