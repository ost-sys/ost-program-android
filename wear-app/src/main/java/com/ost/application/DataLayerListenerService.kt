package com.ost.application

import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.nio.charset.StandardCharsets

class DataLayerListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val messageClient by lazy { Wearable.getMessageClient(this) }

    private val requestVersionPath = "/request_wear_version"
    private val versionResponsePath = "/wear_version_response"

    private val TAG = "DataLayerListenerService"

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG,"onMessageReceived: ${messageEvent.path}")
        if (messageEvent.path == requestVersionPath) {
            scope.launch {
                try {
                    val packageName = applicationContext.packageName
                    val packageInfo = applicationContext.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
                    val version = packageInfo.versionName?.toByteArray(StandardCharsets.UTF_8)
                    messageClient.sendMessage(messageEvent.sourceNodeId, versionResponsePath, version).await()
                    Log.d(TAG, "Version response sent to ${messageEvent.sourceNodeId}")
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.e(TAG, "Failed to get package info for $packageName", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send version response to ${messageEvent.sourceNodeId}", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}