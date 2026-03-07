package com.ost.application.appmanager

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppListUiState(
    val apps: List<AppInfo> = emptyList(),
    val showSystemApps: Boolean = false,
    val isLoading: Boolean = true
)

class AppViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState = _uiState.asStateFlow()

    fun toggleSystemApps() {
        _uiState.update { currentState ->
            currentState.copy(showSystemApps = !currentState.showSystemApps)
        }
    }

    fun loadApps(packageManager: PackageManager) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            val appInfoList = installedApps.map { app ->
                val isSystemAppFlag = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemAppFlag = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                AppInfo(
                    name = app.loadLabel(packageManager).toString(),
                    packageName = app.packageName,
                    icon = app.loadIcon(packageManager),
                    isSystemApp = isSystemAppFlag || isUpdatedSystemAppFlag
                )
            }.sortedBy { it.name.lowercase() }

            _uiState.update {
                it.copy(
                    apps = appInfoList,
                    isLoading = false
                )
            }
        }
    }
    fun requestAppDeletion(context: Context, packageName: String) {
        val packageInstaller = context.packageManager.packageInstaller

        val intent = Intent(context, UninstallBroadcastReceiver::class.java).apply {
            action = UninstallBroadcastReceiver.ACTION_UNINSTALL_RESULT
        }

        val sender = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            packageInstaller.uninstall(packageName, sender.intentSender)
        } catch (e: Exception) {
            Toast.makeText(context, "Error starting uninstall process: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("WearRecents")
    fun openAppInfoScreen(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}