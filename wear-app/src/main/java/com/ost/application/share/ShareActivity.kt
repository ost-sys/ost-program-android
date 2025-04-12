package com.ost.application.share

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ProgressIndicatorDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import com.ost.application.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class ShareActivity : ComponentActivity() {

    private val viewModel: WearShareViewModel by viewModels()
    private var urisToShare: ArrayList<Uri>? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                NotificationHelper.createNotificationChannel(this)
            } else {
                println("WARN: Notification permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermissionIfNeeded()

        if (intent?.action == "com.ost.application.action.SEND_FILES") {
            urisToShare = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            Log.d(Constants.TAG, "ShareActivity started for SEND_FILES with ${urisToShare?.size ?: 0} URIs")
            if (!urisToShare.isNullOrEmpty()) {
                if (!viewModel.isDiscovering.value) {
                    viewModel.startDiscovery()
                }
            } else {
                Log.e(Constants.TAG, "SEND_FILES action received but no URIs found!")
                Toast.makeText(this, "Error: No files to send", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }


        setContent {
            ShareApp(
                viewModel = viewModel,
                isSendMode = !urisToShare.isNullOrEmpty(),
                onDeviceSelected = { device ->
                    urisToShare?.firstOrNull()?.let { uri ->
                        viewModel.sendFile(device, uri)
                    } ?: run {
                        Log.e(Constants.TAG, "No URI to send, although in send mode!")
                        Toast.makeText(this, "Error: File URI missing", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            if (viewModel.isDiscovering.value) {
                viewModel.stopDiscovery()
            }
        }
    }
}

@Composable
fun ShareApp(
    viewModel: WearShareViewModel,
    isSendMode: Boolean,
    onDeviceSelected: (DiscoveredDevice) -> Unit
) {
    val isServiceActive by viewModel.isServiceActive.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()
    val lastReceivedFile by viewModel.lastReceivedFile.collectAsState()
    val context = LocalContext.current

    val isTransferring = transferProgress != null
    val isToggleEnabled = !isDiscovering && !isTransferring && !isSendMode

    MaterialTheme {
        Scaffold(
            timeText = { TimeText() }
        ) {
            val isSending = isTransferring && (statusText.contains("Sending", ignoreCase = true) || statusText.contains("Connecting", ignoreCase = true) || statusText.contains("Sent:", ignoreCase = true))

            Box(modifier = Modifier.fillMaxSize()) {

                val showSendUI = isSendMode || isDiscovering || isSending

                if (showSendUI) {
                    SendUI(
                        viewModel = viewModel,
                        discoveredDevices = discoveredDevices,
                        isDiscovering = isDiscovering,
                        isSendingTransferActive = isSending,
                        statusText = statusText,
                        transferProgress = transferProgress,
                        onDeviceSelected = onDeviceSelected,
                        onScanClicked = { viewModel.startDiscovery() },
                        onCancelClicked = {
                            if (isDiscovering) viewModel.stopDiscovery()
                            (context as? ShareActivity)?.finish()
                        }
                    )
                } else {
                    ReceiveUI(
                        viewModel = viewModel,
                        isServiceActive = isServiceActive,
                        isToggleEnabled = isToggleEnabled,
                        statusText = statusText,
                        lastReceivedFile = lastReceivedFile,
                        context = context
                    )
                }

                if (isDiscovering || isTransferring) {
                    isTransferring && statusText.contains("Receiving", ignoreCase = true)
                    val isIndeterminate = isDiscovering && !isTransferring

                    TransferProgressOverlay(
                        isIndeterminate = isIndeterminate,
                        statusText = statusText,
                        progress = if (isIndeterminate) null else transferProgress
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun SendUI(
    viewModel: WearShareViewModel,
    discoveredDevices: List<DiscoveredDevice>,
    isDiscovering: Boolean,
    isSendingTransferActive: Boolean,
    statusText: String,
    transferProgress: Int?,
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    onScanClicked: () -> Unit,
    onCancelClicked: () -> Unit
) {
    val scalingListState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(discoveredDevices.isNotEmpty(), isDiscovering) {
        if (discoveredDevices.isNotEmpty() && !isDiscovering) {
            delay(100)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Log.w(Constants.TAG, "SendUI ScalingLazyColumn focus request failed", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.send_file),
            style = MaterialTheme.typography.title3,
            textAlign = TextAlign.Center
        )

        Text(
            text = statusText,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
            if (discoveredDevices.isNotEmpty()) {
                ScalingLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .onRotaryScrollEvent {
                            if (!isDiscovering && !isSendingTransferActive) {
                                coroutineScope.launch {
                                    scalingListState.scrollBy(it.verticalScrollPixels)
                                }
                                true
                            } else {
                                false
                            }
                        }
                        .focusable(),
                    state = scalingListState,
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        count = discoveredDevices.size,
                        key = { index -> discoveredDevices[index].serviceInfo.serviceName ?: index }
                    ) { index ->
                        val device = discoveredDevices[index]
                        DeviceChip(device = device, onClick = {
                            if (device.isResolved) {
                                onDeviceSelected(device)
                            } else if (!device.isResolving) {
                                viewModel.resolveDevice(device.serviceInfo)
                            }
                        })
                    }
                }
            } else if (!isDiscovering && !isSendingTransferActive) {
                Button(
                    onClick = onScanClicked,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_refresh_24dp), contentDescription = "Scan")
                        Spacer(Modifier.width(4.dp))
                        Text("Scan Again")
                    }
                }
            }
        }
        Button(
            onClick = onCancelClicked,
            colors = ButtonDefaults.secondaryButtonColors(),
            modifier = Modifier
                .padding(top = 4.dp)
                .size(24.dp)
        ) {
            Icon(painterResource(R.drawable.ic_cancel_24dp), contentDescription = "Cancel")
        }
    }
}


@Composable
fun DeviceChip(device: DiscoveredDevice, onClick: () -> Unit) {
    val iconRes = when (device.deviceType) {
        Constants.VALUE_DEVICE_PHONE -> R.drawable.ic_smartphone_24dp
        Constants.VALUE_DEVICE_WATCH -> R.drawable.ic_watch_24dp
        else -> R.drawable.ic_computer_24dp
    }
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = device.isResolved || !device.isResolving,
        label = { Text(device.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = {
            Text(
                text = when {
                    device.isResolved -> device.host ?: stringResource(R.string.resolved)
                    device.isResolving -> stringResource(R.string.resolving_i)
                    else -> stringResource(R.string.tap_to_resolve)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
        },
        icon = {
            if (device.isResolving) {
                CircularProgressIndicator(modifier = Modifier.size(ChipDefaults.IconSize), strokeWidth = 1.dp)
            } else {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = device.deviceType,
                    modifier = Modifier.size(ChipDefaults.IconSize)
                )
            }
        },
        colors = ChipDefaults.primaryChipColors()
    )
}


@SuppressLint("StringFormatMatches")
@Composable
fun ReceiveUI(
    viewModel: WearShareViewModel,
    isServiceActive: Boolean,
    isToggleEnabled: Boolean,
    statusText: String,
    lastReceivedFile: File?,
    context: android.content.Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.file_share),
            style = MaterialTheme.typography.title3
        )

        Spacer(modifier = Modifier.height(12.dp))

        ToggleChip(
            modifier = Modifier.fillMaxWidth(),
            checked = isServiceActive,
            onCheckedChange = { isActive ->
                viewModel.setServiceActive(isActive)
            },
            enabled = isToggleEnabled,
            label = { Text(stringResource(R.string.make_discoverable)) },
            toggleControl = {
                Switch(
                    checked = isServiceActive,
                    enabled = isToggleEnabled
                )
            },
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = statusText,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )

        lastReceivedFile?.let { file ->
            Spacer(modifier = Modifier.height(8.dp))
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val message = context.getString(
                        R.string.received_location_downloads,
                        file.name,
                        Constants.FILES_DIR
                    )
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    Log.i(Constants.TAG, "Chip clicked: ${file.absolutePath}")
                },
                label = {
                    Text(
                        text = file.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                icon = { Icon(painterResource(R.drawable.ic_check_circle_24dp), contentDescription = "Received file") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Composable
fun TransferProgressOverlay(
    isIndeterminate: Boolean,
    statusText: String,
    progress: Int?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (!isIndeterminate) MaterialTheme.colors.background.copy(alpha = 0.8f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (isIndeterminate) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 1.dp)
                    .clearAndSetSemantics {},
                progress = (progress ?: 0) / 100f,
                startAngle = 295.5f,
                endAngle = 245.5f,
                strokeWidth = ProgressIndicatorDefaults.FullScreenStrokeWidth,
                trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.1f)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 1.dp)
                    .clearAndSetSemantics {},
                progress = (progress ?: 0) / 100f,
                startAngle = 295.5f,
                endAngle = 245.5f,
                strokeWidth = ProgressIndicatorDefaults.FullScreenStrokeWidth,
                trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.1f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            if (!isIndeterminate) {
                Text(
                    text = "${progress ?: 0}%",
                    style = MaterialTheme.typography.display3,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}