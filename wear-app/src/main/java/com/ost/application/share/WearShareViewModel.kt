package com.ost.application.share

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class IncomingTransferRequest(
    val requestId: String,
    val senderDeviceName: String,
    val fileNames: List<String>,
    val totalSize: Long,
    val deferredConfirmation: CompletableDeferred<Boolean>
)

data class UiConfirmationState(
    val isSuccess: Boolean,
    val message: String,
    val iconRes: Int
)

class WearShareViewModel(application: Application) : AndroidViewModel(application) {

    private var wearShareService: WearShareService? = null
    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _statusText = MutableStateFlow(application.getString(R.string.receiver_stopped))
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _transferProgress = MutableStateFlow<Int?>(null)
    val transferProgress: StateFlow<Int?> = _transferProgress.asStateFlow()

    private val _transferFileStatus = MutableStateFlow<String?>(null)
    val transferFileStatus: StateFlow<String?> = _transferFileStatus.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _lastReceivedFiles = MutableStateFlow<List<File>>(emptyList())
    val lastReceivedFiles: StateFlow<List<File>> = _lastReceivedFiles.asStateFlow()

    private val _incomingTransferRequest = MutableStateFlow<IncomingTransferRequest?>(null)
    val incomingTransferRequest: StateFlow<IncomingTransferRequest?> = _incomingTransferRequest.asStateFlow()

    private val _uiConfirmationState = MutableStateFlow<UiConfirmationState?>(null)
    val uiConfirmationState: StateFlow<UiConfirmationState?> = _uiConfirmationState.asStateFlow()

    private val _transferTotalFiles = MutableStateFlow<Int?>(null)
    val transferTotalFiles: StateFlow<Int?> = _transferTotalFiles.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WearShareService.ServiceBinder
            wearShareService = binder.getService()
            _isServiceBound.value = true
            observeServiceState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            wearShareService = null
            _isServiceBound.value = false
            resetStateToDisconnected()
        }
    }

    init {
        bindToService()
    }

    private fun bindToService() {
        Intent(getApplication(), WearShareService::class.java).also { intent ->
            getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun observeServiceState() {
        wearShareService?.let { service ->
            viewModelScope.launch {
                service.isServiceActive.collect { _isServiceActive.value = it }
            }
            viewModelScope.launch {
                service.transferTotalFiles.collect { _transferTotalFiles.value = it }
            }
            viewModelScope.launch {
                service.isDiscovering.collect { _isDiscovering.value = it }
            }
            viewModelScope.launch {
                service.statusText.collect { status ->
                    _statusText.value = status
                    val app = getApplication<Application>()
                    // Определяем, нужно ли показать Confirmation экран
                    val isSuccessStatus = status.startsWith(app.getString(R.string.sent_prefix), ignoreCase = true) || status.startsWith(app.getString(R.string.received_prefix), ignoreCase = true)
                    val isErrorStatus = status.startsWith(Constants.ERROR_PREFIX, ignoreCase = true) || status.contains(Constants.CANCELLED_KEYWORD, ignoreCase = true)

                    if (isSuccessStatus) {
                        _uiConfirmationState.value = UiConfirmationState(
                            isSuccess = true,
                            message = status,
                            iconRes = R.drawable.ic_check_circle_24dp
                        )
                    } else if (isErrorStatus) {
                        _uiConfirmationState.value = UiConfirmationState(
                            isSuccess = false,
                            message = status,
                            iconRes = R.drawable.ic_error_24dp
                        )
                    }
                }
            }
            viewModelScope.launch {
                service.transferProgress.collect { _transferProgress.value = it }
            }
            viewModelScope.launch {
                service.transferFileStatus.collect { _transferFileStatus.value = it }
            }
            viewModelScope.launch {
                service.discoveredDevices.collect { _discoveredDevices.value = it }
            }
            viewModelScope.launch {
                service.lastReceivedFiles.collect { _lastReceivedFiles.value = it }
            }
            viewModelScope.launch {
                service.incomingTransferRequest.collect { _incomingTransferRequest.value = it }
            }
        }
    }

    private fun resetStateToDisconnected() {
        _isServiceActive.value = false
        _isDiscovering.value = false
        _statusText.value = getApplication<Application>().getString(R.string.receiver_stopped)
        _transferProgress.value = null
        _transferFileStatus.value = null
        _discoveredDevices.value = emptyList()
        _lastReceivedFiles.value = emptyList()
        _incomingTransferRequest.value = null
        _uiConfirmationState.value = null
        _transferTotalFiles.value = null
    }

    fun setServiceActive(isActive: Boolean) {
        val intent = Intent(getApplication(), WearShareService::class.java).apply {
            action = if (isActive) Constants.ACTION_START_SERVICE else Constants.ACTION_STOP_SERVICE
        }
        getApplication<Application>().startService(intent)
    }

    fun startDiscovery() {
        val intent = Intent(getApplication(), WearShareService::class.java).apply {
            action = Constants.ACTION_START_DISCOVERY
        }
        getApplication<Application>().startService(intent)
    }

    fun stopDiscovery() {
        val intent = Intent(getApplication(), WearShareService::class.java).apply {
            action = Constants.ACTION_STOP_DISCOVERY
        }
        getApplication<Application>().startService(intent)
    }

    fun resolveDevice(serviceInfo: NsdServiceInfo) {
        wearShareService?.resolveDevice(serviceInfo)
    }

    fun sendFiles(targetDevice: DiscoveredDevice, fileUris: ArrayList<Uri>) {
        val intent = Intent(getApplication(), WearShareService::class.java).apply {
            action = Constants.ACTION_SEND_FILES
            putParcelableArrayListExtra(Constants.EXTRA_FILE_URIS, fileUris)
            putExtra(Constants.EXTRA_TARGET_DEVICE, targetDevice)
        }
        getApplication<Application>().startService(intent)
    }

    fun cancelTransfer() {
        val intent = Intent(getApplication(), WearShareService::class.java).apply {
            action = Constants.ACTION_CANCEL_TRANSFER
        }
        getApplication<Application>().startService(intent)
    }

    fun acceptIncomingTransfer(requestId: String) {
        val intent = Intent(getApplication(), WearShareService::class.java).apply {
            action = Constants.ACTION_ACCEPT_RECEIVE
            putExtra(Constants.EXTRA_REQUEST_ID, requestId)
        }
        getApplication<Application>().startService(intent)
    }

    fun rejectIncomingTransfer(requestId: String) {
        val intent = Intent(getApplication(), WearShareService::class.java).apply {
            action = Constants.ACTION_REJECT_RECEIVE
            putExtra(Constants.EXTRA_REQUEST_ID, requestId)
        }
        getApplication<Application>().startService(intent)
    }

    fun clearUiConfirmationState() {
        _uiConfirmationState.value = null
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }
}