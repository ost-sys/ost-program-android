package com.ost.application

import android.app.Application
import com.ost.application.ui.core.util.DarkModeUtils.reapplyDarkModePrefs

class StargazersApp : Application() {
    override fun onCreate() {
        super.onCreate()
        reapplyDarkModePrefs()
    }
}