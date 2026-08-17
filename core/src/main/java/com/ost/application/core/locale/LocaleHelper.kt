package com.ost.application.core.locale
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
object LocaleHelper {
    fun setLocale(locale: Locale?) {
        val appLocale = if (locale != null) LocaleListCompat.create(locale) else LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
    fun isFollowingSystem(): Boolean = AppCompatDelegate.getApplicationLocales().isEmpty
    fun getCurrentLocale(): Locale {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) locales[0]!! else getSystemLocale()
    }
    fun getSystemLocale(): Locale = Locale.getDefault(Locale.Category.DISPLAY)
    fun getCurrentLanguageTag(): String? =
        if (isFollowingSystem()) null else getCurrentLocale().toLanguageTag()
}
