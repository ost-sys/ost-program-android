package com.ost.application.core.share

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class LocalSendFileSender(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onStatusUpdate: (String) -> Unit
) {
    private val TAG = "LocalSendFileSender"
    private val client = LocalSendClient()

    private val _isSendingActive = MutableStateFlow(false)
    val isSendingActive: StateFlow<Boolean> = _isSendingActive.asStateFlow()

    private val _transferProgress = MutableStateFlow<Int?>(null)
    val transferProgress: StateFlow<Int?> = _transferProgress.asStateFlow()

    private val _transferFileStatus = MutableStateFlow<String?>(null)
    val transferFileStatus: StateFlow<String?> = _transferFileStatus.asStateFlow()

    private var sendJob: Job? = null
    private var activeSessionId: String? = null
    private var activeIp: String? = null
    private var activePort: Int = 0

    private data class FileEntry(
        val id: String,
        val uri: Uri,
        val fileName: String,
        val size: Long,
        val mimeType: String
    )

    fun sendFiles(device: DiscoveredDevice, uris: List<Uri>) {
        val ip = device.ipAddress?.hostAddress
        val port = device.port
        if (ip == null || port <= 0) {
            onStatusUpdate("Error: Device details missing.")
            return
        }
        if (sendJob?.isActive == true) {
            onStatusUpdate("A transfer is already in progress.")
            return
        }

        _isSendingActive.value = true
        _transferProgress.value = 0
        _transferFileStatus.value = null

        sendJob = scope.launch {
            try {

                onStatusUpdate("Preparing files...")
                val entries = mutableListOf<FileEntry>()
                var totalBytes = 0L
                for (uri in uris) {
                    if (!isActive) throw CancellationException("Cancelled during preparation")
                    val (name, size) = resolveFileInfo(uri) ?: run {
                        onStatusUpdate("Error: Failed to read file info for $uri")
                        return@launch
                    }
                    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    entries.add(FileEntry(UUID.randomUUID().toString(), uri, name, size, mime))
                    totalBytes += size
                }
                if (entries.isEmpty()) {
                    onStatusUpdate("Error: No files to send.")
                    return@launch
                }

                val fingerprint = LocalSendCertificateHelper.getOrGenerateKeyStore().second
                val selfInfo = LocalSendInfo(
                    alias = Build.MODEL,
                    version = "2.1",
                    deviceModel = Build.MODEL,
                    deviceType = LocalSendDeviceType.MOBILE.value,
                    fingerprint = fingerprint,
                    port = 53317,
                    protocol = "https",
                    download = false
                )

                onStatusUpdate("Connecting to ${device.name}...")
                val filesMap = entries.associate { e ->
                    val fileType = when {
                        e.mimeType.startsWith("image/") || e.fileName.endsWith(".jpg", true) || e.fileName.endsWith(".jpeg", true) || e.fileName.endsWith(".png", true) || e.fileName.endsWith(".webp", true) || e.fileName.endsWith(".gif", true) || e.fileName.endsWith(".heic", true) -> "image"
                        e.mimeType.startsWith("video/") || e.fileName.endsWith(".mp4", true) || e.fileName.endsWith(".mkv", true) || e.fileName.endsWith(".mov", true) || e.fileName.endsWith(".webm", true) -> "video"
                        e.mimeType == "application/pdf" || e.fileName.endsWith(".pdf", true) -> "pdf"
                        e.mimeType.startsWith("text/") || e.fileName.endsWith(".txt", true) || e.fileName.endsWith(".md", true) || e.fileName.endsWith(".json", true) || e.fileName.endsWith(".xml", true) -> "text"
                        else -> "other"
                    }
                    e.id to LocalSendFileMeta(
                        id = e.id,
                        fileName = e.fileName,
                        size = e.size,
                        fileType = fileType
                    )
                }
                val prepareRequest = LocalSendPrepareRequest(info = selfInfo, files = filesMap)
                val prepareResponse = client.prepareUpload(ip, port, prepareRequest)

                if (prepareResponse == null) {
                    onStatusUpdate("Error: Transfer rejected or connection failed.")
                    return@launch
                }

                val sessionId = prepareResponse.sessionId
                activeSessionId = sessionId
                activeIp = ip
                activePort = port

                onStatusUpdate("Sending ${entries.size} file(s)...")
                _transferProgress.value = 0
                val totalFinal = totalBytes
                val bytesSent = AtomicLong(0)
                val perFileBytesSent = ConcurrentHashMap<String, Long>()

                val uploadJobs = entries.map { entry ->
                    val token = prepareResponse.files[entry.id]
                    if (token == null) {

                        Log.d(TAG, "No token for ${entry.fileName}, skipping (already at receiver)")
                        return@map async { true }
                    }
                    async {
                        if (!isActive) throw CancellationException("Upload cancelled")
                        _transferFileStatus.value = entry.fileName
                        client.uploadFile(
                            context = context,
                            ip = ip,
                            port = port,
                            sessionId = sessionId,
                            fileId = entry.id,
                            token = token,
                            uri = entry.uri,
                            onProgress = { sentForFile ->
                                val prev = perFileBytesSent.put(entry.id, sentForFile) ?: 0L
                                val delta = sentForFile - prev
                                val totalNow = bytesSent.addAndGet(delta)
                                if (totalFinal > 0) {
                                    val pct = ((totalNow * 100) / totalFinal).toInt().coerceIn(0, 100)
                                    _transferProgress.value = pct
                                }
                            }
                        )
                    }
                }

                val results = uploadJobs.awaitAll()
                val allOk = results.all { it }

                if (allOk) {
                    _transferProgress.value = 100
                    onStatusUpdate("SENT: ${entries.size} file(s)")
                    Log.i(TAG, "Transfer complete: ${entries.size} files to ${device.name}")
                } else {
                    onStatusUpdate("ERROR: Some files failed to upload.")
                }

            } catch (e: CancellationException) {
                Log.w(TAG, "Transfer cancelled: ${e.message}")
                cancelSession()
                onStatusUpdate("CANCELED")
            } catch (e: Exception) {
                Log.e(TAG, "Transfer error: ${e.message}", e)
                onStatusUpdate("ERROR: ${e.message}")
            } finally {
                _isSendingActive.value = false
                _transferProgress.value = null
                _transferFileStatus.value = null
                activeSessionId = null
                activeIp = null
                activePort = 0
            }
        }
    }

    fun cancelTransfer() {
        sendJob?.cancel(CancellationException("Cancelled by user"))
    }

    private fun cancelSession() {
        val sid = activeSessionId ?: return
        val ip = activeIp ?: return
        val port = activePort
        scope.launch {
            try { client.cancelSession(ip, port, sid) } catch (_: Exception) {}
        }
    }

    private fun resolveFileInfo(uri: Uri): Pair<String, Long>? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE),
                null, null, null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else -1L
                if (name == null || size < 0) return null
                val cleanName = java.io.File(name).name
                    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    .trim().take(240).ifBlank { "file_${System.currentTimeMillis()}" }
                cleanName to size
            }
        } catch (e: Exception) {
            Log.e(TAG, "resolveFileInfo error for $uri: ${e.message}")
            null
        }
    }

    fun shutdown() {
        cancelTransfer()
    }
}
