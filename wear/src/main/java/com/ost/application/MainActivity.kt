package com.ost.application

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.tooling.preview.devices.WearDevices
import com.ost.application.components.ConfirmationActivity
import com.ost.application.presentation.BatteryActivity
import com.ost.application.presentation.CPUActivity
import com.ost.application.presentation.DefaultActivity
import com.ost.application.presentation.DisplayActivity
import com.ost.application.util.CardItem
import com.ost.application.util.GitHubRelease
import com.ost.application.util.ListItem
import com.ost.application.util.ListItems
import com.ost.application.util.RetrofitClient
import com.ost.application.util.startActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        MainList(listState)
    }
}

@Composable
fun MainList(listState: ScalingLazyListState) {
    val context = LocalContext.current
    var latestRelease by remember { mutableStateOf<GitHubRelease?>(null) }

    val aboutWatch = stringResource(R.string.about_the_watch)
    val cpu = stringResource(R.string.cpu)
    val battery = stringResource(R.string.battery)
    val display = stringResource(R.string.display)

    val items = remember {
        listOf(
            ListItem(aboutWatch, null, R.drawable.ic_watch_24dp) {
                startActivity(
                    context,
                    DefaultActivity::class.java
                )
            },
            ListItem(cpu, null, R.drawable.ic_cpu_24dp) {
                startActivity(
                    context,
                    CPUActivity::class.java
                )
            },
            ListItem(battery, null, R.drawable.ic_battery_24dp) {
                startActivity(
                    context,
                    BatteryActivity::class.java
                )
            },
            ListItem(display, null, R.drawable.ic_display_settings_24dp) {
                startActivity(
                    context,
                    DisplayActivity::class.java
                )
            },
        )
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp, 12.dp),
        state = listState,
    ) {
        item {
            ListItems(stringResource(R.string.app_name), null)
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
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VersionInfo("${stringResource(R.string.version)} ${BuildConfig.VERSION_NAME}")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        checkForUpdates(context,
                            onUpdateAvailable = { release ->
                                val message = "${context.getString(R.string.new_update_available)}: ${release.tag_name}"
                                val intent = ConfirmationActivity.newIntent(context, message, R.drawable.ic_update_24dp)
                                context.startActivity(intent)

                            },
                            onLatestVersion = {
                                val message = context.getString(R.string.you_are_updated)
                                val intent = ConfirmationActivity.newIntent(context, message, R.drawable.ic_update_good_24dp)
                                context.startActivity(intent)
                            },
                            onError = { errorMessage ->
                                val intent = ConfirmationActivity.newIntent(context, errorMessage, R.drawable.ic_error_24dp)
                                context.startActivity(intent)
                            }
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_update_24dp),
                        contentDescription = "Check for updates",
                        modifier = Modifier
                            .size(ButtonDefaults.DefaultIconSize)
                            .wrapContentSize(align = Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
fun VersionInfo(version: String) {
    Text(
        text = version,
        style = MaterialTheme.typography.body2,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxSize()
    )
}

fun checkForUpdates(
    context: Context,
    onUpdateAvailable: (GitHubRelease) -> Unit,
    onLatestVersion: () -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.instance.getReleases().execute()
            if (response.isSuccessful) {
                val releases = response.body()
                if (!releases.isNullOrEmpty()) {
                    val latestRelease = releases[0]
                    val latestVersion = latestRelease.tag_name

                    val currentVersion = BuildConfig.VERSION_NAME

                    if (latestVersion != currentVersion) {
                        CoroutineScope(Dispatchers.Main).launch {
                            onUpdateAvailable(latestRelease)
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            onLatestVersion()
                        }
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    onError(context.getString(R.string.update_check_error))
                }
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                onError("${context.getString(R.string.error)}: ${e.message}")
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}