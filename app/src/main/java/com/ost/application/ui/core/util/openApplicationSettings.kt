package com.ost.application.ui.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ost.application.BuildConfig

fun Context.openApplicationSettings() {
    val intent = Intent(
        "android.settings.APPLICATION_DETAILS_SETTINGS",
        Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
    )
    intent.setFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    )
    startActivity(intent)
}