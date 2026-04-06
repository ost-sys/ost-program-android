package com.ost.application.ui.screen.applist

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppListUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val showSystemApps: Boolean = true,
    val searchQuery: String = "",
    val isRootAvailable: Boolean = false,
    val apps: List<AppInfo> = emptyList()
)

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val _rawAppList = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _showSystemApps = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _isRootAvailable = MutableStateFlow(false)

    // Автоматическая реактивная фильтрация в фоновом потоке
    // Автоматическая реактивная фильтрация в фоновом потоке
    val uiState: StateFlow<AppListUiState> = combine(
        _rawAppList,
        _isRootAvailable,
        combine(_isLoading, _error, ::Pair),
        combine(_showSystemApps, _searchQuery, ::Pair)
    ) { apps, root, status, filters ->
        val loading = status.first
        val err = status.second
        val showSystem = filters.first
        val query = filters.second

        val filtered = apps.filter { if (showSystem) true else !it.isSystemApp }
            .filter {
                query.isBlank() ||
                        it.name.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }

        AppListUiState(
            isLoading = loading,
            error = err,
            showSystemApps = showSystem,
            searchQuery = query,
            isRootAvailable = root,
            apps = filtered
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppListUiState())
    // Слушатель системных удалений
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!replacing) {
                    refresh(showLoadingIndicator = false)
                }
            }
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
            addDataScheme("package")
        }
        getApplication<Application>().registerReceiver(packageReceiver, filter)

        viewModelScope.launch(Dispatchers.IO) {
            _isRootAvailable.value = RootUtils.isRootAvailable
        }
        refresh(true)
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(packageReceiver)
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun toggleSystemApps() { _showSystemApps.value = !_showSystemApps.value }

    fun refresh(showLoadingIndicator: Boolean = true) {
        viewModelScope.launch {
            if (showLoadingIndicator) _isLoading.value = true
            _error.value = null
            try {
                val freshList = withContext(Dispatchers.IO) {
                    getInstalledApps(getApplication())
                }
                _rawAppList.value = freshList
            } catch (e: Exception) {
                _error.value = getApplication<Application>().getString(
                    R.string.error_loading_app_list, e.localizedMessage ?: "Unknown"
                )
            } finally {
                if (showLoadingIndicator) _isLoading.value = false
            }
        }
    }

    fun uninstallAppRoot(packageName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = RootUtils.uninstallAppRoot(packageName)
            if (success) { refresh(false) }
            withContext(Dispatchers.Main) { onResult(success) }
        }
    }
}

class AppListViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppListViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}