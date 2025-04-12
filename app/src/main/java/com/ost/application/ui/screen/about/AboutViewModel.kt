package com.ost.application.ui.screen.about

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.ost.application.BuildConfig
import com.ost.application.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Random

private const val TAG = "AboutViewModel"

enum class UpdateState { IDLE, CHECKING, AVAILABLE, NOT_AVAILABLE, ERROR }
enum class DownloadStatus { IDLE, DOWNLOADING, COMPLETE, FAILED }
enum class WearUpdateCheckState { IDLE, CHECKING_GITHUB, REQUESTING_INSTALLED, UP_TO_DATE, AVAILABLE, ERROR }

@Stable
data class AboutUiState(
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val latestPhoneVersionName: String? = null,
    val updateState: UpdateState = UpdateState.IDLE,
    val updateError: String? = null,
    val changelog: String? = null,
    val apkUrl: String? = null,
    val downloadProgress: Int = 0,
    val downloadStatus: DownloadStatus = DownloadStatus.IDLE,
    val showDownloadDialog: Boolean = false,
    val showUpdateConfirmDialog: Boolean = false,
    val showChangelogDialog: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val canInstallUnknownApps: Boolean = false,
    val canRequestPackageInstalls: Boolean = false,
    val showRandomMessage: String? = null,
    val showUpdateAvailableHint: Boolean = false,
    val wearNodeConnected: Boolean = false,
    val installedWearVersionName: String? = null,
    val latestWearVersionName: String? = null,
    val wearUpdateCheckState: WearUpdateCheckState = WearUpdateCheckState.IDLE
)

sealed class AboutAction {
    data class ShowToast(val message: String) : AboutAction()
    data class ShowToastRes(val messageResId: Int) : AboutAction()
    data class LaunchIntent(val intent: Intent) : AboutAction()
    data class RequestPermission(val permission: String) : AboutAction()
    object RequestInstallPermission : AboutAction()
    object RequestStoragePermissionLegacy : AboutAction()
}

class AboutViewModel(application: Application) : AndroidViewModel(application),
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    private val _action = Channel<AboutAction>(Channel.BUFFERED)
    val action = _action.receiveAsFlow()

    private val downloadManager = application.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var currentDownloadId: Long? = null
    private var downloadProgressJob: Job? = null

    private val messageClient by lazy { Wearable.getMessageClient(application) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(application) }
    private val nodeClient by lazy { Wearable.getNodeClient(application) }

    private val wearAppCapability = "ost_wear_app_companion"
    private val requestVersionPath = "/request_wear_version"
    private val versionResponsePath = "/wear_version_response"

    private val phoneReleaseAssetName = "app-release.apk"

    private val randomMessages = listOf(
        "Callback? Ping!", "It's me, OST Tools", "You got notification!", "Привет, мир!",
        "Success!", "S C H L E C K", "Who are you?", "Operating System Tester",
        "Subscribe to my channel please :D", "Access denied", "I know you here!",
        "I sent your IP-address моему создателю! Жди докс", "0x000000", "OK Google",
        "ыыыыыыыыыыыыы", "Hello and, again, welcome to the Aperture Science computer-aided enrichment center.",
        "Easier to assimilate than explain",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
    )

    init {
        loadInitialPermissions()
        Log.d(TAG, ">>> Adding listeners...")
        messageClient.addListener(this)
        capabilityClient.addListener(this, "wear://*/$wearAppCapability".toUri(), CapabilityClient.FILTER_ALL)
        Log.d(TAG, ">>> Listeners ADDED")
        checkWearConnectionAndRequestVersion()
    }

    private fun checkWearConnectionAndRequestVersion() {
        Log.d(TAG, "Checking Wear connection and requesting version if connected...")
        if (uiState.value.wearUpdateCheckState == WearUpdateCheckState.REQUESTING_INSTALLED) {
            Log.d(TAG, "Version request already in progress.")
            return
        }
        _uiState.update { it.copy(wearUpdateCheckState = WearUpdateCheckState.REQUESTING_INSTALLED, installedWearVersionName = null) }

        viewModelScope.launch {
            try {
                val connectedNodes: List<Node> = nodeClient.connectedNodes.await()
                Log.d(TAG, "Found ${connectedNodes.size} connected nodes.")
                if (connectedNodes.isNotEmpty()) {
                    _uiState.update { it.copy(wearNodeConnected = true) }
                    requestInstalledWearVersionInternal(connectedNodes.first())
                } else {
                    Log.w(TAG, "No connected Wear nodes found.")
                    setWearUpdateState(WearUpdateCheckState.IDLE)
                    _uiState.update { it.copy(wearNodeConnected = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get connected nodes", e)
                if (e !is CancellationException) {
                    setWearUpdateState(WearUpdateCheckState.ERROR)
                    _uiState.update { it.copy(wearNodeConnected = false) }
                }
            }
        }
    }

    private fun requestInstalledWearVersionInternal(node: Node) {
        Log.d(TAG, "Requesting installed version from node: ${node.displayName} (${node.id})")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Attempting to send /request_wear_version to ${node.id}")
                messageClient.sendMessage(node.id, requestVersionPath, null).await()
                Log.i(TAG, "Successfully sent /request_wear_version to ${node.id}")

                withContext(Dispatchers.Main) {
                    delay(5000)
                    if (uiState.value.wearUpdateCheckState == WearUpdateCheckState.REQUESTING_INSTALLED) {
                        Log.w(TAG, "Request for installed Wear version timed out after 15s")
                        setWearUpdateState(WearUpdateCheckState.ERROR)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send /request_wear_version to ${node.id}", e)
                if (e !is CancellationException) {
                    withContext(Dispatchers.Main) {
                        setWearUpdateState(WearUpdateCheckState.ERROR)
                    }
                }
            }
        }
    }


    private fun loadInitialPermissions() {
        val context = getApplication<Application>()
        val hasStorage = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val canInstall = context.packageManager.canRequestPackageInstalls()

        _uiState.update {
            it.copy(
                hasStoragePermission = hasStorage,
                canInstallUnknownApps = canInstall,
                canRequestPackageInstalls = canInstall
            )
        }
    }

    fun refreshPermissions() {
        loadInitialPermissions()
    }

    fun checkUpdate(showToast: Boolean = true) {
        if (_uiState.value.updateState == UpdateState.CHECKING) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(updateState = UpdateState.CHECKING, wearUpdateCheckState = WearUpdateCheckState.CHECKING_GITHUB, updateError = null) }

            var latestPhoneVersionFromRelease: String? = null
            var latestWearVersionFromRelease: String? = null
            var phoneApkDownloadUrl: String? = null
            var releaseBody: String? = null
            var errorMsg: String? = null

            if (!isNetworkAvailable()) {
                errorMsg = getString(R.string.no_internet_connection_detected)
            } else {
                try {
                    val apiUrl = URL("https://api.github.com/repos/ost-sys/ost-program-android/releases")
                    val connection = apiUrl.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connection.setRequestProperty("User-Agent", "OST-Tools-App")
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()

                        val releases = JSONArray(response.toString())
                        if (releases.length() > 0) {
                            val latestRelease = releases.getJSONObject(0)
                            releaseBody = latestRelease.getString("body")

                            releaseBody?.lines()?.forEach { bodyLine ->
                                val trimmedLine = bodyLine.trim()
                                if (trimmedLine.startsWith("**Latest Phone Version:**")) {
                                    latestPhoneVersionFromRelease = trimmedLine.substringAfter(":**").trim()
                                } else if (trimmedLine.startsWith("**Latest Wear OS Version:**")) {
                                    latestWearVersionFromRelease = trimmedLine.substringAfter(":**").trim()
                                }
                            }

                            val assets = latestRelease.getJSONArray("assets")
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                if (asset.getString("name") == phoneReleaseAssetName) {
                                    phoneApkDownloadUrl = asset.getString("browser_download_url")
                                    break
                                }
                            }

                            if (latestPhoneVersionFromRelease == null || latestWearVersionFromRelease == null) {
                                errorMsg = getString(R.string.error_parsing_release_notes)
                                Log.e(TAG, "Could not parse versions from release body: $releaseBody")
                            } else if (phoneApkDownloadUrl == null) {
                                errorMsg = getString(R.string.error_finding_apk)
                                Log.e(TAG, "Could not find asset '$phoneReleaseAssetName' in release")
                            }

                        } else {
                            errorMsg = getString(R.string.no_releases_found)
                        }
                    } else {
                        errorMsg = "${getString(R.string.error_getting_version_information)} (${connection.responseCode})"
                        try {
                            val errorStream = connection.errorStream
                            if (errorStream != null) {
                                val errorReader = BufferedReader(InputStreamReader(errorStream))
                                val errorResponse = StringBuilder()
                                var errorLine: String?
                                while (errorReader.readLine().also { errorLine = it } != null) {
                                    errorResponse.append(errorLine)
                                }
                                errorReader.close()
                                Log.e(TAG, "GitHub API Error Response: $errorResponse")
                            }
                        } catch (e: Exception) { /* Ignore */ }
                    }
                    connection.disconnect()

                } catch (e: IOException) {
                    Log.e(TAG, "Network error checking update", e)
                    errorMsg = getString(R.string.no_internet_connection_detected)
                } catch (e: JSONException) {
                    Log.e(TAG, "JSON parsing error checking update", e)
                    errorMsg = getString(R.string.data_analysis_error)
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error checking update", e)
                    errorMsg = getString(R.string.error)
                }
            }

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(latestWearVersionName = latestWearVersionFromRelease) }

                if (errorMsg != null) {
                    _uiState.update { it.copy(
                        updateState = UpdateState.ERROR,
                        updateError = errorMsg,
                        latestPhoneVersionName = null,
                        apkUrl = null,
                        changelog = null,
                        showUpdateAvailableHint = false
                    )}
                    if (showToast) {
                        _action.send(AboutAction.ShowToast(errorMsg))
                    }
                } else if (latestPhoneVersionFromRelease != null && phoneApkDownloadUrl != null && releaseBody != null) {
                    val currentVersion = _uiState.value.currentVersionName
                    val updateAvailable = compareVersions(latestPhoneVersionFromRelease, currentVersion) > 0

                    _uiState.update {
                        it.copy(
                            updateState = if (updateAvailable) UpdateState.AVAILABLE else UpdateState.NOT_AVAILABLE,
                            latestPhoneVersionName = latestPhoneVersionFromRelease,
                            apkUrl = phoneApkDownloadUrl,
                            changelog = releaseBody,
                            showUpdateAvailableHint = updateAvailable
                        )
                    }
                    updateWearUpdateStatusIfNeeded()

                    if (showToast) {
                        if (updateAvailable) {
                            _action.send(AboutAction.ShowToastRes(R.string.update_available))
                        } else {
                            _action.send(AboutAction.ShowToastRes(R.string.latest_version_installed))
                        }
                    }
                } else {
                    val fallbackError = getString(R.string.error)
                    _uiState.update { it.copy(
                        updateState = UpdateState.ERROR,
                        updateError = fallbackError,
                        latestPhoneVersionName = null,
                        apkUrl = null,
                        changelog = null,
                        showUpdateAvailableHint = false
                    )}
                    if (showToast) {
                        _action.send(AboutAction.ShowToast(fallbackError))
                    }
                }
            }
        }
    }

    private fun compareVersions(v1: String?, v2: String?): Int {
        if (v1 == null || v2 == null) return -1
        return try {
            val ver1 = v1.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
            val ver2 = v2.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(ver1.size, ver2.size)
            for (i in 0 until maxLen) {
                val p1 = ver1.getOrElse(i) { 0 }
                val p2 = ver2.getOrElse(i) { 0 }
                if (p1 != p2) {
                    return p1.compareTo(p2)
                }
            }
            0
        } catch (_: Exception) {
            v1.compareTo(v2)
        }
    }

    private fun updateWearUpdateStatusIfNeeded() {
        val currentState = _uiState.value
        val latestVersion = currentState.latestWearVersionName
        val installedVersion = currentState.installedWearVersionName

        if (latestVersion != null && installedVersion != null && currentState.wearUpdateCheckState != WearUpdateCheckState.ERROR) {
            val wearUpdateAvailable = compareVersions(latestVersion, installedVersion) > 0
            val newState = if (wearUpdateAvailable) WearUpdateCheckState.AVAILABLE else WearUpdateCheckState.UP_TO_DATE
            Log.d(TAG, "Updating Wear status: installed=$installedVersion, latest=$latestVersion, state=$newState")
            setWearUpdateState(newState)
        } else if (currentState.wearUpdateCheckState != WearUpdateCheckState.REQUESTING_INSTALLED && currentState.wearUpdateCheckState != WearUpdateCheckState.ERROR && currentState.wearUpdateCheckState != WearUpdateCheckState.IDLE) {
            Log.d(TAG, "Still waiting for installed Wear version or connection...")
        }
    }

    private fun setWearUpdateState(newState: WearUpdateCheckState) {
        Log.d(TAG, "Setting Wear update state to: $newState")
        _uiState.update { it.copy(wearUpdateCheckState = newState) }
    }

    fun showUpdateConfirmation() {
        if (_uiState.value.updateState == UpdateState.AVAILABLE) {
            _uiState.update { it.copy(showUpdateConfirmDialog = true) }
        } else if (_uiState.value.updateState == UpdateState.NOT_AVAILABLE){
            viewModelScope.launch { _action.send(AboutAction.ShowToastRes(R.string.latest_version_installed)) }
        } else {
            viewModelScope.launch { _action.send(AboutAction.ShowToastRes(R.string.update_check_failed_or_in_progress)) }
        }
    }

    fun dismissUpdateConfirmation() {
        _uiState.update { it.copy(showUpdateConfirmDialog = false) }
    }

    fun startDownload() {
        dismissUpdateConfirmation()
        val url = _uiState.value.apkUrl ?: return
        val context = getApplication<Application>()

        val hasStoragePerm = _uiState.value.hasStoragePermission

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasStoragePerm) {
            viewModelScope.launch { _action.send(AboutAction.RequestStoragePermissionLegacy) }
            return
        }

        try {
            val fileName = phoneReleaseAssetName
            val destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                File(it, fileName)
            } ?: run {
                Log.w(TAG, "External files dir is null, falling back to public Downloads")
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            }
            if (destination.exists()) {
                destination.delete()
            }

            val request = DownloadManager.Request(url.toUri())
                .setTitle("${getString(R.string.app_name)} ${getString(R.string.update)}")
                .setDescription(getString(R.string.downloading_update))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destination))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            currentDownloadId = downloadManager.enqueue(request)
            _uiState.update { it.copy(showDownloadDialog = true, downloadStatus = DownloadStatus.DOWNLOADING, downloadProgress = 0) }
            monitorDownloadProgress(currentDownloadId!!)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting download", e)
            _uiState.update { it.copy(downloadStatus = DownloadStatus.FAILED, showDownloadDialog = false) }
            viewModelScope.launch { _action.send(AboutAction.ShowToast(getString(R.string.fail) + ": " + e.localizedMessage)) }
        }
    }

    private fun monitorDownloadProgress(downloadId: Long) {
        downloadProgressJob?.cancel()
        downloadProgressJob = viewModelScope.launch(Dispatchers.IO) {
            var isDownloading = true
            while (isActive && isDownloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                var cursor: Cursor? = null
                try {
                    cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        @SuppressLint("Range") val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        @SuppressLint("Range") val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                        @SuppressLint("Range") val totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        @SuppressLint("Range") val downloadedBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        @SuppressLint("Range") val localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))

                        withContext(Dispatchers.Main) {
                            when (status) {
                                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> {
                                    val progress = if (totalBytes > 0) {
                                        ((downloadedBytes * 100) / totalBytes).toInt()
                                    } else { 0 }
                                    _uiState.update { it.copy(downloadProgress = progress, downloadStatus = DownloadStatus.DOWNLOADING) }
                                }
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    _uiState.update { it.copy(downloadProgress = 100, downloadStatus = DownloadStatus.COMPLETE, showDownloadDialog = false) }
                                    isDownloading = false
                                    installApk(localUri)
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    Log.e(TAG, "Download failed. Reason: $reason")
                                    _uiState.update { it.copy(downloadStatus = DownloadStatus.FAILED, showDownloadDialog = false) }
                                    _action.send(AboutAction.ShowToast("${getString(R.string.fail)} (${getDownloadErrorReason(reason)})"))
                                    isDownloading = false
                                }
                                else -> {
                                    _uiState.update { it.copy(downloadStatus = DownloadStatus.DOWNLOADING, downloadProgress = 0) }
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "Download cursor is null or empty for ID: $downloadId")
                        isDownloading = false
                        withContext(Dispatchers.Main) {
                            if (_uiState.value.downloadStatus == DownloadStatus.DOWNLOADING) {
                                _uiState.update { it.copy(downloadStatus = DownloadStatus.FAILED, showDownloadDialog = false) }
                                _action.send(AboutAction.ShowToastRes(R.string.fail))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error querying download status", e)
                    isDownloading = false
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(downloadStatus = DownloadStatus.FAILED, showDownloadDialog = false) }
                        _action.send(AboutAction.ShowToastRes(R.string.error))
                    }
                } finally {
                    cursor?.close()
                }
                if (isDownloading) delay(500)
            }
        }
    }

    private fun installApk(localUriString: String?) {
        if (localUriString == null) {
            viewModelScope.launch { _action.send(AboutAction.ShowToastRes(R.string.update_file_not_found)) }
            return
        }

        val context = getApplication<Application>()
        val fileUri = localUriString.toUri()
        val installIntent = Intent(Intent.ACTION_VIEW)

        val apkUri: Uri =
            try {
                val file = File(fileUri.path!!)
                FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting FileProvider URI", e)
                viewModelScope.launch { _action.send(AboutAction.ShowToast(getString(R.string.error))) }
                return
            }

        installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (!context.packageManager.canRequestPackageInstalls()) {
            viewModelScope.launch { _action.send(AboutAction.RequestInstallPermission) }
            viewModelScope.launch { _action.send(AboutAction.ShowToastRes(R.string.install_unknown_apps_permission_q)) }
            return
        }

        viewModelScope.launch {
            try {
                _action.send(AboutAction.LaunchIntent(installIntent))
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "No activity found to handle install intent", e)
                _action.send(AboutAction.ShowToastRes(R.string.no_suitable_activity_found))
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception launching install intent", e)
                _action.send(AboutAction.ShowToastRes(R.string.error))
            }
        }
    }

    fun cancelDownload() {
        currentDownloadId?.let {
            downloadManager.remove(it)
            currentDownloadId = null
        }
        downloadProgressJob?.cancel()
        _uiState.update { it.copy(showDownloadDialog = false, downloadStatus = DownloadStatus.IDLE, downloadProgress = 0) }
        viewModelScope.launch { _action.send(AboutAction.ShowToastRes(R.string.download_canceled)) }
    }

    fun onAppIconClick() {
        val randomIndex = Random().nextInt(randomMessages.size)
        val message = randomMessages[randomIndex]
        viewModelScope.launch {
            _action.send(AboutAction.ShowToast(message))
        }
    }

    fun onSocialClick(url: String) {
        launchUrl(url)
    }

    fun onAppInfoClick() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", getApplication<Application>().packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        viewModelScope.launch { _action.send(AboutAction.LaunchIntent(intent)) }
    }

    fun onChangelogClick() {
        if (!_uiState.value.changelog.isNullOrEmpty()) {
            _uiState.update { it.copy(showChangelogDialog = true) }
        } else {
            checkUpdate(showToast = true)
        }
    }

    fun dismissChangelogDialog() {
        _uiState.update { it.copy(showChangelogDialog = false) }
    }

    private fun launchUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        viewModelScope.launch {
            try {
                _action.send(AboutAction.LaunchIntent(intent))
            } catch (_: ActivityNotFoundException) {
                _action.send(AboutAction.ShowToastRes(R.string.no_suitable_activity_found))
            }
        }
    }

    private fun getString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getDownloadErrorReason(reasonCode: Int): String {
        return when (reasonCode) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot Resume"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Device Not Found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File Already Exists"
            DownloadManager.ERROR_FILE_ERROR -> "File Error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP Data Error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient Space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too Many Redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP Code"
            DownloadManager.ERROR_UNKNOWN -> "Unknown Error"
            else -> "Unknown Error ($reasonCode)"
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Message received from ${messageEvent.sourceNodeId}: ${messageEvent.path}")
        when (messageEvent.path) {
            versionResponsePath -> {
                val installedVersion = String(messageEvent.data, StandardCharsets.UTF_8)
                Log.d(TAG, "Received installed wear version: $installedVersion")
                _uiState.update { it.copy(installedWearVersionName = installedVersion) }
                updateWearUpdateStatusIfNeeded()
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.w(TAG, ">>> onCapabilityChanged CALLED for capability: ${capabilityInfo.name}")
        Log.w(TAG, "Capability changed: ${capabilityInfo.name}, Nodes: ${capabilityInfo.nodes.joinToString { it.displayName }} Count: ${capabilityInfo.nodes.size}")
        if (capabilityInfo.name == wearAppCapability) {
            viewModelScope.launch {
                val nodes = nodeClient.connectedNodes.await()
                val isConnected = nodes.isNotEmpty()
                if (uiState.value.wearNodeConnected != isConnected) {
                    Log.d(TAG, "Wear node connection status changed via capability: $isConnected")
                    _uiState.update { it.copy(wearNodeConnected = isConnected) }
                    if (isConnected) {
                        checkWearConnectionAndRequestVersion()
                    } else {
                        setWearUpdateState(WearUpdateCheckState.IDLE)
                        _uiState.update { it.copy(installedWearVersionName = null) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageClient.removeListener(this)
        capabilityClient.removeListener(this)
        downloadProgressJob?.cancel()
    }
}