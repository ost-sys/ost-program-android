package com.ost.application.share

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Log
import com.ost.application.R
import com.ost.application.share.NotificationHelper.formatFileSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class WearShareService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val nsdManager: NsdManager by lazy { getSystemService(NSD_SERVICE) as NsdManager }
    private var serverSocketRef: AtomicReference<ServerSocket?> = AtomicReference(null)
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var registeredServiceName: String? = null
    private var discoveryJob: Job? = null
    private var serverJob: Job? = null
    private var transferJob: Job? = null
    private val clientHandlerJobs = ConcurrentHashMap<String, Job>()

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _transferProgress = MutableStateFlow<Int?>(null)
    val transferProgress: StateFlow<Int?> = _transferProgress.asStateFlow()

    private val _transferFileStatus = MutableStateFlow<String?>(null)
    val transferFileStatus: StateFlow<String?> = _transferFileStatus.asStateFlow()

    private val _discoveredDevicesMap = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevicesMap
        .asStateFlow()
        .map { it.values.filter { d -> d.isResolved || d.isResolving }.sortedBy { device -> device.name } }
        .stateIn(serviceScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastReceivedFiles = MutableStateFlow<List<File>>(emptyList())
    val lastReceivedFiles: StateFlow<List<File>> = _lastReceivedFiles.asStateFlow()

    private val _incomingTransferRequestInternal = MutableStateFlow<IncomingTransferRequest?>(null)
    val incomingTransferRequest: StateFlow<IncomingTransferRequest?> = _incomingTransferRequestInternal.asStateFlow()

    private val incomingTransferConfirmations = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    override fun onCreate() {
        super.onCreate()
        Log.d(Constants.TAG, "Service: onCreate")
        NotificationHelper.createNotificationChannel(this)
        _statusText.value = getString(R.string.receiver_stopped)
        updateOngoingActivity() // Initial update for Idle state
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            Log.d(Constants.TAG, "Service: onStartCommand with action: $action")
            when (action) {
                Constants.ACTION_START_SERVICE -> startServiceInternal()
                Constants.ACTION_STOP_SERVICE -> stopServiceInternal()
                Constants.ACTION_START_DISCOVERY -> startDiscoveryInternal()
                Constants.ACTION_STOP_DISCOVERY -> stopDiscoveryInternal()
                Constants.ACTION_SEND_FILES -> {
                    val urisToShare = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(Constants.EXTRA_FILE_URIS, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra(Constants.EXTRA_FILE_URIS)
                    }
                    val targetDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Constants.EXTRA_TARGET_DEVICE, DiscoveredDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Constants.EXTRA_TARGET_DEVICE)
                    }
                    if (urisToShare != null && targetDevice != null) {
                        sendFiles(targetDevice, urisToShare)
                    } else {
                        serviceScope.launch(Dispatchers.Main) { _statusText.value = getString(R.string.error_send_intent_missing_data) }
                    }
                }
                Constants.ACTION_CANCEL_TRANSFER -> cancelTransfer()
                Constants.ACTION_ACCEPT_RECEIVE -> {
                    val requestId = intent.getStringExtra(Constants.EXTRA_REQUEST_ID)
                    requestId?.let {
                        Log.d(Constants.TAG, "Service: Received ACCEPT for request ID: $it")
                        respondToIncomingTransfer(it, true)
                    } ?: run {
                        Log.e(Constants.TAG, "Service: Received ACCEPT with null requestId!")
                    }
                }
                Constants.ACTION_REJECT_RECEIVE -> {
                    val requestId = intent.getStringExtra(Constants.EXTRA_REQUEST_ID)
                    requestId?.let {
                        Log.d(Constants.TAG, "Service: Received REJECT for request ID: $it")
                        respondToIncomingTransfer(it, false)
                    } ?: run {
                        Log.e(Constants.TAG, "Service: Received REJECT with null requestId!")
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return ServiceBinder()
    }

    inner class ServiceBinder : Binder() {
        fun getService(): WearShareService = this@WearShareService
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(Constants.TAG, "Service: onDestroy")
        serviceScope.cancel()
        stopDiscoveryInternal()
        stopServiceInternal()
        NotificationHelper.cancelNotification(this, Constants.NOTIFICATION_ID_TRANSFER)
        NotificationHelper.stopOngoingActivity(this)
        Log.d(Constants.TAG, "Service: All components stopped and serviceScope cancelled.")
    }

    private fun startServiceInternal() {
        serviceScope.launch {
            if (_isServiceActive.value || serverJob?.isActive == true) {
                Log.d(Constants.TAG, "Service: startServiceInternal called, but receiver already active or job running.")
                return@launch
            }
            if (_isDiscovering.value || transferJob?.isActive == true || _incomingTransferRequestInternal.value != null) {
                withContext(Dispatchers.Main.immediate) {
                    _statusText.value = getString(R.string.stop_sending_searching_first)
                }
                Log.d(Constants.TAG, "Service: startServiceInternal blocked: discovery, transfer, or incoming request active.")
                return@launch
            }

            _isServiceActive.value = true
            _lastReceivedFiles.value = emptyList()
            _transferProgress.value = null
            _transferFileStatus.value = null
            _statusText.value = getString(R.string.starting_receiver)
            updateOngoingActivity()

            serverJob = launch(CoroutineExceptionHandler { _, throwable ->
                serviceScope.launch(Dispatchers.Main) {
                    _statusText.value = getString(R.string.internal_server_error, throwable.message)
                }
                Log.e(Constants.TAG, "Service: Server job unhandled exception: ${throwable.message}", throwable)
                serviceScope.launch(Dispatchers.IO) { stopServiceInternal() }
            }) {
                var serverSocketSuccess = false
                var nsdRegisteredOrAttempted = false
                try {
                    serverSocketRef.set(ServerSocket(Constants.TRANSFER_PORT).apply { reuseAddress = true })
                    serverSocketSuccess = true
                    if (!isActive) return@launch

                    initializeRegistrationListener()
                    val nsdCanRegister = registerNsdService()
                    nsdRegisteredOrAttempted = true
                    if (!nsdCanRegister) {
                        throw IOException(getString(R.string.nsd_registration_initiation_failed))
                    }

                    withContext(Dispatchers.Main.immediate) {
                        _statusText.value = getString(R.string.waiting_for_registration)
                    }
                    updateOngoingActivity()

                    while (isActive && _isServiceActive.value) {
                        try {
                            val clientSocket = serverSocketRef.get()!!.accept()
                            if (!_isServiceActive.value) {
                                clientSocket.close()
                                Log.d(Constants.TAG, "Service: Client connected, but receiver is stopping. Closing client socket.")
                                break
                            }
                            val clientId = clientSocket.remoteSocketAddress.toString()
                            clientHandlerJobs[clientId]?.cancel()
                            val handlerJob = launch { handleClient(clientSocket) }
                            clientHandlerJobs[clientId] = handlerJob
                            handlerJob.invokeOnCompletion {
                                clientHandlerJobs.remove(clientId)
                                Log.d(Constants.TAG, "Service: Client handler for $clientId completed or cancelled: $it")
                            }
                            Log.d(Constants.TAG, "Service: Client connected: $clientId. Handled by new coroutine.")
                        } catch (e: SocketException) {
                            if (!isActive || serverSocketRef.get()?.isClosed == true) {
                                Log.d(Constants.TAG, "Service: SocketException: Socket closed or job cancelled. Exiting accept loop. Message: ${e.message}")
                                break
                            }
                            Log.e(Constants.TAG, "Service: SocketException in server accept loop: ${e.message}", e)
                            delay(500)
                        } catch (e: IOException) {
                            if (!isActive) {
                                Log.d(Constants.TAG, "Service: IOException: Job cancelled. Exiting accept loop. Message: ${e.message}")
                                break
                            }
                            Log.e(Constants.TAG, "Service: IOException in server accept loop: ${e.message}", e)
                            delay(500)
                        }
                    }
                    Log.d(Constants.TAG, "Service: Server accept loop finished. isActive=$isActive, _isServiceActive=${_isServiceActive.value}")

                } catch (e: CancellationException) {
                    Log.d(Constants.TAG, "Service: Server job caught CancellationException: ${e.message}")
                    if (!serverSocketSuccess || !nsdRegisteredOrAttempted) {
                        _isServiceActive.value = false
                    }
                } catch (e: Exception) {
                    Log.e(Constants.TAG, "Service: Fatal server error: ${e.message}", e)
                    withContext(Dispatchers.Main.immediate) {
                        _statusText.value = getString(R.string.fatal_server_error, e.message)
                    }
                    _isServiceActive.value = false
                } finally {
                    Log.d(Constants.TAG, "Service: serverJob finally block entered.")
                    if (_isServiceActive.value && this.coroutineContext.job == serverJob) {
                        _isServiceActive.value = false
                        withContext(Dispatchers.Main.immediate) {
                            _statusText.value = getString(R.string.receiver_stopped_unexpected)
                            _transferProgress.value = null
                            _transferFileStatus.value = null
                        }
                    }
                    closeServerSocket()
                    unregisterNsdServiceInternal()
                    cancelAllClientHandlers()
                    updateOngoingActivity()
                    Log.d(Constants.TAG, "Service: serverJob finally block finished.")
                }
            }
        }
    }

    private fun stopServiceInternal() {
        serviceScope.launch {
            if (!_isServiceActive.value && serverJob?.isActive != true) {
                Log.d(Constants.TAG, "Service: stopServiceInternal called, but receiver already stopped.")
                return@launch
            }
            _isServiceActive.value = false
            withContext(Dispatchers.Main.immediate) {
                _statusText.value = getString(R.string.stopping_receiver)
                _transferProgress.value = null
                _transferFileStatus.value = null
                _lastReceivedFiles.value = emptyList()
            }
            updateOngoingActivity()

            serverJob?.cancel(CancellationException("Server stopped by user"))
            serverJob?.join()
            serverJob = null

            cancelAllClientHandlers()
            closeServerSocket()
            unregisterNsdServiceInternal()
            NotificationHelper.cancelNotification(this@WearShareService)
            withContext(Dispatchers.Main.immediate) {
                _statusText.value = getString(R.string.receiver_stopped)
            }
            updateOngoingActivity()
        }
    }

    private fun updateOngoingActivity() {
        serviceScope.launch(Dispatchers.Main.immediate) {
            val statusTextForOA: String
            val iconResForOA: Int

            when {
                _incomingTransferRequestInternal.value != null -> {
                    statusTextForOA = getString(R.string.notification_service_incoming_request)
                    iconResForOA = R.drawable.ic_notification_24dp // Incoming request icon
                }
                transferJob?.isActive == true && _transferProgress.value != null -> {
                    val isSending = statusText.value.contains(getString(R.string.sending_prefix), ignoreCase = true) || statusText.value.contains(getString(R.string.connecting_prefix), ignoreCase = true)
                    statusTextForOA = if (isSending) getString(R.string.notification_service_sending) else getString(R.string.notification_service_receiving)
                    iconResForOA = if (isSending) R.drawable.ic_publish_24dp else R.drawable.ic_download_24dp // Sending/Receiving icon
                    val builder = NotificationHelper.buildForegroundServiceNotification(this@WearShareService, statusTextForOA)
                    // Pass progress details to Ongoing Activity
                    NotificationHelper.startOrUpdateOngoingActivity(this@WearShareService, builder, statusTextForOA, iconResForOA, _transferProgress.value, _incomingTransferRequestInternal.value?.totalSize ?: 0L, isSending)
                    return@launch // Return early if transfer is ongoing with progress
                }
                _isServiceActive.value -> { // Receiving active, but not processing specific request/transfer yet
                    statusTextForOA = getString(R.string.notification_service_receiving)
                    iconResForOA = R.drawable.ic_download_24dp // Receiving icon (idle listening)
                }
                _isDiscovering.value -> {
                    statusTextForOA = getString(R.string.notification_service_discovering)
                    iconResForOA = R.drawable.ic_travel_explore_24dp // Discovering icon
                }
                else -> { // Idle state
                    statusTextForOA = getString(R.string.notification_service_idle)
                    iconResForOA = R.drawable.ic_notification_24dp // Default idle icon
                }
            }

            if (_isServiceActive.value || _isDiscovering.value || transferJob?.isActive == true || _incomingTransferRequestInternal.value != null) {
                val builder = NotificationHelper.buildForegroundServiceNotification(this@WearShareService, statusTextForOA)
                NotificationHelper.startOrUpdateOngoingActivity(this@WearShareService, builder, statusTextForOA, iconResForOA)
            } else {
                NotificationHelper.stopOngoingActivity(this@WearShareService)
            }
        }
    }


    private fun closeServerSocket() {
        serverSocketRef.getAndSet(null)?.let { socket ->
            if (!socket.isClosed) {
                try { socket.close() } catch (ex: IOException) { Log.e(Constants.TAG, "Service: Error closing server socket: ${ex.message}") }
            }
        }
    }

    private suspend fun cancelAllClientHandlers() {
        val jobsToCancel = clientHandlerJobs.values.toList()
        clientHandlerJobs.clear()
        jobsToCancel.forEach { it.cancel(CancellationException("Server stopping")) }
        delay(100)
    }

    private suspend fun handleClient(clientSocket: Socket) {
        var reader: BufferedReader? = null
        var writer: PrintWriter? = null
        var fileOutputStream: BufferedOutputStream? = null
        var socketInputStream: InputStream? = null
        val receivedFilesInfo = mutableListOf<FileTransferInfo>()
        val successfullySavedFiles = mutableListOf<File>()
        var currentFileReceiving: File? = null
        var totalExpectedSize: Long = 0
        var numberOfFilesExpected: Int = 0
        var receiveSuccess = false
        var requestId: String? = null
        var senderDeviceName: String = ""

        try {
            clientSocket.soTimeout = 15000
            socketInputStream = clientSocket.getInputStream()
            reader = BufferedReader(InputStreamReader(socketInputStream, Charsets.UTF_8))
            writer = PrintWriter(OutputStreamWriter(clientSocket.getOutputStream(), Charsets.UTF_8), true)

            var tempRequestLine: String?
            do {
                tempRequestLine = withTimeoutOrNull(15000) { reader.readLine() }
                if (tempRequestLine == null) {
                    Log.e(Constants.TAG, "Receiver: Timeout or null line received during initial handshake.")
                    throw IOException(getString(R.string.error_empty_request_from_client))
                }
                Log.d(Constants.TAG, "Receiver: Initial read attempt: '$tempRequestLine'")
            } while (tempRequestLine.trim().isEmpty() && currentCoroutineContext().isActive)

            val requestLine = tempRequestLine.trim()

            Log.d(Constants.TAG, "Receiver: Raw request line received: '$requestLine'")
            val parts = requestLine.split(Constants.CMD_SEPARATOR)
            val command = parts.getOrNull(0)
            Log.d(Constants.TAG, "Receiver: Parsed command: '$command', Parts count: ${parts.size}")
            if (parts.size > 1) {
                Log.d(Constants.TAG, "Receiver: Second part (expected value): '${parts.getOrNull(1)}'")
            }

            when (command) {
                Constants.CMD_REQUEST_SEND_MULTI -> {
                    Log.d(Constants.TAG, "Receiver: Received CMD_REQUEST_SEND_MULTI.")
                    if (parts.size < 4) throw IllegalArgumentException("Invalid MULTI_REQUEST format: $requestLine")
                    senderDeviceName = parts[1].decodeFromURL() ?: "Unknown"
                    numberOfFilesExpected = parts[2].toIntOrNull() ?: 0
                    totalExpectedSize = parts[3].toLongOrNull() ?: 0L
                    if (numberOfFilesExpected <= 0 || totalExpectedSize < 0) {
                        throw IllegalArgumentException("Invalid count ($numberOfFilesExpected) or size ($totalExpectedSize) in MULTI_REQUEST")
                    }
                    withContext(Dispatchers.Main.immediate) {
                        if (!_isServiceActive.value) throw CancellationException("Service stopped before receiving multi meta")
                        _statusText.value = getString(R.string.incoming_multi_from_device, numberOfFilesExpected, totalExpectedSize.formatFileSize(this@WearShareService), senderDeviceName)
                        _transferProgress.value = 0
                        _transferFileStatus.value = null
                        _lastReceivedFiles.value = emptyList()
                    }
                    repeat(numberOfFilesExpected) { index ->
                        val metaLine = withTimeoutOrNull(10000) { reader.readLine() }
                            ?: throw IOException("Timeout waiting for metadata for file ${index + 1}")
                        Log.d(Constants.TAG, "Receiver: Received meta line for file ${index+1}: '$metaLine'")
                        val metaParts = metaLine.split(Constants.CMD_SEPARATOR)
                        if (metaParts.size < 3 || metaParts[0] != Constants.FILE_META) {
                            throw IllegalArgumentException("Invalid FILE_META format: $metaLine")
                        }
                        val fileName = File(metaParts[1].decodeFromURL() ?: metaParts[1]).name
                        val fileSize = metaParts[2].toLongOrNull() ?: -1L
                        if (fileName.isBlank() || fileSize < 0) {
                            throw IllegalArgumentException("Invalid metadata: name='$fileName', size=$fileSize")
                        }
                        receivedFilesInfo.add(FileTransferInfo(uri = null, name = fileName, size = fileSize))
                    }
                    val calculatedTotalSize = receivedFilesInfo.sumOf { it.size }
                    if (calculatedTotalSize != totalExpectedSize) {
                        totalExpectedSize = calculatedTotalSize
                    }
                }
                Constants.CMD_REQUEST_SEND -> {
                    Log.d(Constants.TAG, "Receiver: Received CMD_REQUEST_SEND.")
                    if (parts.size < 4) throw IllegalArgumentException("Invalid SEND_REQUEST format: $requestLine")
                    senderDeviceName = parts[1].decodeFromURL() ?: "Unknown"
                    val fileName = File(parts[2].decodeFromURL() ?: parts[2]).name
                    val fileSize = parts[3].toLongOrNull() ?: 0L
                    if (fileName.isBlank() || fileSize <= 0) {
                        throw IllegalArgumentException(getString(R.string.invalid_filename_or_size))
                    }
                    numberOfFilesExpected = 1
                    totalExpectedSize = fileSize
                    receivedFilesInfo.add(FileTransferInfo(uri = null, name = fileName, size = fileSize))
                    withContext(Dispatchers.Main.immediate) {
                        if (!_isServiceActive.value) throw CancellationException("Service stopped before receiving single file")
                        _statusText.value = getString(R.string.incoming_single_from_device, fileName, senderDeviceName)
                        _transferProgress.value = 0
                        _transferFileStatus.value = null
                        _lastReceivedFiles.value = emptyList()
                    }
                }
                else -> {
                    writer.println(Constants.CMD_REJECT)
                    Log.e(Constants.TAG, "Receiver: Unknown command received: '$requestLine'. Rejecting transfer.")
                    throw IllegalArgumentException("Unknown command: $requestLine")
                }
            }

            requestId = UUID.randomUUID().toString()
            val confirmationDeferred = CompletableDeferred<Boolean>(parent = currentCoroutineContext().job)
            incomingTransferConfirmations[requestId] = confirmationDeferred

            withContext(Dispatchers.Main.immediate) {
                _incomingTransferRequestInternal.value = IncomingTransferRequest(
                    requestId = requestId,
                    senderDeviceName = senderDeviceName,
                    fileNames = receivedFilesInfo.map { it.name },
                    totalSize = totalExpectedSize,
                    deferredConfirmation = confirmationDeferred
                )
            }
            // Show notification for incoming request (for background scenario)
            NotificationHelper.showIncomingFileConfirmationNotification(
                this@WearShareService,
                requestId,
                receivedFilesInfo.map { it.name },
                totalExpectedSize,
                senderDeviceName
            )
            updateOngoingActivity() // Update OA for incoming request state

            Log.d(Constants.TAG, "Receiver: Incoming transfer request from $senderDeviceName, waiting for user confirmation.")

            val userAccepted = withTimeoutOrNull(30000) { confirmationDeferred.await() } ?: false
            withContext(Dispatchers.Main.immediate) { _incomingTransferRequestInternal.value = null } // Hide Alert
            NotificationHelper.cancelNotification(this@WearShareService, Constants.NOTIFICATION_ID_INCOMING_FILE) // Cancel background notification
            updateOngoingActivity() // Update OA after decision

            if (!userAccepted) {
                writer.println(Constants.CMD_REJECT)
                Log.d(Constants.TAG, "Receiver: User rejected or timed out for request $requestId. Sending REJECT.")
                withContext(Dispatchers.Main.immediate) {
                    _statusText.value = getString(R.string.transfer_rejected)
                    _transferProgress.value = null
                    _transferFileStatus.value = null
                }
                NotificationHelper.showCompletionNotification(this@WearShareService, getString(R.string.transfer_rejected_title), false, getString(R.string.transfer_rejected_message))
                return
            } else {
                writer.println(Constants.CMD_ACCEPT)
                Log.d(Constants.TAG, "Receiver: User accepted request $requestId. Sending ACCEPT.")
                withContext(Dispatchers.Main.immediate) {
                    _statusText.value = getString(R.string.receiving_multi_start, numberOfFilesExpected)
                    _transferProgress.value = 0
                }
                NotificationHelper.showTransferNotification(this@WearShareService, getString(R.string.notif_receiving_multi_title, numberOfFilesExpected), 0, totalExpectedSize, false)
                val receiveDir = getPublicDownloadOstDir()
                    ?: throw IOException(getString(R.string.target_directory_download_unavailable_or_could_not_be_created, Constants.FILES_DIR))
                clientSocket.soTimeout = 60000
                val buffer = ByteArray(16384)
                var lastOverallProgressUpdate = -1
                var totalBytesReceivedOverall: Long = 0

                for ((index, fileInfo) in receivedFilesInfo.withIndex()) {
                    if (!currentCoroutineContext().isActive) throw CancellationException("Receive cancelled before file ${index + 1}")
                    currentFileReceiving = ensureReceiveFile(receiveDir, fileInfo.name)
                    val currentFileStatusText = getString(
                        R.string.receiving_file_status,
                        index + 1,
                        numberOfFilesExpected,
                        fileInfo.name
                    )
                    withContext(Dispatchers.Main.immediate) {
                        if (!_isServiceActive.value) throw CancellationException("Service stopped before receiving file ${index + 1}")
                        _transferFileStatus.value = currentFileStatusText
                    }
                    NotificationHelper.showTransferNotification(this@WearShareService, currentFileStatusText, _transferProgress.value ?: 0, totalExpectedSize, false)
                    updateOngoingActivity() // Update OA icon/status during transfer

                    try {
                        fileOutputStream = BufferedOutputStream(FileOutputStream(currentFileReceiving))
                    } catch (fosEx: Exception) {
                        withContext(Dispatchers.Main.immediate) { _statusText.value = getString(R.string.error_creating_file, currentFileReceiving.name, fosEx.message) }
                        currentFileReceiving.delete()
                        throw fosEx
                    }

                    var bytesReceivedForCurrentFile: Long = 0
                    var bytesReadCurrentLoop: Int
                    while (currentCoroutineContext().isActive && bytesReceivedForCurrentFile < fileInfo.size) {
                        try {
                            val bytesToRead = minOf(buffer.size.toLong(), fileInfo.size - bytesReceivedForCurrentFile).toInt()
                            bytesReadCurrentLoop = socketInputStream.read(buffer, 0, bytesToRead)
                        } catch (timeoutEx: SocketTimeoutException) {
                            if (!currentCoroutineContext().isActive || !_isServiceActive.value) {
                                throw CancellationException("Receive cancelled during read timeout.")
                            } else {
                                throw timeoutEx
                            }
                        }
                        if (bytesReadCurrentLoop == -1) {
                            throw IOException(getString(R.string.error_client_disconnected_early_file, fileInfo.name))
                        }
                        if (bytesReadCurrentLoop > 0) {
                            try {
                                fileOutputStream.write(buffer, 0, bytesReadCurrentLoop)
                                bytesReceivedForCurrentFile += bytesReadCurrentLoop
                                totalBytesReceivedOverall += bytesReadCurrentLoop
                            } catch (writeEx: IOException) {
                                throw writeEx
                            }
                        }
                        val currentOverallProgress: Int = if (totalExpectedSize > 0) {
                            ((totalBytesReceivedOverall * 100) / totalExpectedSize).toInt()
                        } else if (numberOfFilesExpected > 0) {
                            val progressWithinFile = if (fileInfo.size > 0) bytesReceivedForCurrentFile.toDouble() / fileInfo.size else 1.0
                            val overallFileProgress = index + progressWithinFile
                            ((overallFileProgress * 100) / numberOfFilesExpected).toInt()
                        } else 100
                        if (currentOverallProgress > lastOverallProgressUpdate) {
                            withContext(Dispatchers.Main.immediate) {
                                if (isActive && _isServiceActive.value) _transferProgress.value = currentOverallProgress
                            }
                            if (currentOverallProgress % 5 == 0 || currentOverallProgress >= 99) {
                                NotificationHelper.showTransferNotification(this@WearShareService, currentFileStatusText, currentOverallProgress, totalExpectedSize, false)
                            }
                            lastOverallProgressUpdate = currentOverallProgress
                            updateOngoingActivity() // Update OA progress
                        }
                    }
                    try {
                        fileOutputStream.flush()
                        fileOutputStream.close()
                        fileOutputStream = null
                    } catch (_: IOException) {}
                    if(!currentCoroutineContext().isActive) throw CancellationException("Receive cancelled after file ${index + 1}")
                    if (bytesReceivedForCurrentFile == fileInfo.size) {
                        successfullySavedFiles.add(currentFileReceiving)
                        val filePath = currentFileReceiving.absolutePath
                        MediaScannerConnection.scanFile(
                            this@WearShareService,
                            arrayOf(filePath),
                            null
                        ) { path, uri ->
                            Log.d("MediaScan", "Scanned $path -> $uri")
                        }
                        currentFileReceiving = null
                    } else {
                        currentFileReceiving.delete()
                        throw IOException("File size mismatch for ${fileInfo.name}")
                    }
                }
                receiveSuccess = true
                withContext(Dispatchers.Main.immediate) {
                    _transferProgress.value = 100
                    _statusText.value = getString(R.string.received_multi_success, numberOfFilesExpected)
                    _transferFileStatus.value = null
                    _lastReceivedFiles.value = successfullySavedFiles.toList()
                }
                NotificationHelper.showCompletionNotification(this@WearShareService, getString(R.string.notif_received_multi_title_success, numberOfFilesExpected), true)
            }
        } catch(e: CancellationException) {
            Log.w(Constants.TAG, "Receiver: Transfer cancelled: ${e.message}")
            withContext(Dispatchers.Main.immediate) {
                _statusText.value = getString(R.string.receive_cancelled)
                _transferProgress.value = null
                _transferFileStatus.value = null
                _lastReceivedFiles.value = emptyList()
            }
            NotificationHelper.cancelNotification(this@WearShareService, Constants.NOTIFICATION_ID_TRANSFER)
            currentFileReceiving?.delete()
            successfullySavedFiles.forEach { runCatching { it.delete() } }
            NotificationHelper.showCompletionNotification(this@WearShareService, getString(R.string.transfer_cancelled_title), false, getString(R.string.transfer_cancelled_message))
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Receiver: Error during client handling: ${e.message}", e)
            val errorMsg = e.message ?: e.javaClass.simpleName
            withContext(Dispatchers.Main.immediate) {
                _statusText.value = getString(R.string.receive_error, errorMsg)
                _transferProgress.value = null
                _transferFileStatus.value = null
                _lastReceivedFiles.value = emptyList()
            }
            val firstFileName = receivedFilesInfo.firstOrNull()?.name ?: getString(R.string.unknown_filename)
            NotificationHelper.showCompletionNotification(this@WearShareService, firstFileName, false, errorMsg)
            currentFileReceiving?.delete()
            successfullySavedFiles.forEach { runCatching { it.delete() } }
        } finally {
            if (requestId != null) {
                incomingTransferConfirmations.remove(requestId)
            }
            serviceScope.launch(Dispatchers.Main.immediate) {
                _incomingTransferRequestInternal.value = null
            }
            try { fileOutputStream?.close() } catch (_: IOException) { }
            try { socketInputStream?.close() } catch (_: IOException) { }
            try { writer?.close() } catch (_: Exception) { }
            try { reader?.close() } catch (_: IOException) { }
            try { clientSocket.close() } catch (_: IOException) { }

            if (_isServiceActive.value) {
                withContext(Dispatchers.Main.immediate) {
                    _transferProgress.value = null
                    _transferFileStatus.value = null
                    val currentStatus = _statusText.value
                    val isError = currentStatus.startsWith(Constants.ERROR_PREFIX, ignoreCase = true)
                    val isCancelled = currentStatus.contains(Constants.CANCELLED_KEYWORD, ignoreCase = true)
                    val isReceived = currentStatus.startsWith(Constants.RECEIVING_PREFIX, ignoreCase = true) || currentStatus.startsWith(Constants.SENT_PREFIX, ignoreCase = true)
                    if (!isError && !isCancelled) {
                        delay(if (isReceived) 3000 else 500)
                        if (_isServiceActive.value && _transferProgress.value == null) {
                            _statusText.value = getString(R.string.waiting_for_connection)
                        }
                    }
                }
            } else if (!_isServiceActive.value && !_statusText.value.startsWith(Constants.ERROR_PREFIX)) {
                withContext(Dispatchers.Main.immediate) {
                    _statusText.value = getString(R.string.receiver_stopped)
                }
            }
            updateOngoingActivity()
        }
    }

    fun respondToIncomingTransfer(requestId: String, accept: Boolean) {
        Log.d(Constants.TAG, "Service: respondToIncomingTransfer: requestId=$requestId, accept=$accept")
        incomingTransferConfirmations[requestId]?.complete(accept)
    }

    private fun initializeRegistrationListener() {
        if (registrationListener != null) return
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                registeredServiceName = nsdServiceInfo.serviceName
                serviceScope.launch(Dispatchers.Main.immediate) {
                    if (_isServiceActive.value && _statusText.value == getString(R.string.waiting_for_registration)) {
                        _statusText.value = getString(R.string.waiting_for_connection)
                    }
                }
                updateOngoingActivity()
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                registeredServiceName = null
                registrationListener = null
                serviceScope.launch(Dispatchers.Main.immediate) {
                    _statusText.value = getString(R.string.error_nsd_registration_failed, errorCode)
                    if (_isServiceActive.value) {
                        stopServiceInternal()
                    }
                }
                updateOngoingActivity()
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                if (registeredServiceName == serviceInfo.serviceName) registeredServiceName = null
                if (this == registrationListener) {
                    registrationListener = null
                }
                updateOngoingActivity()
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                registeredServiceName = null
                if (this == registrationListener) registrationListener = null
                updateOngoingActivity()
            }
        }
    }

    @SuppressLint("HardwareIds")
    private suspend fun registerNsdService(): Boolean {
        if (registrationListener == null) {
            initializeRegistrationListener()
            if (registrationListener == null) return false
        }
        val currentListener = registrationListener!!
        if (registeredServiceName != null) {
            try {
                withContext(Dispatchers.Main) { nsdManager.unregisterService(currentListener) }
                delay(500)
            } catch (_: Exception) { Log.e(Constants.TAG, "Service: Error unregistering old NSD service: ${registeredServiceName}")}
            registeredServiceName = null
        }
        val deviceName = try {
            Build.MODEL.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
        } catch (_: Exception) { getString(R.string.default_wear_device_name) }
        val randomSuffix = (1000..9999).random()
        val serviceName = "${Constants.SERVICE_NAME_PREFIX}${deviceName}_${randomSuffix}"
        val port = serverSocketRef.get()?.localPort ?: Constants.TRANSFER_PORT
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = Constants.SERVICE_TYPE
            this.port = port
            setAttribute(Constants.KEY_DEVICE_TYPE, Constants.VALUE_DEVICE_WATCH)
        }
        try {
            withContext(Dispatchers.Main) {
                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, currentListener)
            }
            return true
        } catch (e: Exception) {
            withContext(Dispatchers.Main.immediate) {
                _statusText.value = getString(R.string.error_nsd_registration_exception, e.javaClass.simpleName)
            }
            registrationListener = null
            Log.e(Constants.TAG, "Service: Exception during NSD service registration: ${e.message}", e)
            return false
        }
    }

    private suspend fun unregisterNsdServiceInternal() {
        val listener = registrationListener
        val serviceName = registeredServiceName
        if (listener != null && serviceName != null) {
            try {
                withContext(Dispatchers.Main) {
                    nsdManager.unregisterService(listener)
                }
                Log.d(Constants.TAG, "Service: NSD service unregistered: $serviceName")
            } catch (ex: IllegalArgumentException) {
                Log.w(Constants.TAG, "Service: NSD unregistration failed: listener not registered. ${ex.message}")
            } catch (ex: Exception) {
                Log.e(Constants.TAG, "Service: Error during NSD service unregistration: ${ex.message}", ex)
            } finally {
                if (registrationListener === listener) {
                    registrationListener = null
                }
                if(registeredServiceName == serviceName) {
                    registeredServiceName = null
                }
            }
        } else {
            Log.d(Constants.TAG, "Service: No active NSD listener or service name to unregister.")
            registrationListener = null
            registeredServiceName = null
        }
    }

    private fun startDiscoveryInternal() {
        serviceScope.launch {
            if (_isDiscovering.value || discoveryJob?.isActive == true) {
                Log.d(Constants.TAG, "Service: startDiscoveryInternal called, but discovery already active.")
                return@launch
            }
            if (_isServiceActive.value || transferJob?.isActive == true || _incomingTransferRequestInternal.value != null) {
                withContext(Dispatchers.Main.immediate) {
                    _statusText.value = getString(R.string.stop_receiving_sending_first)
                }
                Log.d(Constants.TAG, "Service: startDiscoveryInternal blocked: receiver, transfer, or incoming request active.")
                return@launch
            }
            withContext(Dispatchers.Main.immediate) {
                _isDiscovering.value = true
                _discoveredDevicesMap.value = emptyMap()
                _statusText.value = getString(R.string.searching_for_devices)
            }
            updateOngoingActivity()

            initializeDiscoveryListener()
            val listener = discoveryListener
            if (listener == null) {
                withContext(Dispatchers.Main.immediate) {
                    _isDiscovering.value = false
                    _statusText.value = getString(R.string.error_discovery_init_failed)
                }
                Log.e(Constants.TAG, "Service: Discovery listener is null, cannot start discovery.")
                return@launch
            }
            discoveryJob = launch {
                try {
                    withContext(Dispatchers.Main) {
                        nsdManager.discoverServices(Constants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
                        Log.d(Constants.TAG, "Service: discoverServices call issued successfully.")
                    }
                    while (isActive) {
                        delay(Long.MAX_VALUE) // Keep coroutine alive
                    }
                } catch (ex: CancellationException) {
                    Log.d(Constants.TAG, "Service: Discovery job cancelled: ${ex.message}")
                } catch (e: Exception) {
                    Log.e(Constants.TAG, "Service: Unexpected error in discovery job: ${e.message}", e)
                    withContext(Dispatchers.Main.immediate) {
                        _statusText.value = getString(R.string.discovery_error, e.message)
                        _isDiscovering.value = false
                    }
                } finally {
                    stopDiscoveryInternal()
                }
            }
        }
    }

    private fun stopDiscoveryInternal() {
        serviceScope.launch {
            if (!_isDiscovering.value && discoveryJob?.isActive != true) {
                Log.d(Constants.TAG, "Service: stopDiscoveryInternal called, but discovery already stopped.")
                return@launch
            }
            val jobToCancel = discoveryJob
            val listenerToStop = discoveryListener
            jobToCancel?.cancel(CancellationException("Discovery stopped"))
            discoveryJob = null
            if (listenerToStop != null) {
                try {
                    withContext(Dispatchers.Main) {
                        nsdManager.stopServiceDiscovery(listenerToStop)
                        Log.d(Constants.TAG, "Service: stopServiceDiscovery call issued.")
                    }
                } catch (ex: IllegalArgumentException) {
                    Log.w(Constants.TAG, "Service: stopServiceDiscovery failed: listener not registered. ${ex.message}")
                } catch (ex: Exception) {
                    Log.e(Constants.TAG, "Service: Error stopping discovery: ${ex.message}", ex)
                } finally {
                    if (discoveryListener === listenerToStop) {
                        discoveryListener = null
                    }
                }
            }
            withContext(Dispatchers.Main.immediate) {
                if (_isDiscovering.value) {
                    _isDiscovering.value = false
                    val currentStatus = _statusText.value
                    val searchingStatus = getString(R.string.searching_for_devices)
                    val foundStatusPrefix = getString(R.string.found_prefix)
                    val resolvingPrefix = getString(R.string.resolving_prefix)
                    if (currentStatus == searchingStatus || currentStatus.startsWith(foundStatusPrefix) || currentStatus.startsWith(resolvingPrefix)) {
                        _statusText.value = getString(R.string.search_stopped)
                    }
                    _discoveredDevicesMap.value = _discoveredDevicesMap.value.filterValues { it.isResolved }
                }
            }
            updateOngoingActivity()
        }
    }

    private fun initializeDiscoveryListener() {
        if (discoveryListener != null) return
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) { Log.d(Constants.TAG, "Service: Discovery started: $regType") }
            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(Constants.TAG, "Service: Service found: ${service.serviceName}, type: ${service.serviceType}")
                if (service.serviceName == registeredServiceName) {
                    Log.d(Constants.TAG, "Service: Skipping self-discovered service: ${service.serviceName}")
                    return
                }
                if (service.serviceType != Constants.SERVICE_TYPE || !service.serviceName.startsWith(Constants.SERVICE_NAME_PREFIX)) {
                    Log.d(Constants.TAG, "Service: Skipping irrelevant service: ${service.serviceName}, type: ${service.serviceType}")
                    return
                }
                serviceScope.launch(Dispatchers.Main.immediate) {
                    val currentMap = _discoveredDevicesMap.value
                    if (!currentMap.containsKey(service.serviceName) || currentMap[service.serviceName]?.isResolved == false) {
                        _discoveredDevicesMap.value = currentMap + (service.serviceName to DiscoveredDevice(service))
                        if (_isDiscovering.value && _statusText.value == getString(R.string.searching_for_devices)) {
                            _statusText.value = getString(R.string.found_device_s, 1)
                        }
                        resolveDevice(service)
                    }
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(Constants.TAG, "Service: Service lost: ${service.serviceName}")
                if (service.serviceName == registeredServiceName) return
                serviceScope.launch(Dispatchers.Main.immediate) {
                    val currentMap = _discoveredDevicesMap.value
                    if (currentMap.containsKey(service.serviceName)) {
                        _discoveredDevicesMap.value = currentMap - service.serviceName
                        val remainingResolved = _discoveredDevicesMap.value.count { it.value.isResolved }
                        if (_isDiscovering.value) {
                            if (remainingResolved > 0) {
                                _statusText.value = getString(R.string.found_device_s, remainingResolved)
                            } else {
                                _statusText.value = getString(R.string.searching_for_devices)
                            }
                        }
                    }
                }
            }
            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(Constants.TAG, "Service: Discovery stopped: $serviceType")
                if (this == discoveryListener) discoveryListener = null
            }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(Constants.TAG, "Service: Discovery start failed: Error code $errorCode")
                serviceScope.launch(Dispatchers.Main.immediate) {
                    _statusText.value = getString(R.string.error_discovery_start_failed, errorCode)
                    _isDiscovering.value = false
                    discoveryListener = null
                }
                serviceScope.launch { stopDiscoveryInternal() }
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(Constants.TAG, "Service: Discovery stop failed: Error code $errorCode")
                if (this == discoveryListener) discoveryListener = null
            }
        }
    }

    fun resolveDevice(serviceInfo: NsdServiceInfo) {
        val serviceName = serviceInfo.serviceName ?: return
        if (serviceInfo.host != null && serviceInfo.port > 0) {
            serviceScope.launch(Dispatchers.Main.immediate) {
                val map = _discoveredDevicesMap.value
                map[serviceName]?.let {
                    val resolvedDevice = DiscoveredDevice(serviceInfo, true, false)
                    _discoveredDevicesMap.value = map + (serviceName to resolvedDevice)
                    Log.d(Constants.TAG, "Service: Device ${serviceName} already resolved: ${serviceInfo.host}:${serviceInfo.port}")
                }
            }
            return
        }

        serviceScope.launch(Dispatchers.Main.immediate) {
            val device = _discoveredDevicesMap.value[serviceName]
            if (device == null || device.isResolved || device.isResolving) return@launch
            val deviceDisplayName = DiscoveredDevice.extractDeviceName(serviceName)
            _discoveredDevicesMap.value = _discoveredDevicesMap.value + (serviceName to device.copy(isResolving = true))
            _statusText.value = getString(R.string.resolving, deviceDisplayName)
            Log.d(Constants.TAG, "Service: Starting resolution for device: $serviceName")

            val resolveListener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(failedServiceInfo: NsdServiceInfo, errorCode: Int) {
                    val name = failedServiceInfo.serviceName ?: "Unknown"
                    val failedDeviceDisplayName = DiscoveredDevice.extractDeviceName(name)
                    Log.e(Constants.TAG, "Service: Resolution failed for $name: Error code $errorCode")
                    serviceScope.launch(Dispatchers.Main.immediate) {
                        val map = _discoveredDevicesMap.value
                        map[name]?.let {
                            _discoveredDevicesMap.value = map + (name to it.copy(isResolving = false, isResolved = false))
                        }
                        if (_statusText.value == getString(R.string.resolving, failedDeviceDisplayName)) {
                            _statusText.value = getString(R.string.resolve_failed_for, failedDeviceDisplayName)
                        }
                    }
                }
                override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                    val name = resolvedServiceInfo.serviceName ?: "Unknown"
                    val resolvedDeviceDisplayName = DiscoveredDevice.extractDeviceName(name)
                    Log.d(Constants.TAG, "Service: Service resolved: $name -> ${resolvedServiceInfo.host}:${resolvedServiceInfo.port}")
                    serviceScope.launch(Dispatchers.Main.immediate) {
                        val map = _discoveredDevicesMap.value
                        map[name]?.let {
                            val resolvedDevice = DiscoveredDevice(resolvedServiceInfo, true, false)
                            _discoveredDevicesMap.value = map + (name to resolvedDevice)
                        }
                        val resolvedCount = _discoveredDevicesMap.value.count { it.value.isResolved }
                        if (_isDiscovering.value && (_statusText.value == getString(R.string.resolving, resolvedDeviceDisplayName) || _statusText.value.startsWith(getString(R.string.found_prefix)))) {
                            _statusText.value = getString(R.string.found_device_s, resolvedCount)
                        }
                    }
                }
            }
            try {
                withContext(Dispatchers.Main) {
                    nsdManager.resolveService(serviceInfo, resolveListener)
                }
            } catch (e: Exception) {
                val errorDeviceDisplayName = DiscoveredDevice.extractDeviceName(serviceName)
                Log.e(Constants.TAG, "Service: Error resolving service $serviceName: ${e.message}", e)
                serviceScope.launch(Dispatchers.Main.immediate) {
                    _discoveredDevicesMap.value[serviceName]?.let {
                        _discoveredDevicesMap.value = _discoveredDevicesMap.value + (serviceName to it.copy(isResolving = false, isResolved = false))
                    }
                    if (_statusText.value == getString(R.string.resolving, errorDeviceDisplayName)) {
                        _statusText.value = getString(R.string.resolve_error_for, errorDeviceDisplayName)
                    }
                }
            }
        }
    }

    fun sendFiles(targetDevice: DiscoveredDevice, fileUris: List<Uri>) {
        val targetHost = targetDevice.host
        val targetPort = targetDevice.port
        if (targetHost == null || targetPort == null || targetPort <= 0) {
            serviceScope.launch { _statusText.value = getString(R.string.error_device_details_missing) }
            Log.e(Constants.TAG, "Sender: Target device details missing (host: $targetHost, port: $targetPort).")
            return
        }
        if (transferJob?.isActive == true) {
            serviceScope.launch { _statusText.value = getString(R.string.transfer_already_active) }
            Log.w(Constants.TAG, "Sender: Transfer already active, skipping new send request.")
            return
        }

        transferJob = serviceScope.launch(CoroutineExceptionHandler { _, throwable ->
            val errorMessage = throwable.message ?: "Unknown error"
            serviceScope.launch(Dispatchers.Main.immediate) {
                _statusText.value = getString(R.string.send_error, errorMessage)
                _transferProgress.value = null
                _transferFileStatus.value = null
            }
            NotificationHelper.showCompletionNotification(this@WearShareService, getString(R.string.send_failed), false, errorMessage)
            Log.e(Constants.TAG, "Sender: Transfer job encountered unhandled exception: $errorMessage", throwable)
            transferJob = null
            updateOngoingActivity()
        }) {
            val contentResolver = applicationContext.contentResolver
            val filesToSendInfo = mutableListOf<FileTransferInfo>()
            var totalSize: Long = 0
            var hasError = false
            withContext(Dispatchers.Main.immediate) {
                _statusText.value = getString(R.string.preparing_files)
                _transferProgress.value = 0
                _transferFileStatus.value = null
            }
            updateOngoingActivity()
            Log.d(Constants.TAG, "Sender: Preparing files for sending...")

            for (uri in fileUris) {
                if (!isActive) {
                    Log.d(Constants.TAG, "Sender: Coroutine cancelled during file preparation.")
                    hasError = true
                    break
                }
                val fileInfoPair = getFileInfoFromUri(this@WearShareService, uri)
                if (fileInfoPair == null) {
                    withContext(Dispatchers.Main.immediate) {
                        _statusText.value = getString(R.string.error_file_info_multi, uri.lastPathSegment ?: "unknown")
                        _transferProgress.value = null
                    }
                    Log.e(Constants.TAG, "Sender: Failed to get file info for URI: $uri")
                    hasError = true
                    break
                }
                val (name, size) = fileInfoPair
                if (size < 0) {
                    withContext(Dispatchers.Main.immediate) {
                        _statusText.value = getString(R.string.error_file_size_negative, name)
                        _transferProgress.value = null
                    }
                    Log.e(Constants.TAG, "Sender: File size negative for file: $name")
                    hasError = true
                    break
                }
                filesToSendInfo.add(FileTransferInfo(uri = uri, name = name, size = size))
                totalSize += size
            }
            if (hasError || filesToSendInfo.isEmpty()) {
                withContext(Dispatchers.Main.immediate){
                    if(_statusText.value == getString(R.string.preparing_files)) {
                        _statusText.value = getString(R.string.transfer_failed)
                    }
                }
                Log.e(Constants.TAG, "Sender: File preparation failed or no files to send.")
                transferJob = null
                updateOngoingActivity()
                return@launch
            }
            val numberOfFiles = filesToSendInfo.size
            withContext(Dispatchers.Main.immediate) {
                _statusText.value = getString(R.string.connecting_to, targetDevice.name)
                _transferProgress.value = 0
                stopDiscoveryInternal()
            }
            updateOngoingActivity()
            NotificationHelper.showTransferNotification(this@WearShareService, getString(R.string.notif_sending_multi_title, numberOfFiles), 0, totalSize, true)
            Log.d(Constants.TAG, "Sender: Connecting to ${targetHost}:${targetPort}...")

            var clientSocket: Socket? = null
            var outputStream: OutputStream? = null
            var inputStream: InputStream? = null
            var reader: BufferedReader? = null
            var writer: PrintWriter? = null
            var fileInputStream: InputStream? = null
            var transferSuccess = false
            var totalBytesSentOverall: Long = 0

            try {
                clientSocket = Socket()
                clientSocket.connect(InetSocketAddress(targetHost, targetPort), 10000)
                clientSocket.soTimeout = 15000
                Log.d(Constants.TAG, "Sender: Connected. Local address: ${clientSocket.localAddress}:${clientSocket.localPort}")

                withContext(Dispatchers.Main.immediate) {
                    _statusText.value = getString(R.string.requesting_send_multi, numberOfFiles)
                }
                updateOngoingActivity()
                NotificationHelper.showTransferNotification(this@WearShareService, getString(R.string.notif_sending_multi_title, numberOfFiles), 0, totalSize, true)
                Log.d(Constants.TAG, "Sender: Requesting send multi for ${numberOfFiles} files.")

                outputStream = clientSocket.getOutputStream()
                inputStream = clientSocket.getInputStream()
                writer = PrintWriter(OutputStreamWriter(outputStream, Charsets.UTF_8), true)
                reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

                val senderName = Build.MODEL.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20).encodeForURL()

                val requestHeader = "${Constants.CMD_REQUEST_SEND_MULTI}${Constants.CMD_SEPARATOR}${senderName}${Constants.CMD_SEPARATOR}$numberOfFiles${Constants.CMD_SEPARATOR}$totalSize"
                Log.d(Constants.TAG, "Sender: Sending header: '$requestHeader'")
                writer.println(requestHeader)
                filesToSendInfo.forEach { fileInfo ->
                    val encodedFileName = fileInfo.name.encodeForURL()
                    val metaLine = "${Constants.FILE_META}${Constants.CMD_SEPARATOR}$encodedFileName${Constants.CMD_SEPARATOR}${fileInfo.size}"
                    Log.d(Constants.TAG, "Sender: Sending meta line: '$metaLine'")
                    writer.println(metaLine)
                }
                writer.flush()
                Log.d(Constants.TAG, "Sender: Waiting for response from receiver...")
                val response = withTimeoutOrNull(15000) { reader.readLine() }
                Log.d(Constants.TAG, "Sender: Received response: '$response'")
                if (response == Constants.CMD_ACCEPT) {
                    val initialSendStatus = getString(R.string.sending_multi_start, numberOfFiles, totalSize.formatFileSize(this@WearShareService))
                    withContext(Dispatchers.Main.immediate) {
                        _statusText.value = initialSendStatus
                        _transferProgress.value = 0
                    }
                    updateOngoingActivity()
                    NotificationHelper.showTransferNotification(this@WearShareService, getString(R.string.notif_sending_multi_title, numberOfFiles), 0, totalSize, true)
                    clientSocket.soTimeout = 60000
                    val buffer = ByteArray(16384)
                    var lastOverallProgressUpdate = -1
                    val progressUpdateInterval = 100L
                    var lastUpdateTime = System.currentTimeMillis()
                    for ((index, fileInfo) in filesToSendInfo.withIndex()) {
                        if (!currentCoroutineContext().isActive) throw CancellationException("Send cancelled before file ${index + 1}")
                        val currentFileStatusText = getString(R.string.sending_file_status, index + 1, numberOfFiles, fileInfo.name)
                        withContext(Dispatchers.Main.immediate) {
                            _transferFileStatus.value = currentFileStatusText
                        }
                        NotificationHelper.showTransferNotification(this@WearShareService, currentFileStatusText, _transferProgress.value ?: 0, totalSize, true)
                        updateOngoingActivity() // Update OA icon/status during transfer
                        fileInputStream = contentResolver.openInputStream(fileInfo.uri!!) ?: throw IOException("Failed to open InputStream for ${fileInfo.uri}")
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
                                throw CancellationException("Connection lost during send", sockEx)
                            }
                            val currentOverallProgress: Int = if (totalSize > 0) {
                                ((totalBytesSentOverall * 100) / totalSize).toInt()
                            } else if (numberOfFiles > 0) {
                                val progressWithinFile = if (fileInfo.size > 0) bytesSentCurrentFile.toDouble() / fileInfo.size else 1.0
                                val overallFileProgress = index + progressWithinFile
                                ((overallFileProgress * 100) / numberOfFiles).toInt()
                            } else 100
                            val currentTime = System.currentTimeMillis()
                            if ((currentOverallProgress > lastOverallProgressUpdate && (currentTime - lastUpdateTime >= progressUpdateInterval)) || currentOverallProgress == 100) {
                                withContext(Dispatchers.Main.immediate) {
                                    if (isActive) _transferProgress.value = currentOverallProgress
                                }
                                NotificationHelper.showTransferNotification(this@WearShareService, currentFileStatusText, currentOverallProgress, totalSize, true)
                                lastOverallProgressUpdate = currentOverallProgress
                                lastUpdateTime = currentTime
                            }
                        }
                        fileInputStream.close()
                        fileInputStream = null
                        if (!currentCoroutineContext().isActive) throw CancellationException("Send cancelled after file ${index + 1}")
                        if (bytesSentCurrentFile != fileInfo.size) throw IOException("Sent bytes ($bytesSentCurrentFile) mismatch for file ${fileInfo.name} (expected ${fileInfo.size})")
                    }
                    outputStream.flush()
                    if (totalBytesSentOverall != totalSize) throw IOException("Total sent bytes ($totalBytesSentOverall) mismatch (expected $totalSize)")
                    transferSuccess = true
                    withContext(Dispatchers.Main.immediate) {
                        _transferProgress.value = 100
                        _statusText.value = getString(R.string.sent_multi_success, numberOfFiles)
                        _transferFileStatus.value = null
                    }
                    NotificationHelper.showCompletionNotification(this@WearShareService, getString(R.string.notif_sent_multi_title_success, numberOfFiles), true)
                } else {
                    val errorReason = response ?: "No response from receiver."
                    throw IOException(getString(R.string.error_server_rejected_transfer) + " Response: $errorReason")
                }
            } finally {
                Log.d(Constants.TAG, "Sender: sendFiles finally block entered. transferSuccess=$transferSuccess")
                try { writer?.close() } catch (ex: Exception) { Log.e(Constants.TAG, "Sender: Error closing writer: ${ex.message}") }
                try { reader?.close() } catch (ex: IOException) { Log.e(Constants.TAG, "Sender: Error closing reader: ${ex.message}") }
                try { outputStream?.close() } catch (ex: IOException) { Log.e(Constants.TAG, "Sender: Error closing outputStream: ${ex.message}") }
                try { inputStream?.close() } catch (ex: IOException) { Log.e(Constants.TAG, "Sender: Error closing inputStream: ${ex.message}") }
                try { fileInputStream?.close() } catch (ex: IOException) { Log.e(Constants.TAG, "Sender: Error closing fileInputStream: ${ex.message}") }
                try { clientSocket?.close() } catch (ex: IOException) { Log.e(Constants.TAG, "Sender: Error closing clientSocket: ${ex.message}") }

                withContext(Dispatchers.Main.immediate) {
                    _transferProgress.value = null
                    _transferFileStatus.value = null
                    if (transferSuccess) {
                        delay(2000)
                        _statusText.value = getString(R.string.transfer_finished)
                    } else if (!_statusText.value.startsWith(Constants.ERROR_PREFIX) && !_statusText.value.contains(Constants.CANCELLED_KEYWORD)) {
                        _statusText.value = getString(R.string.transfer_failed)
                    }
                }
                transferJob = null
                updateOngoingActivity()
                Log.d(Constants.TAG, "Sender: sendFiles finally block finished.")
            }
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
                        } catch (ioe: Exception) { Log.e(Constants.TAG, "Error getting available bytes from URI: ${ioe.message}"); size = -1L }
                    }
                    if (size >= 0) cleanName to size else null
                } else null
            } ?: null
        } catch (e: Exception) { Log.e(Constants.TAG, "Error getting file info from URI: ${e.message}"); null }
    }

    @SuppressLint("StringFormatMatches")
    private fun getPublicDownloadOstDir(): File? {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fallbackDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val baseDir = publicDownloads ?: fallbackDir ?: run {
            serviceScope.launch(Dispatchers.Main.immediate) { _statusText.value = getString(R.string.error_cannot_access_downloads) }
            Log.e(Constants.TAG, "Could not find a suitable download directory.")
            return null
        }
        val ostDir = File(baseDir, Constants.FILES_DIR)
        try {
            if (!ostDir.exists()) {
                if (ostDir.mkdirs()) {
                    Log.d(Constants.TAG, "Created download directory: ${ostDir.absolutePath}")
                    return ostDir
                } else {
                    serviceScope.launch(Dispatchers.Main.immediate) { _statusText.value = getString(R.string.error_cannot_create_download_subdir, Constants.FILES_DIR) }
                    Log.e(Constants.TAG, "Failed to create download directory: ${ostDir.absolutePath}")
                    return null
                }
            } else if (!ostDir.isDirectory) {
                serviceScope.launch(Dispatchers.Main.immediate) { _statusText.value = getString(R.string.error_download_subdir_is_file, Constants.FILES_DIR) }
                Log.e(Constants.TAG, "Download path exists but is not a directory: ${ostDir.absolutePath}")
                return null
            } else if (!ostDir.canWrite()) {
                serviceScope.launch(Dispatchers.Main.immediate) { _statusText.value = getString(R.string.error_permission_denied_download_subdir, Constants.FILES_DIR)}
                Log.e(Constants.TAG, "Download directory not writable: ${ostDir.absolutePath}")
                return null
            } else {
                Log.d(Constants.TAG, "Using existing download directory: ${ostDir.absolutePath}")
                return ostDir
            }
        } catch (e: SecurityException) {
            serviceScope.launch(Dispatchers.Main.immediate) { _statusText.value = getString(R.string.error_permission_denied_download_subdir, Constants.FILES_DIR) }
            Log.e(Constants.TAG, "SecurityException accessing download directory: ${e.message}", e)
            return null
        } catch (e: Exception) {
            serviceScope.launch(Dispatchers.Main.immediate) { _statusText.value = getString(R.string.error_accessing_download_subdir, Constants.FILES_DIR, e.message ?: "") }
            Log.e(Constants.TAG, "Unexpected error accessing download directory: ${e.message}", e)
            return null
        }
    }

    private fun ensureReceiveFile(targetDir: File, originalFileName: String): File {
        val sanitizedFileName = originalFileName.replace(Regex("[^a-zA-Z0-9.\\-_ ]"), "_").trim().take(240).ifBlank { "file_${System.currentTimeMillis()}" }
        var file = File(targetDir, sanitizedFileName)
        if (!file.exists()) return file
        var counter = 0
        val baseName = file.nameWithoutExtension
        val extension = file.extension.let { if (it.isNotEmpty()) ".$it" else "" }
        while (file.exists() && counter < 999) {
            counter++
            val newName = "$baseName($counter)$extension".take(250)
            file = File(targetDir, newName)
        }
        return file
    }

    fun cancelTransfer() {
        val jobToCancel = transferJob
        if (jobToCancel != null && jobToCancel.isActive) {
            serviceScope.launch(Dispatchers.Main.immediate) {
                _statusText.value = getString(R.string.cancelling_transfer)
            }
            Log.d(Constants.TAG, "Service: Cancelling transfer job: ${jobToCancel.job.key}")
            jobToCancel.cancel(CancellationException("Transfer cancelled by user"))
            transferJob = null
            updateOngoingActivity()
        } else {
            Log.d(Constants.TAG, "Service: cancelTransfer called, but no active transferJob found.")
        }
    }
}

fun String.encodeForURL(): String {
    return try { URLEncoder.encode(this, Charsets.UTF_8.name()) } catch (ex: Exception) { Log.e(Constants.TAG, "Error encoding URL: ${ex.message}"); this }
}

fun String.decodeFromURL(): String? {
    return try { URLDecoder.decode(this, Charsets.UTF_8.name()) } catch (ex: Exception) { Log.e(Constants.TAG, "Error decoding URL: ${ex.message}"); this }
}