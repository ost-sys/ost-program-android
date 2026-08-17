package com.ost.application.share

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import com.ost.application.R
import com.ost.application.core.share.Constants
import com.ost.application.core.share.DiscoveredDevice
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        private const val LS_NOTIF_ID       = Constants.NOTIFICATION_ID_SERVICE_FOREGROUND + 1
        private const val TAG               = "LocalSendService"
        private const val ANNOUNCE_INTERVAL = 5000L
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var nsdHandler: NsdHandler
    private lateinit var server: LocalSendServer
    private lateinit var fileSender: LocalSendFileSender

    private var multicastSocket: MulticastSocket? = null
    private var udpAnnounceJob: Job? = null
    private var udpListenJob: Job? = null

    private val _isReceiveActive    = MutableStateFlow(false)
    val isReceiveActive: StateFlow<Boolean> = _isReceiveActive.asStateFlow()

    private val _statusText         = MutableStateFlow("Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _incomingRequest    = MutableStateFlow<LocalSendPrepareRequest?>(null)
    val incomingRequest: StateFlow<LocalSendPrepareRequest?> = _incomingRequest.asStateFlow()

    private val _lastReceivedFiles  = MutableStateFlow<List<File>>(emptyList())
    val lastReceivedFiles: StateFlow<List<File>> = _lastReceivedFiles.asStateFlow()

    private val _transferTotalFiles = MutableStateFlow<Int?>(null)
    val transferTotalFiles: StateFlow<Int?> = _transferTotalFiles.asStateFlow()

    val isDiscovering    get() = nsdHandler.isDiscovering
    val discoveredDevices  get() = nsdHandler.discoveredDevices
    val isSendingActive  get() = fileSender.isSendingActive
    val transferProgress   get() = fileSender.transferProgress
    val transferFileStatus get() = fileSender.transferFileStatus

    private var pendingDecision: CompletableDeferred<Boolean>? = null
    private val fileProgressMap = ConcurrentHashMap<String, Long>()
    private var totalBytesToReceive = 0L

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        NotificationHelper.createNotificationChannel(this)

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

                updateForeground()
                deferred.await()
            }
            srv.onTransferProgress = { _, fileId, bytesReceived ->
                fileProgressMap[fileId] = bytesReceived
                val received = fileProgressMap.values.sum()
                if (totalBytesToReceive > 0) {
                    val pct = ((received * 100) / totalBytesToReceive).toInt().coerceIn(0, 100)
                    _statusText.value = "Receiving... $pct%"
                    updateForeground()
                }
            }
            srv.onTransferComplete = { _, _, file ->
                fileProgressMap.remove(file.name)
                _statusText.value = "RECEIVED"
                totalBytesToReceive = 0
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
            ACTION_LS_STOP_DISCOVERY  -> stopDiscovery()
            ACTION_LS_CANCEL -> {
                fileSender.cancelTransfer()
                pendingDecision?.complete(false)
                pendingDecision = null
                _incomingRequest.value = null
            }
            ACTION_LS_ACCEPT -> {
                pendingDecision?.complete(true)
                pendingDecision = null
                _incomingRequest.value = null
                _statusText.value = "Receiving..."
            }
            ACTION_LS_REJECT -> {
                pendingDecision?.complete(false)
                pendingDecision = null
                _incomingRequest.value = null
                _statusText.value = "Transfer rejected"
            }
            ACTION_LS_SEND_FILES -> {
                val uris: ArrayList<Uri>? = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        intent.getParcelableArrayListExtra(EXTRA_FILE_URIS, Uri::class.java)
                    else
                        @Suppress("DEPRECATION") intent.getParcelableArrayListExtra(EXTRA_FILE_URIS)
                } catch (_: Exception) { null }

                val device: DiscoveredDevice? = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        intent.getParcelableExtra(EXTRA_TARGET_DEVICE, DiscoveredDevice::class.java)
                    else
                        @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_TARGET_DEVICE)
                } catch (_: Exception) { null }

                if (uris != null && device != null) {
                    stopDiscovery()
                    _transferTotalFiles.value = uris.size
                    fileSender.sendFiles(device, uris)
                }
            }
        }
        return START_NOT_STICKY
    }

    private var connectivityManager: ConnectivityManager? = null
    private var wifiNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private fun acquireWifiOnWear() {
        try {
            if (wifiLock == null) {
                val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                wifiLock = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WearLocalSendWifiLock")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
            if (wifiNetworkCallback == null) {
                connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d(TAG, "Wear OS Wi-Fi network available: $network")
                        val (_, fp) = LocalSendCertificateHelper.getOrGenerateKeyStore()
                        val devName = Build.MODEL.replace(Regex("[^a-zA-Z0-9 ]"), "").trim().ifBlank { "Wear OS" }
                        startSubnetScan(devName, fp, server.actualPort)
                        startUdpAnnounce(devName, fp, announce = true, port = server.actualPort)
                    }
                    override fun onLost(network: Network) {
                        Log.d(TAG, "Wear OS Wi-Fi network lost: $network")
                    }
                }
                wifiNetworkCallback = callback
                connectivityManager?.requestNetwork(request, callback)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire Wi-Fi on Wear OS: ${e.message}")
        }
    }

    private fun releaseWifiOnWear() {
        try {
            wifiNetworkCallback?.let {
                connectivityManager?.unregisterNetworkCallback(it)
            }
            wifiNetworkCallback = null
            wifiLock?.let { if (it.isHeld) it.release() }
            wifiLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing Wi-Fi on Wear OS: ${e.message}")
        }
    }

    private fun startReceive() {
        if (_isReceiveActive.value) return
        try {
            acquireWifiOnWear()
            val (port, fingerprint) = server.start(LS_PORT)
            _isReceiveActive.value = true

            val deviceName = Build.MODEL.replace(Regex("[^a-zA-Z0-9 ]"), "").trim().ifBlank { "Wear OS" }
            val safeAlias  = deviceName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(60)

            nsdHandler.startServiceRegistration(
                port = port,
                customServiceName = safeAlias,
                extraAttributes = mapOf(
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
            ) { status ->
                _statusText.value = status
                updateForeground()
            }

            startUdpAnnounce(deviceName, fingerprint, announce = true, port = port)
            startUdpListen(deviceName, fingerprint)
            startSubnetScan(deviceName, fingerprint, port)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start receive: ${e.message}", e)
            _statusText.value = "ERROR: ${e.message}"
        }
        updateForeground()
    }

    private fun stopReceive() {
        server.stop()
        nsdHandler.stopServiceRegistration()
        stopUdpMulticast()
        _isReceiveActive.value = false
        _statusText.value = "Ready"
        if (!isDiscovering.value) {
            subnetScanJob?.cancel()
            subnetScanJob = null
            releaseWifiOnWear()
        }
        updateForeground()
    }

    private var subnetScanJob: Job? = null

    private fun startDiscovery() {
        acquireWifiOnWear()
        val (_, fingerprint) = LocalSendCertificateHelper.getOrGenerateKeyStore()
        val deviceName = Build.MODEL.replace(Regex("[^a-zA-Z0-9 ]"), "").trim().ifBlank { "Wear OS" }

        nsdHandler.startDiscovery { status ->
            _statusText.value = status
            updateForeground()
        }

        startUdpAnnounce(deviceName, fingerprint, announce = true, port = server.actualPort)
        startUdpListen(deviceName, fingerprint)
        startSubnetScan(deviceName, fingerprint, server.actualPort)
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

    private fun stopDiscovery() {
        nsdHandler.stopDiscovery()
        subnetScanJob?.cancel()
        subnetScanJob = null
        if (!_isReceiveActive.value) {
            stopUdpMulticast()
            releaseWifiOnWear()
        }
    }

    private fun buildAnnouncementJson(alias: String, fingerprint: String, announce: Boolean, port: Int = LS_PORT): ByteArray {
        return JSONObject().apply {
            put("alias",       alias)
            put("version",     "2.0")
            put("deviceModel", Build.MODEL)
            put("deviceType",  "mobile")
            put("fingerprint", fingerprint)
            put("port",        port)
            put("protocol",    "https")
            put("download",    false)
            put("announce",    announce)
        }.toString().toByteArray(Charsets.UTF_8)
    }

    private fun startUdpAnnounce(deviceName: String, fingerprint: String, announce: Boolean, port: Int = LS_PORT) {
        udpAnnounceJob?.cancel()
        udpAnnounceJob = serviceScope.launch(Dispatchers.IO) {
            try {
                val group = InetAddress.getByName(LS_MULTICAST_ADDR)
                val payload = buildAnnouncementJson(deviceName, fingerprint, announce, port = port)
                DatagramSocket().use { sock ->
                    sock.broadcast = true
                    val packetMulticast = DatagramPacket(payload, payload.size, group, LS_PORT)
                    val packetBroadcast = DatagramPacket(payload, payload.size, InetAddress.getByName("255.255.255.255"), LS_PORT)

                    sock.send(packetMulticast)
                    try { sock.send(packetBroadcast) } catch (_: Exception) {}

                    SubnetScanner.getAllLocalIpv4AndSubnets().forEach { (_, prefix) ->
                        try {
                            val subnetBcast = DatagramPacket(payload, payload.size, InetAddress.getByName("${prefix}255"), LS_PORT)
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

                        Log.d(TAG, "UDP: received from $alias (${packet.address.hostAddress}:$port) announce=$isAnnounce")

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
                            Log.d(TAG, "UDP: replied to $alias at ${packet.address.hostAddress}:$port")
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
        val isSending = fileSender.isSendingActive.value
        val isReceiving = totalBytesToReceive > 0 && fileProgressMap.isNotEmpty()
        val isTransferActive = isSending || isReceiving

        val builder = if (isTransferActive) {
            val progress = if (isSending) {
                fileSender.transferProgress.value
            } else {
                val received = fileProgressMap.values.sum()
                ((received * 100) / totalBytesToReceive).toInt().coerceIn(0, 100)
            }
            val statusText = if (isSending) "Sending..." else "Receiving..."
            NotificationHelper.buildOngoingNotification(
                context = this,
                notificationId = LS_NOTIF_ID,
                targetActivity = LocalSendActivity::class.java,
                title = "LocalSend",
                contentText = _statusText.value,
                statusText = statusText,
                iconRes = R.drawable.ic_notification_24dp,
                progress = progress,
                totalSize = totalBytesToReceive
            )
        } else {
            NotificationHelper.buildForegroundServiceNotification(this, _statusText.value)
        }
        startForeground(LS_NOTIF_ID, builder.build())
    }

    override fun onBind(intent: Intent?): IBinder = ServiceBinder()

    inner class ServiceBinder : Binder() {
        fun getService(): LocalSendService = this@LocalSendService
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        releaseWifiOnWear()
        server.stop()
        fileSender.shutdown()
        nsdHandler.shutdown()
        stopUdpMulticast()
        serviceScope.cancel()
        NotificationHelper.stopOngoingActivity(this, LS_NOTIF_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
