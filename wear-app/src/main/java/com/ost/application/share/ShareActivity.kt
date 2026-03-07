package com.ost.application.share

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ProgressIndicatorDefaults
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import androidx.wear.compose.material.dialog.Confirmation
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.curvedText
import com.ost.application.R
import com.ost.application.explorer.ImageActivity
import com.ost.application.explorer.TextEditorActivity
import com.ost.application.explorer.VideoActivity
import com.ost.application.explorer.music.MusicActivity
import com.ost.application.share.NotificationHelper.formatFileSize
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
                Toast.makeText(this, R.string.warn_notification_permission, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermissionIfNeeded()
        handleIntent(intent)
        setContent {
            val isSendMode = !urisToShare.isNullOrEmpty()
            ShareApp(
                viewModel = viewModel,
                isSendMode = isSendMode,
                onDeviceSelected = { device ->
                    urisToShare?.let { uris ->
                        viewModel.sendFiles(device, uris)
                    } ?: run {
                        Toast.makeText(this, "Error: File URIs missing", Toast.LENGTH_SHORT).show()
                    }
                },
                openFile = { file ->
                    openFile(this, file) { msg, isErr ->
                        Toast.makeText(this, msg, if(isErr) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "com.ost.application.action.SEND_FILES") {
            urisToShare = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            if (!urisToShare.isNullOrEmpty()) {
                if (!viewModel.isDiscovering.value && viewModel.transferProgress.value == null && viewModel.incomingTransferRequest.value == null) {
                    viewModel.startDiscovery()
                }
            } else {
                Toast.makeText(this, "Error: No files to send", Toast.LENGTH_LONG).show()
                finish()
            }
        } else {
            urisToShare = null
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onStop() {
        super.onStop()
    }

    private fun installApk(context: Context, file: File, showDialog: (message: String, isError: Boolean) -> Unit) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
            showDialog(context.getString(R.string.starting_package_installer), false)
        } catch (e: IllegalArgumentException) {
            showDialog(context.getString(R.string.error_accessing_apk_e, e.localizedMessage ?: "FileProvider error"), true)
        }
        catch (e: ActivityNotFoundException) {
            showDialog(context.getString(R.string.package_installer_not_found), true)
        }
        catch (e: Exception) {
            showDialog(context.getString(R.string.failed_to_start_package_installer), true)
        }
    }

    fun openFile(context: Context, file: File, showDialog: (message: String, isError: Boolean) -> Unit) {
        val vibrator = context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))

        val intent : Intent? = when {
            file.name.endsWith(".apk", true) -> {
                installApk(context, file) { msg, isErr -> showDialog(msg, isErr) }
                null
            }
            file.name.endsWith(".txt", true) || file.name.endsWith(".json", true) || file.name.endsWith(".xml", true) || file.name.endsWith(".log", true) -> {
                Intent(context, TextEditorActivity::class.java).apply { putExtra("filePath", file.absolutePath) }
            }
            file.name.endsWith(".png", true) || file.name.endsWith(".jpg", true) || file.name.endsWith(".jpeg", true) || file.name.endsWith(".gif", true) || file.name.endsWith(".bmp", true)-> {
                Intent(context, ImageActivity::class.java).apply { putExtra("imagePath", file.absolutePath) }
            }
            file.name.endsWith(".mp4", true) || file.name.endsWith(".avi", true) || file.name.endsWith(".mkv", true) || file.name.endsWith(".webm", true) -> {
                Intent(context, VideoActivity::class.java).apply { putExtra("videoPath", file.absolutePath) }
            }
            file.name.endsWith(".mp3", true) || file.name.endsWith(".m4a", true) || file.name.endsWith(".wav", true) || file.name.endsWith(".ogg", true) || file.name.endsWith(".aac", true)-> {
                Intent(context, MusicActivity::class.java).apply { putExtra("musicPath", file.absolutePath) }
            }
            else -> {
                try {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                } catch (e: IllegalArgumentException) {
                    showDialog(context.getString(R.string.error_accessing_file_e, e.localizedMessage ?: "FileProvider error"), true)
                    null
                } catch (e: Exception) {
                    showDialog(context.getString(R.string.cannot_open_file_type), true)
                    null
                }
            }
        }
        try {
            intent?.let { context.startActivity(it) }
        } catch (e: ActivityNotFoundException) {
            showDialog(context.getString(R.string.no_app_installed_to_open_this_file_type), true)
        } catch (e: Exception) {
            showDialog(context.getString(R.string.error_opening_file_e, e.localizedMessage ?: "Unknown error"), true)
        }
    }
}

@Composable
fun ShareApp(
    viewModel: WearShareViewModel,
    isSendMode: Boolean,
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    openFile: (File) -> Unit
) {
    val isServiceActive by viewModel.isServiceActive.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()
    val lastReceivedFiles by viewModel.lastReceivedFiles.collectAsState()
    val incomingTransferRequest by viewModel.incomingTransferRequest.collectAsState()
    val uiConfirmationState by viewModel.uiConfirmationState.collectAsState()
    val context = LocalContext.current

    val isTransferring = transferProgress != null
    val isToggleEnabled = !isDiscovering && !isTransferring && !isSendMode && incomingTransferRequest == null

    MaterialTheme {
        AppScaffold (
            timeText = {
                if (isTransferring && transferProgress != null) {
                    val progressText = "${transferProgress!!}%"
                    val progressTextStyle =
                        TimeTextDefaults.timeTextStyle(color = MaterialTheme.colorScheme.primary)
                    TimeText(
                        startLinearContent = {
                            Text(
                                text = progressText,
                                style = progressTextStyle
                            )
                        },
                        startCurvedContent = {
                            curvedText(
                                text = progressText,
                                style = CurvedTextStyle(progressTextStyle)
                            )
                        }
                    )
                } else {
                    TimeText()
                }
            },
        ) {
            val currentStatus = statusText
            val isSending = isTransferring && (currentStatus.contains(
                context.getString(R.string.sending_prefix),
                ignoreCase = true
            ) || currentStatus.contains(
                context.getString(R.string.connecting_prefix),
                ignoreCase = true
            ) || currentStatus.contains(context.getString(R.string.sent_prefix), ignoreCase = true))
            val isReceiving = isTransferring && (currentStatus.contains(
                context.getString(R.string.receiving_prefix),
                ignoreCase = true
            ) || currentStatus.contains(
                context.getString(R.string.incoming_prefix),
                ignoreCase = true
            ))
            val showSendUI = isSendMode || isDiscovering || isSending

            ScreenScaffold {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Crossfade(
                        targetState = showSendUI,
                        label = "ScreenTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { currentShowSendUI ->
                        if (currentShowSendUI) {
                            SendUI(
                                viewModel = viewModel,
                                discoveredDevices = discoveredDevices,
                                isDiscovering = isDiscovering,
                                isSendingTransferActive = isSending,
                                statusText = statusText,
                                onDeviceSelected = onDeviceSelected,
                                onScanClicked = { viewModel.startDiscovery() },
                                onCancelClicked = {
                                    if (isDiscovering) viewModel.stopDiscovery()
                                    if (isSending) viewModel.cancelTransfer()
                                    (context as? ShareActivity)?.finish()
                                }
                            )
                        } else {
                            ReceiveUI(
                                viewModel = viewModel,
                                isServiceActive = isServiceActive,
                                isToggleEnabled = isToggleEnabled,
                                statusText = statusText,
                                lastReceivedFiles = lastReceivedFiles,
                                openFile = openFile,
                                context = context
                            )
                        }
                    }

                    if (isDiscovering || isTransferring) {
                        val isIndeterminate =
                            isDiscovering && !isTransferring || isTransferring && transferProgress == 0 && (isSending || isReceiving)
                        FixedCircularProgress(
                            isIndeterminate = isIndeterminate,
                            progress = if (isIndeterminate) null else transferProgress
                        )
                    }
                }
            }

            val request = incomingTransferRequest
            if (request != null) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = { viewModel.rejectIncomingTransfer(request.requestId) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download_24dp),
                            contentDescription = stringResource(R.string.incoming_request_alert_cd),
                        )
                    },
                    title = {
                        Text(
                            stringResource(R.string.notif_incoming_files_title),
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(
                                R.string.notif_incoming_files_details,
                                request.senderDeviceName,
                                request.fileNames.joinToString(", "),
                                request.totalSize.formatFileSize(context)
                            ),
                            textAlign = TextAlign.Center,
                        )
                    },
                    dismissButton = {
                        Button(
                            colors = ButtonDefaults.filledTonalButtonColors(),
                            onClick = {
                                viewModel.rejectIncomingTransfer(request.requestId)
                            },
                        ) {
                            Icon(painterResource(R.drawable.ic_cancel_24dp), null)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.acceptIncomingTransfer(request.requestId)
                            }
                        ) {
                            Icon(painterResource(R.drawable.ic_check_circle_24dp), null)
                        }
                    }
                )
            }

            uiConfirmationState?.let { state ->
                Confirmation(
                    onTimeout = { viewModel.clearUiConfirmationState() },
                    icon = {
                        Icon(
                            painter = painterResource(id = state.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                                .wrapContentSize(align = Alignment.Center),
                            tint = if (state.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    },
                    durationMillis = Constants.CONFIRMATION_TIMEOUT_MILLIS
                ) {
                    Text(
                        text = state.message,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
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
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    onScanClicked: () -> Unit,
    onCancelClicked: () -> Unit
) {
    val scalingListState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    LaunchedEffect(discoveredDevices.isNotEmpty(), isDiscovering, isSendingTransferActive) {
        if (discoveredDevices.isNotEmpty() && !isDiscovering && !isSendingTransferActive) {
            delay(150)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .onRotaryScrollEvent {
                if (!isDiscovering && !isSendingTransferActive) {
                    coroutineScope.launch { scalingListState.scrollBy(it.verticalScrollPixels) }
                    true
                } else false
            }
            .focusable(),
        state = scalingListState,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        anchorType = ScalingLazyListAnchorType.ItemCenter
    ) {
        item {
            Text(text = stringResource(R.string.send_file), textAlign = TextAlign.Center)
        }
        item {
            Text(text = statusText, textAlign = TextAlign.Center, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp))
        }
        item { Spacer(Modifier.height(4.dp)) }

        if (discoveredDevices.isNotEmpty() && !isSendingTransferActive) {
            items(items = discoveredDevices, key = { it.id }) { device ->
                DeviceChip(device = device, onClick = {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    if (device.isResolved) onDeviceSelected(device)
                    else if (!device.isResolving) viewModel.resolveDevice(device.serviceInfo)
                })
            }
        } else if (!isDiscovering && !isSendingTransferActive) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_search_off_24dp),
                        contentDescription = stringResource(R.string.no_devices_found_cd),
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.no_devices_found_hint), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        onScanClicked()
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_refresh_24dp), contentDescription = stringResource(R.string.scan))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.scan_again))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Button(
                onClick = {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    onCancelClicked()
                },
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                val iconRes = if (isSendingTransferActive || isDiscovering) R.drawable.ic_cancel_24dp else R.drawable.ic_close_24dp
                val contentDesc = stringResource(if (isSendingTransferActive || isDiscovering) R.string.cancel else R.string.close)
                Icon(painter = painterResource(iconRes), contentDescription = contentDesc)
            }
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
                }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp
            )
        },
        icon = {
            if (device.isResolving) {
                CircularProgressIndicator(modifier = Modifier.size(ChipDefaults.IconSize), strokeWidth = 1.dp)
            } else {
                Icon(painter = painterResource(id = iconRes), contentDescription = device.deviceType, modifier = Modifier.size(ChipDefaults.IconSize))
            }
        },
        colors = ChipDefaults.primaryChipColors(contentColor = if (device.isResolved) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
    )
}

@SuppressLint("StringFormatMatches")
@Composable
fun ReceiveUI(
    viewModel: WearShareViewModel,
    isServiceActive: Boolean,
    isToggleEnabled: Boolean,
    statusText: String,
    lastReceivedFiles: List<File>,
    openFile: (File) -> Unit,
    context: Context
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    val maxRecentFilesToShow = 3

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
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
        item { Text(text = stringResource(R.string.file_share), textAlign = TextAlign.Center) }

        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth(),
                checked = isServiceActive,
                onCheckedChange = { isActive ->
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    viewModel.setServiceActive(isActive)
                },
                enabled = isToggleEnabled,
                label = { Text(stringResource(R.string.make_discoverable)) },
            )
        }
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (lastReceivedFiles.isNotEmpty()) {
            val filesToDisplay = lastReceivedFiles.takeLast(maxRecentFilesToShow).reversed()
            val hasMoreFiles = lastReceivedFiles.size > maxRecentFilesToShow

            items(filesToDisplay) { file ->
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { openFile(file) },
                    label = {
                        Text(
                            text = file.name,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = {
                        Icon(painterResource(id = getFileIcon(file.extension)), contentDescription = null)
                    },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }

            if (hasMoreFiles) {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                            Toast.makeText(context, "TODO: Open full received files list", Toast.LENGTH_SHORT).show()
                        },
                        label = {
                            Text(stringResource(R.string.view_all_received_files_count, lastReceivedFiles.size))
                        },
                        icon = {
                            Icon(painterResource(R.drawable.ic_folder_24dp), contentDescription = stringResource(R.string.view_all_files_cd))
                        },
                        colors = ChipDefaults.primaryChipColors()
                    )
                }
            }
        }
    }
}

@Composable
fun FixedCircularProgress(
    isIndeterminate: Boolean,
    progress: Int?
) {
    val progressValue = if (isIndeterminate) 0f else (progress ?: 0) / 100f
    CircularProgressIndicator(
        modifier = Modifier
            .fillMaxSize()
            .clearAndSetSemantics {},
        progress = progressValue,
        startAngle = 295.5f,
        endAngle = 245.5f,
        indicatorColor = MaterialTheme.colorScheme.primary,
        strokeWidth = ProgressIndicatorDefaults.FullScreenStrokeWidth,
        trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
    )
}

@Composable
fun getFileIcon(extension: String): Int {
    return when (extension.lowercase()) {
        "txt", "json", "xml", "log", "md" -> R.drawable.ic_document_file_24dp
        "png", "jpg", "jpeg", "gif", "bmp", "webp" -> R.drawable.ic_image_24dp
        "mp4", "avi", "mkv", "webm" -> R.drawable.ic_video_24dp
        "mp3", "m4a", "wav", "ogg", "aac", "flac" -> R.drawable.ic_music_24dp
        "apk" -> R.drawable.ic_android_24dp
        else -> R.drawable.ic_draft_24dp
    }
}