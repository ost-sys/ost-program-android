package com.ost.application.core.settings.sync
import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ost.application.core.settings.TemperatureUnit
import com.ost.application.core.settings.TimingSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await
object SettingsSyncPaths {
    const val TIMING_SETTINGS_PATH = "/timing_settings"
    const val LANGUAGE_PATH = "/language_settings"
    const val TEMPERATURE_UNIT_PATH = "/temperature_unit_settings"
    const val GITHUB_TOKEN_PRESENCE_PATH = "/github_token_presence"
    const val KEY_TOTAL_DURATION = "total_duration"
    const val KEY_NOISE_DURATION = "noise_duration"
    const val KEY_BW_NOISE_DURATION = "bw_noise_duration"
    const val KEY_HORIZONTAL_DURATION = "horizontal_duration"
    const val KEY_VERTICAL_DURATION = "vertical_duration"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_LANGUAGE_TAG = "language_tag"
    const val KEY_TEMPERATURE_UNIT = "temperature_unit"
    const val KEY_GITHUB_TOKEN_PRESENT = "github_token_present"
    const val MESSAGE_OPEN_SETTINGS_PATH = "/open_settings"
    const val STARGAZERS_REQUEST_PATH = "/stargazers_request"
    const val STARGAZERS_RESPONSE_PATH = "/stargazers_response"
}
class SettingsSyncClient(private val context: Context) {
    private val dataClient: DataClient? by lazy {
        runCatching { Wearable.getDataClient(context) }.getOrNull()
    }
    private val nodeClient: NodeClient? by lazy {
        runCatching { Wearable.getNodeClient(context) }.getOrNull()
    }
    private val messageClient: MessageClient? by lazy {
        runCatching { Wearable.getMessageClient(context) }.getOrNull()
    }
    suspend fun pushTimingSettings(settings: TimingSettings) {
        val client = dataClient ?: return
        runCatching {
            val request = PutDataMapRequest.create(SettingsSyncPaths.TIMING_SETTINGS_PATH).apply {
                dataMap.putInt(SettingsSyncPaths.KEY_TOTAL_DURATION, settings.totalDuration)
                dataMap.putInt(SettingsSyncPaths.KEY_NOISE_DURATION, settings.noiseDuration)
                dataMap.putInt(SettingsSyncPaths.KEY_BW_NOISE_DURATION, settings.blackWhiteNoiseDuration)
                dataMap.putInt(SettingsSyncPaths.KEY_HORIZONTAL_DURATION, settings.horizontalDuration)
                dataMap.putInt(SettingsSyncPaths.KEY_VERTICAL_DURATION, settings.verticalDuration)
                dataMap.putLong(SettingsSyncPaths.KEY_TIMESTAMP, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            client.putDataItem(request).await()
        }
    }
    suspend fun pushLanguageTag(languageTag: String?) {
        val client = dataClient ?: return
        runCatching {
            val request = PutDataMapRequest.create(SettingsSyncPaths.LANGUAGE_PATH).apply {
                dataMap.putString(SettingsSyncPaths.KEY_LANGUAGE_TAG, languageTag ?: "")
                dataMap.putLong(SettingsSyncPaths.KEY_TIMESTAMP, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            client.putDataItem(request).await()
        }
    }
    suspend fun pushTemperatureUnit(unit: TemperatureUnit) {
        val client = dataClient ?: return
        runCatching {
            val request = PutDataMapRequest.create(SettingsSyncPaths.TEMPERATURE_UNIT_PATH).apply {
                dataMap.putString(SettingsSyncPaths.KEY_TEMPERATURE_UNIT, unit.name)
                dataMap.putLong(SettingsSyncPaths.KEY_TIMESTAMP, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            client.putDataItem(request).await()
        }
    }
    suspend fun pushGithubTokenPresence(isPresent: Boolean) {
        val client = dataClient ?: return
        runCatching {
            val request = PutDataMapRequest.create(SettingsSyncPaths.GITHUB_TOKEN_PRESENCE_PATH).apply {
                dataMap.putBoolean(SettingsSyncPaths.KEY_GITHUB_TOKEN_PRESENT, isPresent)
                dataMap.putLong(SettingsSyncPaths.KEY_TIMESTAMP, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            client.putDataItem(request).await()
        }
    }
    suspend fun getLastSyncedGithubTokenPresence(): Boolean {
        val client = dataClient ?: return false
        return runCatching {
            val items = client.dataItems.await()
            try {
                for (i in 0 until items.count) {
                    val item = items[i]
                    if (item.uri.path == SettingsSyncPaths.GITHUB_TOKEN_PRESENCE_PATH) {
                        return@runCatching DataMapItem.fromDataItem(item).dataMap
                            .getBoolean(SettingsSyncPaths.KEY_GITHUB_TOKEN_PRESENT)
                    }
                }
                false
            } finally {
                items.release()
            }
        }.getOrDefault(false)
    }
    suspend fun getLastSyncedTimingSettings(): TimingSettings? {
        val client = dataClient ?: return null
        return runCatching {
            val items = client.dataItems.await()
            try {
                for (i in 0 until items.count) {
                    val item = items[i]
                    if (item.uri.path == SettingsSyncPaths.TIMING_SETTINGS_PATH) {
                        return@runCatching DataMapItem.fromDataItem(item).dataMap.toTimingSettings()
                    }
                }
                null
            } finally {
                items.release()
            }
        }.getOrNull()
    }
    suspend fun getLastSyncedLanguageTag(): String? {
        val client = dataClient ?: return null
        return runCatching {
            val items = client.dataItems.await()
            try {
                for (i in 0 until items.count) {
                    val item = items[i]
                    if (item.uri.path == SettingsSyncPaths.LANGUAGE_PATH) {
                        return@runCatching DataMapItem.fromDataItem(item).dataMap.getString(SettingsSyncPaths.KEY_LANGUAGE_TAG)
                    }
                }
                null
            } finally {
                items.release()
            }
        }.getOrNull()
    }
    suspend fun getLastSyncedTemperatureUnit(): TemperatureUnit? {
        val client = dataClient ?: return null
        return runCatching {
            val items = client.dataItems.await()
            try {
                for (i in 0 until items.count) {
                    val item = items[i]
                    if (item.uri.path == SettingsSyncPaths.TEMPERATURE_UNIT_PATH) {
                        val key = DataMapItem.fromDataItem(item).dataMap.getString(SettingsSyncPaths.KEY_TEMPERATURE_UNIT)
                        return@runCatching TemperatureUnit.fromKey(key)
                    }
                }
                null
            } finally {
                items.release()
            }
        }.getOrNull()
    }
    fun observeTimingSettings(): Flow<TimingSettings> {
        val client = dataClient ?: return emptyFlow()
        return callbackFlow {
            val listener = DataClient.OnDataChangedListener { events: DataEventBuffer ->
                for (event in events) {
                    if (event.type == DataEvent.TYPE_CHANGED &&
                        event.dataItem.uri.path == SettingsSyncPaths.TIMING_SETTINGS_PATH
                    ) {
                        trySend(DataMapItem.fromDataItem(event.dataItem).dataMap.toTimingSettings())
                    }
                }
            }
            runCatching { client.addListener(listener) }
            awaitClose { runCatching { client.removeListener(listener) } }
        }
    }
    fun observeLanguageTag(): Flow<String?> {
        val client = dataClient ?: return emptyFlow()
        return callbackFlow {
            val listener = DataClient.OnDataChangedListener { events: DataEventBuffer ->
                for (event in events) {
                    if (event.type == DataEvent.TYPE_CHANGED &&
                        event.dataItem.uri.path == SettingsSyncPaths.LANGUAGE_PATH
                    ) {
                        trySend(DataMapItem.fromDataItem(event.dataItem).dataMap.getString(SettingsSyncPaths.KEY_LANGUAGE_TAG))
                    }
                }
            }
            runCatching { client.addListener(listener) }
            awaitClose { runCatching { client.removeListener(listener) } }
        }
    }
    fun observeTemperatureUnit(): Flow<TemperatureUnit> {
        val client = dataClient ?: return emptyFlow()
        return callbackFlow {
            val listener = DataClient.OnDataChangedListener { events: DataEventBuffer ->
                for (event in events) {
                    if (event.type == DataEvent.TYPE_CHANGED &&
                        event.dataItem.uri.path == SettingsSyncPaths.TEMPERATURE_UNIT_PATH
                    ) {
                        val key = DataMapItem.fromDataItem(event.dataItem).dataMap.getString(SettingsSyncPaths.KEY_TEMPERATURE_UNIT)
                        trySend(TemperatureUnit.fromKey(key))
                    }
                }
            }
            runCatching { client.addListener(listener) }
            awaitClose { runCatching { client.removeListener(listener) } }
        }
    }
    suspend fun isCounterpartNodeConnected(): Boolean {
        val client = nodeClient ?: return false
        return runCatching { client.connectedNodes.await().isNotEmpty() }.getOrDefault(false)
    }
    suspend fun requestOpenSettingsOnPhone(): Boolean {
        val nodes = nodeClient ?: return false
        val messages = messageClient ?: return false
        return runCatching {
            val connectedNodes = nodes.connectedNodes.await()
            if (connectedNodes.isEmpty()) return@runCatching false
            connectedNodes.forEach { node ->
                messages.sendMessage(node.id, SettingsSyncPaths.MESSAGE_OPEN_SETTINGS_PATH, ByteArray(0)).await()
            }
            true
        }.getOrDefault(false)
    }
    private fun DataMap.toTimingSettings() = TimingSettings(
        totalDuration = getInt(SettingsSyncPaths.KEY_TOTAL_DURATION, TimingSettings.Defaults.TOTAL_DURATION),
        noiseDuration = getInt(SettingsSyncPaths.KEY_NOISE_DURATION, TimingSettings.Defaults.NOISE_DURATION),
        blackWhiteNoiseDuration = getInt(SettingsSyncPaths.KEY_BW_NOISE_DURATION, TimingSettings.Defaults.BW_NOISE_DURATION),
        horizontalDuration = getInt(SettingsSyncPaths.KEY_HORIZONTAL_DURATION, TimingSettings.Defaults.HORIZONTAL_DURATION),
        verticalDuration = getInt(SettingsSyncPaths.KEY_VERTICAL_DURATION, TimingSettings.Defaults.VERTICAL_DURATION)
    )
}
sealed class WearSyncState {
    object Unavailable : WearSyncState()
    object Disabled : WearSyncState()
    object Enabled : WearSyncState()
}
