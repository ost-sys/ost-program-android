package com.ost.application.presentation.display
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import com.ost.application.core.display.DisplayInfo
import com.ost.application.core.display.DisplayInfoProvider
import com.ost.application.core.display.DisplayInfoStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
data class WearDisplayInfoUiState(
    val resolution: String = "N/A",
    val refreshRate: String = "N/A",
    val dpi: String = "N/A",
    val diagonal: String = "N/A",
    val orientation: String = "N/A",
    val stylusSupport: String = "N/A",
    val isLoading: Boolean = true
)
private fun DisplayInfo.toUiState(isLoading: Boolean) = WearDisplayInfoUiState(
    resolution = resolution,
    refreshRate = refreshRate,
    dpi = dpi,
    diagonal = diagonal,
    orientation = orientation,
    stylusSupport = stylusSupport,
    isLoading = isLoading
)
class WearDisplayInfoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WearDisplayInfoUiState())
    val uiState: StateFlow<WearDisplayInfoUiState> = _uiState.asStateFlow()
    private var updateJob: Job? = null
    fun startUpdates(context: Context) {
        if (updateJob?.isActive == true) return
        val strings = context.toDisplayInfoStrings()
        updateJob = DisplayInfoProvider.observeDisplayInfo(context.applicationContext, strings)
            .onEach { info ->
                withContext(Dispatchers.Main) {
                    _uiState.value = info.toUiState(isLoading = false)
                }
            }
            .launchIn(viewModelScope)
    }
    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
    }
}
private fun Context.toDisplayInfoStrings() = DisplayInfoStrings(
    hz = getString(R.string.hz),
    dpi = getString(R.string.dpi),
    inches = getString(R.string.inches),
    portrait = getString(R.string.portrait),
    landscape = getString(R.string.landscape),
    supported = getString(R.string.support),
    unsupported = getString(R.string.unsupported)
)
