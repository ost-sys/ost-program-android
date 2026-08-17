package com.ost.application.ui.screen.storage
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.core.storage.StorageInfo
import com.ost.application.core.storage.StorageInfoProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
class StorageViewModel(application: Application) : AndroidViewModel(application) {
    private val provider = StorageInfoProvider()
    private val _storageInfo = MutableStateFlow(provider.getStorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()
    init {
        viewModelScope.launch {
            while (isActive) {
                _storageInfo.value = provider.getStorageInfo()
                delay(3000)
            }
        }
    }
}
