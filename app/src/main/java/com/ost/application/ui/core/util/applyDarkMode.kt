package com.ost.application.ui.core.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import com.ost.application.data.model.DarkMode

fun applyDarkMode(darkModeOption: DarkMode) {
    when (darkModeOption) {
        DarkMode.AUTO -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
        DarkMode.DISABLED -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        DarkMode.ENABLED ->  AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
    }
}