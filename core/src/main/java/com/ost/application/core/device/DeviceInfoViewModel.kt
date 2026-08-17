package com.ost.application.core.device
import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
enum class DeviceFormFactor { PHONE, TABLET, UNKNOWN }
enum class BiometricStatus {
    SUPPORTED_AND_ENROLLED,
    SUPPORTED_NOT_ENROLLED,
    UNSUPPORTED,
    UNKNOWN,
    CHECKING
}
enum class ToastType {
    SUCCESS, FAIL, EASTER_EGG_NOT_FOUND, ERROR
}
data class DefaultInfoUiState(
    val deviceName: String = "Loading...",
    val model: String = "---",
    val codename: String = "---",
    val androidVersion: String = "---",
    val brand: String = "---",
    val board: String = "---",
    val buildNumber: String = "---",
    val sdkVersion: String = "---",
    val deviceFormFactor: DeviceFormFactor = DeviceFormFactor.UNKNOWN,
    val buildFingerprint: String = "---",
    val securityPatch: String = "---",
    val kernelVersion: String = "---",
    val bootloader: String = "---",
    val radioVersion: String = "---",
    val partitionStyle: String = "---",
    val isTrebleSupported: Boolean = false,
    val uptime: String = "---",
    val biometricStatus: BiometricStatus = BiometricStatus.CHECKING,
    val isFingerprintTestable: Boolean = false,
    val isLoadingName: Boolean = true
)
sealed class DefaultInfoAction {
    object ShowBiometricPrompt : DefaultInfoAction()
    data class LaunchEasterEgg(val intent: Intent) : DefaultInfoAction()
    data class ShowToast(val type: ToastType) : DefaultInfoAction()
    data class ShowToastMsg(val message: String) : DefaultInfoAction()
}
class DeviceInfoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DeviceRepository = DeviceRepositoryImpl(application)
    private val _uiState = MutableStateFlow(DefaultInfoUiState())
    val uiState: StateFlow<DefaultInfoUiState> = _uiState.asStateFlow()
    private val _action = Channel<DefaultInfoAction>(Channel.BUFFERED)
    val action = _action.receiveAsFlow()
    private var updateJob: Job? = null
    private var clickCount = 0
    private var lastClickTime = 0L
    private var easterEggHandlerJob: Job? = null
    init {
        loadStaticInfo()
        fetchDeviceName()
        startPeriodicUpdates()
    }
    private fun loadStaticInfo() {
        _uiState.update {
            it.copy(
                androidVersion = repository.getAndroidVersion(),
                brand = repository.getBrand(),
                board = repository.getBoard(),
                buildNumber = repository.getBuildNumber(),
                sdkVersion = repository.getSdkInt(),
                buildFingerprint = repository.getBuildFingerprint(),
                deviceFormFactor = repository.getDeviceFormFactor(),
                securityPatch = repository.getSecurityPatch(),
                kernelVersion = repository.getKernelVersion(),
                bootloader = repository.getBootloader(),
                radioVersion = repository.getRadioVersion(),
                partitionStyle = repository.getPartitionStyle(),
                isTrebleSupported = repository.isTrebleSupported()
            )
        }
    }
    private fun fetchDeviceName() {
        _uiState.update { it.copy(isLoadingName = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val modelName = repository.getDeviceModel()
            val codename = repository.getCodename()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        deviceName = modelName,
                        model = modelName,
                        codename = codename,
                        isLoadingName = false
                    )
                }
            }
        }
    }
    fun startPeriodicUpdates() {
        stopPeriodicUpdates()
        updateJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                updateDynamicInfo()
                delay(1000)
            }
        }
    }
    fun stopPeriodicUpdates() {
        updateJob?.cancel()
        updateJob = null
    }
    private suspend fun updateDynamicInfo() {
        val context = getApplication<Application>()
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        var status = BiometricStatus.UNKNOWN
        var testable = false
        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                status = BiometricStatus.SUPPORTED_AND_ENROLLED
                testable = true
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                status = BiometricStatus.SUPPORTED_NOT_ENROLLED
                testable = false
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                status = BiometricStatus.UNSUPPORTED
                testable = false
            }
        }
        val uptime = formatUptime(SystemClock.elapsedRealtime())
        withContext(Dispatchers.Main) {
            _uiState.update {
                it.copy(
                    biometricStatus = status,
                    isFingerprintTestable = testable,
                    uptime = uptime
                )
            }
        }
    }
    private fun formatUptime(elapsedMillis: Long): String {
        val totalSeconds = elapsedMillis / 1000
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val hms = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        return if (days > 0) "${days}d $hms" else hms
    }
    @SuppressLint("PrivateApi")
    fun getSystemProperty(key: String): String? {
        return try {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, key) as? String
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun onAndroidVersionClicked() {
        val currentTime = SystemClock.uptimeMillis()
        easterEggHandlerJob?.cancel()
        if (currentTime - lastClickTime < 500) {
            clickCount++
        } else {
            clickCount = 1
        }
        lastClickTime = currentTime
        if (clickCount >= 3) {
            performEasterEggAction()
            clickCount = 0
        } else {
            easterEggHandlerJob = viewModelScope.launch {
                delay(1000.milliseconds)
            }
        }
    }
    private fun performEasterEggAction() {
        val componentName = when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.BAKLAVA -> ComponentName("com.android.egg", "com.android.egg.landroid.MainActivity")
            Build.VERSION_CODES.VANILLA_ICE_CREAM -> ComponentName("com.android.egg", "com.android.egg.landroid.MainActivity")
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> ComponentName("com.android.egg", "com.android.egg.landroid.MainActivity")
            Build.VERSION_CODES.TIRAMISU -> ComponentName("com.android.egg", "com.android.egg.ComponentActivationActivity")
            Build.VERSION_CODES.S_V2, Build.VERSION_CODES.S -> ComponentName("com.android.egg", "com.android.egg.PlatLogoActivity")
            Build.VERSION_CODES.R -> ComponentName("com.android.egg", "com.android.egg.neko.NekoActivationActivity")
            Build.VERSION_CODES.Q -> ComponentName("com.android.egg", "com.android.egg.quares.QuaresActivity")
            Build.VERSION_CODES.P -> ComponentName("com.android.egg", "com.android.egg.paint.PaintActivity")
            else -> null
        }
        if (componentName != null) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = componentName
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val pm = getApplication<Application>().packageManager
                if (intent.resolveActivity(pm) != null) {
                    viewModelScope.launch { _action.send(DefaultInfoAction.LaunchEasterEgg(intent)) }
                } else {
                    viewModelScope.launch { _action.send(DefaultInfoAction.ShowToast(ToastType.EASTER_EGG_NOT_FOUND)) }
                }
            } catch (e: Exception) {
                viewModelScope.launch { _action.send(DefaultInfoAction.ShowToast(ToastType.ERROR)) }
            }
        } else {
            viewModelScope.launch { _action.send(DefaultInfoAction.ShowToast(ToastType.ERROR)) }
        }
    }
    fun handleBiometricAuthResult(success: Boolean, message: CharSequence?) {
        viewModelScope.launch {
            if (success) {
                _action.send(DefaultInfoAction.ShowToast(ToastType.SUCCESS))
            } else {
                _action.send(DefaultInfoAction.ShowToastMsg(message?.toString() ?: "Failed"))
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
        stopPeriodicUpdates()
        easterEggHandlerJob?.cancel()
    }
}
