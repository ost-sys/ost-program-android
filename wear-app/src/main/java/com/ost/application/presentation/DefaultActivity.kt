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
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.InfoListScreenContent
import com.ost.application.util.ListItem

class DefaultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                DefaultScreen()
            }
        }
    }
}

@Composable
fun DefaultScreen() {
    val context = LocalContext.current

    val items = remember {
        listOfNotNull(
            ListItem(
                context.getString(R.string.system_version),
                Build.VERSION.RELEASE ?: "N/A",
                null,
                true,
                CardPosition.TOP,
                null
            ),
            ListItem(
                context.getString(R.string.brand),
                Build.BRAND ?: "N/A",
                null,
                true,
                CardPosition.MIDDLE,
                null
            ),
            ListItem(
                context.getString(R.string.board),
                Build.BOARD ?: "N/A",null,
                true,
                CardPosition.MIDDLE,
                null
            ),
            getBuildNumber()?.let {
                ListItem(
                    context.getString(R.string.build_number),
                    it,
                    null,
                    true,
                    CardPosition.MIDDLE,
                    null
                )
            },
            ListItem(
                "SDK",
                Build.VERSION.SDK_INT.toString(),
                null,
                true,
                CardPosition.MIDDLE,
                null),
            ListItem(
                context.getString(R.string.build_fingerprint),
                Build.FINGERPRINT ?: "N/A",
                null,
                true,
                CardPosition.BOTTOM,
                null
            )
        )
    }

    val screenTitle = remember { getDeviceModel() ?: "Unknown Device" }

    AppScaffold(
        timeText = { TimeText() }
    ) {
        val listState = rememberScalingLazyListState()
        ScreenScaffold(
            scrollState = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            InfoListScreenContent(
                listState = listState,
                screenTitle = screenTitle,
                items = items,
                icon = R.drawable.ic_watch_24dp
            )
        }
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