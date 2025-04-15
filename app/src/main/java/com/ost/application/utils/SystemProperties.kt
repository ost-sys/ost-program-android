package com.ost.application.utils

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("PrivateApi")
fun getSystemProperty(key: String): String? {
    return try {
        val systemPropertiesClass = Class.forName("android.os.SystemProperties")
        systemPropertiesClass.getMethod("get", String::class.java).invoke(null, key) as? String
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getDeviceTypeStringResource(context: Context): String {
    val characteristics = getSystemProperty("ro.build.characteristics")
    return when (characteristics) {
        "phone" -> "Phone"
        "tablet" -> "Tablet"
        else -> {
            val deviceType = "Device"
            if (!characteristics.isNullOrBlank()) "$deviceType ($characteristics)" else deviceType
        }
    }
}


fun getBuildNumber(): String {
    return if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
        getSystemProperty("ro.build.id") ?: ""
    } else {
        getSystemProperty("ro.system.build.id") ?: ""
    }
}