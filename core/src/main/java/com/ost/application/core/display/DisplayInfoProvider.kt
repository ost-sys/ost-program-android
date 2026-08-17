package com.ost.application.core.display
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.InputDevice
import android.view.RoundedCorner
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt
object DisplayInfoProvider {
    @SuppressLint("DiscouragedApi", "PrivateApi")
    fun getDisplayInfo(
        context: Context,
        strings: DisplayInfoStrings,
        diagonalDecimals: Int = 2
    ): DisplayInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = windowManager?.defaultDisplay ?: return DisplayInfo()
        val metrics = context.resources.displayMetrics
        return DisplayInfo(
            resolution = resolveResolution(windowManager, display, metrics),
            refreshRate = "${display.refreshRate.toInt()} ${strings.hz}",
            dpi = "${metrics.densityDpi} ${strings.dpi}",
            diagonal = "${resolveDiagonalInches(windowManager, display, diagonalDecimals)} ${strings.inches}",
            orientation = when (context.resources.configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> strings.portrait
                Configuration.ORIENTATION_LANDSCAPE -> strings.landscape
                else -> "N/A"
            },
            stylusSupport = if (hasStylusSupport()) strings.supported else strings.unsupported,
            cornerRadius = resolveCornerRadius(windowManager, strings) ?: "N/A"
        )
    }
    private fun resolveCornerRadius(windowManager: WindowManager, strings: DisplayInfoStrings): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return try {
            val insets = windowManager.currentWindowMetrics.windowInsets
            val topLeft = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius
            val topRight = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius
            val bottomLeft = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius
            val bottomRight = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius
            val all = listOfNotNull(topLeft, topRight, bottomLeft, bottomRight)
            when {
                all.isEmpty() -> null
                all.all { it == all.first() } -> "${all.first()} ${strings.px}"
                topLeft == topRight && bottomLeft == bottomRight ->
                    "↑ ${topLeft ?: 0} / ↓ ${bottomLeft ?: 0} ${strings.px}"
                else -> "${topLeft ?: 0} / ${topRight ?: 0} / ${bottomLeft ?: 0} / ${bottomRight ?: 0} ${strings.px}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun observeDisplayInfo(
        context: Context,
        strings: DisplayInfoStrings,
        intervalMs: Long = 500L,
        diagonalDecimals: Int = 2
    ): Flow<DisplayInfo> = flow {
        while (currentCoroutineContext().isActive) {
            emit(getDisplayInfo(context, strings, diagonalDecimals))
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.Default)
    @SuppressLint("DiscouragedApi", "PrivateApi")
    private fun resolveResolution(
        windowManager: WindowManager,
        display: Display,
        metrics: DisplayMetrics
    ): String = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            "${bounds.height()} x ${bounds.width()}"
        } else {
            val size = Point()
            display.javaClass.getMethod("getRealSize", Point::class.java).invoke(display, size)
            "${size.y} x ${size.x}"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        display.getMetrics(metrics)
        "${metrics.heightPixels} x ${metrics.widthPixels}"
    }
    @SuppressLint("DiscouragedApi", "PrivateApi")
    private fun resolveDiagonalInches(
        windowManager: WindowManager,
        display: Display,
        decimals: Int
    ): String = try {
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
        String.format(Locale.US, "%.${decimals}f", sqrt(x + y))
    } catch (e: Exception) {
        e.printStackTrace()
        "N/A"
    }
    private fun hasStylusSupport(): Boolean = try {
        InputDevice.getDeviceIds().any { id ->
            InputDevice.getDevice(id)?.supportsSource(InputDevice.SOURCE_STYLUS) == true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
