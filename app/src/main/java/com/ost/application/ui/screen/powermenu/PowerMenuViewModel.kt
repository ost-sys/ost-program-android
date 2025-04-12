package com.ost.application.ui.screen.powermenu // Создай этот пакет, если его нет

import android.os.SystemClock
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import com.ost.application.utils.getSystemProperty
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PowerAction(val command: String, val messageResId: Int) {
    POWER_OFF("reboot -p", R.string.turn_off_q),
    REBOOT("reboot", R.string.reboot_system_q),
    RECOVERY("reboot recovery", R.string.reboot_recovery_q),
    DOWNLOAD("reboot download", R.string.reboot_download_q),
    FASTBOOT("reboot bootloader", R.string.reboot_fastboot_q),
    FASTBOOTD("reboot fastboot", R.string.reboot_fastbootd_q)
}

enum class RootAccessState {
    CHECKING,
    GRANTED,
    DENIED
}

data class PowerMenuUiState(
    val rootState: RootAccessState = RootAccessState.CHECKING,
    val statusTextResId: Int = R.string.access_request_sent,
    val statusColor: Color = Color.Unspecified,
    val isSamsungDevice: Boolean = false,
    val showDialogFor: PowerAction? = null,
    val lastClickTime: Long = 0L
) {
    val isPowerOffEnabled: Boolean get() = rootState == RootAccessState.GRANTED
    val isRebootEnabled: Boolean get() = rootState == RootAccessState.GRANTED
    val isRecoveryEnabled: Boolean get() = rootState == RootAccessState.GRANTED
    val isDownloadModeEnabled: Boolean get() = rootState == RootAccessState.GRANTED && isSamsungDevice
    val isFastbootEnabled: Boolean get() = rootState == RootAccessState.GRANTED
    val isFastbootdEnabled: Boolean get() = rootState == RootAccessState.GRANTED
}

class PowerMenuViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PowerMenuUiState())
    val uiState: StateFlow<PowerMenuUiState> = _uiState.asStateFlow()

    init {
        checkDeviceType()
        checkRootAccess()
    }

    private fun checkDeviceType() {
        viewModelScope.launch(Dispatchers.IO) {
            val isSamsung = getSystemProperty("ro.product.system.brand")?.equals("samsung", ignoreCase = true) ?: false
            _uiState.update { it.copy(isSamsungDevice = isSamsung) }
        }
    }

    fun checkRootAccess() {
        _uiState.update { it.copy(rootState = RootAccessState.CHECKING, statusTextResId = R.string.access_request_sent) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = Shell.cmd("su -c echo success").exec()
            val newState = if (result.isSuccess) {
                RootAccessState.GRANTED
            } else {
                RootAccessState.DENIED
            }
            val newTextResId = if (newState == RootAccessState.GRANTED) R.string.access_granted else R.string.access_denied

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(rootState = newState, statusTextResId = newTextResId) }
            }
        }
    }

    fun onPowerActionClick(action: PowerAction) {
        val currentTime = SystemClock.uptimeMillis()
        if (currentTime - _uiState.value.lastClickTime > 600L) {
            _uiState.update { it.copy(showDialogFor = action, lastClickTime = currentTime) }
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showDialogFor = null) }
    }

    fun executeCommand(action: PowerAction) {
        viewModelScope.launch(Dispatchers.IO) {
            Shell.cmd(action.command).exec()
        }
    }
}