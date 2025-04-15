package com.ost.application.util

import android.content.Context
import android.content.Intent

fun startActivity(context: Context, activityClass: Class<*>) {
    val intent = Intent(context, activityClass)
    context.startActivity(intent)
}