package com.ost.application.util

import android.annotation.SuppressLint

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