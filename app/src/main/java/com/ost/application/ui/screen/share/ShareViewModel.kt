package com.ost.application.ui.screen.share

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import com.ost.application.core.share.Constants
import com.ost.application.core.share.DiscoveredDevice
import com.ost.application.core.share.IncomingTransferRequest
import com.ost.application.core.share.ReceivedFilesLedger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class UiEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : UiEvent()
}

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedProtocol = MutableStateFlow(ShareProtocol.OST_SHARE)
    val selectedProtocol: StateFlow<ShareProtocol> = _selectedProtocol.asStateFlow()

    private var shareService: ShareService? = null
    private var localSendService: LocalSendService? = null

    private val _isServiceRunning   = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _isServiceBound     = MutableStateFlow(false)
    private val _isReceivingActive  = MutableStateFlow(false)
    val isReceivingActive: StateFlow<Boolean> = _isReceivingActive.asStateFlow()

    private val _isDiscovering      = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _statusText         = MutableStateFlow(application.getString(R.string.idle_status))
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _transferProgress   = MutableStateFlow<Int?>(null)
    val transferProgress: StateFlow<Int?> = _transferProgress.asStateFlow()

    private val _transferFileStatus = MutableStateFlow<String?>(null)
    val transferFileStatus: StateFlow<String?> = _transferFileStatus.asStateFlow()

    private val _discoveredDevices  = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _lastReceivedFiles  = MutableStateFlow<List<File>>(emptyList())
    val lastReceivedFiles: StateFlow<List<File>> = _lastReceivedFiles.asStateFlow()

    private val _incomingTransferRequest = MutableStateFlow<IncomingTransferRequest?>(null)
    val incomingTransferRequest: StateFlow<IncomingTransferRequest?> = _incomingTransferRequest.asStateFlow()

    private val _isCleaningUp       = MutableStateFlow(false)
    val isCleaningUp: StateFlow<Boolean> = _isCleaningUp.asStateFlow()

    private val _isServiceStuck     = MutableStateFlow(false)
    val isServiceStuck: StateFlow<Boolean> = _isServiceStuck.asStateFlow()

    val isTransferActive: StateFlow<Boolean> = _transferProgress
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _stagedUris = MutableStateFlow<List<Uri>>(emptyList())
    val stagedUris: StateFlow<List<Uri>> = _stagedUris.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private var selectedDevice: DiscoveredDevice? = null
    private var observationJobs: List<Job> = emptyList()

    private val ostServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ShareService.ServiceBinder
            shareService = binder.getService()
            _isServiceBound.value = true
            if (_selectedProtocol.value == ShareProtocol.OST_SHARE) {
                observeActiveProtocol()
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            shareService = null
            if (_selectedProtocol.value == ShareProtocol.OST_SHARE) {
                _isServiceBound.value = false
                resetStateToDisconnected()
            }
        }
    }

    private val localSendServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocalSendService.ServiceBinder
            localSendService = binder.getService()
            if (_selectedProtocol.value == ShareProtocol.LOCALSEND) {
                _isServiceBound.value = true
                observeActiveProtocol()
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            localSendService = null
            if (_selectedProtocol.value == ShareProtocol.LOCALSEND) {
                _isServiceBound.value = false
                resetStateToDisconnected()
            }
        }
    }

    init {
        viewModelScope.launch {
            _isCleaningUp.collect { cleaning ->
                if (cleaning) {
                    delay(8_000)
                    if (_isCleaningUp.value) _isServiceStuck.value = true
                } else {
                    _isServiceStuck.value = false
                }
            }
        }
        loadReceivedFiles()
        bindServices()
    }

    fun setProtocol(protocol: ShareProtocol) {
        if (_selectedProtocol.value == protocol) return
        _selectedProtocol.value = protocol
        selectedDevice = null
        observeActiveProtocol()
    }

    private fun bindServices() {
        val app = getApplication<Application>()
        Intent(app, ShareService::class.java).also { intent ->
            app.bindService(intent, ostServiceConnection, Context.BIND_AUTO_CREATE)
        }
        Intent(app, LocalSendService::class.java).also { intent ->
            app.bindService(intent, localSendServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun observeActiveProtocol() {
        observationJobs.forEach { it.cancel() }
        when (_selectedProtocol.value) {
            ShareProtocol.OST_SHARE -> {
                shareService?.let { service ->
                    _isServiceBound.value = true
                    observationJobs = listOf(
                        viewModelScope.launch { service.isServiceActive.collect { _isServiceRunning.value = it } },
                        viewModelScope.launch { service.serviceStatus.collect { _statusText.value = it.statusText } },
                        viewModelScope.launch { service.isReceivingActive.collect { _isReceivingActive.value = it } },
                        viewModelScope.launch { service.isDiscovering.collect { _isDiscovering.value = it } },
                        viewModelScope.launch { service.transferProgress.collect { _transferProgress.value = it } },
                        viewModelScope.launch { service.transferFileStatus.collect { _transferFileStatus.value = it } },
                        viewModelScope.launch { service.discoveredDevices.collect { _discoveredDevices.value = it } },
                        viewModelScope.launch { service.lastReceivedFiles.collect { _lastReceivedFiles.value = it } },
                        viewModelScope.launch { service.incomingTransferRequestService.collect { _incomingTransferRequest.value = it } },
                        viewModelScope.launch { service.isCleaningUp.collect { _isCleaningUp.value = it } }
                    )
                } ?: run {
                    _isServiceBound.value = false
                    resetStateToDisconnected()
                }
            }
            ShareProtocol.LOCALSEND -> {
                localSendService?.let { service ->
                    _isServiceBound.value = true
                    observationJobs = listOf(
                        viewModelScope.launch { service.isReceiveActive.collect { _isServiceRunning.value = it } },
                        viewModelScope.launch { service.statusText.collect { _statusText.value = it } },
                        viewModelScope.launch { service.isReceiveActive.collect { _isReceivingActive.value = it } },
                        viewModelScope.launch { service.isDiscovering.collect { _isDiscovering.value = it } },
                        viewModelScope.launch { service.transferProgress.collect { _transferProgress.value = it } },
                        viewModelScope.launch { service.transferFileStatus.collect { _transferFileStatus.value = it } },
                        viewModelScope.launch { service.discoveredDevices.collect { _discoveredDevices.value = it } },
                        viewModelScope.launch { service.lastReceivedFiles.collect { _lastReceivedFiles.value = it } },
                        viewModelScope.launch { service.incomingTransferRequest.collect { _incomingTransferRequest.value = it } }
                    )
                } ?: run {
                    _isServiceBound.value = false
                    resetStateToDisconnected()
                }
            }
        }
    }

    fun loadReceivedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fallbackDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val baseDir = publicDownloads ?: fallbackDir ?: return@launch
            val ostDir = File(baseDir, Constants.FILES_DIR)

            val validEntries = ReceivedFilesLedger.getValidEntries(ostDir)
            val files = validEntries.mapNotNull {
                val f = File(ostDir, it.fileName)
                if (f.exists()) f else null
            }
            _lastReceivedFiles.value = files
        }
    }

    fun deleteReceivedFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            if (file.exists()) {
                file.delete()
            }
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fallbackDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val baseDir = publicDownloads ?: fallbackDir ?: return@launch
            val ostDir = File(baseDir, Constants.FILES_DIR)
            ReceivedFilesLedger.removeEntry(ostDir, file.name)
            loadReceivedFiles()
        }
    }

    private fun resetStateToDisconnected() {
        _isServiceRunning.value = false
        _isReceivingActive.value = false
        _isDiscovering.value = false
        _statusText.value = getApplication<Application>().getString(R.string.idle_status)
        _transferProgress.value = null
        _transferFileStatus.value = null
        _discoveredDevices.value = emptyList()
        _incomingTransferRequest.value = null
        _isCleaningUp.value = false
        _isServiceStuck.value = false
    }

    fun handlePermissionsGranted() {
    }

    fun setReceivingActive(active: Boolean) {
        when (_selectedProtocol.value) {
            ShareProtocol.OST_SHARE -> {
                val actionToPerform = if (active) Constants.ACTION_START_RECEIVING else Constants.ACTION_STOP_RECEIVING
                sendActionToOstService(actionToPerform)
            }
            ShareProtocol.LOCALSEND -> {
                if (active) {
                    localSendService?.startReceive() ?: sendActionToLocalSendService(LocalSendService.ACTION_LS_START_RECEIVE)
                } else {
                    localSendService?.stopReceive() ?: sendActionToLocalSendService(LocalSendService.ACTION_LS_STOP_RECEIVE)
                }
            }
        }
    }

    fun startService() {
        when (_selectedProtocol.value) {
            ShareProtocol.OST_SHARE -> sendActionToOstService(Constants.ACTION_LAUNCH_SERVICE)
            ShareProtocol.LOCALSEND -> {
                localSendService?.startReceive() ?: sendActionToLocalSendService(LocalSendService.ACTION_LS_START_RECEIVE)
            }
        }
    }

    fun stopService() {
        when (_selectedProtocol.value) {
            ShareProtocol.OST_SHARE -> sendActionToOstService(Constants.ACTION_STOP_SERVICE)
            ShareProtocol.LOCALSEND -> {
                localSendService?.stopReceive() ?: sendActionToLocalSendService(LocalSendService.ACTION_LS_STOP_RECEIVE)
            }
        }
    }

    fun restartService() {
        stopService()
        viewModelScope.launch {
            delay(300)
            startService()
        }
    }

    fun startDiscovery() {
        toggleDiscovery(true)
    }

    fun stopDiscovery() {
        toggleDiscovery(false)
    }

    fun toggleDiscovery(active: Boolean) {
        when (_selectedProtocol.value) {
            ShareProtocol.OST_SHARE -> {
                val actionToPerform = if (active) Constants.ACTION_START_DISCOVERY else Constants.ACTION_STOP_DISCOVERY
                sendActionToOstService(actionToPerform)
            }
            ShareProtocol.LOCALSEND -> {
                if (active) {
                    localSendService?.startDiscovery() ?: sendActionToLocalSendService(LocalSendService.ACTION_LS_START_DISCOVERY)
                } else {
                    localSendService?.stopDiscovery() ?: sendActionToLocalSendService(LocalSendService.ACTION_LS_STOP_DISCOVERY)
                }
            }
        }
    }

    fun setSelectedDevice(device: DiscoveredDevice) {
        selectedDevice = device
    }

    fun addStagedUris(uris: List<Uri>) {
        _stagedUris.value = (_stagedUris.value + uris).distinct()
    }

    fun removeStagedUri(uri: Uri) {
        _stagedUris.value = _stagedUris.value.filter { it != uri }
    }

    fun clearStagedUris() {
        _stagedUris.value = emptyList()
    }

    fun clearSelectedDevice() {
        selectedDevice = null
    }

    fun acceptIncomingTransfer(requestId: String) {
        when (_selectedProtocol.value) {
            ShareProtocol.OST_SHARE -> respondToIncomingTransfer(requestId, true)
            ShareProtocol.LOCALSEND -> localSendService?.acceptTransfer() ?: sendActionToLocalSendService(LocalSendService.ACTION_LS_ACCEPT)
        }
    }

    fun rejectIncomingTransfer(requestId: String) {
        when (_selectedProtocol.value) {
            ShareProtocol.OST_SHARE -> respondToIncomingTransfer(requestId, false)
            ShareProtocol.LOCALSEND -> localSendService?.rejectTransfer() ?: sendActionToLocalSendService(LocalSendService.ACTION_LS_REJECT)
        }
    }

    fun prepareToSendToDevice(device: DiscoveredDevice) {
        selectedDevice = device
    }

    fun sendFilesToSelectedDevice(uris: List<Uri>) {
        val target = selectedDevice
        if (target == null) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.error_no_device_selected), isError = true))
            }
            return
        }
        if (uris.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.no_files_selected), isError = true))
            }
            return
        }

        when (_selectedProtocol.value) {
            ShareProtocol.OST_SHARE -> {
                val intent = Intent(getApplication(), ShareService::class.java).apply {
                    action = Constants.ACTION_SEND_FILES
                    putParcelableArrayListExtra(Constants.EXTRA_FILE_URIS, ArrayList(uris))
                    putExtra(Constants.EXTRA_TARGET_DEVICE, target)
                }
                startForegroundOrNormalService(intent)
            }
            ShareProtocol.LOCALSEND -> {
                localSendService?.sendFiles(uris, target) ?: run {
                    val intent = Intent(getApplication(), LocalSendService::class.java).apply {
                        action = LocalSendService.ACTION_LS_SEND_FILES
                        putParcelableArrayListExtra(LocalSendService.EXTRA_FILE_URIS, ArrayList(uris))
                        putExtra(LocalSendService.EXTRA_TARGET_DEVICE, target)
                    }
                    startForegroundOrNormalService(intent)
                }
            }
        }
    }

    fun cancelTransfer() {
        when (_selectedProtocol.value) {
            ShareProtocol.OST_SHARE -> sendActionToOstService(Constants.ACTION_CANCEL_TRANSFER)
            ShareProtocol.LOCALSEND -> localSendService?.cancelTransfer() ?: sendActionToLocalSendService(LocalSendService.ACTION_LS_CANCEL)
        }
    }

    private fun respondToIncomingTransfer(requestId: String, accept: Boolean) {
        val actionToPerform = if (accept) Constants.ACTION_ACCEPT_RECEIVE else Constants.ACTION_REJECT_RECEIVE
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            action = actionToPerform
            putExtra(Constants.EXTRA_REQUEST_ID, requestId)
        }
        startForegroundOrNormalService(intent)
    }

    fun forceStopService() {
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            action = Constants.ACTION_SHUTDOWN_SERVICE
        }
        try { getApplication<Application>().startService(intent) } catch (_: Exception) {}
        viewModelScope.launch {
            delay(1000)
            if (_isServiceBound.value) {
                runCatching { getApplication<Application>().unbindService(ostServiceConnection) }
                resetStateToDisconnected()
            }
        }
    }

    private fun sendActionToOstService(action: String) {
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            this.action = action
        }
        startForegroundOrNormalService(intent)
    }

    private fun sendActionToLocalSendService(action: String) {
        val intent = Intent(getApplication(), LocalSendService::class.java).apply {
            this.action = action
        }
        startForegroundOrNormalService(intent)
    }

    private fun startForegroundOrNormalService(intent: Intent) {
        val app = getApplication<Application>()
        try {
            app.startForegroundService(intent)
        } catch (_: Exception) {
            app.startService(intent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val app = getApplication<Application>()
        if (_isServiceBound.value) {
            try { app.unbindService(ostServiceConnection) } catch (_: Exception) {}
            try { app.unbindService(localSendServiceConnection) } catch (_: Exception) {}
            _isServiceBound.value = false
        }
        observationJobs.forEach { it.cancel() }
    }
}
