package com.ost.application.share
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
import com.ost.application.core.R as CoreR
class WearShareService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var nsdHandler: NsdHandler
    private lateinit var fileReceiver: FileReceiver
    private lateinit var fileSender: FileSender
    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()
    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()
    private val _transferTotalFiles = MutableStateFlow<Int?>(null)
    val transferTotalFiles: StateFlow<Int?> = _transferTotalFiles.asStateFlow()
    lateinit var isDiscovering: StateFlow<Boolean>
    lateinit var transferProgress: StateFlow<Int?>
    lateinit var transferFileStatus: StateFlow<String?>
    lateinit var discoveredDevices: StateFlow<List<DiscoveredDevice>>
    lateinit var lastReceivedFiles: StateFlow<List<File>>
    lateinit var incomingTransferRequest: StateFlow<IncomingTransferRequest?>
    private val notificationListener = object : TransferNotificationListener {
        override fun onShowIncomingRequest(requestId: String, senderName: String, fileNames: List<String>, totalSize: Long) {
            NotificationHelper.showIncomingFileConfirmationNotification(
                this@WearShareService,
                requestId,
                fileNames,
                totalSize,
                senderName
            )
        }
        override fun onTransferring(progress: Int, fileUris: List<Uri>, totalSize: Long, isSending: Boolean) {
            val title = if (isSending) {
                getString(R.string.notif_sending_multi_title, fileUris.size)
            } else {
                getString(R.string.notif_receiving_multi_title, fileUris.size)
            }
            NotificationHelper.showTransferNotification(
                this@WearShareService,
                title,
                progress,
                totalSize,
                isSending
            )
            updateOngoingActivity()
        }
        override fun onCompleted(fileUris: List<Uri>, isSending: Boolean) {
            val title = if (isSending) {
                getString(R.string.notif_sent_multi_title_success, fileUris.size)
            } else {
                getString(R.string.notif_received_multi_title_success, fileUris.size)
            }
            NotificationHelper.showCompletionNotification(
                this@WearShareService,
                title,
                true
            )
            updateOngoingActivity()
        }
        override fun onFailed(reason: String?) {
            NotificationHelper.showCompletionNotification(
                this@WearShareService,
                getString(R.string.transfer_failed),
                false,
                reason
            )
            updateOngoingActivity()
        }
        override fun onCancelled(reason: String?) {
            NotificationHelper.showCompletionNotification(
                this@WearShareService,
                getString(CoreR.string.cancelling_transfer),
                false,
                reason
            )
            updateOngoingActivity()
        }
        override fun onCancelNotification(notificationId: Int) {
            NotificationHelper.cancelNotification(this@WearShareService, notificationId)
        }
    }
    override fun onCreate() {
        super.onCreate()
        Log.d(Constants.TAG, "WearShareService: onCreate")
        NotificationHelper.createNotificationChannel(this)
        _statusText.value = getString(CoreR.string.receiver_stopped)
        nsdHandler = NsdHandler(applicationContext, serviceScope, Constants.VALUE_DEVICE_WATCH)
        fileReceiver = FileReceiver(applicationContext, serviceScope, notificationListener) { status ->
            _statusText.value = status
            updateOngoingActivity()
        }
        fileSender = FileSender(applicationContext, serviceScope, notificationListener) { status ->
            _statusText.value = status
            updateOngoingActivity()
        }
        isDiscovering = nsdHandler.isDiscovering
        discoveredDevices = nsdHandler.discoveredDevices
        lastReceivedFiles = fileReceiver.lastReceivedFiles
        incomingTransferRequest = fileReceiver.incomingTransferRequest
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
            fileReceiver.isReceivingActive.collect { active ->
                _isServiceActive.value = active
                updateOngoingActivity()
            }
        }
        serviceScope.launch {
            transferProgress.collect {
                updateOngoingActivity()
            }
        }
        updateOngoingActivity()
    }
    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            Log.d(Constants.TAG, "WearShareService: onStartCommand with action: $action")
            when (action) {
                Constants.ACTION_START_SERVICE -> startServiceInternal()
                Constants.ACTION_STOP_SERVICE -> stopServiceInternal()
                Constants.ACTION_START_DISCOVERY -> startDiscoveryInternal()
                Constants.ACTION_STOP_DISCOVERY -> stopDiscoveryInternal()
                Constants.ACTION_SEND_FILES -> {
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
                        stopDiscoveryInternal()
                        _transferTotalFiles.value = urisToShare.size
                        fileSender.sendFiles(targetDevice, urisToShare)
                    }
                }
                Constants.ACTION_CANCEL_TRANSFER -> cancelTransfer()
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
    private fun startServiceInternal() {
        if (!fileReceiver.isReceivingActive.value) {
            fileReceiver.startReceiving()
            nsdHandler.startServiceRegistration(Constants.TRANSFER_PORT) { status ->
                _statusText.value = status
                updateOngoingActivity()
            }
        }
    }
    private fun stopServiceInternal() {
        fileReceiver.stopReceiving()
        nsdHandler.stopServiceRegistration()
        _statusText.value = getString(CoreR.string.receiver_stopped)
        updateOngoingActivity()
    }
    private fun startDiscoveryInternal() {
        nsdHandler.startDiscovery { status ->
            _statusText.value = status
            updateOngoingActivity()
        }
    }
    private fun stopDiscoveryInternal() {
        nsdHandler.stopDiscovery()
        updateOngoingActivity()
    }
    fun resolveDevice(serviceInfo: android.net.nsd.NsdServiceInfo) {
        nsdHandler.resolveDevice(serviceInfo)
    }
    fun cancelTransfer() {
        if (fileSender.isSendingActive.value) {
            fileSender.cancelTransfer()
        }
        if (fileReceiver.isReceivingActive.value) {
            fileReceiver.cancelIncomingTransfer()
        }
    }
    private fun updateOngoingActivity() {
        val isSending = fileSender.isSendingActive.value
        val isReceiving = fileReceiver.transferProgress.value != null
        val isTransferActive = isSending || isReceiving

        val notifBuilder = if (isTransferActive) {
            val progress = if (isSending) {
                fileSender.transferProgress.value
            } else {
                fileReceiver.transferProgress.value
            }
            val statusText = getString(CoreR.string.transfer_active)
            NotificationHelper.buildOngoingNotification(
                context = this,
                notificationId = Constants.NOTIFICATION_ID_SERVICE_FOREGROUND,
                targetActivity = ShareActivity::class.java,
                title = getString(R.string.app_name),
                contentText = _statusText.value,
                statusText = statusText,
                iconRes = R.drawable.ic_notification_24dp,
                progress = progress
            )
        } else {
            NotificationHelper.buildForegroundServiceNotification(this, _statusText.value)
        }
        startForeground(Constants.NOTIFICATION_ID_SERVICE_FOREGROUND, notifBuilder.build())
    }
    override fun onBind(intent: Intent?): IBinder {
        return ServiceBinder()
    }
    inner class ServiceBinder : Binder() {
        fun getService(): WearShareService = this@WearShareService
    }
    override fun onDestroy() {
        Log.d(Constants.TAG, "WearShareService: onDestroy")
        fileReceiver.shutdown()
        fileSender.shutdown()
        nsdHandler.shutdown()
        serviceScope.cancel()
        NotificationHelper.stopOngoingActivity(this, Constants.NOTIFICATION_ID_SERVICE_FOREGROUND)
        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationHelper.cancelNotification(this, Constants.NOTIFICATION_ID_TRANSFER)
        super.onDestroy()
    }
}
