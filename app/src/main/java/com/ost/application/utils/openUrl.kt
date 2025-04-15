package com.ost.application.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

@JvmOverloads
fun Context.openUrl(urlString: String, onException: ((Exception) -> Unit)? = null ){
    try {
        val url = Uri.parse(urlString)

        val browserSelectorIntent = Intent()
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse("http:"))

        val targetIntent = Intent()
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(url)

        targetIntent.apply {
            if (getPackage() == null && selector != browserSelectorIntent) {
                selector = browserSelectorIntent
            }
        }

        if (this !is Activity) {
            targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(targetIntent)
    }
    catch(e: Exception){
        Log.e("openUrl", e.message.toString())
        onException?.invoke(e)
    }
}