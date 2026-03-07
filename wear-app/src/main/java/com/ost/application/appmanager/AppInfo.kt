package com.ost.application.appmanager

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isSystemApp: Boolean
)