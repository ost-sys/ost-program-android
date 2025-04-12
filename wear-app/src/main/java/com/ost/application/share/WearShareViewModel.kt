package com.ost.application.share

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.log10
import kotlin.math.pow

data class DiscoveredDevice(
    val serviceInfo: NsdServiceInfo,
    var isResolved: Boolean = false,
    var isResolving: Boolean = false
) {
    val name: String get() = serviceInfo.serviceName ?: "Unknown Device"
    val host: String? get() = if (isResolved) serviceInfo.host?.hostAddress else null
    val port: Int? get() = if (isResolved) serviceInfo.port else null
    val deviceType: String get() = serviceInfo.attributes[Constants.KEY_DEVICE_TYPE]?.toString(Charsets.UTF_8) ?: Constants.VALUE_DEVICE_UNKNOWN
}

data class IncomingTransferRequest(
    val fileName: String,
    val fileSize: Long,
    val clientAddress: java.net.InetAddress
)


@SuppressLint("MissingPermission")
class WearShareViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val isServerGloballyActive = AtomicBoolean(false)
        private val serverSocketRef = AtomicReference<ServerSocket?>(null)
        private var globalServerJob: Job? = null
        private var globalRegistrationListener: NsdManager.RegistrationListener? = null
        private var globalRegisteredServiceName: String? = null
        private val globalServerMutex = Mutex()
    }

    private val nsdManager = application.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var discoveryJob: Job? = null
    private val discoveryMutex = Mutex()
    private var currentResolveListener: NsdManager.ResolveListener? = null

    private var context = getApplication<Application>()

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering

    private val _statusText = MutableStateFlow(context.getString(R.string.receiver_stopped))
    val statusText: StateFlow<String> = _statusText

    private val _transferProgress = MutableStateFlow<Int?>(null)
    val transferProgress: StateFlow<Int?> = _transferProgress

    private val _incomingRequest = MutableStateFlow<IncomingTransferRequest?>(null)
    val incomingRequest: StateFlow<IncomingTransferRequest?> = _incomingRequest

    private val _discoveredDevicesMap = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevicesMap
        .map { it.values.sortedBy { device -> device.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastReceivedFile = MutableStateFlow<File?>(null)
    val lastReceivedFile: StateFlow<File?> = _lastReceivedFile

    private val serverExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        viewModelScope.launch {
            _statusText.value = context.getString(R.string.internal_server_error, throwable.message)
            GlobalScope.launch(Dispatchers.IO) { stopServerInternal() }
        }
    }
    private val serverCoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + serverExceptionHandler)

    init {
        _isServiceActive.value = isServerGloballyActive.get()
        if (_isServiceActive.value) {
            _statusText.value = context.getString(R.string.waiting_for_connection)
        }
    }

    fun setServiceActive(isActive: Boolean) {
        viewModelScope.launch {
            if (isActive && !isServerGloballyActive.get()) {
                if (_isDiscovering.value || _transferProgress.value != null) {
                    _statusText.value = context.getString(R.string.stop_sending_searching_first)
                    _isServiceActive.value = false
                    return@launch
                }
                startServerInternal()
            } else if (!isActive && isServerGloballyActive.get()) {
                stopServerInternal()
            }
        }
    }

    private suspend fun startServerInternal() {
        globalServerMutex.withLock {
            if (isServerGloballyActive.get() || globalServerJob?.isActive == true) {
                return@withLock
            }

            isServerGloballyActive.set(true)
            withContext(Dispatchers.Main.immediate) {
                _isServiceActive.value = true
                _lastReceivedFile.value = null
                _statusText.value = context.getString(R.string.starting_receiver)
            }

            globalServerJob = serverCoroutineScope.launch {
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
                        throw IOException(context.getString(R.string.nsd_registration_initiation_failed))
                    }

                    while (isActive) {
                        try {
                            val clientSocket = serverSocketRef.get()!!.accept()
                            launch { handleClient(clientSocket) }
                        } catch (e: SocketException) {
                            if (!isActive) break
                            delay(500)
                        } catch (e: IOException) {
                            if (!isActive) break
                            delay(500)
                        }
                    }
                } catch (_: CancellationException) {
                    if (!serverSocketSuccess || !nsdRegisteredOrAttempted) {
                        isServerGloballyActive.set(false)
                        withContext(Dispatchers.Main.immediate) { _isServiceActive.value = false }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main.immediate) {
                        _statusText.value = context.getString(R.string.fatal_server_error, e.message)
                    }
                    isServerGloballyActive.set(false)
                    withContext(Dispatchers.Main.immediate) { _isServiceActive.value = false }
                } finally {
                    if (isServerGloballyActive.get() && this.coroutineContext.job == globalServerJob) {
                        isServerGloballyActive.set(false)
                        withContext(Dispatchers.Main.immediate) {
                            if (_isServiceActive.value) {
                                _isServiceActive.value = false
                                _statusText.value = context.getString(R.string.receiver_stopped_unexpected)
                                _transferProgress.value = null
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun stopServerInternal() {
        globalServerMutex.withLock {
            if (!isServerGloballyActive.get() && globalServerJob?.isActive != true) {
                return@withLock
            }

            isServerGloballyActive.set(false)
            withContext(Dispatchers.Main.immediate) {
                if (_isServiceActive.value) {
                    _isServiceActive.value = false
                    _statusText.value = context.getString(R.string.stopping_receiver)
                    _transferProgress.value = null
                    _incomingRequest.value = null
                    _lastReceivedFile.value = null
                } else if (globalServerJob?.isActive == true) {
                    _statusText.value = context.getString(R.string.force_stopping)
                }
            }

            val jobToCancel = globalServerJob
            if (jobToCancel?.isActive == true) {
                try {
                    jobToCancel.cancelAndJoin()
                } catch (_: CancellationException) {
                } catch(_: Exception) {
                } finally {
                    if (globalServerJob === jobToCancel) {
                        globalServerJob = null
                    }
                }
            } else {
                globalServerJob = null
            }

            serverSocketRef.getAndSet(null)?.let { socket ->
                if (!socket.isClosed) {
                    try { socket.close() } catch (_: IOException) { }
                }
            }

            val listener = globalRegistrationListener
            if (listener != null) {
                try {
                    withContext(Dispatchers.Main) {
                        nsdManager.unregisterService(listener)
                    }
                } catch (_: IllegalArgumentException) {
                } catch (_: Exception) {
                } finally {
                    globalRegistrationListener = null
                    globalRegisteredServiceName = null
                }
            } else {
                globalRegisteredServiceName = null
            }

            NotificationHelper.cancelNotification(getApplication())

            withContext(Dispatchers.Main.immediate) {
                if (!_statusText.value.startsWith(context.getString(R.string.error_prefix), ignoreCase = true)) {
                    if (!isServerGloballyActive.get()) {
                        _statusText.value = context.getString(R.string.receiver_stopped)
                    }
                }
            }
        }
    }


    private suspend fun handleClient(clientSocket: Socket) {
        clientSocket.inetAddress.hostAddress ?: context.getString(R.string.unknown_ip)
        var reader: BufferedReader? = null
        var writer: PrintWriter? = null
        var fileOutputStream: BufferedOutputStream? = null
        var socketInputStream: InputStream? = null
        var fileToReceive: File? = null
        var successfullySavedFile: File? = null
        var fileName = context.getString(R.string.unknown_filename)
        var fileSize = 0L
        var receiveSuccess = false

        try {
            clientSocket.soTimeout = 15000
            reader = BufferedReader(InputStreamReader(clientSocket.getInputStream(), Charsets.UTF_8))
            writer = PrintWriter(OutputStreamWriter(clientSocket.getOutputStream(), Charsets.UTF_8), true)

            val requestLine = withTimeoutOrNull(15000) { reader.readLine() }
            if (requestLine == null) throw IOException(context.getString(R.string.error_empty_request_from_client))

            val parts = requestLine.split(Constants.CMD_SEPARATOR)
            if (parts.size < 3 || parts[0] != Constants.CMD_REQUEST_SEND) {
                writer.println(Constants.CMD_REJECT)
                throw IllegalArgumentException(context.getString(R.string.error_invalid_request_format, requestLine))
            }

            fileName = File(parts[1]).name
            fileSize = parts[2].toLongOrNull() ?: 0L
            if (fileName.isBlank() || fileSize <= 0) {
                writer.println(Constants.CMD_REJECT)
                throw IllegalArgumentException(context.getString(R.string.invalid_filename_or_size))
            }

            val userAccepted = true
            if (userAccepted) {
                writer.println(Constants.CMD_ACCEPT)
                withContext(Dispatchers.Main.immediate) {
                    _statusText.value = context.getString(R.string.receiving, fileName, formatFileSize(fileSize))
                    _transferProgress.value = 0
                    _lastReceivedFile.value = null
                }
                NotificationHelper.showTransferNotification(getApplication(), fileName, 0, fileSize, false, isIndeterminate = true)

                val receiveDir = getPublicDownloadOstDir()
                if (receiveDir == null) throw IOException(context.getString(R.string.target_directory_download_unavailable_or_could_not_be_created, Constants.FILES_DIR))

                fileToReceive = ensureReceiveFile(receiveDir, fileName)

                clientSocket.soTimeout = 60000
                socketInputStream = clientSocket.getInputStream()
                try {
                    fileOutputStream = BufferedOutputStream(FileOutputStream(fileToReceive))
                    NotificationHelper.showTransferNotification(getApplication(), fileName, 0, fileSize, false, isIndeterminate = false)
                } catch (fosEx: Exception) {
                    withContext(Dispatchers.Main.immediate) { _statusText.value = context.getString(R.string.error_creating_file, fileToReceive.name, fosEx.message) }
                    fileToReceive.delete()
                    throw fosEx
                }

                val buffer = ByteArray(16384)
                var bytesRead: Int
                var totalBytesReceived: Long = 0
                var lastProgressUpdate = -1

                while (socketInputStream.read(buffer).also { bytesRead = it } != -1 && currentCoroutineContext().isActive) {
                    try {
                        fileOutputStream.write(buffer, 0, bytesRead)
                    } catch (writeEx: IOException) {
                        throw writeEx
                    }
                    totalBytesReceived += bytesRead

                    val currentProgress = if (fileSize > 0) ((totalBytesReceived * 100) / fileSize).toInt() else 0
                    if (currentProgress > lastProgressUpdate) {
                        withContext(Dispatchers.Main.immediate) {
                            if (isActive) _transferProgress.value = currentProgress
                        }
                        if (currentProgress % 5 == 0 || currentProgress == 100) {
                            NotificationHelper.showTransferNotification(getApplication(), fileName, currentProgress, fileSize, false)
                        }
                        lastProgressUpdate = currentProgress
                    }
                }

                if(!currentCoroutineContext().isActive) throw CancellationException(context.getString(R.string.receive_cancelled_during_stream))

                try {
                    fileOutputStream.flush()
                    fileOutputStream.close()
                    fileOutputStream = null
                } catch (_: IOException) {
                }


                if (totalBytesReceived == fileSize) {
                    receiveSuccess = true
                    successfullySavedFile = fileToReceive
                    withContext(Dispatchers.Main.immediate) {
                        _transferProgress.value = 100
                        _statusText.value = context.getString(R.string.received_file, successfullySavedFile?.name ?: fileName)
                        _lastReceivedFile.value = successfullySavedFile
                    }
                    NotificationHelper.showCompletionNotification(getApplication(), successfullySavedFile?.name ?: fileName, true)
                    fileToReceive = null
                } else {
                    fileToReceive?.delete()
                    withContext(Dispatchers.Main.immediate) {
                        _statusText.value = context.getString(R.string.error_file_incomplete)
                        _lastReceivedFile.value = null
                    }
                    NotificationHelper.showCompletionNotification(getApplication(), fileName, false, context.getString(R.string.incomplete_file))
                    throw IOException(context.getString(R.string.file_size_mismatch_expected_got, fileSize, totalBytesReceived))
                }

            } else {
                writer.println(Constants.CMD_REJECT)
                withContext(Dispatchers.Main.immediate) {
                    _statusText.value = context.getString(R.string.transfer_rejected)
                }
            }
        } catch(e: CancellationException) {
            withContext(Dispatchers.Main.immediate) {
                _statusText.value = context.getString(R.string.receive_cancelled)
                _transferProgress.value = null
                _lastReceivedFile.value = null
            }
            NotificationHelper.cancelNotification(getApplication())
            fileToReceive?.delete()
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.javaClass.simpleName
            withContext(Dispatchers.Main.immediate) {
                _statusText.value = context.getString(R.string.receive_error, errorMsg)
                _transferProgress.value = null
                _lastReceivedFile.value = null
            }
            NotificationHelper.showCompletionNotification(getApplication(), fileName, false, errorMsg)
            fileToReceive?.delete()
        } finally {
            try { fileOutputStream?.close() } catch (_: IOException) { }
            try { socketInputStream?.close() } catch (_: IOException) { }
            try { writer?.close() } catch (_: Exception) { }
            try { reader?.close() } catch (_: IOException) { }
            try { clientSocket.close() } catch (_: IOException) { }

            if (serverCoroutineScope.isActive && isServerGloballyActive.get()) {
                withContext(Dispatchers.Main.immediate) {
                    _transferProgress.value = null
                    val currentStatus = _statusText.value
                    val isError = currentStatus.startsWith(context.getString(R.string.error_prefix), ignoreCase = true)
                    val isCancelled = currentStatus.contains(context.getString(R.string.cancelled_keyword), ignoreCase = true)
                    val isReceived = currentStatus.startsWith(context.getString(R.string.received_prefix), ignoreCase = true)

                    if (!isError && !isCancelled && !isReceived) {
                        if (isServerGloballyActive.get()) {
                            _statusText.value = context.getString(R.string.waiting_for_connection)
                        }
                    } else if (isReceived) {
                        delay(3000)
                        if (isServerGloballyActive.get() && !_statusText.value.startsWith(context.getString(R.string.receiving_prefix), ignoreCase = true)) {
                            _statusText.value = context.getString(R.string.waiting_for_connection)
                        }
                    }
                }
            } else if (!isServerGloballyActive.get() && !_statusText.value.startsWith(context.getString(R.string.error_prefix))) {
                withContext(Dispatchers.Main.immediate) {
                    if (!isServerGloballyActive.get()) _statusText.value = context.getString(R.string.receiver_stopped)
                }
            }
        }
    }

    private fun initializeRegistrationListener() {
        if (globalRegistrationListener != null) {
            return
        }
        globalRegistrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                globalRegisteredServiceName = nsdServiceInfo.serviceName
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    if (isServerGloballyActive.get() && !_statusText.value.startsWith(context.getString(R.string.receiving_prefix), ignoreCase = true)) {
                        _statusText.value = context.getString(R.string.waiting_for_connection)
                    }
                }
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                globalRegisteredServiceName = null
                globalRegistrationListener = null
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    _statusText.value = context.getString(R.string.error_nsd_registration_failed, errorCode)
                    if (isServerGloballyActive.get()) {
                        GlobalScope.launch(Dispatchers.IO) { stopServerInternal() }
                    }
                }
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                if (globalRegisteredServiceName == serviceInfo.serviceName) globalRegisteredServiceName = null
                if (this == globalRegistrationListener) {
                    globalRegistrationListener = null
                }
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                globalRegisteredServiceName = null
                if (this == globalRegistrationListener) globalRegistrationListener = null
            }
        }
    }

    @SuppressLint("HardwareIds")
    private suspend fun registerNsdService(): Boolean {
        if (globalRegistrationListener == null) {
            initializeRegistrationListener()
            if (globalRegistrationListener == null) {
                return false
            }
        }
        val currentListener = globalRegistrationListener!!
        if (globalRegisteredServiceName != null) {
            try {
                withContext(Dispatchers.Main) { nsdManager.unregisterService(currentListener) }
                delay(500)
            } catch (_: Exception) { }
            globalRegisteredServiceName = null
        }

        val deviceName = try {
            Build.MODEL.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }.take(20)
        } catch (_: Exception) { context.getString(R.string.default_wear_device_name) }
        val serviceNameBase = "${Constants.SERVICE_NAME_PREFIX}${deviceName}"

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = serviceNameBase
            serviceType = Constants.SERVICE_TYPE
            port = Constants.TRANSFER_PORT
            setAttribute(Constants.KEY_DEVICE_TYPE, Constants.VALUE_DEVICE_WATCH)
        }

        try {
            withContext(Dispatchers.Main) {
                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, currentListener)
            }
            return true
        } catch (e: Exception) {
            withContext(Dispatchers.Main.immediate) {
                _statusText.value = context.getString(R.string.error_nsd_registration_exception, e.javaClass.simpleName)
            }
            globalRegistrationListener = null
            return false
        }
    }

    fun startDiscovery() {
        viewModelScope.launch(Dispatchers.IO) {
            discoveryMutex.withLock {
                if (_isDiscovering.value || discoveryJob?.isActive == true) {
                    return@launch
                }
                if (isServerGloballyActive.get() || _transferProgress.value != null) {
                    withContext(Dispatchers.Main.immediate) {
                        _statusText.value = context.getString(R.string.stop_receiving_sending_first)
                    }
                    return@launch
                }

                withContext(Dispatchers.Main.immediate) {
                    _isDiscovering.value = true
                    _discoveredDevicesMap.value = emptyMap()
                    _statusText.value = context.getString(R.string.searching_for_devices)
                }

                initializeDiscoveryListener()
                val listener = discoveryListener
                if (listener == null) {
                    withContext(Dispatchers.Main.immediate) {
                        _isDiscovering.value = false
                        _statusText.value = context.getString(R.string.error_discovery_init_failed)
                    }
                    return@launch
                }

                discoveryJob = launch {
                    try {
                        withContext(Dispatchers.Main) {
                            nsdManager.discoverServices(Constants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
                        }
                        while (isActive) {
                            delay(1000)
                        }
                    } catch (_: CancellationException) {
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main.immediate) {
                            _statusText.value = context.getString(R.string.discovery_error, e.message)
                            _isDiscovering.value = false
                        }
                    }
                }
            }
        }
    }

    fun stopDiscovery() {
        viewModelScope.launch(Dispatchers.IO) {
            discoveryMutex.withLock {
                stopDiscoveryInternal()
            }
        }
    }

    private suspend fun stopDiscoveryInternal() {
        if (!_isDiscovering.value && discoveryJob?.isActive != true) {
            return
        }

        val jobToCancel = discoveryJob
        val listenerToStop = discoveryListener

        jobToCancel?.cancel()
        discoveryJob = null

        if (listenerToStop != null) {
            try {
                withContext(Dispatchers.Main) {
                    nsdManager.stopServiceDiscovery(listenerToStop)
                }
            } catch (_: IllegalArgumentException) {
            } catch (_: Exception) {
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
                val searchingStatus = context.getString(R.string.searching_for_devices)
                val foundStatusPrefix = context.getString(R.string.found_prefix)

                if (currentStatus == searchingStatus || currentStatus.startsWith(foundStatusPrefix)) {
                    _statusText.value = context.getString(R.string.search_stopped)
                }
            }
        }
    }


    private fun initializeDiscoveryListener() {
        if (discoveryListener != null) {
            return
        }
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) { }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceName == globalRegisteredServiceName) {
                    return
                }
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    _statusText.value = context.getString(R.string.found_service, service.serviceName)
                    val currentMap = _discoveredDevicesMap.value.toMutableMap()
                    if (!currentMap.containsKey(service.serviceName)) {
                        currentMap[service.serviceName] = DiscoveredDevice(service)
                        _discoveredDevicesMap.value = currentMap
                    }
                    resolveDevice(service)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                if (service.serviceName == globalRegisteredServiceName) return

                viewModelScope.launch(Dispatchers.Main.immediate) {
                    val currentMap = _discoveredDevicesMap.value.toMutableMap()
                    if (currentMap.remove(service.serviceName) != null) {
                        _discoveredDevicesMap.value = currentMap
                        if (_discoveredDevicesMap.value.isEmpty() && _isDiscovering.value) {
                            _statusText.value = context.getString(R.string.searching_for_devices)
                        } else if (_isDiscovering.value) {
                            val resolvedCount = _discoveredDevicesMap.value.count { it.value.isResolved }
                            _statusText.value = context.getString(R.string.found_device_s, resolvedCount)
                        }
                    }
                }
            }

            override fun onDiscoveryStopped(serviceType: String) { }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    _statusText.value = context.getString(R.string.error_discovery_start_failed, errorCode)
                    _isDiscovering.value = false
                    discoveryListener = null
                }
                viewModelScope.launch { stopDiscoveryInternal() }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                if (this == discoveryListener) discoveryListener = null
            }
        }
    }

    fun resolveDevice(serviceInfo: NsdServiceInfo) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            val currentMap = _discoveredDevicesMap.value.toMutableMap()
            currentMap[serviceInfo.serviceName]?.let {
                if (!it.isResolved && !it.isResolving) {
                    currentMap[serviceInfo.serviceName] = it.copy(isResolving = true)
                    _discoveredDevicesMap.value = currentMap
                } else {
                    return@launch
                }
            } ?: return@launch

            currentResolveListener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(failedServiceInfo: NsdServiceInfo, errorCode: Int) {
                    viewModelScope.launch(Dispatchers.Main.immediate) {
                        val map = _discoveredDevicesMap.value.toMutableMap()
                        map[failedServiceInfo.serviceName]?.let {
                            map[failedServiceInfo.serviceName] = it.copy(isResolving = false, isResolved = false)
                        }
                        _discoveredDevicesMap.value = map
                        if (_statusText.value.startsWith(context.getString(R.string.resolving_prefix))) {
                            _statusText.value = context.getString(R.string.resolve_failed_for, failedServiceInfo.serviceName)
                        }
                    }
                    if (this == currentResolveListener) currentResolveListener = null
                }

                override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                    viewModelScope.launch(Dispatchers.Main.immediate) {
                        val map = _discoveredDevicesMap.value.toMutableMap()
                        map[resolvedServiceInfo.serviceName]?.let {
                            map[resolvedServiceInfo.serviceName] = it.copy(serviceInfo = resolvedServiceInfo, isResolving = false, isResolved = true)
                        }
                        _discoveredDevicesMap.value = map
                        val resolvedCount = _discoveredDevicesMap.value.count { it.value.isResolved }
                        val currentStatus = _statusText.value
                        val resolvingPrefix = context.getString(R.string.resolving_prefix)
                        val foundPrefix = context.getString(R.string.found_prefix)

                        if (currentStatus.startsWith(resolvingPrefix) || currentStatus.startsWith(foundPrefix)) {
                            if (_isDiscovering.value) {
                                _statusText.value = context.getString(R.string.found_device_s, resolvedCount)
                            }
                        }
                    }
                    if (this == currentResolveListener) currentResolveListener = null
                }
            }
            try {
                withContext(Dispatchers.Main) {
                    _statusText.value = context.getString(R.string.resolving, serviceInfo.serviceName)
                    nsdManager.resolveService(serviceInfo, currentResolveListener!!)
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    val map = _discoveredDevicesMap.value.toMutableMap()
                    map[serviceInfo.serviceName]?.let {
                        map[serviceInfo.serviceName] = it.copy(isResolving = false, isResolved = false)
                    }
                    _discoveredDevicesMap.value = map
                    if (_statusText.value.startsWith(context.getString(R.string.resolving_prefix))) {
                        _statusText.value = context.getString(R.string.resolve_error_for, serviceInfo.serviceName)
                    }
                }
                if (currentResolveListener === this) currentResolveListener = null
            }
        }
    }

    fun sendFile(targetDevice: DiscoveredDevice, fileUri: Uri) {
        val targetHost = targetDevice.host
        val targetPort = targetDevice.port

        if (targetHost == null || targetPort == null || targetPort <= 0) {
            viewModelScope.launch { _statusText.value = context.getString(R.string.error_device_details_missing) }
            return
        }

        viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            viewModelScope.launch(Dispatchers.Main.immediate) {
                _statusText.value = context.getString(R.string.send_error, throwable.message)
                _transferProgress.value = null
            }
            NotificationHelper.showCompletionNotification(getApplication(), context.getString(R.string.send_failed), false, throwable.message)
        }) {
            val contentResolver = getApplication<Application>().contentResolver
            var fileName: String = context.getString(R.string.unknown_filename)
            var fileSize: Long = 0L

            try {
                contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: context.getString(R.string.unknown_filename)
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    } else {
                        throw IOException(context.getString(R.string.cursor_is_empty_for_uri, fileUri))
                    }
                } ?: throw IOException(context.getString(R.string.error_content_resolver_null, fileUri))
            } catch (e: Exception) {
                throw IOException(context.getString(R.string.cannot_get_file_details, e.message))
            }

            if (fileSize <= 0) {
                throw IOException(context.getString(R.string.invalid_file_size_bytes, fileSize))
            }

            withContext(Dispatchers.Main.immediate) {
                _statusText.value = context.getString(R.string.connecting_to, targetDevice.name)
                _transferProgress.value = null
                stopDiscoveryInternal()
            }

            var clientSocket: Socket? = null
            var outputStream: OutputStream? = null
            var inputStream: InputStream? = null
            var reader: BufferedReader? = null
            var writer: PrintWriter? = null

            try {
                clientSocket = Socket()
                clientSocket.connect(InetSocketAddress(targetHost, targetPort), 10000)
                clientSocket.soTimeout = 15000

                withContext(Dispatchers.Main.immediate) {
                    _statusText.value = context.getString(R.string.sending_request)
                }

                outputStream = clientSocket.getOutputStream()
                inputStream = clientSocket.getInputStream()
                writer = PrintWriter(OutputStreamWriter(outputStream, Charsets.UTF_8), true)
                reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

                val request = "${Constants.CMD_REQUEST_SEND}${Constants.CMD_SEPARATOR}$fileName${Constants.CMD_SEPARATOR}$fileSize"
                writer.println(request)

                val response = withTimeoutOrNull(15000) { reader.readLine() }

                if (response == Constants.CMD_ACCEPT) {
                    withContext(Dispatchers.Main.immediate) {
                        _statusText.value = context.getString(R.string.sending, fileName)
                        _transferProgress.value = 0
                    }
                    NotificationHelper.showTransferNotification(getApplication(), fileName, 0, fileSize, true, isIndeterminate = false)

                    clientSocket.soTimeout = 60000

                    contentResolver.openInputStream(fileUri)?.use { fileInputStream ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        var totalBytesSent: Long = 0
                        var lastProgressUpdate = -1

                        while (fileInputStream.read(buffer).also { bytesRead = it } != -1 && currentCoroutineContext().isActive) {
                            try {
                                outputStream.write(buffer, 0, bytesRead)
                            } catch (writeEx: IOException) {
                                throw writeEx
                            }
                            totalBytesSent += bytesRead

                            val currentProgress = if (fileSize > 0) ((totalBytesSent * 100) / fileSize).toInt() else 0
                            if (currentProgress > lastProgressUpdate) {
                                withContext(Dispatchers.Main.immediate) {
                                    if (isActive) _transferProgress.value = currentProgress
                                }
                                if (currentProgress % 5 == 0 || currentProgress == 100) {
                                    NotificationHelper.showTransferNotification(getApplication(), fileName, currentProgress, fileSize, true)
                                }
                                lastProgressUpdate = currentProgress
                            }
                        }
                        outputStream.flush()

                        if (!currentCoroutineContext().isActive) throw CancellationException(context.getString(R.string.send_cancelled_during_stream))

                        withContext(Dispatchers.Main.immediate) {
                            _transferProgress.value = 100
                            _statusText.value = context.getString(R.string.sent, fileName)
                        }
                        NotificationHelper.showCompletionNotification(getApplication(), fileName, true)

                    } ?: throw IOException(context.getString(R.string.error_open_input_stream, fileUri))

                } else {
                    throw IOException(context.getString(R.string.error_server_rejected_transfer))
                }

            } finally {
                try { writer?.close() } catch (_: Exception) {}
                try { reader?.close() } catch (_: IOException) {}
                try { outputStream?.close() } catch (_: IOException) {}
                try { inputStream?.close() } catch (_: IOException) {}
                try { clientSocket?.close() } catch (_: IOException) {}

                withContext(Dispatchers.Main.immediate) {
                    delay(2000)
                    _transferProgress.value = null
                    val currentStatus = _statusText.value
                    if (!currentStatus.startsWith(context.getString(R.string.error_prefix)) && !currentStatus.contains(context.getString(R.string.cancelled_keyword))) {
                        _statusText.value = context.getString(R.string.transfer_finished)
                    }
                }
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun getPublicDownloadOstDir(): File? {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        if (publicDownloads == null) {
            viewModelScope.launch(Dispatchers.Main.immediate) { _statusText.value = context.getString(R.string.error_cannot_access_downloads) }
            return null
        }

        val ostDir = File(publicDownloads, Constants.FILES_DIR)

        try {
            if (!ostDir.exists()) {
                if (ostDir.mkdirs()) {
                    return ostDir
                } else {
                    viewModelScope.launch(Dispatchers.Main.immediate) { _statusText.value = context.getString(R.string.error_cannot_create_download_subdir, Constants.FILES_DIR) }
                    return null
                }
            } else if (!ostDir.isDirectory) {
                viewModelScope.launch(Dispatchers.Main.immediate) { _statusText.value = context.getString(R.string.error_download_subdir_is_file, Constants.FILES_DIR) }
                return null
            } else {
                return ostDir
            }
        } catch (e: SecurityException) {
            viewModelScope.launch(Dispatchers.Main.immediate) { _statusText.value = context.getString(R.string.error_permission_denied_download_subdir, Constants.FILES_DIR) }
            return null
        } catch (e: Exception) {
            viewModelScope.launch(Dispatchers.Main.immediate) { _statusText.value = context.getString(R.string.error_accessing_download_subdir, Constants.FILES_DIR, e.message ?: "") }
            return null
        }
    }


    private fun ensureReceiveFile(targetDir: File, originalFileName: String): File {
        val sanitizedFileName = File(originalFileName).name
        var file = File(targetDir, sanitizedFileName)
        var counter = 0
        val baseName = file.nameWithoutExtension
        val extension = file.extension.let { if (it.isNotEmpty()) ".$it" else "" }

        while (file.exists()) {
            counter++
            val newName = "$baseName($counter)$extension"
            file = File(targetDir, newName)
        }
        return file
    }

    @SuppressLint("DefaultLocale")
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return context.getString(R.string.zero_bytes)
        val units = arrayOf(
            context.getString(R.string.b),
            context.getString(R.string.kb),
            context.getString(R.string.mb),
            context.getString(R.string.gb),
            context.getString(R.string.tb)
        )
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt().coerceAtMost(units.size - 1)
        val safeDigitGroups = digitGroups.coerceAtLeast(0)
        val sizeInUnit = size / 1024.0.pow(safeDigitGroups.toDouble())
        val format = if (safeDigitGroups == 0) "%.0f" else "%.1f"
        val formattedSize = String.format(Locale.getDefault(), format, sizeInUnit)

        return "$formattedSize ${units[safeDigitGroups]}"
    }

    override fun onCleared() {
        super.onCleared()
        GlobalScope.launch(Dispatchers.IO) {
            launch { discoveryMutex.withLock { stopDiscoveryInternal() } }
        }
    }
}