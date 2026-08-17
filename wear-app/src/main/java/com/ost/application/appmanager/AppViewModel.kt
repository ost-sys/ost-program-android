package com.ost.application.appmanager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
data class AppListUiState(
    val apps: List<AppInfo> = emptyList(),
    val showSystemApps: Boolean = false,
    val isLoading: Boolean = true
)
class AppViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState = _uiState.asStateFlow()
    fun toggleSystemApps() {
        _uiState.update { it.copy(showSystemApps = !it.showSystemApps) }
    }
    fun loadApps(packageManager: PackageManager) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val appInfoList = withContext(Dispatchers.IO) {
                packageManager
                    .getInstalledApplications(PackageManager.GET_META_DATA)
                    .map { app ->
                        val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                                || (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        AppInfo(
                            name = app.loadLabel(packageManager).toString(),
                            packageName = app.packageName,
                            icon = app.loadIcon(packageManager),
                            isSystemApp = isSystemApp
                        )
                    }
                    .sortedBy { it.name.lowercase() }
            }
            _uiState.update { it.copy(apps = appInfoList, isLoading = false) }
        }
    }
}
