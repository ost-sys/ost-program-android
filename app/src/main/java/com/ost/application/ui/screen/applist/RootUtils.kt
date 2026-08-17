package com.ost.application.ui.screen.applist
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
object RootUtils {
    private const val UNINSTALL_TIMEOUT_MS = 15000L
    val isRootAvailable: Boolean by lazy { checkRootAccess() }
    private fun checkRootAccess(): Boolean {
        val paths = arrayOf(
            "/sbin/", "/system/bin/", "/system/xbin/",
            "/data/local/xbin/", "/data/local/bin/",
            "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"
        )
        val hasBinary = paths.any { File(it + "su").exists() }
        if (!hasBinary) {
            Log.w("RootUtils", "su binary not found in standard paths.")
            return false
        }
        return try {
            Shell.isAppGrantedRoot() == true || Shell.cmd("id").exec().isSuccess
        } catch (e: Exception) {
            Log.e("RootUtils", "Error checking root access via Shell", e)
            false
        }
    }
    suspend fun uninstallAppRoot(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!isRootAvailable) {
            Log.e("RootUtils", "Root not available, cannot uninstall $packageName")
            return@withContext false
        }
        Log.d("RootUtils", "Attempting to uninstall $packageName with root")
        val result = withTimeoutOrNull(UNINSTALL_TIMEOUT_MS) {
            try {
                val shellResult = Shell.cmd("pm uninstall $packageName").exec()
                val success = shellResult.isSuccess
                Log.i("RootUtils", "Uninstall process for $packageName exitCode: ${shellResult.code}, success: $success")
                if (!success && shellResult.err.isNotEmpty()) {
                    Log.e("RootUtils", "Uninstall error stream: ${shellResult.err.joinToString("\n")}")
                }
                success
            } catch (e: Exception) {
                Log.e("RootUtils", "Exception during root uninstall of $packageName", e)
                false
            }
        }
        if (result == null) {
            Log.e("RootUtils", "Uninstall operation for $packageName timed out after ${UNINSTALL_TIMEOUT_MS}ms.")
            return@withContext false
        }
        return@withContext result
    }
}
