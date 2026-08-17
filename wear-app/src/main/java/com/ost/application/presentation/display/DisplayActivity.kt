package com.ost.application.presentation.display
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.presentation.tools.BurnInRecoveryActivity
import com.ost.application.presentation.tools.PixelTestActivity
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.InfoListScreenContent
import com.ost.application.util.ListItem
import android.content.Context
import android.content.Intent
import android.util.Log
class DisplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                DisplayScreen()
            }
        }
    }
}
@Composable
fun DisplayScreen(viewModel: WearDisplayInfoViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.startUpdates(context.applicationContext)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val listState = rememberScalingLazyListState()
    AppScaffold(timeText = { TimeText() }) {
        ScreenScaffold(
            scrollState = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            InfoListScreenContent(
                listState = listState,
                screenTitle = uiState.resolution,
                items = buildDisplayItems(
                    context = context,
                    uiState = uiState,
                    onPixelTestClick = { startPixelTestActivity(context) },
                    onFixDeadPixelsClick = { startBurnInRecoveryActivity(context) }
                ),
                icon = R.drawable.ic_display_settings_24dp
            )
        }
    }
}
private fun buildDisplayItems(
    context: Context,
    uiState: WearDisplayInfoUiState,
    onPixelTestClick: () -> Unit,
    onFixDeadPixelsClick: () -> Unit
): List<ListItem> = listOf(
    ListItem(context.getString(R.string.refresh_rate), uiState.refreshRate, null, true, CardPosition.TOP, null),
    ListItem("DPI", uiState.dpi, null, true, CardPosition.MIDDLE, null),
    ListItem(context.getString(R.string.screen_diagonal), uiState.diagonal, null, true, CardPosition.MIDDLE, null),
    ListItem(context.getString(R.string.orientation), uiState.orientation, null, true, CardPosition.MIDDLE, null),
    ListItem(context.getString(R.string.stylus_support), uiState.stylusSupport, null, true, CardPosition.BOTTOM, null),
    ListItem(context.getString(R.string.check_for_dead_pixels), null, null, true, CardPosition.SINGLE, onPixelTestClick),
    ListItem(context.getString(R.string.fix_dead_pixels), null, null, true, CardPosition.SINGLE, onFixDeadPixelsClick)
)
private fun startPixelTestActivity(context: Context) {
    try {
        context.startActivity(Intent(context, PixelTestActivity::class.java))
    } catch (e: Exception) {
        Log.e("PixelTestLaunch", "Failed to start PixelTestActivity", e)
    }
}
private fun startBurnInRecoveryActivity(context: Context) {
    try {
        context.startActivity(Intent(context, BurnInRecoveryActivity::class.java))
    } catch (e: Exception) {
        Log.e("BurnInRecoveryLaunch", "Failed to start BurnInRecoveryActivity", e)
    }
}
@Preview(device = "id:wearos_small_round")
@Composable
fun DisplayPreview() {
    DisplayScreen()
}
