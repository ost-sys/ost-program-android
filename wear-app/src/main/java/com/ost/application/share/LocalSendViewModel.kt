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
import com.ost.application.core.share.DiscoveredDevice
import com.ost.application.core.share.LocalSendPrepareRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class LocalSendViewModel(application: Application) : AndroidViewModel(application) {

    private var service: LocalSendService? = null

    private val _isBound = MutableStateFlow(false)

    private val _isReceiveActive   = MutableStateFlow(false)
    val isReceiveActive: StateFlow<Boolean> = _isReceiveActive.asStateFlow()

    private val _isDiscovering     = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _statusText        = MutableStateFlow("Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _transferProgress  = MutableStateFlow<Int?>(null)
    val transferProgress: StateFlow<Int?> = _transferProgress.asStateFlow()

    private val _transferFileStatus = MutableStateFlow<String?>(null)
    val transferFileStatus: StateFlow<String?> = _transferFileStatus.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _lastReceivedFiles = MutableStateFlow<List<File>>(emptyList())
    val lastReceivedFiles: StateFlow<List<File>> = _lastReceivedFiles.asStateFlow()

    private val _incomingRequest   = MutableStateFlow<LocalSendPrepareRequest?>(null)
    val incomingRequest: StateFlow<LocalSendPrepareRequest?> = _incomingRequest.asStateFlow()

    private val _transferTotalFiles = MutableStateFlow<Int?>(null)
    val transferTotalFiles: StateFlow<Int?> = _transferTotalFiles.asStateFlow()

    private val _isSendingActive   = MutableStateFlow(false)
    val isSendingActive: StateFlow<Boolean> = _isSendingActive.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = (binder as LocalSendService.ServiceBinder).getService()
            service = svc
            _isBound.value = true
            observeService(svc)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            _isBound.value = false
        }
    }

    init {
        getApplication<Application>().bindService(
            Intent(getApplication(), LocalSendService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun observeService(svc: LocalSendService) {
        viewModelScope.launch { svc.isReceiveActive.collect { _isReceiveActive.value = it } }
        viewModelScope.launch { svc.isDiscovering.collect   { _isDiscovering.value = it } }
        viewModelScope.launch { svc.statusText.collect      { _statusText.value = it } }
        viewModelScope.launch { svc.transferProgress.collect { _transferProgress.value = it } }
        viewModelScope.launch { svc.transferFileStatus.collect { _transferFileStatus.value = it } }
        viewModelScope.launch { svc.discoveredDevices.collect { _discoveredDevices.value = it } }
        viewModelScope.launch { svc.lastReceivedFiles.collect { _lastReceivedFiles.value = it } }
        viewModelScope.launch { svc.incomingRequest.collect  { _incomingRequest.value = it } }
        viewModelScope.launch { svc.transferTotalFiles.collect { _transferTotalFiles.value = it } }
        viewModelScope.launch { svc.isSendingActive.collect  { _isSendingActive.value = it } }
    }

    fun setReceiveActive(active: Boolean) {
        startServiceAction(if (active) LocalSendService.ACTION_LS_START_RECEIVE else LocalSendService.ACTION_LS_STOP_RECEIVE)
    }

    fun startDiscovery() {
        startServiceAction(LocalSendService.ACTION_LS_START_DISCOVERY)
    }

    fun stopDiscovery() {
        startServiceAction(LocalSendService.ACTION_LS_STOP_DISCOVERY)
    }

    fun resolveDevice(serviceInfo: NsdServiceInfo) {
        service?.resolveDevice(serviceInfo)
    }

    fun sendFiles(device: DiscoveredDevice, uris: ArrayList<Uri>) {
        val intent = Intent(getApplication(), LocalSendService::class.java).apply {
            action = LocalSendService.ACTION_LS_SEND_FILES
            putParcelableArrayListExtra(LocalSendService.EXTRA_FILE_URIS, uris)
            putExtra(LocalSendService.EXTRA_TARGET_DEVICE, device)
        }
        getApplication<Application>().startService(intent)
    }

    fun acceptIncomingTransfer() {
        startServiceAction(LocalSendService.ACTION_LS_ACCEPT)
    }

    fun rejectIncomingTransfer() {
        startServiceAction(LocalSendService.ACTION_LS_REJECT)
    }

    fun cancelTransfer() {
        startServiceAction(LocalSendService.ACTION_LS_CANCEL)
    }

    private fun startServiceAction(action: String) {
        val intent = Intent(getApplication(), LocalSendService::class.java).apply { this.action = action }
        getApplication<Application>().startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(connection)
    }
}
