package com.ost.application.ui.screen.share

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import com.ost.application.R
import com.ost.application.ui.screen.share.NotificationHelper.formatFileSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

@SuppressLint("MissingPermission")
class FileSender(
    private val context: Context,
    private val scope: CoroutineScope,
    private val statusUpdateCallback: (String) -> Unit
) {
    private var transferJob: Job? = null

    private val _isSendingActive = MutableStateFlow(false)
    val isSendingActive: StateFlow<Boolean> = _isSendingActive.asStateFlow()

    private val _transferProgress = MutableStateFlow<Int?>(null)
    val transferProgress: StateFlow<Int?> = _transferProgress.asStateFlow()

    private val _transferFileStatus = MutableStateFlow<String?>(null)
    val transferFileStatus: StateFlow<String?> = _transferFileStatus.asStateFlow()

    fun sendFiles(targetDevice: DiscoveredDevice, fileUris: List<Uri>) {
        val targetHost = targetDevice.ipAddress
        val targetPort = targetDevice.port

        if (targetHost == null || targetPort <= 0) {
            statusUpdateCallback(context.getString(R.string.error_device_details_missing))
            return
        }
        if (transferJob?.isActive == true) {
            statusUpdateCallback(context.getString(R.string.transfer_already_active))
            return
        }

        _isSendingActive.value = true
        _transferProgress.value = null
        _transferFileStatus.value = null

        transferJob = scope.launch(CoroutineExceptionHandler { _, throwable ->
            val errorMessage = throwable.message ?: "Unknown error"
            Log.e(Constants.TAG, "FileSender: Transfer job encountered unhandled exception: $errorMessage", throwable)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                LiveUpdateNotificationManager.showFailed(errorMessage)
            }
        }) {
            val contentResolver = context.contentResolver
            val filesToSendInfo = mutableListOf<FileTransferInfo>()
            var totalSize: Long = 0
            var hasError = false
            var transferOverallSuccess = false

            statusUpdateCallback(context.getString(R.string.preparing_files))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                LiveUpdateNotificationManager.showPreparing()
            }
            _transferProgress.value = 0

            for (uri in fileUris) {
                if (!isActive) {
                    hasError = true
                    break
                }
                val fileInfoPair = getFileInfoFromUri(context, uri)
                if (fileInfoPair == null) {
                    statusUpdateCallback(context.getString(R.string.error_file_info_multi, uri.lastPathSegment ?: "unknown"))
                    hasError = true
                    break
                }
                val (name, size) = fileInfoPair
                if (size < 0) {
                    statusUpdateCallback(context.getString(R.string.error_file_size_negative, name))
                    hasError = true
                    break
                }
                filesToSendInfo.add(FileTransferInfo(uri = uri, name = name, size = size))
                totalSize += size
            }

            if (hasError || filesToSendInfo.isEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    LiveUpdateNotificationManager.showFailed("File preparation failed")
                }
                return@launch
            }

            val numberOfFiles = filesToSendInfo.size
            statusUpdateCallback(context.getString(R.string.connecting_to, targetDevice.name))
            _transferProgress.value = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                LiveUpdateNotificationManager.showConnecting()
            }

            var clientSocket: Socket? = null
            var outputStream: OutputStream? = null
            var inputStream: InputStream? = null
            var reader: BufferedReader? = null
            var writer: PrintWriter? = null
            var fileInputStream: InputStream? = null
            var totalBytesSentOverall: Long = 0

            try {
                clientSocket = Socket()
                clientSocket.connect(InetSocketAddress(targetHost, targetPort), 10000)
                clientSocket.soTimeout = 15000

                statusUpdateCallback(context.getString(R.string.requesting_send_multi, numberOfFiles))

                outputStream = clientSocket.getOutputStream()
                inputStream = clientSocket.getInputStream()
                writer = PrintWriter(OutputStreamWriter(outputStream, Charsets.UTF_8), true)
                reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

                val senderName = Build.MODEL.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20).encodeForURL()

                val requestHeader = "${Constants.CMD_REQUEST_SEND_MULTI}${Constants.CMD_SEPARATOR}${senderName}${Constants.CMD_SEPARATOR}$numberOfFiles${Constants.CMD_SEPARATOR}$totalSize"
                writer.println(requestHeader)
                filesToSendInfo.forEach { fileInfo ->
                    val encodedFileName = fileInfo.name.encodeForURL()
                    val metaLine = "${Constants.FILE_META}${Constants.CMD_SEPARATOR}$encodedFileName${Constants.CMD_SEPARATOR}${fileInfo.size}"
                    writer.println(metaLine)
                }
                writer.flush()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    LiveUpdateNotificationManager.showWaitingForAcceptance()
                }

                val response = withTimeoutOrNull(30000) { reader.readLine() }
                if (response == Constants.CMD_ACCEPT) {
                    val initialSendStatus = context.getString(R.string.sending_multi_start, numberOfFiles, totalSize.formatFileSize(context))
                    statusUpdateCallback(initialSendStatus)
                    _transferProgress.value = 0

                    clientSocket.soTimeout = 60000
                    val buffer = ByteArray(16384)
                    var lastOverallProgressUpdate = -1

                    for ((index, fileInfo) in filesToSendInfo.withIndex()) {
                        if (!currentCoroutineContext().isActive) throw CancellationException("Send cancelled before file ${index + 1}")
                        val currentFileStatusText = context.getString(R.string.sending_file_status, index + 1, numberOfFiles, fileInfo.name)
                        scope.launch(Dispatchers.Main.immediate) {
                            _transferFileStatus.value = currentFileStatusText
                        }

                        fileInputStream = contentResolver.openInputStream(fileInfo.uri!!) ?: throw IOException(context.getString(R.string.failed_open_uri, fileInfo.uri))

                        var bytesReadCurrentFile: Int
                        var bytesSentCurrentFile: Long = 0
                        while (currentCoroutineContext().isActive) {
                            bytesReadCurrentFile = fileInputStream.read(buffer)
                            if (bytesReadCurrentFile == -1) break
                            try {
                                outputStream.write(buffer, 0, bytesReadCurrentFile)
                                bytesSentCurrentFile += bytesReadCurrentFile
                                totalBytesSentOverall += bytesReadCurrentFile
                            } catch (sockEx: IOException) {
                                throw CancellationException(context.getString(R.string.error_connection_lost_during_send), sockEx)
                            }

                            val currentOverallProgress: Int = if (totalSize > 0) {
                                ((totalBytesSentOverall * 100) / totalSize).toInt()
                            } else 100

                            if (currentOverallProgress > lastOverallProgressUpdate) {
                                scope.launch(Dispatchers.Main.immediate) {
                                    if (isActive) _transferProgress.value = currentOverallProgress
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                                    LiveUpdateNotificationManager.showTransferring(currentOverallProgress, fileUris, totalSize, true)
                                }
                                lastOverallProgressUpdate = currentOverallProgress
                            }
                        }
                        fileInputStream.close()
                        fileInputStream = null
                        if (!currentCoroutineContext().isActive) throw CancellationException("Send cancelled after file ${index + 1}")
                        if (bytesSentCurrentFile != fileInfo.size) throw IOException("Sent bytes ($bytesSentCurrentFile) mismatch for file ${fileInfo.name} (expected ${fileInfo.size})")
                    }
                    outputStream.flush()
                    if (totalBytesSentOverall != totalSize) throw IOException(context.getString(R.string.sent_bytes_total_size_mismatch, totalBytesSentOverall, totalSize))

                    scope.launch(Dispatchers.Main.immediate) {
                        _transferProgress.value = 100
                        _transferFileStatus.value = null
                        statusUpdateCallback(context.getString(R.string.sent_multi_success, numberOfFiles))
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                        LiveUpdateNotificationManager.showCompleted(fileUris, true)
                    }
                    transferOverallSuccess = true
                } else {
                    val errorReason = response ?: "No response from receiver."
                    throw IOException(context.getString(R.string.error_server_rejected_transfer) + " Response: $errorReason")
                }
            } catch(e: CancellationException) {
                Log.w(Constants.TAG, "FileSender: sendFiles cancelled: ${e.message}")
                statusUpdateCallback(context.getString(R.string.send_cancelled))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    LiveUpdateNotificationManager.showCancelled(e.message)
                }
            } catch (e: Exception) {
                Log.e(Constants.TAG, "FileSender: Error during send: ${e.message}", e)
                val errorMsg = e.message ?: e.javaClass.simpleName
                statusUpdateCallback(context.getString(R.string.send_error, errorMsg))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    LiveUpdateNotificationManager.showFailed(errorMsg)
                }
            } finally {
                Log.d(Constants.TAG, "FileSender: sendFiles finally block entered. transferOverallSuccess=$transferOverallSuccess")
                try { writer?.close() } catch (ex: Exception) { }
                try { reader?.close() } catch (ex: IOException) { }
                try { outputStream?.close() } catch (ex: IOException) { }
                try { inputStream?.close() } catch (ex: IOException) { }
                try { fileInputStream?.close() } catch (ex: IOException) { }
                try { clientSocket?.close() } catch (ex: IOException) { }

                scope.launch(Dispatchers.Main.immediate) {
                    _transferProgress.value = null
                    _transferFileStatus.value = null
                    if (!transferOverallSuccess) {
                        statusUpdateCallback(context.getString(R.string.idle_status))
                    }
                }
                _isSendingActive.value = false
            }
        }
    }

    fun cancelTransfer() {
        val jobToCancel = transferJob
        if (jobToCancel != null && jobToCancel.isActive) {
            Log.d(Constants.TAG, "FileSender: Cancelling transfer job: ${jobToCancel.job.key}")
            jobToCancel.cancel(CancellationException(context.getString(R.string.cancelled_by_user)))
        } else {
            Log.d(Constants.TAG, "FileSender: cancelTransfer called, but no active transferJob found.")
        }
    }

    private fun getFileInfoFromUri(context: Context, uri: Uri): Pair<String, Long>? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else null
                    val fallbackName = uri.lastPathSegment ?: "file_${System.currentTimeMillis()}"
                    val cleanName = File(name ?: fallbackName).name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(240).ifBlank { fallbackName }
                    var size = if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
                    if (size <= 0) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val availableBytes = inputStream.available()
                                size = if (availableBytes > 0) availableBytes.toLong() else 0L
                            } ?: run { size = -1L }
                        } catch (ex: Exception) { Log.e(Constants.TAG, "FileSender: Error getting available bytes from URI: ${ex.message}"); size = -1L }
                    }
                    if (size >= 0) cleanName to size else null
                } else null
            }
        } catch (e: Exception) { Log.e(Constants.TAG, "FileSender: Error getting file info from URI: ${e.message}"); null }
    }

    fun shutdown() {
        Log.d(Constants.TAG, "FileSender: Shutting down FileSender.")
        cancelTransfer()
    }
}