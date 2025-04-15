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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.ost.application.R
import com.ost.application.util.InfoListScreenContent
import com.ost.application.util.ListItem

class BatteryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BatteryScreen()
            }
        }
    }
}

@Composable
fun BatteryScreen() {
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current
    val loading = stringResource(R.string.loading)

    var batteryStatusTitle by remember { mutableStateOf<String?>(loading) }
    var batteryHealth by remember { mutableStateOf(loading) }
    var batteryTemperature by remember { mutableStateOf(loading) }
    var batteryVoltage by remember { mutableStateOf(loading) }
    var batteryTechnology by remember { mutableStateOf(loading) }

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val batteryPct = if (scale > 0) level / scale.toFloat() else 0f
                    val batteryLevel = (batteryPct * 100).toInt()
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

                    batteryStatusTitle = when (plugged) {
                        BatteryManager.BATTERY_PLUGGED_AC -> "${context.getString(R.string.charging_ac)}: $batteryLevel%"
                        BatteryManager.BATTERY_PLUGGED_USB -> "${context.getString(R.string.charging_usb)}: $batteryLevel%"
                        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "${context.getString(R.string.charging_wireless)}: $batteryLevel%"
                        else -> "${context.getString(R.string.battery_level)}: $batteryLevel%"
                    }

                    batteryHealth = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
                        BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.good)
                        BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.overheat)
                        BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.dead)
                        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.over_voltage)
                        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> context.getString(R.string.unspecified_failure)
                        BatteryManager.BATTERY_HEALTH_COLD -> context.getString(R.string.cold)
                        else -> context.getString(R.string.unknown)
                    }

                    val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                    batteryTemperature = "$temp °C"

                    val volt = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                    batteryVoltage = "$volt ${context.getString(R.string.mv)}"

                    batteryTechnology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: context.getString(R.string.unknown)
                }
            }
        }
    }

    DisposableEffect(context) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        Log.d("BatteryActivity", "BroadcastReceiver registered")
        onDispose {
            context.unregisterReceiver(receiver)
            Log.d("BatteryActivity", "BroadcastReceiver unregistered")
        }
    }

    val items = remember(batteryHealth, batteryTemperature, batteryVoltage, batteryTechnology) {
        listOf(
            ListItem(context.getString(R.string.health), batteryHealth, null, true, null),
            ListItem(context.getString(R.string.temperature), batteryTemperature, null, true, null),
            ListItem(context.getString(R.string.voltage), batteryVoltage, null, true, null),
            ListItem(context.getString(R.string.technology), batteryTechnology, null, true, null)
        )
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        InfoListScreenContent(
            listState = listState,
            screenTitle = batteryStatusTitle,
            items = items
        )
    }
}