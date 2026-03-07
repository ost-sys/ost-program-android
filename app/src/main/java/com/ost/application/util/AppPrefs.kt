package com.ost.application.util

import android.content.Context
import android.preference.PreferenceManager

object AppPrefs {
    private const val SETUP_COMPLETE_KEY = "is_setup_complete"

    fun isSetupComplete(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(SETUP_COMPLETE_KEY, false)
    }

    fun setSetupComplete(context: Context, isComplete: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(SETUP_COMPLETE_KEY, isComplete).apply()
    }
}