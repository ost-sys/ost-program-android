package com.ost.application.data.util

import com.ost.application.data.model.DarkMode

fun determineDarkMode(darkModeValue: String, darkModeAutoValue: Boolean): DarkMode {
    return if (darkModeAutoValue) {
        DarkMode.AUTO
    } else {
        when (darkModeValue) {
            "0" -> DarkMode.DISABLED
            else -> DarkMode.ENABLED
        }
    }
}