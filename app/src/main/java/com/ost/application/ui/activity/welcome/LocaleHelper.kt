package com.ost.application.ui.activity.welcome

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    fun setLocale(locale: Locale?) {
        val appLocale = if (locale != null) {
            LocaleListCompat.create(locale)
        } else {
            LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getCurrentLocale(): Locale {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            locales[0]!!
        } else {
            getSystemLocale()
        }
    }

    fun getSystemLocale(): Locale {
        return Locale.getDefault(Locale.Category.DISPLAY)
    }
}