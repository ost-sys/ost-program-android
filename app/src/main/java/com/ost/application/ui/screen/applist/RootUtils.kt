package com.ost.application.ui.screen.applist

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object RootUtils {

    private const val SU_CHECK_TIMEOUT_MS = 2000L
    private const val UNINSTALL_TIMEOUT_MS = 15000L

    val isRootAvailable: Boolean by lazy { checkRootAccess() }

    private fun checkRootAccess(): Boolean {
        val paths = arrayOf(
            "/sbin/", "/system/bin/", "/system/xbin/",
            "/data/local/xbin/", "/data/local/bin/",
            "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"
        )
        for (path in paths) {
            if (File(path + "su").exists()) {
                Log.i("RootUtils", "su binary found in $path, attempting command check.")
                return canExecuteRootCommand()
            }
        }
        Log.w("RootUtils", "su binary not found in standard paths.")
        return false
    }

    private fun canExecuteRootCommand(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exitValue = try {
                val result = process.waitFor(SU_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                if (result) process.exitValue() else null
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.w("RootUtils", "Root check interrupted.", e)
                null
            }

            Log.i("RootUtils", "su -c id exit value: $exitValue (null means timeout or interrupt)")
            exitValue == 0
        } catch (e: IOException) {
            Log.w("RootUtils", "IOException during root check via exec: ${e.message}")
            false
        } catch (e: SecurityException) {
            Log.w("RootUtils", "SecurityException during root check via exec: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("RootUtils", "Unexpected error during root check via exec", e)
            false
        } finally {
            process?.destroy()
        }
    }

    suspend fun uninstallAppRoot(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!isRootAvailable) {
            Log.e("RootUtils", "Root not available, cannot uninstall $packageName")
            return@withContext false
        }

        Log.d("RootUtils", "Attempting to uninstall $packageName with root")
        var process: Process? = null
        var os: DataOutputStream? = null
        var success = false

        val result = withTimeoutOrNull(UNINSTALL_TIMEOUT_MS) {
            try {
                process = Runtime.getRuntime().exec("su")
                os = DataOutputStream(process.outputStream)

                val command = "pm uninstall $packageName\n"
                os.writeBytes(command)
                os.flush()
                Log.d("RootUtils", "Executed command: $command")

                os.writeBytes("exit\n")
                os.flush()
                Log.d("RootUtils", "Executed command: exit")

                val exitValue = process.waitFor()
                success = exitValue == 0
                Log.i("RootUtils", "Uninstall process for $packageName exited with value: $exitValue. Success: $success")

                if (!success) {
                    logStreamOutput(process)
                }
                success
            } catch (e: IOException) {
                Log.e("RootUtils", "IOException during root uninstall of $packageName", e)
                false
            } catch (e: InterruptedException) {
                Log.e("RootUtils", "InterruptedException during root uninstall of $packageName", e)
                Thread.currentThread().interrupt()
                false
            } catch (e: Exception) {
                Log.e("RootUtils", "Generic Exception during root uninstall of $packageName", e)
                false
            } finally {
                try {
                    os?.close()
                } catch (e: IOException) {
                    Log.w("RootUtils", "Failed to close DataOutputStream", e)
                }
                process?.destroy()
                Log.d("RootUtils", "Uninstall attempt for $packageName finished in finally block.")
            }
        }

        if (result == null) {
            Log.e("RootUtils", "Uninstall operation for $packageName timed out after ${UNINSTALL_TIMEOUT_MS}ms.")
            process?.destroyForcibly()
            return@withContext false
        }

        return@withContext result
    }

    private fun logStreamOutput(process: Process?) {
        if (process == null) return
        try {
            val errorResult = process.errorStream?.bufferedReader()?.readText()
            if (!errorResult.isNullOrBlank()) {
                Log.e("RootUtils", "Uninstall error stream: $errorResult")
            }
            val outputResult = process.inputStream?.bufferedReader()?.readText()
            if (!outputResult.isNullOrBlank()) {
                Log.i("RootUtils", "Uninstall output stream: $outputResult")
            }
        } catch (readError: Exception) {
            Log.w("RootUtils", "Could not read error/output stream from process", readError)
        }
    }
}