package com.ost.application.ui.core.util

import android.annotation.SuppressLint
import android.app.Activity
import androidx.appcompat.util.SeslMisc

val Activity.defaultActivityWindowBackground: Int
    @SuppressLint("RestrictedApi")
    get() {
        return if (SeslMisc.isLightTheme(this)) {
            androidx.appcompat.R.color.sesl_round_and_bgcolor_light
        }else{
            androidx.appcompat.R.color.sesl_round_and_bgcolor_dark
        }
    }