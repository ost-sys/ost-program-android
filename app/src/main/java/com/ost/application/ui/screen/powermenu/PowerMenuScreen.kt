@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.ost.application.ui.screen.powermenu

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.R
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.utils.CustomCardItem

@Composable
fun PowerMenuScreen(
    modifier: Modifier = Modifier,
    viewModel: PowerMenuViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LocalContext.current

    val statusColor = when (uiState.rootState) {
        RootAccessState.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
        RootAccessState.GRANTED -> MaterialTheme.colorScheme.primary
        RootAccessState.DENIED -> MaterialTheme.colorScheme.error
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_power_new_24dp),
                contentDescription = stringResource(R.string.power_menu),
                modifier = Modifier.size(80.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(statusColor)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = uiState.statusTextResId),
                    color = statusColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { viewModel.checkRootAccess() }, modifier = Modifier.size(36.dp).padding(start = 8.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh_24dp),
                        contentDescription = stringResource(R.string.refresh),
                        tint = statusColor
                    )
                }
            }

        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
        ) {
            CustomCardItem(
                icon = R.drawable.ic_power_new_24dp,
                title = stringResource(R.string.turn_off),
                summary = stringResource(R.string.turn_off_s),
                status = uiState.isPowerOffEnabled,
                onClick = if (uiState.isPowerOffEnabled) {
                    { viewModel.onPowerActionClick(PowerAction.POWER_OFF) }
                } else null,
                iconPainter = null
            )
            CustomCardItem(
                icon = R.drawable.ic_restart_24dp,
                title = stringResource(R.string.reboot),
                summary = stringResource(R.string.reboot_system_s),
                status = uiState.isRebootEnabled,
                onClick = if (uiState.isRebootEnabled) { { viewModel.onPowerActionClick(PowerAction.REBOOT) } } else null,
                iconPainter = null
            )
            CustomCardItem(
                icon = R.drawable.ic_flash_on_24dp,
                title = stringResource(R.string.reboot_recovery),
                summary = stringResource(R.string.reboot_recovery_s),
                status = uiState.isRecoveryEnabled,
                onClick = if (uiState.isRecoveryEnabled) { { viewModel.onPowerActionClick(PowerAction.RECOVERY) } } else null,
                iconPainter = null
            )
            CustomCardItem(
                icon = R.drawable.ic_download_for_offline_24dp,
                title = stringResource(R.string.reboot_download),
                summary = stringResource(R.string.reboot_download_s),
                status = uiState.isDownloadModeEnabled,
                onClick = if (uiState.isDownloadModeEnabled) { { viewModel.onPowerActionClick(PowerAction.DOWNLOAD) } } else null,
                iconPainter = null
            )
            CustomCardItem(
                icon = R.drawable.ic_offline_bolt_24dp,
                title = stringResource(R.string.reboot_fastboot),
                summary = stringResource(R.string.reboot_fastboot_s),
                status = uiState.isFastbootEnabled,
                onClick = if (uiState.isFastbootEnabled) { { viewModel.onPowerActionClick(PowerAction.FASTBOOT) } } else null,
                iconPainter = null
            )
            CustomCardItem(
                icon = R.drawable.ic_offline_bolt_24dp,
                title = stringResource(R.string.reboot_fastbootd),
                summary = stringResource(R.string.reboot_fastbootd_s),
                status = uiState.isFastbootdEnabled,
                onClick = if (uiState.isFastbootdEnabled) { { viewModel.onPowerActionClick(PowerAction.FASTBOOTD) } } else null,
                iconPainter = null
            )
        }
    }

    if (uiState.showDialogFor != null) {
        val action = uiState.showDialogFor!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text(stringResource(R.string.attention)) },
            text = { Text(stringResource(id = action.messageResId)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.executeCommand(action)
                        viewModel.dismissDialog()
                    }
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "Power Menu Screen Preview")
@Composable
fun PowerMenuScreenPreview() {
    OSTToolsTheme {
        PowerMenuScreen()
    }
}

@Preview(showBackground = true, name = "Confirmation Dialog Preview")
@Composable
fun ConfirmationDialogPreview() {
    OSTToolsTheme {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Внимание!") },
            text = { Text("Перезагрузить систему?") },
            confirmButton = { TextButton(onClick = { }) { Text("Да") } },
            dismissButton = { TextButton(onClick = { }) { Text("Нет") } }
        )
    }
}