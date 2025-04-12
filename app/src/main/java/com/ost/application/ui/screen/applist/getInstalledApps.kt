package com.ost.application.ui.screen.applist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.Locale
import kotlin.math.abs

suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val packages = try {
        pm.getInstalledApplications(PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES)
    } catch (e: Exception) {
        Log.e("AppListUtils", "Failed to get installed applications", e)
        emptyList()
    }

    val appList = mutableListOf<AppInfo>()

    packages.forEach { appInfo ->
        try {
            val appName = pm.getApplicationLabel(appInfo).toString()
            val packageName = appInfo.packageName
            val icon = try {
                pm.getApplicationIcon(appInfo)
            } catch (e: Exception) {
                Log.w("AppListUtils", "Failed to load icon for $packageName", e)
                null
            }

            val sourceDir = appInfo.sourceDir ?: ""
            var sizeBytes = 0L

            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            if (sourceDir.isNotEmpty()) {
                try {
                    val file = File(sourceDir)
                    if (file.exists() && file.isFile) {
                        sizeBytes = file.length()
                    } else {
                        Log.w("AppListUtils", "APK file not found or not a file for $packageName at $sourceDir. Size set to 0.")
                    }
                } catch (e: SecurityException) {
                    Log.e("AppListUtils", "SecurityException getting size for $packageName: ${e.message}. Size set to 0.")
                } catch (e: Exception) {
                    Log.e("AppListUtils", "Error getting size for $packageName: ${e.message}. Size set to 0.")
                }
            } else {
                Log.w("AppListUtils", "SourceDir is empty for $packageName. Size set to 0.")
            }

            appList.add(AppInfo(appName, packageName, icon, sizeBytes, sourceDir, isSystemApp))

        } catch (e: PackageManager.NameNotFoundException) {
            Log.w("AppListUtils", "NameNotFoundException processing package (maybe uninstalled?): ${appInfo.packageName}", e)
        } catch (e: Exception) {
            Log.e("AppListUtils", "Error processing package: ${appInfo.packageName}", e)
        }
    }

    appList.sortBy { it.name.lowercase() }
    Log.d("AppListUtils", "Found ${appList.size} applications.")
    appList
}

fun formatBytes(bytes: Long): String {
    val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes)
    if (absB < 1024) {
        return "$bytes B"
    }
    var value = absB
    val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
    var i = 40
    while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
        value = value shr 10
        ci.next()
        i -= 10
    }
    value *= java.lang.Long.signum(bytes).toLong()
    return String.format(Locale.ROOT, "%.1f %cB", value / 1024.0, ci.current())
}