package com.ost.application.appmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast

class UninstallBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_UNINSTALL_RESULT) {
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

            when (status) {
                PackageInstaller.STATUS_SUCCESS -> {
                    Toast.makeText(context, "context.getString(R.string.uninstall_success)", Toast.LENGTH_SHORT).show()
                    // Здесь можно отправить сигнал ViewModel, чтобы она обновила список
                }
                PackageInstaller.STATUS_FAILURE_ABORTED -> {
                    // Пользователь сам нажал "Отмена" в системном диалоге
                    Toast.makeText(context, "Uninstall cancelled by user", Toast.LENGTH_SHORT).show()
                }
                PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                    // Удаление заблокировано (например, приложение - админ устройства)
                    Toast.makeText(context, "Uninstall blocked by system", Toast.LENGTH_LONG).show()
                }
                PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                    Toast.makeText(context, "Uninstall failed: App is a device admin or active.", Toast.LENGTH_LONG).show()
                }
                else -> {
                    // Все остальные ошибки
                    val errorMessage = "Uninstall failed: ${message ?: "Unknown error"} (code: $status)"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    Log.d("UNINSTALLER", errorMessage)
                }
            }
        }
    }

    companion object {
        const val ACTION_UNINSTALL_RESULT = "com.ost.application.appmanager.ACTION_UNINSTALL_RESULT"
    }
}