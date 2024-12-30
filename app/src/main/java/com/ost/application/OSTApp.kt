package com.ost.application

import android.app.Application
import com.ost.application.data.StargazersRepo
import com.ost.application.ui.core.util.applyDarkMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class OSTApp : Application() {
    override fun onCreate() {
        super.onCreate()
        applyDarkModeFromPrefs()
    }

    private fun applyDarkModeFromPrefs() = runBlocking {
        val darkMode = StargazersRepo.getInstance(this@OSTApp).stargazersSettingsFlow.first().darkModeOption
        applyDarkMode(darkMode)
    }
}