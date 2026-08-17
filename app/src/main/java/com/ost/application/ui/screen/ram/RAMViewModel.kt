package com.ost.application.ui.screen.ram
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.core.memory.RamInfo
import com.ost.application.core.memory.RamInfoProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
class RAMViewModel(application: Application) : AndroidViewModel(application) {
    private val provider = RamInfoProvider(application)
    private val _ramInfo = MutableStateFlow(provider.getRamInfo())
    val ramInfo: StateFlow<RamInfo> = _ramInfo.asStateFlow()
    init {
        viewModelScope.launch {
            while (isActive) {
                _ramInfo.value = provider.getRamInfo()
                delay(2000)
            }
        }
    }
}
