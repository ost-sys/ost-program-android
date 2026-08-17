package com.ost.application.core.cpu
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
data class CpuCoreInfo(
    val index: Int,
    val curFreqKhz: Long?,
    val minFreqKhz: Long?,
    val maxFreqKhz: Long?
)
data class CpuCluster(
    val coreIndices: List<Int>,
    val minFreqKhz: Long?,
    val maxFreqKhz: Long?
)
data class CpuStaticInfo(
    val socName: String?,
    val coreCount: Int,
    val abis: String,
    val is64BitProcess: Boolean,
    val clusters: List<CpuCluster>,
    val governor: String?,
    val glEsVersion: String?,
    val vulkanVersion: String?,
    val tempCelsius: Float? = null
)
object CpuInfoProvider {
    private const val CPU_SYSFS_ROOT = "/sys/devices/system/cpu"
    fun getCoreCount(): Int {
        val fromSysfs = File(CPU_SYSFS_ROOT)
            .listFiles { f -> f.isDirectory && f.name.matches(Regex("cpu\\d+")) }
            ?.size ?: 0
        return if (fromSysfs > 0) fromSysfs else Runtime.getRuntime().availableProcessors()
    }
    fun getCpuTemperatureCelsius(): Float? {
        return try {
            val thermalDir = File("/sys/class/thermal")
            if (thermalDir.exists() && thermalDir.isDirectory) {
                thermalDir.listFiles { f -> f.isDirectory && f.name.startsWith("thermal_zone") }?.forEach { zone ->
                    val type = try { File(zone, "type").readText().trim().lowercase() } catch (e: Exception) { "" }
                    if (type.contains("cpu") || type.contains("soc") || type.contains("tsens") || type.contains("mtk") || type.contains("exynos") || type == "soc-thermal") {
                        val tempStr = try { File(zone, "temp").readText().trim() } catch (e: Exception) { null }
                        val rawTemp = tempStr?.toFloatOrNull()
                        if (rawTemp != null) {
                            val celsius = if (rawTemp > 1000) rawTemp / 1000f else if (rawTemp > 200) rawTemp / 10f else rawTemp
                            if (celsius in 0f..150f) return celsius
                        }
                    }
                }
                val zone0Temp = try { File("/sys/class/thermal/thermal_zone0/temp").readText().trim().toFloatOrNull() } catch (e: Exception) { null }
                if (zone0Temp != null) {
                    val celsius = if (zone0Temp > 1000) zone0Temp / 1000f else if (zone0Temp > 200) zone0Temp / 10f else zone0Temp
                    if (celsius in 0f..150f) return celsius
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    fun readCores(): List<CpuCoreInfo> = (0 until getCoreCount()).map { i ->
        CpuCoreInfo(
            index = i,
            curFreqKhz = readSysfsLong("$CPU_SYSFS_ROOT/cpu$i/cpufreq/scaling_cur_freq"),
            minFreqKhz = readSysfsLong("$CPU_SYSFS_ROOT/cpu$i/cpufreq/cpuinfo_min_freq"),
            maxFreqKhz = readSysfsLong("$CPU_SYSFS_ROOT/cpu$i/cpufreq/cpuinfo_max_freq")
        )
    }
    fun observeCores(intervalMs: Long = 500L): Flow<List<CpuCoreInfo>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(readCores())
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)
    fun getStaticInfo(context: Context): CpuStaticInfo {
        val cores = readCores()
        return CpuStaticInfo(
            socName = getSocName(),
            coreCount = cores.size,
            abis = Build.SUPPORTED_ABIS.joinToString(", "),
            is64BitProcess = Process.is64Bit(),
            clusters = groupClusters(cores),
            governor = readSysfsString("$CPU_SYSFS_ROOT/cpu0/cpufreq/scaling_governor"),
            glEsVersion = getGlEsVersion(context),
            vulkanVersion = getVulkanVersion(context),
            tempCelsius = getCpuTemperatureCelsius()
        )
    }
    fun getSocName(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manufacturer = Build.SOC_MANUFACTURER.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
            val model = Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
            if (model != null) return listOfNotNull(manufacturer, model).joinToString(" ")
        }
        return getSystemProperty("ro.soc.model")?.takeIf { it.isNotBlank() }
            ?: getSystemProperty("ro.board.platform")?.takeIf { it.isNotBlank() }
            ?: getSystemProperty("ro.hardware")?.takeIf { it.isNotBlank() }
    }

    private fun groupClusters(cores: List<CpuCoreInfo>): List<CpuCluster> {
        return cores
            .groupBy { it.minFreqKhz to it.maxFreqKhz }
            .map { (freqs, groupCores) ->
                CpuCluster(
                    coreIndices = groupCores.map { it.index },
                    minFreqKhz = freqs.first,
                    maxFreqKhz = freqs.second
                )
            }
            .sortedBy { it.maxFreqKhz ?: 0 }
    }
    private fun getGlEsVersion(context: Context): String? {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return activityManager?.deviceConfigurationInfo?.glEsVersion
    }
    private fun getVulkanVersion(context: Context): String? {
        return try {
            val feature = context.packageManager.systemAvailableFeatures
                .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
                ?: return null
            val version = feature.version
            "${version shr 22}.${(version shr 12) and 0x3FF}"
        } catch (e: Exception) {
            null
        }
    }
    private fun readSysfsLong(path: String): Long? = readSysfsString(path)?.toLongOrNull()
    private fun readSysfsString(path: String): String? = try {
        File(path).readText().trim().ifBlank { null }
    } catch (e: Exception) {
        null
    }
    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String): String? {
        return try {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, key) as? String
        } catch (e: Exception) {
            null
        }
    }
}
