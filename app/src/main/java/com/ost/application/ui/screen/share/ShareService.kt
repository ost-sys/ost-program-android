package com.ost.application.ui.screen.share
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.ost.application.R
import com.ost.application.core.share.Constants
import com.ost.application.core.share.DiscoveredDevice
import com.ost.application.core.share.FileReceiver
import com.ost.application.core.share.FileSender
import com.ost.application.core.share.IncomingTransferRequest
import com.ost.application.core.share.NsdHandler
import com.ost.application.core.share.ServiceOverallStatus
import com.ost.application.core.share.TransferNotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
class ShareService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var nsdHandler: NsdHandler
    private lateinit var fileReceiver: FileReceiver
    private lateinit var fileSender: FileSender
    private val _serviceStatus: MutableStateFlow<ServiceOverallStatus> = MutableStateFlow(ServiceOverallStatus.Idle(""))
    val serviceStatus: StateFlow<ServiceOverallStatus> = _serviceStatus.asStateFlow()
    private val _isReceivingActive = MutableStateFlow(false)
    val isReceivingActive: StateFlow<Boolean> = _isReceivingActive.asStateFlow()
    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()
    lateinit var isDiscovering: StateFlow<Boolean>
    lateinit var transferProgress: StateFlow<Int?>
    lateinit var transferFileStatus: StateFlow<String?>
    lateinit var discoveredDevices: StateFlow<List<DiscoveredDevice>>
    lateinit var lastReceivedFiles: StateFlow<List<File>>
    lateinit var incomingTransferRequestService: StateFlow<IncomingTransferRequest?>
    private val _isCleaningUp = MutableStateFlow(false)
    val isCleaningUp: StateFlow<Boolean> = _isCleaningUp.asStateFlow()
    private val notificationListener = object : TransferNotificationListener {
        override fun onPreparing() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                LiveUpdateNotificationManager.showPreparing()
            }
        }
        override fun onConnecting() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                LiveUpdateNotificationManager.showConnecting()
            }
        }
        override fun onWaitingForAcceptance() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                LiveUpdateNotificationManager.showWaitingForAcceptance()
            }
        }
        override fun onTransferring(progress: Int, fileUris: List<Uri>, totalSize: Long, isSending: Boolean) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                LiveUpdateNotificationManager.showTransferring(progress, fileUris, totalSize, isSending)
            }
        }
        override fun onCompleted(fileUris: List<Uri>, isSending: Boolean) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                LiveUpdateNotificationManager.showCompleted(fileUris, isSending)
            }
        }
        override fun onCancelled(reason: String?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                LiveUpdateNotificationManager.showCancelled(reason ?: "Cancelled")
            }
        }
        override fun onFailed(reason: String?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                LiveUpdateNotificationManager.showFailed(reason ?: "Unknown error")
            }
        }
        override fun onShowIncomingRequest(requestId: String, senderName: String, fileNames: List<String>, totalSize: Long) {
            NotificationHelper.showIncomingFileConfirmationNotification(
                this@ShareService,
                requestId,
                fileNames,
                totalSize,
                senderName
            )
        }
        override fun onCancelNotification(notificationId: Int) {
            NotificationHelper.cancelNotification(this@ShareService, notificationId)
        }
    }
    override fun onCreate() {
        super.onCreate()
        Log.d(Constants.TAG, "ShareService: onCreate")
        NotificationHelper.createAppNotificationChannels(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            LiveUpdateNotificationManager.initialize(this)
        }
        _serviceStatus.value = ServiceOverallStatus.Idle(getString(R.string.idle_status))
        nsdHandler = NsdHandler(applicationContext, serviceScope, Constants.VALUE_DEVICE_PHONE)
        fileReceiver = FileReceiver(applicationContext, serviceScope, notificationListener) { status ->
            updateOverallStatusFromComponent(status, isReceiving = true)
        }
        fileSender = FileSender(applicationContext, serviceScope, notificationListener) { status ->
            updateOverallStatusFromComponent(status, isSending = true)
        }
        isDiscovering = nsdHandler.isDiscovering
        discoveredDevices = nsdHandler.discoveredDevices
        lastReceivedFiles = fileReceiver.lastReceivedFiles
        incomingTransferRequestService = fileReceiver.incomingTransferRequest
        transferProgress = combine(
            fileReceiver.transferProgress,
            fileSender.transferProgress
        ) { receiverProgress, senderProgress ->
            receiverProgress ?: senderProgress
        }.distinctUntilChanged().stateIn(
            scope = serviceScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
        transferFileStatus = combine(
            fileReceiver.transferFileStatus,
            fileSender.transferFileStatus
        ) { receiverFileStatus, senderFileStatus ->
            receiverFileStatus ?: senderFileStatus
        }.distinctUntilChanged().stateIn(
            scope = serviceScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
        serviceScope.launch {
            combine(
                fileReceiver.isReceivingActive,
                fileSender.isSendingActive,
                nsdHandler.isDiscovering,
                incomingTransferRequestService,
                isCleaningUp
            ) { isReceiving, isSending, isDiscovering, incomingRequest, isCleaning ->
                val newStatus = when {
                    isCleaning -> ServiceOverallStatus.CleaningUp(getString(R.string.stopping_receiver))
                    isSending -> serviceStatus.value.takeIf { it is ServiceOverallStatus.Sending } ?: ServiceOverallStatus.Sending(getString(R.string.transfer_active))
                    isReceiving -> serviceStatus.value.takeIf { it is ServiceOverallStatus.Receiving } ?: ServiceOverallStatus.Receiving(getString(R.string.receiver_active_waiting))
                    isDiscovering -> serviceStatus.value.takeIf { it is ServiceOverallStatus.Discovering } ?: ServiceOverallStatus.Discovering(getString(R.string.searching))
                    incomingRequest != null -> ServiceOverallStatus.ReceivingRequest(getString(R.string.incoming_request, incomingRequest.senderDeviceName))
                    else -> ServiceOverallStatus.Idle(getString(R.string.idle_status))
                }
                _serviceStatus.value = newStatus
                _isReceivingActive.value = isReceiving
            }.collect {}
        }
        serviceScope.launch {
            combine(_serviceStatus, _isServiceActive) { status, active ->
                status to active
            }.collect { (status, active) ->
                if (active) {
                    val contentText = when (status) {
                        is ServiceOverallStatus.Idle -> getString(R.string.notification_service_idle)
                        is ServiceOverallStatus.Receiving -> getString(R.string.notification_service_receiving)
                        is ServiceOverallStatus.Sending -> getString(R.string.notification_service_sending)
                        is ServiceOverallStatus.Discovering -> getString(R.string.notification_service_discovering)
                        is ServiceOverallStatus.CleaningUp -> getString(R.string.notification_service_cleaning_up)
                        is ServiceOverallStatus.ReceivingRequest -> getString(R.string.notification_service_incoming_request)
                    }
                    startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE,
                        NotificationHelper.buildForegroundServiceNotification(this@ShareService, contentText).build()
                    )
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        }
    }
    private fun updateOverallStatusFromComponent(statusText: String, isSending: Boolean = false, isReceiving: Boolean = false, isDiscovering: Boolean = false) {
        if (_isCleaningUp.value) return
        _serviceStatus.value = when {
            isSending -> ServiceOverallStatus.Sending(statusText)
            isReceiving -> ServiceOverallStatus.Receiving(statusText)
            isDiscovering -> ServiceOverallStatus.Discovering(statusText)
            else -> ServiceOverallStatus.Idle(statusText)
        }
    }
    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            Log.d(Constants.TAG, "ShareService: onStartCommand with action: $action")
            when (action) {
                Constants.ACTION_LAUNCH_SERVICE -> {
                    _isServiceActive.value = true
                }
                Constants.ACTION_START_RECEIVING -> {
                    _isServiceActive.value = true
                    if (isCleaningUp.value) {
                        Log.d(Constants.TAG, "ShareService: Cannot start receiving, currently cleaning up.")
                        return START_NOT_STICKY
                    }
                    if (!fileReceiver.isReceivingActive.value) {
                        fileReceiver.startReceiving()
                        serviceScope.launch {
                            val port = Constants.TRANSFER_PORT
                            nsdHandler.startServiceRegistration(port) { statusText -> updateOverallStatusFromComponent(statusText) }
                        }
                    } else {
                        Log.d(Constants.TAG, "ShareService: Receiver already active, not restarting.")
                    }
                }
                Constants.ACTION_STOP_RECEIVING -> {
                    if (isCleaningUp.value) {
                        Log.d(Constants.TAG, "ShareService: Cannot stop receiving, currently cleaning up.")
                        return START_NOT_STICKY
                    }
                    serviceScope.launch {
                        runCatching { fileReceiver.stopReceiving() }
                        runCatching { nsdHandler.stopServiceRegistration() }
                        _serviceStatus.value = ServiceOverallStatus.Idle(getString(R.string.idle_status))
                    }
                }
                Constants.ACTION_STOP_SERVICE, Constants.ACTION_SHUTDOWN_SERVICE -> {
                    _isServiceActive.value = false
                    if (isCleaningUp.value) {
                        Log.d(Constants.TAG, "ShareService: Already cleaning up, not restarting cleanup.")
                        return START_NOT_STICKY
                    }
                    _isCleaningUp.value = true
                    serviceScope.launch {
                        try {
                            runCatching { fileReceiver.stopReceiving() }
                                .onFailure { Log.e(Constants.TAG, "StopReceiving failed: ${it.message}", it) }
                            runCatching { nsdHandler.stopServiceRegistration() }
                                .onFailure { Log.e(Constants.TAG, "StopServiceRegistration failed: ${it.message}", it) }
                            runCatching { nsdHandler.stopDiscovery() }
                        } finally {
                            _isCleaningUp.value = false
                            Log.d(Constants.TAG, "ShareService: stopService complete. Stopping foreground and calling stopSelf.")
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                    }
                }
                Constants.ACTION_START_DISCOVERY -> {
                    _isServiceActive.value = true
                    if (isCleaningUp.value) {
                        Log.d(Constants.TAG, "ShareService: Cannot start discovery, currently cleaning up.")
                        return START_NOT_STICKY
                    }
                    if (fileReceiver.isReceivingActive.value || fileSender.isSendingActive.value) {
                        _serviceStatus.value = ServiceOverallStatus.Idle(getString(R.string.stop_receiving_sending_first))
                        return START_NOT_STICKY
                    }
                    nsdHandler.startDiscovery { statusText -> updateOverallStatusFromComponent(statusText, isDiscovering = true) }
                }
                Constants.ACTION_STOP_DISCOVERY -> {
                    nsdHandler.stopDiscovery()
                }
                Constants.ACTION_SEND_FILES -> {
                    _isServiceActive.value = true
                    if (isCleaningUp.value || fileReceiver.isReceivingActive.value) {
                        _serviceStatus.value = ServiceOverallStatus.Idle(getString(R.string.transfer_unavailable))
                        return START_NOT_STICKY
                    }
                    val urisToShare = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableArrayListExtra(Constants.EXTRA_FILE_URIS, Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableArrayListExtra(Constants.EXTRA_FILE_URIS)
                        }
                    } catch (e: Exception) {
                        Log.e(Constants.TAG, "Error parsing file URIs extra", e)
                        null
                    }
                    val targetDevice = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(Constants.EXTRA_TARGET_DEVICE, DiscoveredDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(Constants.EXTRA_TARGET_DEVICE)
                        }
                    } catch (e: Exception) {
                        Log.e(Constants.TAG, "Error parsing target device extra", e)
                        null
                    }
                    if (urisToShare != null && targetDevice != null) {
                        nsdHandler.stopDiscovery()
                        fileSender.sendFiles(targetDevice, urisToShare)
                    } else {
                        _serviceStatus.value = ServiceOverallStatus.Idle(getString(R.string.error_send_intent_missing_data))
                    }
                }
                Constants.ACTION_CANCEL_TRANSFER -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                        LiveUpdateNotificationManager.showCancelled(getString(R.string.cancelled_by_user))
                    }
                    if (fileSender.isSendingActive.value) {
                        fileSender.cancelTransfer()
                    }
                    if (fileReceiver.isReceivingActive.value) {
                        fileReceiver.cancelIncomingTransfer()
                    }
                }
                Constants.ACTION_ACCEPT_RECEIVE -> {
                    val requestId = intent.getStringExtra(Constants.EXTRA_REQUEST_ID)
                    requestId?.let { fileReceiver.respondToIncomingTransfer(it, true) }
                }
                Constants.ACTION_REJECT_RECEIVE -> {
                    val requestId = intent.getStringExtra(Constants.EXTRA_REQUEST_ID)
                    requestId?.let { fileReceiver.respondToIncomingTransfer(it, false) }
                }
            }
        }
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder {
        return ServiceBinder()
    }
    inner class ServiceBinder : Binder() {
        fun getService(): ShareService = this@ShareService
    }
    override fun onDestroy() {
        Log.d(Constants.TAG, "ShareService: onDestroy")
        _isCleaningUp.value = true
        try {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                fileReceiver.shutdown()
                fileSender.shutdown()
                nsdHandler.shutdown()
            }
        } catch (e: Exception) {
            Log.e(Constants.TAG, "ShareService: Error during shutdown: ${e.message}", e)
        } finally {
            serviceScope.cancel()
            Log.d(Constants.TAG, "ShareService: serviceScope cancelled.")
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationHelper.cancelNotification(this, Constants.NOTIFICATION_ID_TRANSFER)
        NotificationHelper.cancelNotification(this, Constants.NOTIFICATION_ID_INCOMING_FILE)
        _isCleaningUp.value = false
        _serviceStatus.value = ServiceOverallStatus.Idle(getString(R.string.receiver_stopped))
        super.onDestroy()
    }
}
