package com.ost.application.ui.fragment.applist

import androidx.apppickerview.widget.AppPickerView.TYPE_LIST
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ost.application.data.AppsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class AppsScreenState(
    val listTypeSelected: Int = TYPE_LIST,
    val appList: ArrayList<String> = ArrayList(),
    val showSystem: Boolean = false,
    val systemAppsCount: Int = 0,
    val isLoading:Boolean = true
)

class AppListModel (
    private val appsRepo: AppsRepo
): ViewModel() {

    val appPickerScreenStateFlow = combine(
        appsRepo.appsFlow,
        appsRepo.appPreferenceFlow
    ){ apps, prefs ->
        AppsScreenState(
            listTypeSelected = prefs.appPickerType,
            appList = ArrayList(
                apps
                    .filter {
                        prefs.showSystem || !it.isSystemApp
                    }.map {
                        it.packageName
                    }
            ),
            showSystem = prefs.showSystem,
            systemAppsCount = apps.count { it.isSystemApp },
            isLoading = false
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            AppsScreenState()
        )

    fun toggleShowSystem() = viewModelScope.launch {
        val current =  appPickerScreenStateFlow.value.showSystem
        appsRepo.setShowSystemApps(!current)
    }
}

class AppListModelFactory(private val appsRepo: AppsRepo) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppListModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppListModel(appsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
