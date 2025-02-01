package com.ost.application.ui.core.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import com.ost.application.data.StargazersRepo
import com.ost.application.data.model.DarkMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object DarkModeUtils {

    fun String.toDarkMode(): DarkMode {
        return when(this) {
            "0" -> DarkMode.DISABLED
            "1" -> DarkMode.ENABLED
            "2" -> DarkMode.AUTO
            else -> throw IllegalArgumentException("Unknown dark mode value: $this")
        }
    }

    var darkMode: DarkMode
        set(value) {
            when (value) {
                DarkMode.AUTO -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                DarkMode.DISABLED -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
                DarkMode.ENABLED ->  AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
            }
        }
        get() {
            return when (AppCompatDelegate.getDefaultNightMode()){
                MODE_NIGHT_FOLLOW_SYSTEM -> DarkMode.AUTO
                MODE_NIGHT_YES -> DarkMode.ENABLED
                else -> DarkMode.DISABLED
            }
        }

    fun Context.reapplyDarkModePrefs(){
        runBlocking {
            darkMode = StargazersRepo(this@reapplyDarkModePrefs).stargazersSettingsFlow.first().darkModeOption
        }
    }
}