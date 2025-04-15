package com.ost.application.ui.screen.share

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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.math.log10
import kotlin.math.pow

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    private val nsdManager = application.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var registeredServiceName: String? = null
    private val _isDiscoveryActive = MutableLiveData<Boolean>(false)
    val isDiscoveryActive: LiveData<Boolean> get() = _isDiscoveryActive
    private var selectedDevice: DiscoveredDevice? = null

    private val _discoveredDevices = MutableLiveData<Map<String, DiscoveredDevice>>(emptyMap())
    val discoveredDevices: LiveData<List<DiscoveredDevice>> = _discoveredDevices.map { map ->
        map.values.filter { it.isResolved }.sortedBy { it.name }
    }

    private val _transferStatus = MutableLiveData<String>("Idle.")
    val transferStatus: LiveData<String> get() = _transferStatus

    private val _transferProgress = MutableLiveData<Int?>()
    val transferProgress: LiveData<Int?> get() = _transferProgress

    private val _isTransferActive = MutableLiveData<Boolean>(false)
    val isTransferActive: LiveData<Boolean> get() = _isTransferActive

    private val _isReceivingActive = MutableLiveData<Boolean>(false)
    val isReceivingActive: LiveData<Boolean> get() = _isReceivingActive

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var clientHandlerJob: Job? = null
    private var transferJob: Job? = null
    var context = getApplication<Application>()


    fun setSelectedDevice(device: DiscoveredDevice) {
        selectedDevice = device
    }
    fun clearSelectedDevice() {
        selectedDevice = null
    }
    fun sendFileToSelectedDevice(fileUri: Uri) {
        val deviceToSend = selectedDevice
        if (deviceToSend != null) {
            sendFile(deviceToSend, fileUri)
        } else {
            _transferStatus.postValue(context.getString(R.string.error_no_device_selected))
        }
    }
    fun handlePermissionsGranted() {
        ensureDownloadOstDirectoryExists()
        if (isReceivingActive.value != true && isTransferActive.value != true) {
            startDiscovery()
        } else if (isReceivingActive.value == true) {
            if (_isTransferActive.value != true) {
                _transferStatus.postValue(context.getString(R.string.receiver_active_waiting))
            }
        }
    }

    fun setReceivingActive(isActive: Boolean) {
        viewModelScope.launch {
            if (isActive && _isReceivingActive.value != true) {
                startReceiving()
            } else if (!isActive && _isReceivingActive.value == true) {
                stopReceiving()
                if (_isTransferActive.value != true) {
                    startDiscovery()
                }
            }
        }
    }

    private fun startReceiving() {
        if (serverJob?.isActive == true) {
            return
        }
        _transferStatus.postValue(context.getString(R.string.starting_receiver))
        _isReceivingActive.postValue(false)
        stopDiscovery()

        serverJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                try {
                    serverSocket = ServerSocket(Constants.TRANSFER_PORT)
                    serverSocket?.reuseAddress = true
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _transferStatus.value = context.getString(
                            R.string.error_port_busy_or_unusable,
                            Constants.TRANSFER_PORT
                        )
                        _isReceivingActive.value = false
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    if (isActive) {
                        initializeRegistrationListener()
                        registerNsdService()
                    } else {
                        stopServerSocket()
                        return@withContext
                    }
                }

                if (isActive) {
                    val currentlyReceiving = withContext(Dispatchers.Main.immediate) { _isReceivingActive.value == true }
                    if (!currentlyReceiving) {
                        stopServerSocket()
                        return@launch
                    }

                    while (isActive) {
                        try {
                            val clientSocket = serverSocket!!.accept()
                            val stillReceiving = withContext(Dispatchers.Main.immediate) { _isReceivingActive.value == true }
                            if (!stillReceiving || !isActive) {
                                try { clientSocket.close() } catch (_: IOException) {}
                                break
                            }
                            clientHandlerJob?.cancelAndJoin()
                            clientHandlerJob = launch { handleIncomingConnection(clientSocket) }
                        } catch (e: SocketException) {
                            if (!isActive) break
                            if (serverSocket?.isClosed == false) delay(200)
                            else break
                        } catch (e: IOException) {
                            if (!isActive) break
                            delay(500)
                        }
                    }
                }

            } catch (e: CancellationException) {
            } catch (e: Exception) {
                if (isActive) {
                    withContext(Dispatchers.Main.immediate) {
                        _transferStatus.value = context.getString(R.string.receive_error, e.message)
                        _isReceivingActive.value = false
                    }
                }
            } finally {
                stopServerSocket()
                if (coroutineContext[Job] == serverJob) serverJob = null
                withContext(Dispatchers.Main.immediate) {
                    if (_isReceivingActive.value == true) {
                    }
                }
            }
        }
    }

    fun stopReceiving() {
        if (!_isReceivingActive.value!! && serverJob?.isActive != true) {
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.Main.immediate) {
                _transferStatus.value = context.getString(R.string.stopping_receiver)
            }

            clientHandlerJob?.cancelAndJoin()
            clientHandlerJob = null
            serverJob?.cancelAndJoin()
            serverJob = null

            unregisterNsdService()
            stopServerSocket()

            withContext(Dispatchers.Main.immediate) {
                _isReceivingActive.value = false
                if (_isTransferActive.value == true && _transferStatus.value?.contains(context.getString(R.string.receiving)) == true) {
                    _transferStatus.value = context.getString(R.string.receive_cancelled)
                    _transferProgress.value = null
                    _isTransferActive.value = false
                } else if (_transferStatus.value != context.getString(R.string.transfer_in_progress)) {
                    _transferStatus.value = context.getString(R.string.receiver_stopped)
                }
                if (_isTransferActive.value == true) {
                    _transferProgress.value = null
                    _isTransferActive.value = false
                }
            }
            NotificationHelper.cancelNotification(getApplication())
        }
    }


    private fun stopServerSocket() {
        try {
            if (serverSocket?.isClosed == false) { serverSocket?.close() }
        } catch (e: IOException) {
        } finally {
            serverSocket = null
        }
    }

    private suspend fun handleIncomingConnection(clientSocket: Socket) {
        var reader: BufferedReader? = null
        var writer: PrintWriter? = null
        var fileOutputStream: BufferedOutputStream? = null
        var socketInputStream: InputStream? = null
        var fileName = "unknown"
        var fileSize = 0L
        var fileToReceive: File? = null
        var receiveSuccess = false
        var totalBytesReceived: Long = 0
        var bytesRead: Int = 0
        val initialReceivingState = withContext(Dispatchers.Main.immediate) { _isReceivingActive.value == true }

        try {
            if (!initialReceivingState) {
                throw CancellationException("Receiving mode stopped before handling connection.")
            }

            clientSocket.soTimeout = 15000
            socketInputStream = clientSocket.getInputStream()
            reader = BufferedReader(InputStreamReader(socketInputStream, Charsets.UTF_8))
            writer = PrintWriter(OutputStreamWriter(clientSocket.getOutputStream(), Charsets.UTF_8), true)

            val requestLine = withTimeoutOrNull(15000) { reader.readLine() }
            if (requestLine == null) throw IOException(context.getString(R.string.client_disconnected_or_sent_empty_request_timeout))

            val parts = requestLine.split(Constants.CMD_SEPARATOR)
            if (parts.size < 3 || parts[0] != Constants.CMD_REQUEST_SEND) {
                writer.print(Constants.CMD_REJECT + "\n"); writer.flush()
                throw IllegalArgumentException(
                    context.getString(
                        R.string.invalid_request_format,
                        requestLine
                    ))
            }
            fileName = File(parts[1].removePercentEncoding() ?: parts[1]).name
            fileSize = parts[2].toLongOrNull() ?: 0L
            if (fileName.isBlank() || fileSize <= 0) {
                writer.print(Constants.CMD_REJECT + "\n"); writer.flush()
                throw IllegalArgumentException(context.getString(R.string.invalid_filename_or_size))
            }

            withContext(Dispatchers.Main.immediate) {
                if (_isReceivingActive.value != true) {
                    throw CancellationException("Receiving mode stopped before accepting file.")
                }
                _transferStatus.value = context.getString(R.string.incoming, fileName)
                _transferProgress.value = 0
                _isTransferActive.value = true
            }

            val userAccepted = true

            if (userAccepted) {
                writer.print(Constants.CMD_ACCEPT + "\n"); writer.flush()
                withContext(Dispatchers.Main) { _transferStatus.value =
                    context.getString(R.string.receiving, fileName) }
                NotificationHelper.showTransferNotification(getApplication(), fileName, 0, fileSize, false)

                clientSocket.soTimeout = 60000
                val receiveDir = ensureDownloadOstDirectoryExists() ?: throw IOException(
                    context.getString(
                        R.string.target_directory_error
                    ))
                fileToReceive = ensureReceiveFileExists(receiveDir, fileName) ?: throw IOException(
                    context.getString(
                        R.string.cannot_create_target_file
                    ))
                fileOutputStream = BufferedOutputStream(FileOutputStream(fileToReceive))

                val buffer = ByteArray(16384)
                var lastProgressUpdate = -1
                System.currentTimeMillis()

                while (currentCoroutineContext().isActive) {
                    try {
                        bytesRead = socketInputStream.read(buffer)
                    } catch (timeoutEx: SocketTimeoutException) {
                        if (!currentCoroutineContext().isActive || withContext(Dispatchers.Main.immediate){_isReceivingActive.value != true}) {
                            throw CancellationException("Transfer cancelled during read timeout.")
                        } else {
                            throw timeoutEx
                        }
                    }

                    if (bytesRead == -1) {
                        break
                    }

                    if (bytesRead > 0) {
                        try {
                            fileOutputStream.write(buffer, 0, bytesRead)
                            totalBytesReceived += bytesRead
                        } catch (writeEx: IOException) {
                            throw CancellationException(context.getString(R.string.file_write_error), writeEx)
                        }
                    }

                    val currentProgress = if (fileSize > 0) ((totalBytesReceived * 100) / fileSize).toInt() else 0
                    if (currentProgress > lastProgressUpdate) {
                        withContext(Dispatchers.Main.immediate) {
                            if (isActive && _isTransferActive.value == true) _transferProgress.value = currentProgress
                        }
                        NotificationHelper.showTransferNotification(getApplication(), fileName, currentProgress, fileSize, false)
                        lastProgressUpdate = currentProgress
                    }

                    if (fileSize > 0 && totalBytesReceived >= fileSize) {
                        break
                    }
                }

                fileOutputStream.flush()

                if (!currentCoroutineContext().isActive) throw CancellationException(
                    context.getString(
                        R.string.cancelled_after_loop
                    ))

                if (totalBytesReceived == fileSize) {
                    withContext(Dispatchers.Main) {
                        if (_transferProgress.value != 100) _transferProgress.value = 100
                        _transferStatus.value = context.getString(R.string.received, fileName)
                    }
                    NotificationHelper.showCompletionNotification(getApplication(), fileName, true, false)
                    receiveSuccess = true
                } else {
                    if (bytesRead == -1 && totalBytesReceived < fileSize) {
                        throw IOException(context.getString(R.string.error_client_disconnected_early))
                    } else {
                        throw IOException(context.getString(R.string.file_size_mismatch_after_transfer))
                    }
                }

            } else {
                writer.print(Constants.CMD_REJECT + "\n"); writer.flush()
                withContext(Dispatchers.Main.immediate) {
                    _transferStatus.value = context.getString(R.string.rejected_locally)
                    _isTransferActive.value = false
                    _transferProgress.value = null
                }
            }

        } catch (e: CancellationException) {
            withContext(Dispatchers.Main.immediate) {
                _transferStatus.value = context.getString(R.string.receive_cancelled)
                if (_isTransferActive.value == true) {
                    _transferProgress.value = null
                    _isTransferActive.value = false
                }
            }
            NotificationHelper.cancelNotification(getApplication())
        } catch (e: SocketTimeoutException) {
            withContext(Dispatchers.Main.immediate) {
                _transferStatus.value = context.getString(R.string.error_timeout)
                if (_isTransferActive.value == true) {
                    _transferProgress.value = null
                    _isTransferActive.value = false
                }
            }
            NotificationHelper.showCompletionNotification(getApplication(), fileName, false, false,
                context.getString(
                    R.string.timeout
                ))
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.javaClass.simpleName
            withContext(Dispatchers.Main.immediate) {
                _transferStatus.value = context.getString(R.string.error_msg_s, errorMsg)
                if (_isTransferActive.value == true) {
                    _transferProgress.value = null
                    _isTransferActive.value = false
                }
            }
            NotificationHelper.showCompletionNotification(getApplication(), fileName, false, false, errorMsg)
        } finally {
            try { fileOutputStream?.close() } catch (_: IOException) { }
            try { writer?.close() } catch (_: Exception) { }
            try { reader?.close() } catch (_: IOException) { }
            try { socketInputStream?.close()} catch (_: IOException) {}
            try { clientSocket.close() } catch (_: IOException) { }

            if (!receiveSuccess) {
                fileToReceive?.let { file ->
                    try {
                        if(file.exists()) {
                            if (!file.delete()) { }
                        }
                    } catch (e: Exception) { }
                }
            }

            withContext(Dispatchers.Main.immediate) {
                if (_isTransferActive.value == true) {
                    _transferProgress.value = null
                    _isTransferActive.value = false
                }

                val currentReceivingState = _isReceivingActive.value == true
                if (currentReceivingState) {
                    if (receiveSuccess) {
                        delay(1500)
                        if (_isReceivingActive.value == true && _isTransferActive.value == false) {
                            _transferStatus.value = context.getString(R.string.receiver_active_waiting)
                        }
                    } else if (_isTransferActive.value == false) {
                        _transferStatus.value = context.getString(R.string.receiver_active_waiting)
                    }
                } else {
                    if (_transferStatus.value != context.getString(R.string.receiver_stopped)) {
                        if (_isDiscoveryActive.value == true) {
                            _transferStatus.value = context.getString(R.string.searching)
                        } else {
                            _transferStatus.value = context.getString(R.string.receiver_stopped)
                        }
                    }
                }
            }

            if (kotlin.coroutines.coroutineContext[Job] == clientHandlerJob) {
                clientHandlerJob = null
            }
        }
    }


    private fun ensureReceiveFileExists(directory: File, originalFileName: String): File? {
        val sanitizedFileName = File(originalFileName).name.filter { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' || it == ' ' }.trim()
        if (sanitizedFileName.isBlank()) return null

        return try {
            var file = File(directory, sanitizedFileName)
            if (!file.name.contains('.')) {
                file = File(directory, sanitizedFileName + "_file")
            }

            var counter = 0
            val baseName = file.nameWithoutExtension
            val extension = file.extension.let { if (it.isNotEmpty()) ".$it" else "" }

            while (file.exists() && counter < 999) {
                counter++
                val newName = "$baseName($counter)$extension"
                if (newName.length > 250) {
                    return null
                }
                file = File(directory, newName)
            }

            if (counter >= 999) return null

            if (file.createNewFile()) {
                file
            } else {
                if (file.exists()) file else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun initializeRegistrationListener() {
        if (registrationListener != null) return
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                registeredServiceName = nsdServiceInfo.serviceName
                _isReceivingActive.postValue(true)
                _transferStatus.postValue(context.getString(R.string.receiver_active_waiting))
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                registeredServiceName = null
                viewModelScope.launch(Dispatchers.Main) {
                    _transferStatus.value = context.getString(
                        R.string.error_nsd_register_failed, errorCode
                    )
                    _isReceivingActive.value = false
                    stopServerSocket()
                }
                registrationListener = null
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                if (registeredServiceName == serviceInfo.serviceName) {
                    registeredServiceName = null
                }
                registrationListener = null
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                registeredServiceName = null
                registrationListener = null
            }
        }
    }
    @SuppressLint("HardwareIds")
    private fun registerNsdService() {
        val listener = registrationListener ?: run {
            viewModelScope.launch(Dispatchers.Main) {
                _transferStatus.value = context.getString(R.string.error_internal_state)
                _isReceivingActive.value = false
            }
            stopServerSocket()
            return
        }
        if (registeredServiceName != null) {
            _transferStatus.postValue(context.getString(R.string.receiver_active_waiting))
            return
        }

        val deviceName = Build.MODEL.replace("[^a-zA-Z0-9_]".toRegex(), "_").take(20)
        val randomSuffix = (100..999).random()
        val serviceName = "${Constants.SERVICE_NAME_PREFIX}${deviceName}_Phone_$randomSuffix"

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = Constants.SERVICE_TYPE
            this.port = Constants.TRANSFER_PORT
            setAttribute(Constants.KEY_DEVICE_TYPE, Constants.VALUE_DEVICE_PHONE)
        }
        try {
            if (registrationListener != null) {
                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
            } else {
                throw IllegalStateException("RegistrationListener was null during registerService call")
            }
        } catch (e: Exception) {
            viewModelScope.launch(Dispatchers.Main) {
                _transferStatus.value = context.getString(R.string.error_nsd_exception)
                _isReceivingActive.value = false
            }
            registrationListener = null
            stopServerSocket()
        }
    }
    private fun unregisterNsdService() {
        registrationListener?.let { listener ->
            try {
                nsdManager.unregisterService(listener)
            } catch (e: Exception) {
                registrationListener = null
                registeredServiceName = null
            }
        } ?: run {
            registeredServiceName = null
        }
    }

    fun startDiscovery() {
        if (_isDiscoveryActive.value == true) { return }
        if (_isReceivingActive.value == true || _transferStatus.value == context.getString(R.string.starting_receiver)) {
            return
        }
        if (_isTransferActive.value == true) {
            return
        }

        _discoveredDevices.value = emptyMap()
        _transferStatus.postValue(context.getString(R.string.searching))
        _isDiscoveryActive.postValue(true)

        try {
            discoveryListener?.let {
                try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {}
                discoveryListener = null
            }

            initializeDiscoveryListener()
            discoveryListener?.let { listener ->
                nsdManager.discoverServices(Constants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            } ?: throw IllegalStateException("DiscoveryListener was null during discoverServices call")

        } catch (e: Exception) {
            _transferStatus.postValue(context.getString(R.string.error_discovery_start))
            _isDiscoveryActive.postValue(false)
            discoveryListener = null
        }
    }
    fun stopDiscovery() {
        if (_isDiscoveryActive.value != true && discoveryListener == null) {
            return
        }

        discoveryListener?.let { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            }
            catch (e: Exception) {
                _isDiscoveryActive.postValue(false)
                discoveryListener = null
                if (_isTransferActive.value != true && _isReceivingActive.value != true) {
                    _transferStatus.postValue(context.getString(R.string.discovery_stopped))
                }
            }
        } ?: run {
            if (_isDiscoveryActive.value == true) {
                _isDiscoveryActive.postValue(false)
                if (_isTransferActive.value != true && _isReceivingActive.value != true) {
                    _transferStatus.postValue(context.getString(R.string.discovery_stopped))
                }
            }
        }
    }
    private fun initializeDiscoveryListener() {
        if (discoveryListener != null) return
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                _isDiscoveryActive.postValue(true)
                if (_isTransferActive.value != true && _isReceivingActive.value != true) {
                    _transferStatus.postValue(context.getString(R.string.searching))
                }
            }
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceName == registeredServiceName) {
                    return
                }
                if (service.serviceType != Constants.SERVICE_TYPE || !service.serviceName.startsWith(Constants.SERVICE_NAME_PREFIX)) {
                    return
                }
                viewModelScope.launch(Dispatchers.Main) {
                    val currentDevices = _discoveredDevices.value ?: emptyMap()
                    val existingDevice = currentDevices[service.serviceName]
                    if (existingDevice == null) {
                        _discoveredDevices.value = currentDevices + (service.serviceName to DiscoveredDevice(service))
                        resolveService(service)
                    } else if (!existingDevice.isResolved && !existingDevice.isResolving) {
                        resolveService(service)
                    }
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                viewModelScope.launch(Dispatchers.Main){
                    val currentDevices = _discoveredDevices.value ?: emptyMap()
                    val lostDevice = currentDevices[service.serviceName]
                    if (lostDevice != null) {
                        val name = lostDevice.name
                        _discoveredDevices.value = currentDevices - service.serviceName
                        if (_isTransferActive.value != true && _isReceivingActive.value != true) {
                            _transferStatus.postValue(context.getString(R.string.lost, name))
                            if(_isDiscoveryActive.value==true){
                                delay(2000)
                                if(_isDiscoveryActive.value==true && _isTransferActive.value != true && _isReceivingActive.value != true) {
                                    _transferStatus.postValue(context.getString(R.string.searching))
                                }
                            }
                        }
                    }
                }
            }
            override fun onDiscoveryStopped(serviceType: String) {
                _isDiscoveryActive.postValue(false)
                if(_isTransferActive.value != true && _isReceivingActive.value != true) {
                    _transferStatus.postValue(context.getString(R.string.discovery_stopped))
                }
                discoveryListener = null
            }
            override fun onStartDiscoveryFailed(serviceType: String, errCode: Int) {
                _transferStatus.postValue(
                    context.getString(
                        R.string.error_discovery_failed, errCode
                    ))
                _isDiscoveryActive.postValue(false)
                discoveryListener = null
            }
            override fun onStopDiscoveryFailed(serviceType: String, errCode: Int) {
                discoveryListener = null
            }
        }
    }
    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val isCurrentlyResolving = _discoveredDevices.value?.get(serviceInfo.serviceName)?.isResolving == true
        if (isCurrentlyResolving) {
            return
        }

        viewModelScope.launch(Dispatchers.Main.immediate) {
            _discoveredDevices.value = _discoveredDevices.value?.let { currentMap ->
                val device = currentMap[serviceInfo.serviceName]
                if (device != null && !device.isResolved) {
                    currentMap + (serviceInfo.serviceName to device.copy(isResolving = true))
                } else {
                    currentMap
                }
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                private var alreadyHandled = false

                override fun onResolveFailed(si: NsdServiceInfo, errCode: Int) {
                    if(alreadyHandled) return
                    alreadyHandled = true

                    viewModelScope.launch(Dispatchers.Main.immediate){
                        _discoveredDevices.value = _discoveredDevices.value?.minus(si.serviceName)
                        if(_isTransferActive.value != true && _isReceivingActive.value != true) {
                            si.serviceName.removePrefix(Constants.SERVICE_NAME_PREFIX).substringBeforeLast('_').replace("_", " ")
                        }
                    }
                }
                override fun onServiceResolved(si: NsdServiceInfo) {
                    if(alreadyHandled) return
                    alreadyHandled = true

                    val type = si.attributes[Constants.KEY_DEVICE_TYPE]?.let {
                        try { String(it, Charsets.UTF_8) } catch (e: Exception) { Constants.VALUE_DEVICE_UNKNOWN }
                    } ?: Constants.VALUE_DEVICE_UNKNOWN

                    viewModelScope.launch(Dispatchers.Main.immediate){
                        val resolvedDevice = DiscoveredDevice(si, type)
                        _discoveredDevices.value = _discoveredDevices.value?.plus(resolvedDevice.id to resolvedDevice)
                    }
                }
            })
        } catch (e: Exception) {
            viewModelScope.launch(Dispatchers.Main.immediate){
                _discoveredDevices.value = _discoveredDevices.value?.minus(serviceInfo.serviceName)
                if(_isTransferActive.value != true && _isReceivingActive.value != true) {
                }
            }
        }
    }

    fun sendFile(device: DiscoveredDevice, fileUri: Uri) {
        if (transferJob?.isActive == true || _isTransferActive.value == true) {
            _transferStatus.postValue(context.getString(R.string.transfer_in_progress)); return
        }
        if (!device.isResolved || device.ipAddress == null || device.port <= 0) {
            _transferStatus.postValue(context.getString(R.string.device_not_ready)); return
        }

        val fileInfo = getFileInfoFromUri(getApplication(), fileUri) ?: run {
            _transferStatus.postValue(context.getString(R.string.error_file_info))
            NotificationHelper.showCompletionNotification(getApplication(), "?", false, true, context.getString(R.string.read_error))
            return
        }
        val fileName = fileInfo.first; val fileSize = fileInfo.second
        if (fileSize <= 0) {
            _transferStatus.postValue(context.getString(R.string.error_empty_file))
            NotificationHelper.showCompletionNotification(getApplication(), fileName, false, true, "Empty file")
            return
        }
        val targetIp = device.ipAddress; val targetPort = device.port

        stopDiscovery()

        transferJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main.immediate) {
                _transferStatus.value = context.getString(R.string.connecting_to, device.name)
                _transferProgress.value = 0
                _isTransferActive.value = true
            }
            NotificationHelper.showTransferNotification(getApplication(), fileName, 0, fileSize, true, true)

            var socket: Socket? = null; var writer: PrintWriter? = null; var reader: BufferedReader? = null; var fileInputStream: InputStream? = null; var socketOutputStream: BufferedOutputStream? = null; var transferSuccess = false
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(targetIp, targetPort), 7000)
                socket.soTimeout = 15000

                withContext(Dispatchers.Main) { _transferStatus.value =
                    context.getString(R.string.requesting_send) }

                writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)
                reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))

                val encodedFileName = java.net.URLEncoder.encode(fileName, Charsets.UTF_8.name())
                val request = "${Constants.CMD_REQUEST_SEND}${Constants.CMD_SEPARATOR}$encodedFileName${Constants.CMD_SEPARATOR}$fileSize"
                writer.println(request)

                val response = withTimeoutOrNull(15000) { reader.readLine() }

                if (response == Constants.CMD_ACCEPT) {
                    withContext(Dispatchers.Main) { _transferStatus.value =
                        context.getString(R.string.sending, fileName, formatFileSize(fileSize)) }
                    NotificationHelper.showTransferNotification(getApplication(), fileName, 0, fileSize, true, false)
                    socket.soTimeout = 60000

                    fileInputStream = context.contentResolver.openInputStream(fileUri) ?: throw IOException(
                        context.getString(R.string.failed_open_uri, fileUri)
                    )
                    socketOutputStream = BufferedOutputStream(socket.getOutputStream())

                    val buffer = ByteArray(16384); var bytesRead: Int; var totalBytesSent: Long = 0; var lastProgressUpdate = -1; val interval = 100L; var lastUpdate = System.currentTimeMillis()
                    while (isActive) {
                        bytesRead = fileInputStream.read(buffer)
                        if (bytesRead == -1) break

                        try {
                            socketOutputStream.write(buffer, 0, bytesRead)
                            totalBytesSent += bytesRead
                        } catch (sockEx: IOException) {
                            throw CancellationException(context.getString(R.string.error_connection_lost_during_send), sockEx)
                        }

                        val progress = ((totalBytesSent * 100) / fileSize).toInt()
                        val time = System.currentTimeMillis()
                        if (progress > lastProgressUpdate && time - lastUpdate >= interval || progress == 100) {
                            withContext(Dispatchers.Main.immediate) {
                                if (isActive && _isTransferActive.value == true) _transferProgress.value = progress
                            }
                            NotificationHelper.showTransferNotification(getApplication(), fileName, progress, fileSize, true)
                            lastProgressUpdate = progress; lastUpdate = time
                        }
                    }
                    socketOutputStream.flush()

                    if (!isActive) throw CancellationException(context.getString(R.string.cancelled_during_send))

                    if (totalBytesSent != fileSize) {
                        throw IOException(
                            context.getString(
                                R.string.sent_bytes_file_size,
                                totalBytesSent,
                                fileSize
                            ))
                    }

                    transferSuccess = true
                    withContext(Dispatchers.Main) {
                        if (_transferProgress.value != 100) _transferProgress.value = 100
                        _transferStatus.value = context.getString(R.string.sent, fileName)
                    }
                    NotificationHelper.showCompletionNotification(getApplication(), fileName, true, true)

                } else {
                    withContext(Dispatchers.Main) { _transferStatus.value =
                        context.getString(R.string.rejected_by, device.name) }
                    NotificationHelper.showCompletionNotification(getApplication(), fileName, false, true,
                        context.getString(
                            R.string.rejected_by_receiver
                        ))
                }
            } catch (e: CancellationException) {
                withContext(Dispatchers.Main.immediate){
                    _transferStatus.value = context.getString(R.string.transfer_cancelled)
                    if (_isTransferActive.value == true) {
                        _transferProgress.value = null
                        _isTransferActive.value = false
                    }
                }
                NotificationHelper.cancelNotification(getApplication())
            } catch (e: SocketTimeoutException) {
                withContext(Dispatchers.Main.immediate){
                    _transferStatus.value = context.getString(R.string.error_timeout)
                    if (_isTransferActive.value == true) {
                        _transferProgress.value = null
                        _isTransferActive.value = false
                    }
                }
                NotificationHelper.showCompletionNotification(getApplication(),fileName,false,true,"Timeout")
            } catch (e: Exception) {
                val msg=e.message?:e.javaClass.simpleName
                withContext(Dispatchers.Main.immediate){
                    _transferStatus.value = context.getString(R.string.error_msg_s, msg)
                    if (_isTransferActive.value == true) {
                        _transferProgress.value = null
                        _isTransferActive.value = false
                    }
                }
                NotificationHelper.showCompletionNotification(getApplication(),fileName,false,true,msg)
            } finally {
                try{fileInputStream?.close()}catch(_:IOException){}
                try{socketOutputStream?.close()}catch(_:IOException){}
                try{writer?.close()}catch(_:Exception){}
                try{reader?.close()}catch(_:IOException){}
                try{socket?.close()}catch(_:IOException){}

                withContext(Dispatchers.Main.immediate) {
                    if (_isTransferActive.value == true) {
                        _transferProgress.value = null
                        _isTransferActive.value = false
                    }

                    if (transferSuccess) {
                        delay(1500)
                    }

                    if (_isReceivingActive.value == true) {
                        _transferStatus.value = context.getString(R.string.receiver_active_waiting)
                    } else {
                        startDiscovery()
                        if (_isDiscoveryActive.value != true) {
                            _transferStatus.value = "Idle."
                        }
                    }
                }
                if (kotlin.coroutines.coroutineContext[Job] == transferJob) transferJob = null
            }
        }
    }
    fun cancelTransfer() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            if (transferJob?.isActive == true) {
                _transferStatus.value = context.getString(R.string.cancelling)
                transferJob?.cancel(CancellationException(context.getString(R.string.cancelled_by_user)))
            } else if (clientHandlerJob?.isActive == true) {
                _transferStatus.value = context.getString(R.string.cancelling)
                clientHandlerJob?.cancel(CancellationException(context.getString(R.string.cancelled_by_user)))
            } else {
                if (_isTransferActive.value == true) {
                    _transferProgress.value = null
                    _isTransferActive.value = false
                    _transferStatus.value = context.getString(R.string.transfer_cancelled)
                    NotificationHelper.cancelNotification(getApplication())
                }
            }
        }
    }

    @SuppressLint("UnsanitizedFilenameFromContentProvider")
    private fun getFileInfoFromUri(context: Context, uri: Uri): Pair<String, Long>? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else "unknown_${System.currentTimeMillis()}"
                    val cleanName = File(name).name.filter { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' || it == ' ' }.trim().ifBlank { "file_${System.currentTimeMillis()}" }

                    var size = if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L

                    if (size <= 0) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use {
                                val availableBytes = it.available()
                                size = if (availableBytes > 0) availableBytes.toLong() else -1L
                            } ?: run { size = -1L }
                        } catch (ioe: IOException) {
                            size = -1L
                        }
                    }

                    if (size >= 0) cleanName to size else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
    @SuppressLint("DefaultLocale")
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 ${context.getString(R.string.b)}"; val u = arrayOf(context.getString(R.string.b),
            context.getString(R.string.kb), context.getString(R.string.mb), context.getString(R.string.gb),
            context.getString(R.string.tb)); val d = (log10(size.toDouble()) / log10(1024.0)).toInt().coerceAtMost(u.size-1).coerceAtLeast(0); val s = size / 1024.0.pow(d.toDouble()); return (if (d == 0) String.format("%d", size.toInt()) else String.format("%.1f", s)) + " " + u[d]
    }
    fun ensureDownloadOstDirectoryExists(): File? {
        val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?: run {
                viewModelScope.launch(Dispatchers.Main) { _transferStatus.value = "Error: No Downloads dir" }
                return null
            }
        val ostDir = File(dlDir, Constants.FILES_DIR)
        return try {
            if (!ostDir.exists()) {
                if (!ostDir.mkdirs()) {
                    viewModelScope.launch(Dispatchers.Main) { _transferStatus.value = context.getString(R.string.error_cannot_create_dir) }
                    null
                } else {
                    ostDir
                }
            } else if (!ostDir.isDirectory) {
                viewModelScope.launch(Dispatchers.Main) { _transferStatus.value = context.getString(R.string.error_path_not_dir) }
                null
            } else {
                ostDir
            }
        } catch(e: SecurityException){
            viewModelScope.launch(Dispatchers.Main) { _transferStatus.value = context.getString(R.string.error_permission) }
            null
        } catch(e: Exception){
            viewModelScope.launch(Dispatchers.Main) { _transferStatus.value = context.getString(R.string.error_access_dir) }
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            stopDiscovery()
            stopReceiving()
            cancelTransfer()
            discoveryListener = null
            registrationListener = null
        }
    }
}

fun String.removePercentEncoding(): String? {
    return try {
        java.net.URLDecoder.decode(this, Charsets.UTF_8.name())
    } catch (e: Exception) {
        this
    }
}

data class DiscoveredDevice(
    val id: String,
    var name: String,
    var type: String = Constants.VALUE_DEVICE_UNKNOWN,
    var isResolved: Boolean = false,
    var ipAddress: java.net.InetAddress? = null,
    var port: Int = -1,
    var isResolving: Boolean = false
) {
    constructor(serviceInfo: NsdServiceInfo) : this(
        id = serviceInfo.serviceName,
        name = serviceInfo.serviceName
            .removePrefix(Constants.SERVICE_NAME_PREFIX)
            .substringBeforeLast('_')
            .replace("_", " ")
            .ifBlank { serviceInfo.serviceName },
        type = Constants.VALUE_DEVICE_UNKNOWN,
        isResolved = false,
        ipAddress = null,
        port = -1,
        isResolving = false
    )

    constructor(serviceInfo: NsdServiceInfo, resolvedType: String) : this(
        id = serviceInfo.serviceName,
        name = serviceInfo.serviceName
            .removePrefix(Constants.SERVICE_NAME_PREFIX)
            .substringBeforeLast('_')
            .replace("_", " ")
            .ifBlank { serviceInfo.serviceName },
        type = resolvedType,
        isResolved = (serviceInfo.host != null && serviceInfo.port > 0),
        ipAddress = serviceInfo.host,
        port = serviceInfo.port,
        isResolving = false
    )
}