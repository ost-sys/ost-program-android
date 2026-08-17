package com.ost.application.util
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object ThumbnailCache {
    private const val TAG = "ThumbnailCache"
    private const val THUMB_SIZE = 320
    private const val DISK_DIR = "thumbs"
    private const val MEMORY_CACHE_BYTES = 8 * 1024 * 1024
    val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "bmp", "webp", "heic")
    val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "ogg", "aac", "flac", "opus")
    private val memoryCache = object : LruCache<String, Bitmap>(MEMORY_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val knownEmpty: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    suspend fun load(context: Context, file: File): Bitmap? = withContext(Dispatchers.IO) {
        val ext = file.extension.lowercase(Locale.ROOT)
        val isImage = ext in IMAGE_EXTENSIONS
        val isAudio = ext in AUDIO_EXTENSIONS
        if (file.isDirectory || (!isImage && !isAudio)) return@withContext null
        val key = "${file.absolutePath}:${file.lastModified()}:${file.length()}"
        memoryCache.get(key)?.let { return@withContext it }
        if (key in knownEmpty) return@withContext null
        val diskFile = File(diskDir(context), md5(key) + ".jpg")
        if (diskFile.exists()) {
            BitmapFactory.decodeFile(diskFile.absolutePath)?.let {
                memoryCache.put(key, it)
                return@withContext it
            }
        }
        val bitmap = try {
            if (isImage) decodeImageThumb(file) else extractAudioArt(file)
        } catch (e: Exception) {
            Log.w(TAG, "Thumbnail failed for ${file.name}: ${e.message}")
            null
        }
        if (bitmap == null) {
            knownEmpty.add(key)
            return@withContext null
        }
        runCatching {
            FileOutputStream(diskFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        }
        memoryCache.put(key, bitmap)
        bitmap
    }

    suspend fun loadFromUri(context: Context, cacheKey: String, uri: android.net.Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            memoryCache.get(cacheKey)?.let { return@withContext it }
            if (cacheKey in knownEmpty) return@withContext null
            val diskFile = File(diskDir(context), md5(cacheKey) + ".jpg")
            if (diskFile.exists()) {
                BitmapFactory.decodeFile(diskFile.absolutePath)?.let {
                    memoryCache.put(cacheKey, it)
                    return@withContext it
                }
            }
            val bitmap = try {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    ?.let { scaleDownIfNeeded(it) }
            } catch (e: Exception) {
                null
            }
            if (bitmap == null) {
                knownEmpty.add(cacheKey)
                return@withContext null
            }
            runCatching {
                FileOutputStream(diskFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            }
            memoryCache.put(cacheKey, bitmap)
            bitmap
        }
    private fun scaleDownIfNeeded(src: Bitmap): Bitmap {
        val maxDim = maxOf(src.width, src.height)
        if (maxDim <= THUMB_SIZE * 2) return src
        val scale = THUMB_SIZE * 2f / maxDim
        return Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
    }
    private fun decodeImageThumb(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }
    private fun extractAudioArt(file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.embeddedPicture?.let { bytes ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        } finally {
            runCatching { retriever.release() }
        }
    }
    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / (sampleSize * 2) >= THUMB_SIZE && height / (sampleSize * 2) >= THUMB_SIZE) {
            sampleSize *= 2
        }
        return sampleSize
    }
    private fun diskDir(context: Context): File =
        File(context.cacheDir, DISK_DIR).apply { mkdirs() }
    private fun md5(value: String): String =
        MessageDigest.getInstance("MD5").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
