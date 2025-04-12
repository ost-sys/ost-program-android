package com.ost.application

import android.app.Application
import com.ost.application.data.model.StargazersRepo
import kotlinx.coroutines.runBlocking

class OSTApp : Application() {
    override fun onCreate() {
        super.onCreate()
        applyDarkModeFromPrefs()
    }

    private fun applyDarkModeFromPrefs() = runBlocking {
        StargazersRepo.getInstance(this@OSTApp)
    }
}