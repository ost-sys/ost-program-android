package com.ost.application.core.share
import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.ost.application.core.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
@SuppressLint("MissingPermission")
class NsdHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val deviceType: String = Constants.VALUE_DEVICE_PHONE,
    private val serviceType: String = Constants.SERVICE_TYPE
) {
    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    private val resolveMutex = Mutex()
    private val multicastLock: WifiManager.MulticastLock by lazy {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.createMulticastLock("${Constants.TAG}_mcast").apply { setReferenceCounted(false) }
    }
    private var registrationLockHeld = false
    private var discoveryLockHeld = false
    private fun acquireMulticastLockForRegistration() {
        if (registrationLockHeld) return
        runCatching { if (!multicastLock.isHeld) multicastLock.acquire() }
            .onSuccess { registrationLockHeld = true }
            .onFailure { Log.e(Constants.TAG, "NsdHandler: Failed to acquire MulticastLock (registration): ${it.message}") }
    }
    private fun releaseMulticastLockForRegistration() {
        if (!registrationLockHeld) return
        registrationLockHeld = false
        if (!discoveryLockHeld && multicastLock.isHeld) {
            runCatching { multicastLock.release() }
                .onFailure { Log.w(Constants.TAG, "NsdHandler: MulticastLock release warning: ${it.message}") }
        }
    }
    private fun acquireMulticastLockForDiscovery() {
        if (discoveryLockHeld) return
        runCatching { if (!multicastLock.isHeld) multicastLock.acquire() }
            .onSuccess { discoveryLockHeld = true }
            .onFailure { Log.e(Constants.TAG, "NsdHandler: Failed to acquire MulticastLock (discovery): ${it.message}") }
    }
    private fun releaseMulticastLockForDiscovery() {
        if (!discoveryLockHeld) return
        discoveryLockHeld = false
        if (!registrationLockHeld && multicastLock.isHeld) {
            runCatching { multicastLock.release() }
                .onFailure { Log.w(Constants.TAG, "NsdHandler: MulticastLock release warning: ${it.message}") }
        }
    }
    private val _discoveredDevicesMap = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevicesMap
        .asStateFlow()
        .map { map ->
            val unique = mutableMapOf<String, DiscoveredDevice>()
            for (device in map.values) {
                if (!device.isResolved && !device.isResolving) continue
                val hostStr = device.ipAddress?.hostAddress ?: device.host
                val key = when {
                    device.id.isNotBlank() && device.id.length > 20 -> device.id
                    !hostStr.isNullOrBlank() -> "$hostStr:${device.port}"
                    else -> device.name
                }
                val existing = unique[key]
                if (existing == null || (!existing.isResolved && device.isResolved)) {
                    unique[key] = device
                }
            }
            unique.values.sortedBy { it.name }
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
    private fun isMatchingServiceType(discoveredType: String?): Boolean {
        if (discoveredType.isNullOrBlank()) return false
        val normalizedDiscovered = discoveredType.trim().trimEnd('.').lowercase()
        val normalizedExpected = serviceType.trim().trimEnd('.').lowercase()
        return normalizedDiscovered.contains(normalizedExpected) || normalizedExpected.contains(normalizedDiscovered)
    }

    fun addDiscoveredDevice(device: DiscoveredDevice) {
        val current = _discoveredDevicesMap.value.toMutableMap()
        val devIp = device.ipAddress?.hostAddress ?: device.host
        val keysToRemove = current.filter { (_, existing) ->
            val existIp = existing.ipAddress?.hostAddress ?: existing.host
            (device.id.isNotBlank() && existing.id == device.id) ||
            (!devIp.isNullOrBlank() && devIp == existIp && (device.port == existing.port || existing.port == 0)) ||
            (existing.name == device.name && !devIp.isNullOrBlank() && devIp == existIp)
        }.keys
        keysToRemove.forEach { current.remove(it) }
        val canonicalKey = device.id.ifBlank { if (!devIp.isNullOrBlank()) "$devIp:${device.port}" else device.name }
        current[canonicalKey] = device
        _discoveredDevicesMap.value = current
        Log.d(Constants.TAG, "NSD: Injected device: ${device.name} @ $devIp:${device.port}")
    }

    fun startServiceRegistration(port: Int, currentStatusUpdate: (String) -> Unit) {
        val deviceName = Build.MODEL.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
        val randomSuffix = (1000..9999).random()
        val serviceName = "${Constants.SERVICE_NAME_PREFIX}${deviceName}_${randomSuffix}"
        startServiceRegistration(
            port = port,
            customServiceName = serviceName,
            extraAttributes = mapOf(Constants.KEY_DEVICE_TYPE to deviceType),
            currentStatusUpdate = currentStatusUpdate
        )
    }

    fun startServiceRegistration(
        port: Int,
        customServiceName: String,
        extraAttributes: Map<String, String>,
        currentStatusUpdate: (String) -> Unit
    ) {
        if (registrationListener != null) {
            Log.d(Constants.TAG, "NSD: Registration already active or listener exists. Unregistering existing before new registration.")
            stopServiceRegistration()
        }
        acquireMulticastLockForRegistration()
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
                releaseMulticastLockForRegistration()
                _registrationStatus.value = NsdRegistrationStatus.Failed(context.getString(R.string.error_nsd_register_failed, errorCode))
                currentStatusUpdate(context.getString(R.string.error_nsd_register_failed, errorCode))
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(Constants.TAG, "NSD Service Unregistered: ${serviceInfo.serviceName}.")
                if (registrationListener === this) {
                    registeredServiceName = null
                    registrationListener = null
                }
                releaseMulticastLockForRegistration()
                _registrationStatus.value = NsdRegistrationStatus.Idle
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(Constants.TAG, "NSD Unregistration Failed: Error code $errorCode for ${serviceInfo.serviceName}.")
                if (registrationListener === this) {
                    registeredServiceName = null
                    registrationListener = null
                }
                releaseMulticastLockForRegistration()
                _registrationStatus.value = NsdRegistrationStatus.Failed(context.getString(R.string.error_nsd_unregister_failed, errorCode.toString()))
            }
        }
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = customServiceName
            this.serviceType = this@NsdHandler.serviceType
            this.port = port
            extraAttributes.forEach { (k, v) -> setAttribute(k, v) }
        }
        scope.launch(Dispatchers.Main) {
            try {
                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, newListener)
                Log.d(Constants.TAG, "NSD: registerService call issued successfully.")
                _registrationStatus.value = NsdRegistrationStatus.Registering
            } catch (e: Exception) {
                Log.e(Constants.TAG, "NSD: Error registering NSD service: ${e.message}", e)
                releaseMulticastLockForRegistration()
                _registrationStatus.value = NsdRegistrationStatus.Failed(context.getString(R.string.error_nsd_exception, e.javaClass.simpleName))
                currentStatusUpdate(context.getString(R.string.error_nsd_exception, e.javaClass.simpleName))
                registrationListener = null
                registeredServiceName = null
            }
        }
    }
    fun stopServiceRegistration() {
        val listenerToUnregister = registrationListener
        val nameToUnregister = registeredServiceName
        if (listenerToUnregister == null || nameToUnregister == null) {
            Log.d(Constants.TAG, "NSD: No active NSD listener or service name to unregister. Skipping.")
            releaseMulticastLockForRegistration()
            return
        }
        scope.launch(Dispatchers.Main) {
            runCatching {
                nsdManager.unregisterService(listenerToUnregister)
                Log.d(Constants.TAG, "NSD: unregisterService call issued for ${nameToUnregister}.")
            }.onFailure { ex ->
                releaseMulticastLockForRegistration()
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
                    _registrationStatus.value = NsdRegistrationStatus.Failed(context.getString(R.string.error_nsd_unregister_failed, ex.message ?: "Unknown"))
                }
            }
        }
    }
    fun startDiscovery(currentStatusUpdate: (String) -> Unit) {
        if (discoveryJob?.isActive == true) {
            Log.d(Constants.TAG, "NSD: Discovery already active. Skipping.")
            return
        }
        stopDiscovery()
        acquireMulticastLockForDiscovery()
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
                if (!isMatchingServiceType(service.serviceType)) return
                if (serviceType == Constants.SERVICE_TYPE && !service.serviceName.startsWith("OST")) return
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
                releaseMulticastLockForDiscovery()
                _isDiscovering.value = false
                _discoveryStatus.value = NsdDiscoveryStatus.Idle
                scope.launch(Dispatchers.Main.immediate) {
                    currentStatusUpdate(context.getString(R.string.discovery_stopped))
                }
            }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(Constants.TAG, "NSD: Discovery start failed: Error code $errorCode")
                releaseMulticastLockForDiscovery()
                _isDiscovering.value = false
                if (discoveryListener === this) discoveryListener = null
                _discoveryStatus.value = NsdDiscoveryStatus.Failed(context.getString(R.string.error_discovery_failed, errorCode))
                scope.launch(Dispatchers.Main.immediate) {
                    currentStatusUpdate(context.getString(R.string.error_discovery_failed, errorCode))
                }
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(Constants.TAG, "NSD: Discovery stop failed: Error code $errorCode")
                releaseMulticastLockForDiscovery()
                if (discoveryListener === this) discoveryListener = null
                _discoveryStatus.value = NsdDiscoveryStatus.Failed(context.getString(R.string.error_discovery_stop_failed, errorCode))
            }
        }
        this.discoveryListener = newListener
        discoveryJob = scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, newListener)
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
                    currentStatusUpdate(context.getString(R.string.discovery_error, e.message ?: "Unknown"))
                }
                stopDiscovery()
            } finally {
                stopDiscoveryInternal()
            }
        }
    }
    fun stopDiscovery() {
        val jobToCancel = discoveryJob
        val listenerToStop = discoveryListener
        if (jobToCancel == null && listenerToStop == null) {
            Log.d(Constants.TAG, "NSD: No active discovery to stop. Skipping.")
            releaseMulticastLockForDiscovery()
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
                    releaseMulticastLockForDiscovery()
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
        releaseMulticastLockForDiscovery()
        _isDiscovering.value = false
        _discoveryStatus.value = NsdDiscoveryStatus.Idle
        _discoveredDevicesMap.value = _discoveredDevicesMap.value.filterValues { it.isResolved }
        resolvingServices.values.forEach { it.cancel("Discovery stopped") }
        resolvingServices.clear()
        discoveryListener = null
    }
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
            try {
                resolveMutex.withLock {
                    val deferred = CompletableDeferred<Unit>()
                    val resolveListener = object : NsdManager.ResolveListener {
                        override fun onResolveFailed(failedServiceInfo: NsdServiceInfo, errorCode: Int) {
                            val name = failedServiceInfo.serviceName ?: "Unknown"
                            Log.e(Constants.TAG, "NSD: Resolution failed for $name: Error code $errorCode")
                            scope.launch(Dispatchers.Main.immediate) {
                                _discoveredDevicesMap.value = _discoveredDevicesMap.value.toMutableMap().apply {
                                    this[name] = this[name]?.copy(isResolving = false, isResolved = false) ?: DiscoveredDevice(failedServiceInfo).copy(isResolving = false, isResolved = false)
                                }
                            }
                            deferred.complete(Unit)
                        }
                        override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                            val name = resolvedServiceInfo.serviceName ?: "Unknown"
                            Log.d(Constants.TAG, "NSD: Service resolved: $name (IP: ${resolvedServiceInfo.host?.hostAddress}, Port: ${resolvedServiceInfo.port})")
                            val resolvedDevice = DiscoveredDevice(resolvedServiceInfo)
                            val resIp = resolvedDevice.ipAddress?.hostAddress ?: resolvedDevice.host
                            scope.launch(Dispatchers.Main.immediate) {
                                val current = _discoveredDevicesMap.value.toMutableMap()
                                val keysToRemove = current.filter { (k, existing) ->
                                    k == name ||
                                    existing.serviceInfo?.serviceName == resolvedServiceInfo.serviceName ||
                                    (!resIp.isNullOrBlank() && (existing.ipAddress?.hostAddress == resIp || existing.host == resIp) && (existing.port == resolvedDevice.port || existing.port == 0))
                                }.keys
                                keysToRemove.forEach { current.remove(it) }
                                val canonicalKey = resolvedDevice.id.ifBlank { name }
                                current[canonicalKey] = resolvedDevice
                                _discoveredDevicesMap.value = current
                            }
                            deferred.complete(Unit)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        nsdManager.resolveService(serviceInfo, resolveListener)
                    }
                    withTimeoutOrNull(5000) {
                        deferred.await()
                    }
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
