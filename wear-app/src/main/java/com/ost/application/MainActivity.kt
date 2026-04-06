package com.ost.application

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.ost.application.explorer.FileExplorerActivity
import com.ost.application.presentation.BatteryActivity
import com.ost.application.presentation.DefaultActivity
import com.ost.application.presentation.DisplayActivity
import com.ost.application.share.ShareActivity
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardListItem
import com.ost.application.util.CardPosition
import com.ost.application.util.FailDialog
import com.ost.application.util.ListItem
import com.ost.application.util.ListItems
import com.ost.application.util.RetrofitClient
import com.ost.application.util.WavyDivider
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
            OSTToolsTheme {
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
    var dialogInfo by remember { mutableStateOf<DialogInfo?>(null) }
    var isLoadingUpdate by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mainListItems = rememberMainListItems(context)

    val onCheckUpdatesClick: () -> Unit = {
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

    AppScaffold(
        timeText = { TimeText() },
    ) {
        val listState = rememberScalingLazyListState()
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                EdgeButton(
                    onClick = onCheckUpdatesClick,
                    enabled = !isLoadingUpdate,
                ) {
                    if (isLoadingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
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
        ) {
            MainList(
                listState = listState,
                items = mainListItems,
            )
        }
    }

    dialogInfo?.let { info ->
        FailDialog(
            showDialog = true,
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
            ListItem(
                context.getString(R.string.about_the_watch),
                null,
                R.drawable.ic_watch_24dp,
                true,
                CardPosition.TOP
            ) {
                startActivity(context, DefaultActivity::class.java)
            },
            ListItem(
                context.getString(R.string.battery),
                null,
                R.drawable.ic_battery_24dp,
                true,
                CardPosition.MIDDLE
            ) {
                startActivity(context, BatteryActivity::class.java)
            },
            ListItem(
                context.getString(R.string.display),
                null,
                R.drawable.ic_display_settings_24dp,
                true,
                CardPosition.BOTTOM
            ) {
                startActivity(context, DisplayActivity::class.java)
            },
            ListItem(
                context.getString(R.string.file_explorer),
                null,
                R.drawable.ic_folder_24dp,
                true,
                CardPosition.TOP
            ) {
                startActivity(context, FileExplorerActivity::class.java)
            },
            ListItem(
                context.getString(R.string.share),
                null,
                R.drawable.ic_share_24dp,
                true,
                CardPosition.MIDDLE
            ) {
                startActivity(context, ShareActivity::class.java)
            },
            ListItem(
                "Apps",
                null,
                R.drawable.ic_apps_24dp,
                true,
                CardPosition.BOTTOM
            ) {
                Toast.makeText(context, "In progress", Toast.LENGTH_SHORT).show()
            },
        )
    }
}


@Composable
fun MainList(
    listState: ScalingLazyListState,
    items: List<ListItem>,
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
                null
            )
        }
        items(items.size, key = { index -> items[index].title }) { index ->
            val item = items[index]
            CardListItem(
                title = item.title,
                summary = item.summary,
                icon = item.icon,
                status = item.status,
                position = item.position,
                onClick = item.onClick
            )
        }
        item {
            Spacer(modifier = Modifier.size(8.dp))
        }
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WavyDivider()
            }
        }
        item {
            Spacer(modifier = Modifier.size(8.dp))
        }
        item {
            VersionInfo(version = BuildConfig.VERSION_NAME)
        }
    }
}

@Composable
fun VersionInfo(version: String) {
    Text(
        text = version,
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
                } catch (_: Exception) {  }
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