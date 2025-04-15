package com.ost.application

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.ost.application.explorer.FileExplorerActivity
import com.ost.application.presentation.BatteryActivity
import com.ost.application.presentation.CPUActivity
import com.ost.application.presentation.DefaultActivity
import com.ost.application.presentation.DisplayActivity
import com.ost.application.share.ShareActivity
import com.ost.application.util.CardListItem
import com.ost.application.util.ConfimationDialog
import com.ost.application.util.ListItem
import com.ost.application.util.ListItems
import com.ost.application.util.RetrofitClient
import com.ost.application.util.startActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private const val TAG = "WearMainActivity"

data class DialogInfo(val message: String, val iconResId: Int)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val latestVersion: String) : UpdateCheckResult()
    object LatestVersion : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainApp(
                    checkUpdates = { context ->
                        checkForUpdates(context)
                    }
                )
            }
        }
    }
}

@Composable
fun MainApp(
    checkUpdates: suspend (Context) -> UpdateCheckResult
) {
    val listState = rememberScalingLazyListState()
    var dialogInfo by remember { mutableStateOf<DialogInfo?>(null) }
    var isLoadingUpdate by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mainListItems = rememberMainListItems(context)

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        MainList(
            listState = listState,
            items = mainListItems,
            isLoadingUpdate = isLoadingUpdate,
            onCheckUpdatesClick = {
                coroutineScope.launch {
                    isLoadingUpdate = true
                    val result = checkUpdates(context)
                    dialogInfo = when (result) {
                        is UpdateCheckResult.UpdateAvailable -> DialogInfo(
                            context.getString(R.string.update_available_check_phone),
                            R.drawable.ic_update_good_24dp
                        )
                        is UpdateCheckResult.LatestVersion -> DialogInfo(
                            context.getString(R.string.you_are_updated),
                            R.drawable.ic_update_good_24dp
                        )
                        is UpdateCheckResult.Error -> DialogInfo(
                            result.message,
                            R.drawable.ic_error_24dp
                        )
                    }
                    isLoadingUpdate = false
                }
            }
        )
    }

    dialogInfo?.let { info ->
        ConfimationDialog(
            message = info.message,
            iconResId = info.iconResId,
            onDismiss = { dialogInfo = null }
        )
    }
}

@Composable
fun rememberMainListItems(context: Context): List<ListItem> {
    return remember {
        listOf(
            ListItem(context.getString(R.string.about_the_watch), null, R.drawable.ic_watch_24dp, true) {
                startActivity(context, DefaultActivity::class.java)
            },
            ListItem(context.getString(R.string.cpu), null, R.drawable.ic_cpu_24dp, true) {
                startActivity(context, CPUActivity::class.java)
            },
            ListItem(context.getString(R.string.battery), null, R.drawable.ic_battery_24dp, true) {
                startActivity(context, BatteryActivity::class.java)
            },
            ListItem(context.getString(R.string.display), null, R.drawable.ic_display_settings_24dp, true) {
                startActivity(context, DisplayActivity::class.java)
            },
            ListItem(context.getString(R.string.file_explorer), null, R.drawable.ic_folder_24dp, true) {
                startActivity(context, FileExplorerActivity::class.java)
            },
            ListItem(context.getString(R.string.share), null, R.drawable.ic_share_24dp, true) {
                startActivity(context, ShareActivity::class.java)
            },
        )
    }
}


@Composable
fun MainList(
    listState: ScalingLazyListState,
    items: List<ListItem>,
    isLoadingUpdate: Boolean,
    onCheckUpdatesClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp)
) {
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        anchorType = ScalingLazyListAnchorType.ItemCenter
    ) {
        item {
            ListItems(
                stringResource(R.string.app_name),
            )
        }
        items(items.size, key = { index -> items[index].title }) { index ->
            val item = items[index]
            CardListItem(
                title = item.title,
                summary = item.summary,
                icon = item.icon,
                status = item.status,
                onClick = item.onClick
            )
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            UpdateSection(isLoadingUpdate = isLoadingUpdate, onCheckUpdatesClick = onCheckUpdatesClick)
        }
    }
}

@Composable
fun UpdateSection(isLoadingUpdate: Boolean, onCheckUpdatesClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        VersionInfo(version = "${stringResource(R.string.version)} ${BuildConfig.VERSION_NAME}")
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onCheckUpdatesClick,
            enabled = !isLoadingUpdate,
            modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
        ) {
            if (isLoadingUpdate) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    indicatorColor = MaterialTheme.colors.onPrimary,
                    trackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_update_24dp),
                    contentDescription = "Check updates",
                    modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                )
            }
        }
    }
}

@Composable
fun VersionInfo(version: String) {
    Text(
        text = version,
        style = MaterialTheme.typography.caption2,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

suspend fun checkForUpdates(context: Context): UpdateCheckResult {
    return withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.getReleases().execute()
            if (response.isSuccessful) {
                val releases = response.body()
                if (!releases.isNullOrEmpty()) {
                    val latestRelease = releases[0]
                    val releaseBody = latestRelease.body
                    var latestWearVersionFromRelease: String? = null

                    releaseBody.lines().forEach { bodyLine ->
                        val trimmedLine = bodyLine.trim()
                        if (trimmedLine.startsWith("**Latest Wear OS Version:**")) {
                            latestWearVersionFromRelease = trimmedLine.substringAfter(":**").trim()
                            return@forEach
                        }
                    }

                    if (latestWearVersionFromRelease != null) {
                        val currentVersion = BuildConfig.VERSION_NAME
                        if (isNewerVersion(latestWearVersionFromRelease, currentVersion)) {
                            UpdateCheckResult.UpdateAvailable(latestWearVersionFromRelease)
                        } else {
                            UpdateCheckResult.LatestVersion
                        }
                    } else {
                        Log.e(TAG, "Could not parse Wear OS version from release body: $releaseBody")
                        UpdateCheckResult.Error(context.getString(R.string.error_parsing_release_notes))
                    }
                } else {
                    UpdateCheckResult.Error(context.getString(R.string.no_releases_found))
                }
            } else {
                var errorDetails = ""
                try {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrBlank()) {
                        errorDetails = " Server response: $errorBody"
                    }
                } catch (_: Exception) { /* Ignore */ }
                Log.e(TAG, "GitHub API Error: ${response.code()} - ${response.message()}$errorDetails")
                UpdateCheckResult.Error("${context.getString(R.string.update_check_error)} (${response.code()})")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error checking update", e)
            UpdateCheckResult.Error(context.getString(R.string.network_error))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking update", e)
            UpdateCheckResult.Error("${context.getString(R.string.error)}: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}

fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
    return try {
        val ver1 = latestVersion.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        val ver2 = currentVersion.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(ver1.size, ver2.size)
        for (i in 0 until maxLen) {
            val p1 = ver1.getOrElse(i) { 0 }
            val p2 = ver2.getOrElse(i) { 0 }
            if (p1 > p2) return true
            if (p1 < p2) return false
        }
        false
    } catch (e: Exception) {
        Log.e(TAG, "Failed to compare versions: $latestVersion vs $currentVersion", e)
        false
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        MainApp(checkUpdates = { UpdateCheckResult.LatestVersion })
    }
}