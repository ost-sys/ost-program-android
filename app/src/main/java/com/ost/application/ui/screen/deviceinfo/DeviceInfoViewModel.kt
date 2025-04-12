package com.ost.application.ui.screen.deviceinfo

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaredrummler.android.device.DeviceName
import com.ost.application.R
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Stable
data class DefaultInfoUiState(
    val deviceName: String = "Loading...",
    val model: String = "---",
    val codename: String = "---",
    val androidVersion: String = Build.VERSION.RELEASE,
    val brand: String = Build.BRAND,
    val board: String = Build.BOARD,
    val buildNumber: String = "---",
    val sdkVersion: String = Build.VERSION.SDK,
    val deviceType: String = "---",
    val deviceTypeIconRes: Int = R.drawable.ic_phone_android_24dp,
    val ramInfo: String = "---",
    val romInfo: String = "---",
    val buildFingerprint: String = Build.FINGERPRINT,
    val fingerprintStatus: String = "Checking...",
    val isFingerprintTestable: Boolean = false,
    val isLoadingName: Boolean = true
)

open class DefaultInfoAction {
    object ShowBiometricPrompt : DefaultInfoAction()
    data class LaunchEasterEgg(val intent: Intent) : DefaultInfoAction()
    data class ShowToast(val messageResId: Int) : DefaultInfoAction()
    data class ShowToastMsg(val message: String) : DefaultInfoAction()
}

class DeviceInfoViewModel(application: Application) : AndroidViewModel(application) {

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
        val context = getApplication<Application>()
        val deviceChar = getSystemProperty("ro.build.characteristics") ?: "unknown"
        val deviceTypeString = when (deviceChar) {
            "phone" -> context.getString(R.string.phone)
            "tablet" -> context.getString(R.string.tablet)
            else -> context.getString(R.string.device) + " ($deviceChar)"
        }
        val deviceIcon = when (deviceChar) {
            "phone" -> R.drawable.ic_device_24dp
            "tablet" -> R.drawable.ic_tablet_24dp
            else -> R.drawable.ic_phone_android_24dp
        }

        _uiState.update {
            it.copy(
                buildNumber = getBuildNumber(),
                deviceType = deviceTypeString,
                deviceTypeIconRes = deviceIcon
            )
        }
    }

    private fun fetchDeviceName() {
        _uiState.update { it.copy(isLoadingName = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deviceInfo = suspendCoroutine<DeviceName.DeviceInfo> { continuation ->
                    DeviceName.with(getApplication()).request { info, error ->
                        if (error != null) {
                            continuation.resumeWithException(error)
                        } else {
                            continuation.resume(info)
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            deviceName = deviceInfo.marketName ?: deviceInfo.name,
                            model = deviceInfo.model,
                            codename = deviceInfo.codename,
                            isLoadingName = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DefaultInfoViewModel", "Error fetching device name", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            deviceName = Build.MODEL,
                            model = Build.MODEL,
                            codename = Build.DEVICE,
                            isLoadingName = false
                        )
                    }
                    _action.send(DefaultInfoAction.ShowToastMsg("Error getting device name: ${e.message}"))
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

    @SuppressLint("DefaultLocale")
    private suspend fun updateDynamicInfo() {
        val context = getApplication<Application>()
        val memInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(memInfo)
        val availMemoryGB = memInfo.availMem / (1024.0 * 1024 * 1024)
        val totalMemoryGB = memInfo.totalMem / (1024.0 * 1024 * 1024)
        val ramString = "${context.getString(R.string.available)}: ${String.format("%.1f", availMemoryGB)} ${context.getString(R.string.gb)}\n" +
                "${context.getString(R.string.total)}: ${String.format("%.1f", totalMemoryGB)} ${context.getString(R.string.gb)}"

        val internalPath = Environment.getDataDirectory().path
        val internalStatFs = StatFs(internalPath)
        val totalBytes = internalStatFs.blockCountLong * internalStatFs.blockSizeLong
        val availableBytes = internalStatFs.availableBlocksLong * internalStatFs.blockSizeLong
        val usedBytes = totalBytes - availableBytes

        val totalGB = totalBytes / (1024.0 * 1024 * 1024)
        val availableGB = availableBytes / (1024.0 * 1024 * 1024)
        val usedGB = usedBytes / (1024.0 * 1024 * 1024)

        val romString = "${context.getString(R.string.total)}: ${String.format("%.1f", totalGB)} ${context.getString(R.string.gb)}\n" +
                "${context.getString(R.string.available)}: ${String.format("%.1f", availableGB)} ${context.getString(R.string.gb)}\n" +
                "${context.getString(R.string.used)}: ${String.format("%.1f", usedGB)} ${context.getString(R.string.gb)}"


        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)

        var fingerprintString = context.getString(R.string.unsupport)
        var testable = false

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS || canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            fingerprintString = context.getString(R.string.support)
            if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                fingerprintString += "\n" + context.getString(R.string.fingers_not_registered)
                testable = false
            } else {
                fingerprintString += "\n" + context.getString(R.string.fingers_registered)
                testable = true
            }
        } else if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            fingerprintString = context.getString(R.string.unsupport)
            testable = false
        } else {
            fingerprintString = context.getString(R.string.unknown)
            testable = false
        }

        withContext(Dispatchers.Main) {
            _uiState.update {
                it.copy(
                    ramInfo = ramString,
                    romInfo = romString,
                    fingerprintStatus = fingerprintString,
                    isFingerprintTestable = testable
                )
            }
        }
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

    fun getBuildNumber(): String {
        val propName = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            "ro.build.id"
        } else {
            "ro.system.build.id"
        }
        return getSystemProperty(propName) ?: Build.ID
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
                delay(1000)
                clickCount = 0
            }
        }
    }

    private fun performEasterEggAction() {
        val componentName = when (Build.VERSION.SDK_INT) {
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
                    viewModelScope.launch { _action.send(DefaultInfoAction.ShowToast(R.string.easter_egg_not_founded)) }
                }
            } catch (e: Exception) {
                viewModelScope.launch { _action.send(DefaultInfoAction.ShowToast(R.string.error)) }
            }
        } else {
            viewModelScope.launch { _action.send(DefaultInfoAction.ShowToast(R.string.error)) }
        }
    }


    fun onFingerprintTestClicked() {
        if (_uiState.value.isFingerprintTestable) {
            viewModelScope.launch {
                _action.send(DefaultInfoAction.ShowBiometricPrompt)
            }
        } else {
            viewModelScope.launch {
                _action.send(DefaultInfoAction.ShowToast(R.string.error))
            }
        }
    }

    fun handleBiometricAuthResult(success: Boolean, message: CharSequence?) {
        viewModelScope.launch {
            if (success) {
                _action.send(DefaultInfoAction.ShowToast(R.string.success))
            } else {
                _action.send(DefaultInfoAction.ShowToastMsg(message?.toString() ?: getApplication<Application>().getString(R.string.fail)))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPeriodicUpdates()
        easterEggHandlerJob?.cancel()
    }
}