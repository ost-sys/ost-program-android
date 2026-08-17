package com.ost.application.ui.screen.batteryinfo
import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
enum class SamplingContext(val intervalMs: Long) {
    SCREEN_ACTIVE(1_000L),
    APP_FOREGROUND(30_000L),
    LOCKED(60_000L)
}
class BatterySamplingController(private val application: Application) {
    private val isScreenVisible = MutableStateFlow(false)
    fun setScreenVisible(visible: Boolean) {
        isScreenVisible.value = visible
    }
    private val isAppForeground: Flow<Boolean> = callbackFlow {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> trySend(true)
                Lifecycle.Event.ON_STOP -> trySend(false)
                else -> {}
            }
        }
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        lifecycle.addObserver(observer)
        trySend(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        awaitClose { lifecycle.removeObserver(observer) }
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private val isLocked: Flow<Boolean> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> trySend(true)
                    Intent.ACTION_SCREEN_ON -> trySend(true)
                    Intent.ACTION_USER_PRESENT -> trySend(false)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        application.registerReceiver(receiver, filter)
        val powerManager = application.getSystemService(Context.POWER_SERVICE) as? PowerManager
        trySend(powerManager?.isInteractive != true)
        awaitClose { application.unregisterReceiver(receiver) }
    }
    val samplingContext: Flow<SamplingContext> = combine(
        isScreenVisible,
        isAppForeground,
        isLocked
    ) { screenVisible, appForeground, locked ->
        when {
            locked -> SamplingContext.LOCKED
            screenVisible && appForeground -> SamplingContext.SCREEN_ACTIVE
            appForeground -> SamplingContext.APP_FOREGROUND
            else -> SamplingContext.LOCKED
        }
    }.distinctUntilChanged()
}
