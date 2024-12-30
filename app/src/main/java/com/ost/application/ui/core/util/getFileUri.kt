@file:Suppress("NOTHING_TO_INLINE")

package com.ost.application.ui.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
inline fun File.getFileUri(context: Context): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)