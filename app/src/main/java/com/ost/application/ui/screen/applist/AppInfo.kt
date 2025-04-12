package com.ost.application.ui.screen.applist

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val sizeBytes: Long,
    val sourceDir: String,
    val isSystemApp: Boolean
)
