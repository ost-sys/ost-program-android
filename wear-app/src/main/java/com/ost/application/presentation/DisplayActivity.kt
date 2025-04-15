package com.ost.application.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.ost.application.R
import com.ost.application.presentation.tools.PixelTestActivity
import com.ost.application.util.ConfimationDialog
import com.ost.application.util.InfoListScreenContent
import com.ost.application.util.ListItem

private const val WRITE_SETTINGS_REQUEST_CODE = 123

class DisplayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DisplayScreen()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WRITE_SETTINGS_REQUEST_CODE) {
            if (Settings.System.canWrite(this)) {
                Log.i("DisplayActivity", "WRITE_SETTINGS permission granted after return.")
                startPixelTestActivity(this)
            } else {
                Log.w("DisplayActivity", "WRITE_SETTINGS permission still denied after return.")
            }
        }
    }
}

@Composable
fun DisplayScreen() {
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    val writeSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.System.canWrite(context)) {
            Log.i("DisplayScreen", "WRITE_SETTINGS granted via launcher result.")
            startPixelTestActivity(context)
        } else {
            Log.w("DisplayScreen", "WRITE_SETTINGS denied via launcher result.")
            showPermissionDeniedDialog = true
        }
    }

    val displayData = rememberDisplayData(context)

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        InfoListScreenContent(
            listState = listState,
            screenTitle = displayData.screenResolution,
            items = displayData.listItems {
                checkAndRequestWriteSettings(context, writeSettingsLauncher) { granted ->
                    if (granted) {
                        startPixelTestActivity(context)
                    } else {
                        showPermissionDeniedDialog = true
                    }
                }
            }
        )
    }

    if (showPermissionDeniedDialog) {
        ConfimationDialog(
            message = "stringResource(R.string.write_settings_permission_required)",
            iconResId = R.drawable.ic_error_24dp,
            onDismiss = { showPermissionDeniedDialog = false }
        )
    }
}

private data class DisplayInfoData(
    val screenResolution: String,
    val listItems: (() -> Unit) -> List<ListItem>
)

@SuppressLint("DefaultLocale")
@Composable
private fun rememberDisplayData(context: Context): DisplayInfoData {
    return remember {
        val displayMetrics = context.resources.displayMetrics
        val densityDpi = displayMetrics.densityDpi
        val xdpi = displayMetrics.xdpi.takeIf { it > 0 } ?: densityDpi.toFloat()
        val ydpi = displayMetrics.ydpi.takeIf { it > 0 } ?: densityDpi.toFloat()
        val widthPixels = displayMetrics.widthPixels
        val heightPixels = displayMetrics.heightPixels

        val refreshRate = try {
            context.display.refreshRate
        } catch (e: Exception) { 0f }

        val xInches = widthPixels / xdpi
        val yInches = heightPixels / ydpi
        val screenInches = kotlin.math.sqrt((xInches * xInches + yInches * yInches).toDouble())
        val screenResolution = "${widthPixels}x${heightPixels}"

        DisplayInfoData(
            screenResolution = screenResolution,
            listItems = { onPixelTestClick ->
                listOf(
                    ListItem(context.getString(R.string.refresh_rate), String.format("%.0f ${context.getString(R.string.hz)}", refreshRate), null, true, null),
                    ListItem("DPI", "$densityDpi dpi", null, true, null),
                    ListItem(context.getString(R.string.screen_diagonal), String.format("%.1f ${context.getString(R.string.inches)}", screenInches), null, true, null),
                    ListItem(context.getString(R.string.check_for_dead_pixels), null, null, true, onPixelTestClick)
                )
            }
        )
    }
}

private fun checkAndRequestWriteSettings(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onResult: (granted: Boolean) -> Unit
) {
    if (Settings.System.canWrite(context)) {
        Log.d("WriteSettingsCheck", "Permission already granted.")
        onResult(true)
    } else {
        Log.d("WriteSettingsCheck", "Permission not granted. Launching settings screen.")
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        }
        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            Log.e("WriteSettingsCheck", "Failed to launch ACTION_MANAGE_WRITE_SETTINGS", e)
            onResult(false)
        }
    }
}

private fun startPixelTestActivity(context: Context) {
    try {
        val intent = Intent(context, PixelTestActivity::class.java).apply {
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("PixelTestLaunch", "Failed to start PixelTestActivity", e)
    }
}