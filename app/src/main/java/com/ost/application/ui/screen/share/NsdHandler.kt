package com.ost.application.ui.screen.share

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import com.ost.application.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentHashMap

class NsdHandler(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val nsdManager: NsdManager by lazy { context.getSystemService(Context.NSD_SERVICE) as NsdManager }

    private val _discoveredDevicesMap = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevicesMap
        .asStateFlow()
        .map {
            it.values.filter { d -> d.isResolved || d.isResolving }.sortedBy { device -> device.name }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _registrationStatus = MutableStateFlow<NsdRegistrationStatus>(NsdRegistrationStatus.Idle)
    val registrationStatus: StateFlow<NsdRegistrationStatus> = _registrationStatus.asStateFlow()

    private val _discoveryStatus = MutableStateFlow<NsdDiscoveryStatus>(NsdDiscoveryStatus.Idle)
    val discoveryStatus: StateFlow<NsdDiscoveryStatus> = _discoveryStatus.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var registeredServiceName: String? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var discoveryJob: Job? = null
    private val resolvingServices = ConcurrentHashMap<String, Job>()

    @SuppressLint("MissingPermission")
    fun startServiceRegistration(port: Int, currentStatusUpdate: (String) -> Unit) {
        if (registrationListener != null) {
            Log.d(Constants.TAG, "NSD: Registration already active or listener exists. Unregistering existing before new registration.")
            stopServiceRegistration()
        }

        val newListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                registeredServiceName = nsdServiceInfo.serviceName
                registrationListener = this
                _registrationStatus.value = NsdRegistrationStatus.Registered(nsdServiceInfo.serviceName)
                currentStatusUpdate(context.getString(R.string.receiver_active_waiting))
                Log.d(Constants.TAG, "NSD Service Registered: ${nsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(Constants.TAG, "NSD Registration Failed: Error code $errorCode for ${serviceInfo.serviceName}.")
                if (registrationListener === this) {
                    registeredServiceName = null
                    registrationListener = null
                }
                _registrationStatus.value = NsdRegistrationStatus.Failed(context.getString(R.string.error_nsd_register_failed, errorCode))
                currentStatusUpdate(context.getString(R.string.error_nsd_register_failed, errorCode))
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(Constants.TAG, "NSD Service Unregistered: ${serviceInfo.serviceName}.")
                if (registrationListener === this) {
                    registeredServiceName = null
                    registrationListener = null
                }
                _registrationStatus.value = NsdRegistrationStatus.Idle
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(Constants.TAG, "NSD Unregistration Failed: Error code $errorCode for ${serviceInfo.serviceName}.")
                if (registrationListener === this) {
                    registeredServiceName = null
                    registrationListener = null
                }
                _registrationStatus.value = NsdRegistrationStatus.Failed(context.getString(R.string.error_nsd_unregister_failed, errorCode))
            }
        }

        val deviceName = Build.MODEL.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
        val randomSuffix = (1000..9999).random()
        val serviceName = "${Constants.SERVICE_NAME_PREFIX}${deviceName}_${randomSuffix}"
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = Constants.SERVICE_TYPE
            this.port = port
            setAttribute(Constants.KEY_DEVICE_TYPE, Constants.VALUE_DEVICE_PHONE)
        }

        scope.launch(Dispatchers.Main) {
            try {
                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, newListener)
                Log.d(Constants.TAG, "NSD: registerService call issued successfully.")
                _registrationStatus.value = NsdRegistrationStatus.Registering
            } catch (e: Exception) {
                Log.e(Constants.TAG, "NSD: Error registering NSD service: ${e.message}", e)
                _registrationStatus.value = NsdRegistrationStatus.Failed(context.getString(R.string.error_nsd_exception, e.javaClass.simpleName))
                currentStatusUpdate(context.getString(R.string.error_nsd_exception, e.javaClass.simpleName))
                registrationListener = null
                registeredServiceName = null
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopServiceRegistration() {
        val listenerToUnregister = registrationListener
        val nameToUnregister = registeredServiceName

        if (listenerToUnregister == null || nameToUnregister == null) {
            Log.d(Constants.TAG, "NSD: No active NSD listener or service name to unregister. Skipping.")
            return
        }

        scope.launch(Dispatchers.Main) {
            runCatching {
                nsdManager.unregisterService(listenerToUnregister)
                Log.d(Constants.TAG, "NSD: unregisterService call issued for ${nameToUnregister}.")
            }.onFailure { ex ->
                if (ex is CancellationException) {
                    Log.w(Constants.TAG, "NSD: unregisterService was cancelled for ${nameToUnregister}: ${ex.message}")
                } else if (ex is IllegalArgumentException && ex.message == "listener not registered") {
                    Log.d(Constants.TAG, "NSD: unregisterService: listener already unregistered or never registered. Clearing local refs directly.")
                    if (registrationListener === listenerToUnregister) {
                        registrationListener = null
                        registeredServiceName = null
                        _registrationStatus.value = NsdRegistrationStatus.Idle
                    }
                } else {
                    Log.e(Constants.TAG, "NSD: Error unregistering NSD service for ${nameToUnregister}: ${ex.message}", ex)
                    _registrationStatus.value = NsdRegistrationStatus.Failed(context.getString(R.string.error_nsd_unregister_failed, ex.message))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(currentStatusUpdate: (String) -> Unit) {
        if (discoveryJob?.isActive == true) {
            Log.d(Constants.TAG, "NSD: Discovery already active. Skipping.")
            return
        }

        stopDiscovery()

        _isDiscovering.value = true
        _discoveredDevicesMap.value = emptyMap()
        currentStatusUpdate(context.getString(R.string.searching))
        _discoveryStatus.value = NsdDiscoveryStatus.Discovering

        val newListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(Constants.TAG, "NSD: Discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceName == registeredServiceName) return
                if (service.serviceType != Constants.SERVICE_TYPE || !service.serviceName.startsWith(Constants.SERVICE_NAME_PREFIX)) return

                scope.launch(Dispatchers.Main.immediate) {
                    val currentMap = _discoveredDevicesMap.value
                    if (!currentMap.containsKey(service.serviceName) || currentMap[service.serviceName]?.isResolved == false) {
                        _discoveredDevicesMap.value = currentMap + (service.serviceName to DiscoveredDevice(service).copy(isResolving = true))
                        currentStatusUpdate(context.getString(R.string.found_device_s, _discoveredDevicesMap.value.size))
                        resolveDevice(service)
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                if (service.serviceName == registeredServiceName) return
                scope.launch(Dispatchers.Main.immediate) {
                    val currentMap = _discoveredDevicesMap.value
                    if (currentMap.containsKey(service.serviceName)) {
                        _discoveredDevicesMap.value = currentMap - service.serviceName
                        val remainingResolved = _discoveredDevicesMap.value.count { it.value.isResolved }
                        if (_isDiscovering.value) {
                            currentStatusUpdate(
                                if (remainingResolved > 0) context.getString(R.string.found_device_s, remainingResolved)
                                else context.getString(R.string.searching)
                            )
                        }
                    }
                    resolvingServices[service.serviceName]?.cancel("Service lost")
                    resolvingServices.remove(service.serviceName)
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(Constants.TAG, "NSD: Discovery stopped: $serviceType")
                if (discoveryListener === this) discoveryListener = null
                _isDiscovering.value = false
                _discoveryStatus.value = NsdDiscoveryStatus.Idle
                scope.launch(Dispatchers.Main.immediate) {
                    currentStatusUpdate(context.getString(R.string.discovery_stopped))
                }
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(Constants.TAG, "NSD: Discovery start failed: Error code $errorCode")
                _isDiscovering.value = false
                if (discoveryListener === this) discoveryListener = null
                _discoveryStatus.value = NsdDiscoveryStatus.Failed(context.getString(R.string.error_discovery_failed, errorCode))
                scope.launch(Dispatchers.Main.immediate) {
                    currentStatusUpdate(context.getString(R.string.error_discovery_failed, errorCode))
                }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(Constants.TAG, "NSD: Discovery stop failed: Error code $errorCode")
                if (discoveryListener === this) discoveryListener = null
                _discoveryStatus.value = NsdDiscoveryStatus.Failed(context.getString(R.string.error_discovery_stop_failed, errorCode))
            }
        }
        this.discoveryListener = newListener

        discoveryJob = scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    nsdManager.discoverServices(Constants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, newListener)
                    Log.d(Constants.TAG, "NSD: discoverServices call issued successfully.")
                }
                while (isActive) {
                    delay(Long.MAX_VALUE)
                }
            } catch (ex: CancellationException) {
                Log.d(Constants.TAG, "NSD: Discovery job cancelled: ${ex.message}")
            } catch (e: Exception) {
                Log.e(Constants.TAG, "NSD: Unexpected error in discovery job: ${e.message}", e)
                scope.launch(Dispatchers.Main.immediate) {
                    currentStatusUpdate(context.getString(R.string.discovery_error, e.message))
                }
                stopDiscovery()
            } finally {
                stopDiscoveryInternal()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        val jobToCancel = discoveryJob
        val listenerToStop = discoveryListener

        if (jobToCancel == null && listenerToStop == null) {
            Log.d(Constants.TAG, "NSD: No active discovery to stop. Skipping.")
            return
        }

        jobToCancel?.cancel(CancellationException("Discovery stopped by request"))
        discoveryJob = null

        if (listenerToStop != null) {
            scope.launch(Dispatchers.Main) {
                runCatching {
                    nsdManager.stopServiceDiscovery(listenerToStop)
                    Log.d(Constants.TAG, "NSD: stopServiceDiscovery call issued.")
                }.onFailure { ex ->
                    if (ex is IllegalArgumentException && ex.message == "listener not registered") {
                        Log.d(Constants.TAG, "NSD: stopServiceDiscovery: listener already unregistered or never registered. Clearing local refs directly.")
                        if (discoveryListener === listenerToStop) {
                            discoveryListener = null
                        }
                    } else {
                        Log.e(Constants.TAG, "NSD: Error stopping discovery: ${ex.message}", ex)
                    }
                }
            }
        }
        stopDiscoveryInternal()
    }

    private fun stopDiscoveryInternal() {
        _isDiscovering.value = false
        _discoveryStatus.value = NsdDiscoveryStatus.Idle
        _discoveredDevicesMap.value = _discoveredDevicesMap.value.filterValues { it.isResolved }
        resolvingServices.values.forEach { it.cancel("Discovery stopped") }
        resolvingServices.clear()
        discoveryListener = null
    }

    @SuppressLint("MissingPermission")
    fun resolveDevice(serviceInfo: NsdServiceInfo) {
        val serviceName = serviceInfo.serviceName ?: return
        if (resolvingServices.containsKey(serviceName)) {
            Log.d(Constants.TAG, "NSD: Resolution for $serviceName already in progress. Skipping.")
            return
        }

        scope.launch(Dispatchers.Main.immediate) {
            val device = _discoveredDevicesMap.value[serviceName]
            if (device == null || device.isResolved || device.isResolving) return@launch
            _discoveredDevicesMap.value = _discoveredDevicesMap.value + (serviceName to device.copy(isResolving = true))
        }

        val resolveJob = scope.launch {
            val resolveListener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(failedServiceInfo: NsdServiceInfo, errorCode: Int) {
                    val name = failedServiceInfo.serviceName ?: "Unknown"
                    Log.e(Constants.TAG, "NSD: Resolution failed for $name: Error code $errorCode")
                    scope.launch(Dispatchers.Main.immediate) {
                        _discoveredDevicesMap.value = _discoveredDevicesMap.value.toMutableMap().apply {
                            this[name] = this[name]?.copy(isResolving = false, isResolved = false) ?: DiscoveredDevice(name, DiscoveredDevice.extractDeviceName(name), isResolving = false, isResolved = false)
                        }
                    }
                    resolvingServices.remove(name)
                }

                override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                    val name = resolvedServiceInfo.serviceName ?: "Unknown"
                    Log.d(Constants.TAG, "NSD: Service resolved: $name")
                    val resolvedDevice = DiscoveredDevice(resolvedServiceInfo)
                    scope.launch(Dispatchers.Main.immediate) {
                        _discoveredDevicesMap.value = _discoveredDevicesMap.value.toMutableMap().apply {
                            this[name] = resolvedDevice
                        }
                    }
                    resolvingServices.remove(name)
                }
            }
            try {
                withContext(Dispatchers.Main) {
                    nsdManager.resolveService(serviceInfo, resolveListener)
                }
            } catch (e: CancellationException) {
                Log.d(Constants.TAG, "NSD: Resolution job for $serviceName cancelled.")
            } catch (e: Exception) {
                Log.e(Constants.TAG, "NSD: Error resolving service $serviceName: ${e.message}", e)
                scope.launch(Dispatchers.Main.immediate) {
                    _discoveredDevicesMap.value[serviceName]?.let {
                        _discoveredDevicesMap.value = _discoveredDevicesMap.value + (serviceName to it.copy(isResolving = false, isResolved = false))
                    }
                }
            } finally {
                resolvingServices.remove(serviceName)
            }
        }
        resolvingServices[serviceName] = resolveJob
    }

    fun shutdown() {
        Log.d(Constants.TAG, "NSD: Shutting down NsdHandler.")
        stopDiscovery()
        stopServiceRegistration()
        resolvingServices.values.forEach { it.cancel("NSD handler shutting down") }
        resolvingServices.clear()
    }
}

sealed class NsdRegistrationStatus {
    object Idle : NsdRegistrationStatus()
    object Registering : NsdRegistrationStatus()
    data class Registered(val serviceName: String) : NsdRegistrationStatus()
    data class Failed(val message: String) : NsdRegistrationStatus()
}

sealed class NsdDiscoveryStatus {
    object Idle : NsdDiscoveryStatus()
    object Discovering : NsdDiscoveryStatus()
    data class Failed(val message: String) : NsdDiscoveryStatus()
}