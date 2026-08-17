package com.ost.application.util
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
fun isRooted(): Boolean {
    val paths = arrayOf(
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/adb/magisk",
        "/su/bin/su"
    )
    if (paths.any { File(it).exists() }) return true
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val result = reader.readLine() != null
        process.destroy()
        result
    } catch (_: Throwable) {
        false
    }
}
