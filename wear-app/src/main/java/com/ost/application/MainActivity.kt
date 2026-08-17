package com.ost.application
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
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
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.ost.application.appmanager.AppManagerActivity
import com.ost.application.explorer.FileExplorerActivity
import com.ost.application.presentation.battery.BatteryActivity
import com.ost.application.presentation.DefaultActivity
import com.ost.application.presentation.display.DisplayActivity
import com.ost.application.presentation.SettingsActivity
import com.ost.application.share.ShareActivity
import com.ost.application.share.LocalSendActivity
import com.ost.application.theme.OSTToolsTheme
import androidx.wear.compose.material3.ListHeader
import com.ost.application.util.CardListItem
import com.ost.application.util.CardPosition
import com.ost.application.util.ListItem
import com.ost.application.util.WavyDivider
import com.ost.application.util.startActivity
private const val TAG = "WearMainActivity"
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
                MainApp()
            }
        }
    }
}
@Composable
fun MainApp() {
    val context = LocalContext.current
    var showSharePicker by remember { mutableStateOf(false) }

    val mainListItems = rememberMainListItems(context) { showSharePicker = true }
    AppScaffold(timeText = { TimeText() }) {
        val listState = rememberScalingLazyListState()
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                EdgeButton(onClick = { startActivity(context, SettingsActivity::class.java) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_24dp),
                        contentDescription = "Settings",
                        modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                    )
                }
            }
        ) {
            MainList(listState = listState, items = mainListItems)
        }

        if (showSharePicker) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showSharePicker = false }) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Select Method", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showSharePicker = false
                            startActivity(context, ShareActivity::class.java)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OST Share")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showSharePicker = false
                            startActivity(context, LocalSendActivity::class.java)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LocalSend")
                    }
                }
            }
        }
    }
}
@Composable
fun rememberMainListItems(context: Context, onShareClick: () -> Unit): List<ListItem> {
    return remember {
        listOf(
            ListItem(context.getString(R.string.about_the_watch), null, R.drawable.ic_watch_24dp, true, CardPosition.TOP) {
                startActivity(context, DefaultActivity::class.java)
            },
            ListItem(context.getString(R.string.battery), null, R.drawable.ic_battery_24dp, true, CardPosition.MIDDLE) {
                startActivity(context, BatteryActivity::class.java)
            },
            ListItem(context.getString(R.string.display), null, R.drawable.ic_display_settings_24dp, true, CardPosition.BOTTOM) {
                startActivity(context, DisplayActivity::class.java)
            },
            ListItem(context.getString(R.string.power_menu), null, com.ost.application.core.R.drawable.ic_power_new_24dp, true, CardPosition.TOP) {
                startActivity(context, com.ost.application.presentation.powermenu.PowerMenuActivity::class.java)
            },
            ListItem(context.getString(R.string.file_explorer), null, R.drawable.ic_folder_24dp, true, CardPosition.MIDDLE) {
                startActivity(context, FileExplorerActivity::class.java)
            },
            ListItem(context.getString(R.string.share), null, R.drawable.ic_share_24dp, true, CardPosition.MIDDLE) {
                onShareClick()
            },
            ListItem(context.getString(R.string.ram_title), null, com.ost.application.core.R.drawable.ic_memory_alt_24dp, true, CardPosition.MIDDLE) {
                startActivity(context, com.ost.application.presentation.memory.RamActivity::class.java)
            },
            ListItem(context.getString(R.string.storage_title), null, com.ost.application.core.R.drawable.ic_storage_24dp, true, CardPosition.MIDDLE) {
                startActivity(context, com.ost.application.presentation.memory.StorageActivity::class.java)
            },
            ListItem("Apps", null, R.drawable.ic_apps_24dp, true, CardPosition.BOTTOM) {
                startActivity(context, AppManagerActivity::class.java)
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
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        anchorType = ScalingLazyListAnchorType.ItemCenter
    ) {
        item { ListHeader { Text(stringResource(R.string.app_name)) } }
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
        item { Spacer(modifier = Modifier.size(8.dp)) }
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WavyDivider()
            }
        }
        item { Spacer(modifier = Modifier.size(8.dp)) }
        item { VersionInfo(version = BuildConfig.VERSION_NAME) }
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
fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
    return try {
        fun parse(raw: String): Pair<List<Int>, String?> {
            val cleaned = raw.removePrefix("v")
            val dashIdx = cleaned.indexOf('-')
            val (numPart, pre) = if (dashIdx >= 0) {
                cleaned.substring(0, dashIdx) to cleaned.substring(dashIdx + 1)
            } else {
                cleaned to null
            }
            val nums = numPart.split('.').map { it.toIntOrNull() ?: 0 }
            return nums to pre
        }
        val (v1, pre1) = parse(latestVersion)
        val (v2, pre2) = parse(currentVersion)
        val maxLen = maxOf(v1.size, v2.size)
        for (i in 0 until maxLen) {
            val p1 = v1.getOrElse(i) { 0 }
            val p2 = v2.getOrElse(i) { 0 }
            if (p1 > p2) return true
            if (p1 < p2) return false
        }
        return when {
            pre1 == null && pre2 != null -> true
            pre1 != null && pre2 == null -> false
            pre1 != null && pre2 != null -> pre1 > pre2
            else -> false
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to compare versions: $latestVersion vs $currentVersion", e)
        false
    }
}
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        MainApp()
    }
}
