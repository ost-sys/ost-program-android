package com.ost.application.util

import android.R
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri

fun appSettingOpen(context: Context){
    Toast.makeText(
        context,
        "Go to Setting and Enable All Permission",
        Toast.LENGTH_LONG
    ).show()

    val settingIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    settingIntent.data = "package:${context.packageName}".toUri()
    context.startActivity(settingIntent)
}

fun warningPermissionDialog(context: Context,listener : DialogInterface.OnClickListener){
    AlertDialog.Builder(context)
        .setMessage("All permissions are required for this app")
        .setCancelable(false)
        .setPositiveButton(R.string.ok,listener)
        .create()
        .show()
}