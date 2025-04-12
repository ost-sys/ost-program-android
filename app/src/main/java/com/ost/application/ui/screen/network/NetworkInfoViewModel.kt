package com.ost.application.ui.screen.network

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import android.text.format.Formatter
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import java.util.Enumeration

object NetworkType {
    const val WIFI = 1
    const val MOBILE = 2
    const val NONE = 0
}

@Stable
data class NetworkInfoUiState(
    val carrierName: String = "Loading...",
    val countryCode: String = "??",
    val networkTypeString: String = "---",
    val connectivityStatusString: String = "Checking...",
    val ipAddressDisplay: String = "Loading...",
    val iconRes: Int = R.drawable.ic_no_wifi_24dp,
    val isIpMasked: Boolean = false,
    val isLoadingIp: Boolean = true,
    val permissionGranted: Boolean = true
)

sealed class NetworkInfoAction {
    data class RequestPermission(val permission: String) : NetworkInfoAction()
    data class ShowToast(val messageResId: Int) : NetworkInfoAction()
}


class NetworkInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NetworkInfoUiState())
    val uiState: StateFlow<NetworkInfoUiState> = _uiState.asStateFlow()

    private val _action = Channel<NetworkInfoAction>(Channel.BUFFERED)
    val action = _action.receiveAsFlow()

    private var periodicUpdateJob: Job? = null
    private var _originalIpAddress: String = ""

    init {
        checkPermissions()
    }

    private fun checkPermissions() {
        val context = getApplication<Application>()
        val granted = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(permissionGranted = granted) }
    }

    fun requestPermission() {
        viewModelScope.launch {
            _action.send(NetworkInfoAction.RequestPermission(Manifest.permission.READ_PHONE_STATE))
        }
    }

    fun loadInitialDataAndStartUpdates() {
        checkPermissions()
        loadIpAddress()
        startPeriodicUpdates()
    }

    fun stopUpdates() {
        periodicUpdateJob?.cancel()
        periodicUpdateJob = null
    }

    private fun startPeriodicUpdates() {
        stopUpdates()
        periodicUpdateJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                updateNetworkDisplayInfo()
                delay(500)
            }
        }
    }

    private fun loadIpAddress() {
        if (!_uiState.value.isLoadingIp && _originalIpAddress.isNotEmpty()) return

        _uiState.update { it.copy(isLoadingIp = true, ipAddressDisplay = "Loading...") }
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val connStatus = getConnectivityStatus(context)
            var ipResult = getString(R.string.not_connected_to_internet)

            ipResult = when (connStatus) {
                NetworkType.WIFI -> {
                    val localIp = getLocalIpAddress(context)
                    val publicIpDeferred = async { getPublicIp() }
                    val publicIp = publicIpDeferred.await()
                    if (localIp != getString(R.string.failed_to_obtain_ip) && publicIp != getString(R.string.failed_to_obtain_ip)) {
                        "$localIp\n$publicIp"
                    } else if (localIp != getString(R.string.failed_to_obtain_ip)) {
                        localIp
                    } else {
                        getString(R.string.failed_to_obtain_ip)
                    }
                }
                NetworkType.MOBILE -> getMobileIpAddress() ?: getString(R.string.failed_to_obtain_ip)
                else -> getString(R.string.not_connected_to_internet)
            }

            _originalIpAddress = ipResult

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        ipAddressDisplay = if (it.isIpMasked) maskIp(ipResult) else ipResult,
                        isLoadingIp = false
                    )
                }
            }
        }
    }


    private suspend fun updateNetworkDisplayInfo() {
        val context = getApplication<Application>()
        val connStatus = getConnectivityStatus(context)
        val connectivityString = getConnectivityStatusString(connStatus)
        val icon = getConnectivityStatusIconRes(connStatus)
        var carrier = getString(R.string.sim_card_is_not_detected)
        var country = "??"
        var netType = getString(R.string.unknown)

        if (_uiState.value.permissionGranted) {
            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (manager != null) {
                try {
                    carrier = manager.networkOperatorName?.takeIf { it.isNotBlank() } ?: carrier
                    country = manager.networkCountryIso?.uppercase() ?: country
                    netType = getNetworkTypeStringWithLogging(manager)
                } catch (e: SecurityException) {
                    Log.e("NetworkInfoViewModel", "Permission error getting telephony info", e)
                    netType = getString(R.string.permission_not_granted)
                }
            }
        } else {
            netType = getString(R.string.grant_permission_to_continue)
        }


        withContext(Dispatchers.Main) {
            _uiState.update {
                it.copy(
                    carrierName = carrier,
                    countryCode = country,
                    networkTypeString = netType,
                    connectivityStatusString = connectivityString,
                    iconRes = icon
                )
            }
        }
    }

    fun toggleIpMasking() {
        val currentMaskedState = _uiState.value.isIpMasked
        val newMaskedState = !currentMaskedState
        _uiState.update {
            it.copy(
                isIpMasked = newMaskedState,
                ipAddressDisplay = if (newMaskedState) maskIp(_originalIpAddress) else _originalIpAddress
            )
        }
    }

    private fun maskIp(ip: String): String {
        return ip.replace(Regex("[0-9]"), "*")
    }

    private fun getString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }

    private fun getConnectivityStatus(context: Context): Int {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return NetworkType.NONE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return NetworkType.NONE
            val capabilities = cm.getNetworkCapabilities(network) ?: return NetworkType.NONE
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                else -> NetworkType.NONE
            }
        } else {
            @Suppress("DEPRECATION")
            val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            return when (activeNetwork?.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.MOBILE
                else -> NetworkType.NONE
            }
        }
    }

    private fun getConnectivityStatusString(connStatus: Int): String {
        return when (connStatus) {
            NetworkType.WIFI -> getString(R.string.wifi_enabled)
            NetworkType.MOBILE -> getString(R.string.mobile_data_enabled)
            else -> getString(R.string.not_connected_to_internet)
        }
    }

    private fun getConnectivityStatusIconRes(connStatus: Int): Int {
        return when (connStatus) {
            NetworkType.WIFI -> R.drawable.ic_wifi_24dp
            NetworkType.MOBILE -> R.drawable.ic_signal_cellular_24dp
            else -> R.drawable.ic_no_wifi_24dp
        }
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun getLocalIpAddress(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            Formatter.formatIpAddress(wifiManager?.connectionInfo?.ipAddress ?: 0)
                .takeIf { it != "0.0.0.0" } ?: getString(R.string.failed_to_obtain_ip)
        } catch (e: Exception) {
            Log.e("NetworkInfoViewModel", "Error getting local IP", e)
            getString(R.string.failed_to_obtain_ip)
        }
    }

    private suspend fun getPublicIp(): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api64.ipify.org?format=json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
                val response = reader.readText()
                reader.close()
                connection.disconnect()
                JSONObject(response).getString("ip")
            } else {
                connection.disconnect()
                Log.w("NetworkInfoViewModel", "Public IP fetch failed with code: ${connection.responseCode}")
                getString(R.string.failed_to_obtain_ip)
            }
        } catch (e: Exception) {
            Log.e("NetworkInfoViewModel", "Error getting public IP", e)
            getString(R.string.failed_to_obtain_ip)
        }
    }


    private fun getMobileIpAddress(): String? {
        try {
            val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isLoopback && !networkInterface.isVirtual && networkInterface.isUp) {
                    if (networkInterface.displayName.contains("rmnet", ignoreCase = true) ||
                        networkInterface.displayName.contains("ccmni", ignoreCase = true) ||
                        networkInterface.displayName.contains("radio", ignoreCase = true) ||
                        networkInterface.displayName.contains("uwbr", ignoreCase = true)) {

                        val addresses: Enumeration<InetAddress> = networkInterface.inetAddresses
                        while (addresses.hasMoreElements()) {
                            val inetAddress = addresses.nextElement()
                            if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                                return inetAddress.hostAddress
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkInfoViewModel", "Error getting mobile IP", e)
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun getNetworkTypeStringWithLogging(manager: TelephonyManager): String {
        val networkTypeInt: Int = try {
            manager.dataNetworkType
        } catch (e: SecurityException){
            Log.e("NetworkInfoViewModel", "Permission error reading network type", e)
            return getString(R.string.permission_not_granted)
        } catch (e: Exception){
            Log.e("NetworkInfoViewModel", "Exception reading network type", e)
            return getString(R.string.error)
        }

        return when (networkTypeInt) {
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO rev. 0"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO rev. A"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO rev. B"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_IDEN -> "iDen"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD"
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA"
            TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
            TelephonyManager.NETWORK_TYPE_NR -> "NR (5G)"
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> getString(R.string.unknown) + " (0)"
            else -> getString(R.string.unknown) + " ($networkTypeInt)"
        }
    }
}