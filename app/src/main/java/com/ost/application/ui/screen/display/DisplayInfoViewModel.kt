package com.ost.application.ui.screen.display

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.InputDevice
import android.view.WindowManager
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import com.ost.application.ui.screen.display.test.BurnInRecoveryActivity
import com.ost.application.ui.screen.display.test.PixelTestActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

data class DisplayInfoUiState(
    val resolution: String = "N/A",
    val refreshRate: String = "N/A",
    val dpi: String = "N/A",
    val diagonal: String = "N/A",
    val orientation: String = "N/A",
    val stylusSupport: String = "N/A",
    val isLoading: Boolean = true
)

class DisplayInfoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DisplayInfoUiState())
    val uiState: StateFlow<DisplayInfoUiState> = _uiState.asStateFlow()

    private var updateJob: Job? = null

    fun startUpdates(context: Context) {
        if (updateJob?.isActive != true) {
            updateJob = createUpdateFlow(context.applicationContext)
                .flowOn(Dispatchers.Default)
                .onEach { newState ->
                    withContext(Dispatchers.Main) {
                        _uiState.value = newState.copy(isLoading = false)
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun createUpdateFlow(appContext: Context) = flow {
        while (currentCoroutineContext().isActive) {
            val newState = getDisplayInfo(appContext)
            emit(newState)
            delay(500)
        }
    }

    @SuppressLint("DiscouragedApi", "PrivateApi")
    private fun getDisplayInfo(context: Context): DisplayInfoUiState {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = windowManager?.defaultDisplay
        val metrics = context.resources.displayMetrics

        if (display == null) {
            return DisplayInfoUiState(isLoading = false)
        }

        val size = Point()
        val resolutionStr = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.currentWindowMetrics.bounds
                "${bounds.height()} x ${bounds.width()}"
            } else {
                display.javaClass.getMethod("getRealSize", Point::class.java).invoke(display, size)
                "${size.y} x ${size.x}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            display.getMetrics(metrics)
            "${metrics.heightPixels} x ${metrics.widthPixels}"
        }

        val refreshRate = display.refreshRate
        val refreshRateStr = "${refreshRate.toInt()} ${context.getString(R.string.hz)}"

        val densityDpi = metrics.densityDpi
        val dpiStr = "$densityDpi ${context.getString(R.string.dpi)}"

        val diagonalStr = "${getDisplaySize(windowManager, display)} ${context.getString(R.string.inches)}"

        val orientation = context.resources.configuration.orientation
        val orientationStr = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            context.getString(R.string.portrait)
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            context.getString(R.string.landscape)
        } else {
            "N/A"
        }

        val stylusSupportStr = if (hasStylusSupport()) {
            context.getString(R.string.support)
        } else {
            context.getString(R.string.unsupport)
        }

        return DisplayInfoUiState(
            resolution = resolutionStr,
            refreshRate = refreshRateStr,
            dpi = dpiStr,
            diagonal = diagonalStr,
            orientation = orientationStr,
            stylusSupport = stylusSupportStr,
            isLoading = false
        )
    }

    @SuppressLint("DiscouragedApi", "PrivateApi")
    private fun getDisplaySize(windowManager: WindowManager?, display: Display?): String {
        if (windowManager == null || display == null) return "N/A"

        return try {
            val metrics = DisplayMetrics()
            display.getMetrics(metrics)

            val widthPixels: Int
            val heightPixels: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.currentWindowMetrics.bounds
                widthPixels = bounds.width()
                heightPixels = bounds.height()
            } else {
                val realSize = Point()
                display.javaClass.getMethod("getRealSize", Point::class.java).invoke(display, realSize)
                widthPixels = realSize.x
                heightPixels = realSize.y
            }

            val x = (widthPixels / metrics.xdpi.toDouble()).pow(2.0)
            val y = (heightPixels / metrics.ydpi.toDouble()).pow(2.0)
            String.format(Locale.US, "%.2f", sqrt(x + y))
        } catch (e: Exception) {
            e.printStackTrace()
            "N/A"
        }
    }

    private fun hasStylusSupport(): Boolean {
        return try {
            val deviceIds = InputDevice.getDeviceIds()
            deviceIds.any { id ->
                val device = InputDevice.getDevice(id)
                device != null && device.supportsSource(InputDevice.SOURCE_STYLUS)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun onCheckPixelsClicked(context: Context) {
        val intent = Intent(context, PixelTestActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(context, intent, null)
    }

    fun onFixPixelsClicked(context: Context) {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val totalDuration = preferences.getInt("total_duration", 30)
        val noiseDuration = preferences.getInt("noise_duration", 1)
        val horizontalDuration = preferences.getInt("horizontal_duration", 1)
        val verticalDuration = preferences.getInt("vertical_duration", 1)
        val blackWhiteNoiseDuration = preferences.getInt("black_white_noise_duration", 1)

        val intent = Intent(context, BurnInRecoveryActivity::class.java).apply {
            putExtra("totalDuration", totalDuration)
            putExtra("noiseDuration", noiseDuration)
            putExtra("horizontalDuration", horizontalDuration)
            putExtra("verticalDuration", verticalDuration)
            putExtra("blackWhiteNoiseDuration", blackWhiteNoiseDuration)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(context, intent, null)
    }

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
    }
}