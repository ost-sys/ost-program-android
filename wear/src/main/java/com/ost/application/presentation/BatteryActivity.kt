package com.ost.application.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.ost.application.R
import com.ost.application.util.CardItem
import com.ost.application.util.ListItem
import com.ost.application.util.ListItems
import kotlinx.coroutines.launch

class BatteryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatteryScreen()
        }
    }
}

@Composable
fun BatteryScreen() {
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        BatteryList(listState = listState)
    }
}

@Composable
fun BatteryList(listState: ScalingLazyListState) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val loading = stringResource(R.string.loading)

    val batteryStatus = remember { mutableStateOf(loading) }
    val batteryHealth = remember { mutableStateOf(loading) }
    val batteryTemperature = remember { mutableStateOf(loading) }
    val batteryVoltage = remember { mutableStateOf(loading) }
    val batteryTechnology = remember { mutableStateOf(loading) }

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {

                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

                    val batteryPct = level / scale.toFloat()
                    val batteryLevel = (batteryPct * 100).toInt()
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                    val status = when (plugged) {
                        BatteryManager.BATTERY_PLUGGED_AC -> "${context.getString(R.string.charging_ac)}: $batteryLevel%"
                        BatteryManager.BATTERY_PLUGGED_USB -> "${context.getString(R.string.charging_usb)}: $batteryLevel%"
                        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "${context.getString(R.string.charging_wireless)}: $batteryLevel%"
                        else -> "${context.getString(R.string.battery_level)}: $batteryLevel%"
                    }
                    batteryStatus.value = status

                    val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                    batteryHealth.value = when (health) {
                        BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.good)
                        BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.overheat)
                        BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.dead)
                        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.over_voltage)
                        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> context.getString(R.string.unspecified_failure)
                        BatteryManager.BATTERY_HEALTH_COLD -> context.getString(R.string.cold)
                        else -> context.getString(R.string.unknown)
                    }

                    val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                    batteryTemperature.value = "$temperature °C"

                    val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                    batteryVoltage.value = "$voltage ${context.getString(R.string.mv)}"

                    val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: context.getString(R.string.unknown)
                    batteryTechnology.value = technology
                }
            }
        }
    }

    val contextLocal = LocalContext.current
    val filter = remember { IntentFilter(Intent.ACTION_BATTERY_CHANGED) }
    DisposableEffect(contextLocal) {
        contextLocal.registerReceiver(receiver, filter)
        onDispose {
            contextLocal.unregisterReceiver(receiver)
        }
    }

    val health = stringResource(R.string.health)
    val temperature = stringResource(R.string.temperature)
    val voltage = stringResource(R.string.voltage)
    val technology = stringResource(R.string.technology)

    val items = remember(batteryHealth.value, batteryTemperature.value, batteryVoltage.value, batteryTechnology.value) {
        listOf(
            ListItem(health, batteryHealth.value, null) { Log.i("CLICK", "CLICK") },
            ListItem(temperature, batteryTemperature.value, null) { Log.i("CLICK", "CLICK") },
            ListItem(voltage, batteryVoltage.value, null) { Log.i("CLICK", "CLICK") },
            ListItem(technology, batteryTechnology.value, null) { Log.i("CLICK", "CLICK") }
        )
    }

    val sensitivity = 5.0f

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val verticalScroll = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (verticalScroll != 0f) {
                                coroutineScope.launch {
                                    listState.scrollBy(verticalScroll * sensitivity)
                                }
                            }
                        }
                    }
                }
            },
        contentPadding = PaddingValues(2.dp),
        anchorType = ScalingLazyListAnchorType.ItemCenter,
        state = listState
    ) {
        item {
            ListItems(batteryStatus.value, null)
        }

        items(items.size) { index ->
            val item = items[index]
            CardItem(
                title = item.title,
                summary = item.summary,
                icon = item.icon,
                onClick = item.onClick
            )
        }
    }
}