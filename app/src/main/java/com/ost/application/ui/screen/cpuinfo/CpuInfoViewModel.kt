package com.ost.application.ui.screen.cpuinfo

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.regex.Pattern
import kotlin.math.roundToInt

@Stable
data class CpuInfoUiState(
    val cpuName: String = "Loading...",
    val manufacturer: String? = null,
    val abiString: String = "---",
    val coresString: String = "---",
    val clockSpeedString: String = "---",
    val isLoading: Boolean = true
)

class CpuInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CpuInfoUiState())
    val uiState: StateFlow<CpuInfoUiState> = _uiState.asStateFlow()

    init {
        loadCpuInfo()
    }

    private fun loadCpuInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val info = getCpuInfoData()
            _uiState.update {
                it.copy(
                    cpuName = info.cpuName,
                    manufacturer = info.manufacturer,
                    abiString = info.abiString,
                    coresString = info.coresString,
                    clockSpeedString = info.clockSpeedString,
                    isLoading = false
                )
            }
        }
    }

    private data class CpuInfoData(
        val cpuName: String,
        val manufacturer: String?,
        val abiString: String,
        val coresString: String,
        val clockSpeedString: String
    )

    private suspend fun getCpuInfoData(): CpuInfoData = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val cpuName = readCpuName(context)
        val manufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else null
        val abis = Build.SUPPORTED_ABIS?.joinToString(", ") ?: "---"
        val cores = getNumberOfCores().toString()
        val minFreq = readCpuFreq("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq")
        val maxFreq = readCpuFreq("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
        val clockSpeed = formatClockSpeed(minFreq, maxFreq, context)

        CpuInfoData(cpuName, manufacturer, abis, cores, clockSpeed)
    }


    private fun readCpuName(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.SOC_MODEL != Build.UNKNOWN) {
            return Build.SOC_MODEL
        } else {
            return try {
                InputStreamReader(FileInputStream("/proc/cpuinfo")).buffered().useLines { lines ->
                    lines.mapNotNull { line ->
                        if (line.startsWith("Hardware", true)) {
                            line.split(":").getOrNull(1)?.trim()
                        } else if (line.startsWith("model name", true)) {
                            line.split(":").getOrNull(1)?.trim()
                        } else {
                            null
                        }
                    }.firstOrNull() ?: context.getString(R.string.unknown_cpu)
                }
            } catch (e: IOException) {
                Log.e("CpuInfoViewModel", "Error reading /proc/cpuinfo", e)
                context.getString(R.string.error_cpu_info)
            }
        }
    }


    private fun getNumberOfCores(): Int {
        return try {
            class CpuFilter : FileFilter {
                override fun accept(pathname: File): Boolean {
                    return Pattern.matches("cpu[0-9]+", pathname.name)
                }
            }
            File("/sys/devices/system/cpu/").listFiles(CpuFilter())?.size ?: 1
        } catch (e: Exception) {
            Log.w("CpuInfoViewModel", "Could not read CPU cores from /sys/, falling back to Runtime.availableProcessors()", e)
            Runtime.getRuntime().availableProcessors().takeIf { it > 0 } ?: 1
        }
    }

    private fun readCpuFreq(filePath: String): Long? {
        return try {
            File(filePath).bufferedReader().use { it.readLine()?.trim()?.toLongOrNull() }
        } catch (e: IOException) {
            Log.w("CpuInfoViewModel", "Failed to read CPU freq from $filePath", e)
            null
        } catch (e: SecurityException) {
            Log.w("CpuInfoViewModel", "Permission denied reading CPU freq from $filePath", e)
            null
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatClockSpeed(minKHz: Long?, maxKHz: Long?, context: Context): String {
        if (minKHz == null && maxKHz == null) return "---"

        val minMHz = minKHz?.let { (it / 1000.0).roundToInt() }
        val maxGHz = maxKHz?.let { it / 1000000.0 }

        return when {
            minMHz != null && maxGHz != null -> "$minMHz ${context.getString(R.string.mhz)} - ${String.format("%.2f", maxGHz)} ${context.getString(R.string.ghz)}"
            maxGHz != null -> "~ ${String.format("%.2f", maxGHz)} ${context.getString(R.string.ghz)}"
            minMHz != null -> "~ $minMHz ${context.getString(R.string.mhz)}"
            else -> "---"
        }
    }
}