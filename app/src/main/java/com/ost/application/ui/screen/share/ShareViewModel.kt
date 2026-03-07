package com.ost.application.ui.screen.share

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class UiEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : UiEvent()
}

data class IncomingTransferRequest(
    val requestId: String,
    val senderDeviceName: String,
    val fileNames: List<String>,
    val totalSize: Long,
    val deferredConfirmation: CompletableDeferred<Boolean>
)

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    private var shareService: ShareService? = null
    private val _isServiceBound = MutableStateFlow(false)

    val isReceivingActive: StateFlow<Boolean> = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = MutableStateFlow(false)
    val statusText: StateFlow<String> = MutableStateFlow(application.getString(R.string.idle_status))
    val transferProgress: StateFlow<Int?> = MutableStateFlow(null)
    val transferFileStatus: StateFlow<String?> = MutableStateFlow(null)
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = MutableStateFlow(emptyList())
    val lastReceivedFiles: StateFlow<List<File>> = MutableStateFlow(emptyList())
    val incomingTransferRequest: StateFlow<IncomingTransferRequest?> = MutableStateFlow(null)
    val isCleaningUp: StateFlow<Boolean> = MutableStateFlow(false)

    val isTransferActive: StateFlow<Boolean> = transferProgress.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private var selectedDevice: DiscoveredDevice? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ShareService.ServiceBinder
            shareService = binder.getService()
            _isServiceBound.value = true
            observeServiceState()
            viewModelScope.launch {
                handlePermissionsGranted()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shareService = null
            _isServiceBound.value = false
            resetStateToDisconnected()
        }
    }

    init {
        bindToService()
    }

    private fun bindToService() {
        Intent(getApplication(), ShareService::class.java).also { intent ->
            getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun observeServiceState() {
        shareService?.let { service ->
            viewModelScope.launch {
                service.isReceivingActive.collect { (isReceivingActive as MutableStateFlow).value = it }
            }
            viewModelScope.launch {
                service.isDiscovering.collect { (isDiscovering as MutableStateFlow).value = it }
            }
            viewModelScope.launch {
                service.serviceStatus.collect { status ->
                    (statusText as MutableStateFlow).value = status.message
                    val app = getApplication<Application>()
                    when (status) {
                        is ServiceOverallStatus.Idle -> {}
                        is ServiceOverallStatus.Receiving -> {}
                        is ServiceOverallStatus.Sending -> {}
                        is ServiceOverallStatus.Discovering -> {}
                        is ServiceOverallStatus.ReceivingRequest -> {}
                        is ServiceOverallStatus.CleaningUp -> {}
                    }
                    if (status.message.startsWith(app.getString(R.string.sent_multi_success)) || status.message.startsWith(app.getString(R.string.received_multi_success))) {
                        _uiEvent.emit(UiEvent.ShowSnackbar(app.getString(R.string.transfer_complete)))
                    } else if (status.message.startsWith(app.getString(R.string.error_prefix))) {
                        _uiEvent.emit(UiEvent.ShowSnackbar(status.message, isError = true))
                    }
                }
            }
            viewModelScope.launch {
                service.transferProgress.collect { (transferProgress as MutableStateFlow).value = it }
            }
            viewModelScope.launch {
                service.transferFileStatus.collect { (transferFileStatus as MutableStateFlow).value = it }
            }
            viewModelScope.launch {
                service.discoveredDevices.collect { (discoveredDevices as MutableStateFlow).value = it }
            }
            viewModelScope.launch {
                service.lastReceivedFiles.collect { (lastReceivedFiles as MutableStateFlow).value = it }
            }
            viewModelScope.launch {
                service.incomingTransferRequestService.collect { request ->
                    (incomingTransferRequest as MutableStateFlow).value = request
                }
            }
            viewModelScope.launch {
                service.isCleaningUp.collect { (isCleaningUp as MutableStateFlow).value = it }
            }
        }
    }

    private fun resetStateToDisconnected() {
        (isReceivingActive as MutableStateFlow).value = false
        (isDiscovering as MutableStateFlow).value = false
        (statusText as MutableStateFlow).value = getApplication<Application>().getString(R.string.idle_status)
        (transferProgress as MutableStateFlow).value = null
        (transferFileStatus as MutableStateFlow).value = null
        (discoveredDevices as MutableStateFlow).value = emptyList()
        (lastReceivedFiles as MutableStateFlow).value = emptyList()
        (incomingTransferRequest as MutableStateFlow).value = null
        (isCleaningUp as MutableStateFlow).value = false
    }

    fun setSelectedDevice(device: DiscoveredDevice) {
        selectedDevice = device
    }

    fun clearSelectedDevice() {
        selectedDevice = null
    }

    fun sendFilesToSelectedDevice(fileUris: List<Uri>) {
        val deviceToSend = selectedDevice
        if (deviceToSend == null) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.error_no_device_selected), isError = true))
            }
            return
        }
        if (fileUris.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.no_files_selected), isError = true))
            }
            return
        }

        val intent = Intent(getApplication(), ShareService::class.java).apply {
            action = Constants.ACTION_SEND_FILES
            putParcelableArrayListExtra(Constants.EXTRA_FILE_URIS, ArrayList(fileUris))
            putExtra(Constants.EXTRA_TARGET_DEVICE, deviceToSend)
        }
        getApplication<Application>().startService(intent)
    }

    fun handlePermissionsGranted() {
    }

    fun setReceivingActive(isActive: Boolean) {
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            action = if (isActive) Constants.ACTION_START_SERVICE else Constants.ACTION_STOP_SERVICE
        }
        getApplication<Application>().startService(intent)
    }

    fun startDiscovery() {
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            action = Constants.ACTION_START_DISCOVERY
        }
        getApplication<Application>().startService(intent)
    }

    fun stopDiscovery() {
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            action = Constants.ACTION_STOP_DISCOVERY
        }
        getApplication<Application>().startService(intent)
    }

    fun cancelTransfer() {
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            action = Constants.ACTION_CANCEL_TRANSFER
        }
        getApplication<Application>().startService(intent)
    }

    fun acceptIncomingTransfer(requestId: String) {
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            action = Constants.ACTION_ACCEPT_RECEIVE
            putExtra(Constants.EXTRA_REQUEST_ID, requestId)
        }
        getApplication<Application>().startService(intent)
    }

    fun rejectIncomingTransfer(requestId: String) {
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            action = Constants.ACTION_REJECT_RECEIVE
            putExtra(Constants.EXTRA_REQUEST_ID, requestId)
        }
        getApplication<Application>().startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        if (_isServiceBound.value) {
            getApplication<Application>().unbindService(serviceConnection)
        }
    }
}