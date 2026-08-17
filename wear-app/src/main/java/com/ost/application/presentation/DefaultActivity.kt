package com.ost.application.presentation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.core.device.DeviceInfoViewModel
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
fun DefaultScreen(
    viewModel: DeviceInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val items = listOfNotNull(
        ListItem(stringResource(R.string.system_version), uiState.androidVersion, null, true, CardPosition.TOP, null),
        ListItem(stringResource(R.string.brand), uiState.brand, null, true, CardPosition.MIDDLE, null),
        ListItem(stringResource(R.string.board), uiState.board, null, true, CardPosition.MIDDLE, null),
        ListItem(stringResource(R.string.codename), uiState.codename, null, true, CardPosition.MIDDLE, null),
        ListItem(stringResource(R.string.build_number), uiState.buildNumber, null, true, CardPosition.MIDDLE, null),
        ListItem(stringResource(R.string.sdk), uiState.sdkVersion, null, true, CardPosition.MIDDLE, null),
        ListItem(stringResource(R.string.build_fingerprint), uiState.buildFingerprint, null, true, CardPosition.MIDDLE, null),
        ListItem(stringResource(R.string.security_patch), uiState.securityPatch, null, true, CardPosition.MIDDLE, null),
        ListItem(stringResource(R.string.kernel_version), uiState.kernelVersion, null, true, CardPosition.MIDDLE, null),
        ListItem(stringResource(R.string.bootloader), uiState.bootloader, null, true, CardPosition.MIDDLE, null),
        ListItem(stringResource(R.string.baseband_version), uiState.radioVersion, null, true, CardPosition.MIDDLE, null),
        ListItem(stringResource(R.string.partitions), uiState.partitionStyle, null, true, CardPosition.MIDDLE, null),
        ListItem(
            stringResource(R.string.project_treble),
            stringResource(if (uiState.isTrebleSupported) R.string.support else R.string.unsupported),
            null, true, CardPosition.MIDDLE, null
        ),
        ListItem(stringResource(R.string.uptime), uiState.uptime, null, true, CardPosition.BOTTOM, null)
    )
    AppScaffold(timeText = { TimeText() }) {
        val listState = rememberScalingLazyListState()
        ScreenScaffold(
            scrollState = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            InfoListScreenContent(
                listState = listState,
                screenTitle = uiState.deviceName,
                items = items,
                icon = R.drawable.ic_watch_24dp
            )
        }
    }
}
