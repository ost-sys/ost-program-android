@file:Suppress("NOTHING_TO_INLINE")

package com.ost.application.data.model

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

    fun File.createShareIntent(context: Context): Intent? {
        return listOf(this).createShareIntent(context)
    }
    fun List<File>.createShareIntent(context: Context): Intent? { // Возвращаем Intent?
        // Получаем URI для каждого файла
        val contentUris = mapNotNull { f -> // mapNotNull, чтобы пропустить ошибки getFileUri
            try {
                f.getFileUri(context)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to get URI for file: ${f.path}", e)
                null // Пропускаем файл, если URI не получен
            }
        }

        if (contentUris.isEmpty()) {
            Log.e(TAG, "No valid file URIs to share.")
            return null // Возвращаем null, если нет валидных URI
        }

        // Создаем базовый Intent
        val intent = Intent().apply {
            // Устанавливаем флаг доступа к URI
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Устанавливаем MIME тип
            type = MIME_TYPE_VCARD

            // Настраиваем Intent в зависимости от количества файлов
            if (contentUris.size == 1) {
                action = ACTION_SEND // Один файл - ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, contentUris[0])
                Log.d(TAG, "Created ACTION_SEND intent for URI: ${contentUris[0]}")
            } else {
                action = Intent.ACTION_SEND_MULTIPLE // Несколько файлов - ACTION_SEND_MULTIPLE
                // Помещаем список URI в ArrayList
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(contentUris))
                Log.d(TAG, "Created ACTION_SEND_MULTIPLE intent for ${contentUris.size} URIs")
            }
        }
        return intent // Возвращаем настроенный Intent
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