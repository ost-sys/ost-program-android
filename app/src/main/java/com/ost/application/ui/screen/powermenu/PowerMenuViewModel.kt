package com.ost.application.ui.screen.powermenu
import android.content.Context
import android.os.SystemClock
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.ComponentName
import android.content.pm.PackageManager
import com.ost.application.service.PowerTileService
import com.ost.application.service.LockTileService
import com.ost.application.R
import com.ost.application.core.service.OstAccessibilityService
import com.ost.application.util.getSystemProperty
import com.topjohnwu.superuser.Shell
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
enum class HapticEvent {
    CONFIRM,
    REJECT
}
enum class PowerAction(val command: String, val messageResId: Int) {
    POWER_OFF("reboot -p", R.string.turn_off_q),
    REBOOT("reboot", R.string.reboot_system_q),
    RECOVERY("reboot recovery", R.string.reboot_recovery_q),
    DOWNLOAD("reboot download", R.string.reboot_download_q),
    FASTBOOT("reboot bootloader", R.string.reboot_fastboot_q),
    FASTBOOTD("reboot fastboot", R.string.reboot_fastbootd_q),
    ACCESSIBILITY_POWER("", R.string.turn_off_q),
    ACCESSIBILITY_LOCK("", R.string.lock_screen_q)
}
enum class RootAccessState {
    CHECKING,
    GRANTED,
    DENIED
}
data class PowerMenuUiState(
    val rootState: RootAccessState = RootAccessState.CHECKING,
    val statusTextResId: Int = R.string.access_request_sent,
    val isSamsungDevice: Boolean = false,
    val showDialogFor: PowerAction? = null,
    val showAccessibilityPromptDialog: Boolean = false,
    val lastClickTime: Long = 0L,
    val isAccessibilityEnabled: Boolean = false
) {
    val isPowerOffEnabled: Boolean get() = rootState == RootAccessState.GRANTED
    val isRebootEnabled: Boolean get() = rootState == RootAccessState.GRANTED
    val isRecoveryEnabled: Boolean get() = rootState == RootAccessState.GRANTED
    val isDownloadModeEnabled: Boolean get() = rootState == RootAccessState.GRANTED && isSamsungDevice
    val isFastbootEnabled: Boolean get() = rootState == RootAccessState.GRANTED
    val isFastbootdEnabled: Boolean get() = rootState == RootAccessState.GRANTED
}
class PowerMenuViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PowerMenuUiState())
    val uiState = _uiState.asStateFlow()
    private val _hapticEvent = MutableSharedFlow<HapticEvent>(replay = 1)
    val hapticEvent = _hapticEvent.asSharedFlow()
    init {
        checkDeviceType()
        checkRootAccess()
        refreshAccessibilityState()
    }
    fun refreshAccessibilityState() {
        val isEnabled = OstAccessibilityService.isAccessibilityServiceEnabled(getApplication())
        _uiState.update { it.copy(isAccessibilityEnabled = isEnabled) }
        syncQSTilesState()
    }
    private fun syncQSTilesState() {
        val pm = getApplication<Application>().packageManager
        val isRootGranted = _uiState.value.rootState == RootAccessState.GRANTED
        val isAccessibilityGranted = _uiState.value.isAccessibilityEnabled
        val state = if (isRootGranted || isAccessibilityGranted)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val context = getApplication<Application>()
        pm.setComponentEnabledSetting(
            ComponentName(context, PowerTileService::class.java),
            state,
            PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            ComponentName(context, LockTileService::class.java),
            state,
            PackageManager.DONT_KILL_APP
        )
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
            val hasSuBinary = File("/system/bin/su").exists() ||
                              File("/system/xbin/su").exists() ||
                              File("/product/bin/su").exists() ||
                              File("/system/sd/xbin/su").exists() ||
                              File("/vendor/bin/su").exists()
            val isRooted = if (hasSuBinary) {
                withTimeoutOrNull(2000) {
                    runCatching { Shell.isAppGrantedRoot() == true || Shell.cmd("echo success").exec().isSuccess }.getOrDefault(false)
                } ?: false
            } else false
            val newState = if (isRooted) RootAccessState.GRANTED else RootAccessState.DENIED
            val newTextResId = if (newState == RootAccessState.GRANTED) R.string.access_granted else R.string.non_root_mode
            val hapticToSend = if (newState == RootAccessState.GRANTED) HapticEvent.CONFIRM else HapticEvent.REJECT
            _hapticEvent.emit(hapticToSend)
            _uiState.update { it.copy(rootState = newState, statusTextResId = newTextResId) }
            syncQSTilesState()
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
    fun dismissAccessibilityPromptDialog() {
        _uiState.update { it.copy(showAccessibilityPromptDialog = false) }
    }
    fun executeCommand(action: PowerAction, context: Context? = null) {
        when (action) {
            PowerAction.ACCESSIBILITY_POWER -> {
                val success = OstAccessibilityService.performPowerDialog()
                if (!success) {
                    _uiState.update { it.copy(showAccessibilityPromptDialog = true) }
                }
            }
            PowerAction.ACCESSIBILITY_LOCK -> {
                val success = OstAccessibilityService.performLockScreen()
                if (!success) {
                    _uiState.update { it.copy(showAccessibilityPromptDialog = true) }
                }
            }
            else -> {
                viewModelScope.launch(Dispatchers.IO) {
                    Shell.cmd(action.command).exec()
                }
            }
        }
    }
}
