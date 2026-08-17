package com.ost.application.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ProgressIndicatorDefaults
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.core.share.DiscoveredDevice
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardListItem
import com.ost.application.util.CardPosition
import com.ost.application.util.FailDialog
import com.ost.application.util.SuccessDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class LocalSendActivity : ComponentActivity() {
    private val viewModel: LocalSendViewModel by viewModels()
    private var urisToSend: ArrayList<Uri>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            OSTToolsTheme {
                LocalSendApp(
                    viewModel = viewModel,
                    isSendMode = !urisToSend.isNullOrEmpty(),
                    onDeviceSelected = { device ->
                        urisToSend?.let { viewModel.sendFiles(device, it) }
                    },
                    openFile = { file -> openFile(this, file) }
                )
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "com.ost.application.action.LOCALSEND_SEND_FILES") {
            urisToSend = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else
                @Suppress("DEPRECATION") intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)

            if (!urisToSend.isNullOrEmpty()) {
                viewModel.startDiscovery()
            } else {
                finish()
            }
        }
    }

    private fun openFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mime = context.contentResolver.getType(uri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

@Composable
fun LocalSendApp(
    viewModel: LocalSendViewModel,
    isSendMode: Boolean,
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    openFile: (File) -> Unit
) {
    val isReceiveActive    by viewModel.isReceiveActive.collectAsState()
    val isDiscovering      by viewModel.isDiscovering.collectAsState()
    val isSendingActive    by viewModel.isSendingActive.collectAsState()
    val discoveredDevices  by viewModel.discoveredDevices.collectAsState()
    val statusText         by viewModel.statusText.collectAsState()
    val transferProgress   by viewModel.transferProgress.collectAsState()
    val transferTotalFiles by viewModel.transferTotalFiles.collectAsState()
    val lastReceivedFiles  by viewModel.lastReceivedFiles.collectAsState()
    val incomingRequest    by viewModel.incomingRequest.collectAsState()

    val isTransferring = transferProgress != null
    val showSendUI = isSendMode || isDiscovering || isSendingActive

    OSTToolsTheme {
        AppScaffold(timeText = { TimeText() }) {
            if (showSendUI) {

                ScreenScaffold {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LsSendUI(
                            viewModel = viewModel,
                            discoveredDevices = discoveredDevices,
                            isDiscovering = isDiscovering,
                            isSending = isSendingActive,
                            statusText = statusText,
                            onDeviceSelected = onDeviceSelected,
                            onScanClicked = { viewModel.startDiscovery() },
                            onCancelClicked = {
                                viewModel.stopDiscovery()
                                viewModel.cancelTransfer()
                            }
                        )
                        if (isDiscovering || isTransferring) {
                            LsCircularProgress(
                                isIndeterminate = isDiscovering && !isTransferring,
                                progress = if (isDiscovering && !isTransferring) null else transferProgress,
                                totalFiles = transferTotalFiles ?: 1
                            )
                        }
                    }
                }
            } else {

                val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
                Box(Modifier.fillMaxSize()) {
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        if (page == 0) {
                            ScreenScaffold {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    LsReceiveUI(
                                        isReceiveActive = isReceiveActive,
                                        statusText = statusText,
                                        isReceivingTransfer = isTransferring,
                                        onToggle = { viewModel.setReceiveActive(it) },
                                        onCancel = { viewModel.cancelTransfer() }
                                    )
                                    if (isTransferring) {
                                        LsCircularProgress(
                                            isIndeterminate = false,
                                            progress = transferProgress,
                                            totalFiles = transferTotalFiles ?: 1
                                        )
                                    }
                                }
                            }
                        } else {
                            ScreenScaffold {
                                LsReceivedFilesUI(files = lastReceivedFiles, openFile = openFile)
                            }
                        }
                    }
                    androidx.wear.compose.material.HorizontalPageIndicator(
                        pageIndicatorState = object : androidx.wear.compose.material.PageIndicatorState {
                            override val pageCount    get() = 2
                            override val pageOffset   get() = pagerState.currentPageOffsetFraction
                            override val selectedPage get() = pagerState.currentPage
                        }
                    )
                }
            }

            incomingRequest?.let { request ->
                AlertDialog(
                    visible = true,
                    onDismissRequest = { viewModel.rejectIncomingTransfer() },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_download_24dp),
                            contentDescription = null
                        )
                    },
                    title = { Text(stringResource(R.string.receive_files_question), textAlign = TextAlign.Center) },
                    text = {
                        Text(
                            text = stringResource(
                                R.string.localsend_incoming_files_desc,
                                request.info.alias,
                                request.files.size
                            ),
                            textAlign = TextAlign.Center
                        )
                    },
                    dismissButton = {
                        Button(
                            colors = ButtonDefaults.filledTonalButtonColors(),
                            onClick = { viewModel.rejectIncomingTransfer() }
                        ) { Icon(painterResource(R.drawable.ic_cancel_24dp), null) }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.acceptIncomingTransfer() }) {
                            Icon(painterResource(R.drawable.ic_check_circle_24dp), null)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LsSendUI(
    viewModel: LocalSendViewModel,
    discoveredDevices: List<DiscoveredDevice>,
    isDiscovering: Boolean,
    isSending: Boolean,
    statusText: String,
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    onScanClicked: () -> Unit,
    onCancelClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    LaunchedEffect(discoveredDevices.isNotEmpty(), isDiscovering, isSending) {
        if (discoveredDevices.isNotEmpty() && !isDiscovering && !isSending) {
            delay(150)
            runCatching { focusRequester.requestFocus() }
        }
    }

    AppScaffold(timeText = { null }) {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                EdgeButton(onClick = {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    onCancelClicked()
                }) {
                    Icon(
                        painterResource(if (isSending || isDiscovering) R.drawable.ic_cancel_24dp else R.drawable.ic_close_24dp),
                        contentDescription = null
                    )
                }
            }
        ) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .onRotaryScrollEvent {
                        coroutineScope.launch { listState.scrollBy(it.verticalScrollPixels) }
                        true
                    }
                    .focusable(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 8.dp),
                anchorType = ScalingLazyListAnchorType.ItemCenter
            ) {
                item { Text(stringResource(R.string.localsend), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium) }
                item {
                    Text(
                        statusText,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                item { Spacer(Modifier.height(4.dp)) }

                if (discoveredDevices.isNotEmpty() && !isSending) {
                    items(items = discoveredDevices, key = { it.id }) { device ->
                        LsDeviceChip(device = device, onClick = {
                            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                            if (device.isResolved) onDeviceSelected(device)
                            else if (!device.isResolving) device.serviceInfo?.let { viewModel.resolveDevice(it) }
                        })
                    }
                } else if (!isDiscovering && !isSending) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_search_off_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.no_localsend_devices_found), textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                                onScanClicked()
                            }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painterResource(R.drawable.ic_refresh_24dp), contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.scan_again))
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun LsDeviceChip(device: DiscoveredDevice, onClick: () -> Unit) {
    val iconRes = when (device.type) {
        "mobile"   -> R.drawable.ic_smartphone_24dp
        "desktop",
        "laptop",
        "web",
        "headless",
        "server"   -> R.drawable.ic_computer_24dp
        else       -> R.drawable.ic_computer_24dp
    }
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = device.isResolved || !device.isResolving,
        label = { Text(device.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = {
            Text(
                when {
                    device.isResolved  -> device.host ?: stringResource(R.string.status_resolved)
                    device.isResolving -> stringResource(R.string.status_resolving)
                    else               -> stringResource(R.string.tap_to_resolve)
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
                Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize))
            }
        },
        colors = ChipDefaults.primaryChipColors(
            contentColor = if (device.isResolved)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    )
}

@Composable
fun LsReceiveUI(
    isReceiveActive: Boolean,
    statusText: String,
    isReceivingTransfer: Boolean,
    onToggle: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onRotaryScrollEvent {
                coroutineScope.launch { listState.scrollBy(it.verticalScrollPixels) }
                true
            }
            .focusable(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        anchorType = ScalingLazyListAnchorType.ItemCenter
    ) {
        item { Text(stringResource(R.string.localsend), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium) }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth(),
                checked = isReceiveActive,
                onCheckedChange = {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    onToggle(it)
                },
                enabled = !isReceivingTransfer,
                label = { Text(stringResource(R.string.discoverable)) }
            )
        }
        item {
            Text(
                statusText,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isReceivingTransfer) {
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    onClick = {
                        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        onCancel()
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_cancel_24dp), null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
fun LsReceivedFilesUI(files: List<File>, openFile: (File) -> Unit) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val today = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
        }.timeInMillis
    }
    val todayStr = stringResource(R.string.today)
    val olderStr = stringResource(R.string.older)
    val groups = remember(files, todayStr, olderStr) {
        files.groupBy { if (it.lastModified() >= today) todayStr else olderStr }
    }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onRotaryScrollEvent {
                coroutineScope.launch { listState.scrollBy(it.verticalScrollPixels) }
                true
            }
            .focusable(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        anchorType = ScalingLazyListAnchorType.ItemCenter
    ) {
        item {
            Text(
                stringResource(R.string.share_received_files),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (files.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_received_files),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            groups.forEach { (label, groupFiles) ->
                item {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                itemsIndexed(groupFiles, key = { _, f -> f.absolutePath }) { index, file ->
                    val pos = when {
                        groupFiles.size == 1 -> CardPosition.SINGLE
                        index == 0           -> CardPosition.TOP
                        index == groupFiles.size - 1 -> CardPosition.BOTTOM
                        else                 -> CardPosition.MIDDLE
                    }
                    CardListItem(
                        onClick = { openFile(file) },
                        title = file.name,
                        icon = getFileIcon(file.extension),
                        summary = android.text.format.Formatter.formatFileSize(context, file.length()),
                        position = pos,
                        status = true
                    )
                }
            }
        }
    }
}

@Composable
fun LsCircularProgress(isIndeterminate: Boolean, progress: Int?, totalFiles: Int) {
    val targetProgress = if (isIndeterminate) 0f else (progress ?: 0) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "LsProgress"
    )
    if (isIndeterminate) {
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize().clearAndSetSemantics {},
            startAngle = 296.8f,
            endAngle = 243.2f,
            progress = 0f,
            indicatorColor = MaterialTheme.colorScheme.primary,
            strokeWidth = ProgressIndicatorDefaults.FullScreenStrokeWidth,
            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
        )
    } else {
        androidx.wear.compose.material3.SegmentedCircularProgressIndicator(
            segmentCount = totalFiles.coerceAtLeast(1),
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize().clearAndSetSemantics {},
            startAngle = 296.8f,
            endAngle = 243.2f,
            colors = androidx.wear.compose.material3.ProgressIndicatorDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
            ),
            strokeWidth = ProgressIndicatorDefaults.FullScreenStrokeWidth
        )
    }
}
