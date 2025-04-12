package com.ost.application.presentation

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.ost.application.R
import com.ost.application.util.InfoListScreenContent
import com.ost.application.util.ListItem

class DefaultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DefaultScreen()
            }
        }
    }
}

@Composable
fun DefaultScreen() {
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current

    val items = remember {
        listOfNotNull( // Используем listOfNotNull на случай если какая-то информация отсутствует
            ListItem(context.getString(R.string.system_version), Build.VERSION.RELEASE ?: "N/A", null, true, null),
            ListItem(context.getString(R.string.brand), Build.BRAND ?: "N/A", null, true, null),
            ListItem(context.getString(R.string.board), Build.BOARD ?: "N/A", null, true, null),
            getBuildNumber()?.let { ListItem(context.getString(R.string.build_number), it, null, true, null) },
            ListItem("SDK", Build.VERSION.SDK_INT.toString(), null, true, null),
            ListItem(context.getString(R.string.build_fingerprint), Build.FINGERPRINT ?: "N/A", null, true, null)
        )
    }

    val screenTitle = remember { getDeviceModel() ?: "Unknown Device" }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        InfoListScreenContent(
            listState = listState,
            screenTitle = screenTitle,
            items = items
        )
    }
}

@SuppressLint("PrivateApi")
private fun getSystemProperty(key: String?): String? {
    if (key.isNullOrBlank()) return null
    return try {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
            .invoke(null, key) as? String
    } catch (_: Exception) {
        null
    }
}

private fun getBuildNumber(): String? {
    return getSystemProperty("ro.system.build.id")
        ?: getSystemProperty("ro.build.display.id")
        ?: Build.DISPLAY
}

private fun getDeviceModel(): String? {
    return getSystemProperty("ro.product.system.model")
        ?: getSystemProperty("ro.product.model")
        ?: Build.MODEL
}