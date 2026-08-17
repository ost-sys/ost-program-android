package com.ost.application.ui.screen.share

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import com.ost.application.R
import com.ost.application.core.share.Constants
import com.ost.application.core.share.DiscoveredDevice
import com.ost.application.core.share.IncomingTransferRequest
import com.ost.application.core.share.LocalSendCertificateHelper
import com.ost.application.core.share.LocalSendClient
import com.ost.application.core.share.LocalSendDeviceType
import com.ost.application.core.share.LocalSendFileSender
import com.ost.application.core.share.LocalSendInfo
import com.ost.application.core.share.LocalSendPrepareRequest
import com.ost.application.core.share.LocalSendServer
import com.ost.application.core.share.NsdHandler
import com.ost.application.core.share.SubnetScanner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.concurrent.ConcurrentHashMap

class LocalSendService : Service() {

    companion object {
        const val ACTION_LS_START_RECEIVE   = "com.ost.application.localsend.ACTION_START_RECEIVE"
        const val ACTION_LS_STOP_RECEIVE    = "com.ost.application.localsend.ACTION_STOP_RECEIVE"
        const val ACTION_LS_START_DISCOVERY = "com.ost.application.localsend.ACTION_START_DISCOVERY"
        const val ACTION_LS_STOP_DISCOVERY  = "com.ost.application.localsend.ACTION_STOP_DISCOVERY"
        const val ACTION_LS_SEND_FILES      = "com.ost.application.localsend.ACTION_SEND_FILES"
        const val ACTION_LS_CANCEL          = "com.ost.application.localsend.ACTION_CANCEL"
        const val ACTION_LS_ACCEPT          = "com.ost.application.localsend.ACTION_ACCEPT"
        const val ACTION_LS_REJECT          = "com.ost.application.localsend.ACTION_REJECT"
        const val EXTRA_FILE_URIS           = "extra_ls_file_uris"
        const val EXTRA_TARGET_DEVICE       = "extra_ls_target_device"

        private const val LS_SERVICE_TYPE   = "_localsend._tcp."
        private const val LS_PORT           = 53317
        private const val LS_MULTICAST_ADDR = "224.0.0.167"
        private const val LS_NOTIF_ID       = Constants.NOTIFICATION_ID_SERVICE_FOREGROUND + 10
        private const val TAG               = "LocalSendServicePhone"
        private const val ANNOUNCE_INTERVAL = 5000L
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var nsdHandler: NsdHandler
    private lateinit var server: LocalSendServer
    private lateinit var fileSender: LocalSendFileSender

    private var udpAnnounceJob: Job? = null
    private var udpListenJob: Job? = null

    private val _isReceiveActive    = MutableStateFlow(false)
    val isReceiveActive: StateFlow<Boolean> = _isReceiveActive.asStateFlow()

    private val _statusText         = MutableStateFlow("Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _incomingRequest    = MutableStateFlow<LocalSendPrepareRequest?>(null)
    val incomingRequest: StateFlow<LocalSendPrepareRequest?> = _incomingRequest.asStateFlow()

    private val _incomingTransferRequest = MutableStateFlow<IncomingTransferRequest?>(null)
    val incomingTransferRequest: StateFlow<IncomingTransferRequest?> = _incomingTransferRequest.asStateFlow()

    private val _lastReceivedFiles  = MutableStateFlow<List<File>>(emptyList())
    val lastReceivedFiles: StateFlow<List<File>> = _lastReceivedFiles.asStateFlow()

    private val _receiverProgress   = MutableStateFlow<Int?>(null)

    val isDiscovering: StateFlow<Boolean> get() = nsdHandler.isDiscovering
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> get() = nsdHandler.discoveredDevices
    val isSendingActive: StateFlow<Boolean> get() = fileSender.isSendingActive

    lateinit var transferProgress: StateFlow<Int?>
    lateinit var transferFileStatus: StateFlow<String?>

    private var pendingDecision: CompletableDeferred<Boolean>? = null
    private val fileProgressMap = ConcurrentHashMap<String, Long>()
    private var totalBytesToReceive = 0L

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        NotificationHelper.createAppNotificationChannels(this)

        nsdHandler = NsdHandler(
            context = applicationContext,
            scope = serviceScope,
            deviceType = "mobile",
            serviceType = LS_SERVICE_TYPE
        )

        server = LocalSendServer(applicationContext).also { srv ->
            srv.onDeviceRegistered = { device ->
                nsdHandler.addDiscoveredDevice(device)
            }
            srv.onPrepareUpload = { request ->
                val deferred = CompletableDeferred<Boolean>()
                pendingDecision = deferred
                _incomingRequest.value = request

                val fileNames = request.files.values.map { it.fileName }
                totalBytesToReceive = request.files.values.sumOf { it.size }
                fileProgressMap.clear()

                _incomingTransferRequest.value = IncomingTransferRequest(
                    requestId = request.info.fingerprint,
                    senderDeviceName = request.info.alias,
                    fileNames = fileNames,
                    totalSize = totalBytesToReceive,
                    deferredConfirmation = deferred
                )

                updateForeground()
                deferred.await()
            }
            srv.onTransferProgress = { _, fileId, bytesReceived ->
                fileProgressMap[fileId] = bytesReceived
                val received = fileProgressMap.values.sum()
                if (totalBytesToReceive > 0) {
                    val pct = ((received * 100) / totalBytesToReceive).toInt().coerceIn(0, 100)
                    _receiverProgress.value = pct
                    _statusText.value = "Receiving... $pct%"
                    updateForeground()
                }
            }
            srv.onTransferComplete = { _, _, file ->
                fileProgressMap.remove(file.name)
                _statusText.value = "RECEIVED"
                _receiverProgress.value = null
                totalBytesToReceive = 0
                _incomingTransferRequest.value = null
                refreshReceivedFiles()
                updateForeground()
            }
        }

        fileSender = LocalSendFileSender(
            context = applicationContext,
            scope = serviceScope
        ) { status ->
            _statusText.value = status
            updateForeground()
        }

        transferProgress = combine(
            _receiverProgress,
            fileSender.transferProgress
        ) { recProgress, sendProgress ->
            sendProgress ?: recProgress
        }.distinctUntilChanged().stateIn(
            scope = serviceScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        transferFileStatus = fileSender.transferFileStatus

        serviceScope.launch {
            fileSender.transferProgress.collect {
                updateForeground()
            }
        }

        refreshReceivedFiles()
        updateForeground()
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        when (intent?.action) {
            ACTION_LS_START_RECEIVE   -> startReceive()
            ACTION_LS_STOP_RECEIVE    -> stopReceive()
            ACTION_LS_START_DISCOVERY -> startDiscovery()
            ACTION_LS_STOP_DISCOVERY  -> nsdHandler.stopDiscovery()
            ACTION_LS_SEND_FILES      -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(EXTRA_FILE_URIS)
                val target = intent.getParcelableExtra<DiscoveredDevice>(EXTRA_TARGET_DEVICE)
                if (!uris.isNullOrEmpty() && target != null) {
                    sendFiles(uris, target)
                }
            }
            ACTION_LS_CANCEL          -> cancelTransfer()
            ACTION_LS_ACCEPT          -> acceptTransfer()
            ACTION_LS_REJECT          -> rejectTransfer()
        }
        return START_STICKY
    }

    fun startReceive() {
        if (_isReceiveActive.value) return
        Log.d(TAG, "startReceive")
        _isReceiveActive.value = true
        _statusText.value = "LocalSend Active"

        val (port, fingerprint) = server.start(LS_PORT)

        val deviceName = Build.MODEL ?: "Android Phone"
        val txtRecords = mapOf(
            "alias"       to deviceName,
            "version"     to "2.0",
            "v"           to "2.0",
            "deviceModel" to deviceName,
            "devicemodel" to deviceName,
            "deviceType"  to "mobile",
            "devicetype"  to "mobile",
            "fingerprint" to fingerprint,
            "port"        to port.toString(),
            "protocol"    to "https",
            "download"    to "false"
        )
        nsdHandler.startServiceRegistration(
            port = port,
            customServiceName = deviceName,
            extraAttributes = txtRecords,
            currentStatusUpdate = { _statusText.value = it }
        )

        startUdpAnnounce(deviceName, fingerprint, port)
        startUdpListen(deviceName, fingerprint)
        startSubnetScan(deviceName, fingerprint, port)
        updateForeground()
    }

    fun stopReceive() {
        if (!_isReceiveActive.value) return
        Log.d(TAG, "stopReceive")
        _isReceiveActive.value = false
        _statusText.value = "Ready"
        server.stop()
        nsdHandler.stopServiceRegistration()
        stopUdpMulticast()
        if (!isDiscovering.value) {
            subnetScanJob?.cancel()
            subnetScanJob = null
        }
        updateForeground()
    }

    private var subnetScanJob: Job? = null

    fun startDiscovery() {
        Log.d(TAG, "startDiscovery")
        nsdHandler.startDiscovery { status ->
            _statusText.value = status
        }
        val deviceName = Build.MODEL ?: "Android Phone"
        val (keyStore, fp) = LocalSendCertificateHelper.getOrGenerateKeyStore()
        startUdpAnnounce(deviceName, fp, server.actualPort)
        startUdpListen(deviceName, fp)
        startSubnetScan(deviceName, fp, server.actualPort)
    }

    private fun startSubnetScan(selfDeviceName: String, selfFingerprint: String, selfPort: Int) {
        subnetScanJob?.cancel()
        subnetScanJob = SubnetScanner.startSubnetScan(
            scope = serviceScope,
            selfDeviceName = selfDeviceName,
            selfFingerprint = selfFingerprint,
            selfPort = selfPort,
            onDeviceFound = { device ->
                nsdHandler.addDiscoveredDevice(device)
            }
        )
    }

    fun stopDiscovery() {
        Log.d(TAG, "stopDiscovery")
        nsdHandler.stopDiscovery()
        subnetScanJob?.cancel()
        subnetScanJob = null
        if (!_isReceiveActive.value) {
            stopUdpMulticast()
        }
    }

    fun sendFiles(uris: List<Uri>, target: DiscoveredDevice) {
        fileSender.sendFiles(target, uris)
    }

    fun cancelTransfer() {
        fileSender.cancelTransfer()
        pendingDecision?.complete(false)
        pendingDecision = null
        _incomingRequest.value = null
        _incomingTransferRequest.value = null
        _receiverProgress.value = null
        totalBytesToReceive = 0
        updateForeground()
    }

    fun acceptTransfer() {
        pendingDecision?.complete(true)
        pendingDecision = null
        _incomingRequest.value = null
        _incomingTransferRequest.value = null
        updateForeground()
    }

    fun rejectTransfer() {
        pendingDecision?.complete(false)
        pendingDecision = null
        _incomingRequest.value = null
        _incomingTransferRequest.value = null
        updateForeground()
    }

    private fun buildAnnouncementJson(alias: String, fingerprint: String, announce: Boolean, port: Int = LS_PORT): ByteArray {
        return JSONObject().apply {
            put("alias", alias)
            put("version", "2.0")
            put("deviceModel", Build.MODEL)
            put("deviceType", "mobile")
            put("fingerprint", fingerprint)
            put("port", port)
            put("protocol", "https")
            put("download", false)
            put("announce", announce)
        }.toString().toByteArray(Charsets.UTF_8)
    }

    private fun startUdpAnnounce(selfDeviceName: String, selfFingerprint: String, port: Int) {
        udpAnnounceJob?.cancel()
        udpAnnounceJob = serviceScope.launch(Dispatchers.IO) {
            try {
                DatagramSocket().use { sock ->
                    sock.broadcast = true
                    val data = buildAnnouncementJson(selfDeviceName, selfFingerprint, announce = true, port = port)
                    val group = InetAddress.getByName(LS_MULTICAST_ADDR)
                    val packetMulticast = DatagramPacket(data, data.size, group, LS_PORT)
                    val packetBroadcast = DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), LS_PORT)

                    sock.send(packetMulticast)
                    try { sock.send(packetBroadcast) } catch (_: Exception) {}

                    SubnetScanner.getAllLocalIpv4AndSubnets().forEach { (_, prefix) ->
                        try {
                            val subnetBcast = DatagramPacket(data, data.size, InetAddress.getByName("${prefix}255"), LS_PORT)
                            sock.send(subnetBcast)
                        } catch (_: Exception) {}
                    }

                    while (isActive) {
                        delay(ANNOUNCE_INTERVAL)
                        if (!isActive) break
                        sock.send(packetMulticast)
                        try { sock.send(packetBroadcast) } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "UDP announce error: ${e.message}")
            }
        }
    }

    private fun startUdpListen(selfDeviceName: String, selfFingerprint: String) {
        udpListenJob?.cancel()
        udpListenJob = serviceScope.launch(Dispatchers.IO) {
            try {
                val sock = MulticastSocket(null).apply {
                    reuseAddress = true
                    try {
                        bind(java.net.InetSocketAddress(LS_PORT))
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not bind multicast to $LS_PORT, using random port: ${e.message}")
                        bind(java.net.InetSocketAddress(0))
                    }
                    val group = InetAddress.getByName(LS_MULTICAST_ADDR)
                    try {
                        joinGroup(group)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not join multicast group: ${e.message}")
                    }
                    soTimeout = 1000
                }
                sock.use { s ->
                    Log.d(TAG, "UDP multicast listener started on $LS_MULTICAST_ADDR:$LS_PORT")
                    val buf = ByteArray(4096)
                    while (isActive) {
                        val packet = DatagramPacket(buf, buf.size)
                        try {
                            s.receive(packet)
                        } catch (_: java.net.SocketTimeoutException) {
                            continue
                        }
                        val json = try {
                            JSONObject(String(packet.data, 0, packet.length, Charsets.UTF_8))
                        } catch (_: Exception) { continue }

                        val remoteFingerprint = json.optString("fingerprint")
                        if (remoteFingerprint == selfFingerprint) continue

                        val isAnnounce = json.optBoolean("announce", false)
                        val alias      = json.optString("alias", "Unknown")
                        val port       = json.optInt("port", LS_PORT)
                        val deviceType = json.optString("deviceType", "desktop")

                        val discovered = DiscoveredDevice(
                            id          = remoteFingerprint.ifBlank { "${packet.address.hostAddress}:$port" },
                            name        = alias,
                            type        = deviceType,
                            isResolved  = true,
                            ipAddress   = packet.address,
                            port        = port,
                            isResolving = false
                        )
                        nsdHandler.addDiscoveredDevice(discovered)

                        if (isAnnounce) {
                            val reply = buildAnnouncementJson(selfDeviceName, selfFingerprint, announce = false, port = server.actualPort)
                            DatagramSocket().use { replySock ->
                                replySock.send(DatagramPacket(reply, reply.size, packet.address, port))
                            }

                            val selfInfo = LocalSendInfo(
                                alias = selfDeviceName,
                                version = "2.0",
                                deviceModel = Build.MODEL,
                                deviceType = LocalSendDeviceType.MOBILE.value,
                                fingerprint = selfFingerprint,
                                port = server.actualPort,
                                protocol = "https",
                                download = false
                            )
                            serviceScope.launch(Dispatchers.IO) {
                                LocalSendClient().register(packet.address.hostAddress, port, selfInfo)
                            }
                        }
                    }
                    try {
                        val group = InetAddress.getByName(LS_MULTICAST_ADDR)
                        s.leaveGroup(group)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w(TAG, "UDP listen error: ${e.message}")
            }
        }
    }

    private fun stopUdpMulticast() {
        udpAnnounceJob?.cancel()
        udpAnnounceJob = null
        udpListenJob?.cancel()
        udpListenJob = null
    }

    fun resolveDevice(serviceInfo: NsdServiceInfo) = nsdHandler.resolveDevice(serviceInfo)

    private fun refreshReceivedFiles() {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fallbackDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val baseDir = publicDownloads ?: fallbackDir
        val ostDir = File(baseDir, Constants.FILES_DIR)
        _lastReceivedFiles.value = if (ostDir.exists() && ostDir.isDirectory)
            ostDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        else emptyList()
    }

    private fun updateForeground() {
        val notif = NotificationHelper.buildForegroundServiceNotification(
            this,
            _statusText.value
        )
        startForeground(LS_NOTIF_ID, notif.build())
    }

    override fun onBind(intent: Intent?): IBinder = ServiceBinder()

    inner class ServiceBinder : Binder() {
        fun getService(): LocalSendService = this@LocalSendService
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        server.stop()
        fileSender.shutdown()
        nsdHandler.shutdown()
        stopUdpMulticast()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
