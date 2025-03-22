package com.ost.application.ui.core.util

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import com.ost.application.R
import dev.oneuiproject.oneui.widget.Toast

fun appSettingOpen(context: Context){
    Toast.makeText(
        context,
        context.getString(R.string.enable_all_permission_r),
        Toast.LENGTH_LONG
    ).show()

    val settingIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    settingIntent.data = "package:${context.packageName}".toUri()
    context.startActivity(settingIntent)
}

fun warningPermissionDialog(context: Context,listener : DialogInterface.OnClickListener){
    AlertDialog.Builder(context)
        .setMessage(context.getString(R.string.all_permissions_required))
        .setCancelable(false)
        .setPositiveButton(android.R.string.ok,listener)
        .create()
        .show()
}