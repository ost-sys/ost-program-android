@file:Suppress("NOTHING_TO_INLINE")

package com.ost.application.ui.core.util

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.pm.PackageManager
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import java.io.File

object SharingUtils {

    private const val SAMSUNG_QUICK_SHARE_PACKAGE = "com.samsung.android.app.sharelive"
    private const val MIME_TYPE_TEXT = "text/plain"
    private const val MIME_TYPE_VCARD = "text/x-vcard"
    private const val TAG = "SharingUtils"

    inline fun File.share(context: Context) {
        listOf(this).share(context)
    }

    fun List<File>.share(context: Context) {
        val contentUris = map { f -> f.getFileUri(context)}

        if (contentUris.isEmpty()){
            Log.e(TAG, "No file to share.")
            return
        }

        context.createBaseIntent().apply {
            this.type = MIME_TYPE_VCARD
            if (contentUris.size == 1){
                putExtra(Intent.EXTRA_STREAM, contentUris[0] )
            }else{
                action = Intent.ACTION_SEND_MULTIPLE
                putExtra(Intent.EXTRA_STREAM,  ArrayList(contentUris))
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            start(context)
        }
    }

    fun String.share(context: Context) {
        context.createBaseIntent().apply {
            type = MIME_TYPE_TEXT
            putExtra(Intent.EXTRA_TEXT, this@share)
            start(context)
        }
    }

    private inline fun Context.createBaseIntent() =
        Intent().apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            action = ACTION_SEND
            if (isSamsungQuickShareAvailable()) {
                `package` = SAMSUNG_QUICK_SHARE_PACKAGE
            }
        }

    private inline fun Intent.start(context: Context){
        try {
            context.startActivity(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start activity with specific package: ${e.message}")
            // Fallback to default chooser if specific package fails
            `package` = null
            context.startActivity(Intent.createChooser(this, "Share via"))
        }
    }

    fun Context.isSamsungQuickShareAvailable(): Boolean {
        return try {
            packageManager.getPackageInfo(SAMSUNG_QUICK_SHARE_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }.also {
            Log.i(TAG, "isSamsungQuickShareAvailable: $it")
        }
    }


    // Note: This does not actually returns a result to the caller
    // ActivityResultLauncher. The latter will always receive RESULT_CANCELLED.
    fun File.shareForResult(context: Context,
                            resultLauncher: ActivityResultLauncher<Intent>) {

        context.createBaseIntent().apply {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            putExtra(Intent.EXTRA_STREAM, getFileUri(context))
            resultLauncher.launch(this)
        }
    }

}