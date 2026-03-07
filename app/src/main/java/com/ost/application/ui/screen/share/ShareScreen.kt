@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.ost.application.ui.screen.share

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.ui.component.ExpressiveShapeBackground
import com.ost.application.ui.component.ExpressiveShapeType
import com.ost.application.ui.screen.share.NotificationHelper.formatFileSize
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import com.ost.application.util.SectionTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ShareIconState {
    IDLE,
    SEARCHING,
    RECEIVING_ACTIVE,
    TRANSFERRING,
    SUCCESS
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ShareScreen(
    modifier: Modifier = Modifier,
    viewModel: ShareViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    LocalHapticFeedback.current
    val view = LocalView.current

    val discoveredDevices by viewModel.discoveredDevices.collectAsState(initial = emptyList())
    val transferStatus by viewModel.statusText.collectAsState(initial = stringResource(R.string.idle_status))
    val transferProgress by viewModel.transferProgress.collectAsState(initial = null)
    val isTransferActive by viewModel.isTransferActive.collectAsState(initial = false)
    val isReceivingActive by viewModel.isReceivingActive.collectAsState(initial = false)
    val isDiscoveryActive by viewModel.isDiscovering.collectAsState(initial = false)
    val incomingTransferRequest by viewModel.incomingTransferRequest.collectAsState(initial = null)
    val isCleaningUp by viewModel.isCleaningUp.collectAsState(initial = false)

    var showSuccessAnimation by remember { mutableStateOf(false) }

    val sentSuccessPrefix = context.getString(R.string.sent_multi_success).substringBefore("%").trim()
    val receivedSuccessPrefix = context.getString(R.string.received_multi_success).substringBefore("%").trim()

    LaunchedEffect(transferStatus, isTransferActive, isReceivingActive) {
        if (!isTransferActive && !isReceivingActive) {
            val isSuccessStatus = transferStatus.startsWith(sentSuccessPrefix) || transferStatus.startsWith(receivedSuccessPrefix)
            if (isSuccessStatus) {
                if (!showSuccessAnimation) {
                    showSuccessAnimation = true
                    delay(2500)
                    showSuccessAnimation = false
                }
            }
        }
    }

    val currentIconState by remember(isTransferActive, isReceivingActive, isDiscoveryActive, showSuccessAnimation) {
        derivedStateOf {
            if (showSuccessAnimation) ShareIconState.SUCCESS
            else if (isTransferActive) ShareIconState.TRANSFERRING
            else if (isReceivingActive) ShareIconState.RECEIVING_ACTIVE
            else if (isDiscoveryActive) ShareIconState.SEARCHING
            else ShareIconState.IDLE
        }
    }

    val starColor by animateColorAsState(
        targetValue = when (currentIconState) {
            ShareIconState.IDLE -> MaterialTheme.colorScheme.surfaceVariant
            ShareIconState.SEARCHING, ShareIconState.RECEIVING_ACTIVE -> MaterialTheme.colorScheme.secondaryContainer
            ShareIconState.TRANSFERRING -> MaterialTheme.colorScheme.tertiaryContainer
            ShareIconState.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(400), label = "starColor"
    )

    val iconColor by animateColorAsState(
        targetValue = when (currentIconState) {
            ShareIconState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
            ShareIconState.SEARCHING, ShareIconState.RECEIVING_ACTIVE -> MaterialTheme.colorScheme.onSecondaryContainer
            ShareIconState.TRANSFERRING -> MaterialTheme.colorScheme.onTertiaryContainer
            ShareIconState.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = tween(400), label = "iconColor"
    )

    val targetShape = when (currentIconState) {
        ShareIconState.IDLE -> ExpressiveShapeType.CIRCLE
        ShareIconState.SEARCHING -> ExpressiveShapeType.COOKIE_9
        ShareIconState.RECEIVING_ACTIVE -> ExpressiveShapeType.PILL
        ShareIconState.TRANSFERRING -> ExpressiveShapeType.CLOVER_4
        ShareIconState.SUCCESS -> ExpressiveShapeType.SQUARE
    }

    var rotationValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(currentIconState) {
        var lastFrameTime = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameTime ->
                val deltaTime = (frameTime - lastFrameTime) / 1_000_000_000f
                lastFrameTime = frameTime
                val targetSpeedDegreesPerSecond = when (currentIconState) {
                    ShareIconState.TRANSFERRING -> 120f
                    ShareIconState.SEARCHING -> 70f
                    else -> 0f
                }
                rotationValue = (rotationValue + targetSpeedDegreesPerSecond * deltaTime) % 360f
                if (rotationValue < 0) rotationValue += 360f
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "iconInfiniteTransition")
    val pulsatingScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale"
    )
    val pulsatingAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alpha"
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            viewModel.sendFilesToSelectedDevice(uris)
        } else {
            viewModel.clearSelectedDevice()
            coroutineScope.launch { snackbarHostState.showSnackbar(message = context.getString(R.string.no_files_selected), withDismissAction = true) }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(key1 = true) {
        NotificationHelper.createAppNotificationChannels(context.applicationContext)
        checkAndRequestPermissions(context, requestPermissionLauncher) {
            viewModel.handlePermissionsGranted()
        }
        viewModel.uiEvent.collect { event ->
            if (event is UiEvent.ShowSnackbar) {
                coroutineScope.launch { snackbarHostState.showSnackbar(message = event.message, withDismissAction = true) }
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (isDiscoveryActive && !isReceivingActive && !isTransferActive && !isCleaningUp) {
                    viewModel.stopDiscovery()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    action = { data.visuals.actionLabel?.let { actionLabel -> Button(onClick = { data.performAction() }) { Text(actionLabel) } } },
                    containerColor = if (data.visuals.message.startsWith(context.getString(R.string.error_prefix))) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.inverseSurface,
                    contentColor = if (data.visuals.message.startsWith(context.getString(R.string.error_prefix))) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.inverseOnSurface
                ) { Text(text = data.visuals.message) }
            }
        }
    ) { paddingValues ->
        val bottomSpacing = LocalBottomSpacing.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (incomingTransferRequest != null) {
                val request = incomingTransferRequest!!
                AlertDialog(
                    onDismissRequest = { viewModel.rejectIncomingTransfer(request.requestId) },
                    icon = { Icon(painterResource(R.drawable.ic_download_24dp), null) },
                    title = { Text(stringResource(R.string.notif_incoming_files_title)) },
                    text = { Text(stringResource(R.string.notif_incoming_files_details, request.senderDeviceName, request.fileNames.joinToString("\n"), request.totalSize.formatFileSize(context))) },
                    confirmButton = { Button(onClick = { viewModel.acceptIncomingTransfer(request.requestId) }) { Text(stringResource(R.string.accept)) } },
                    dismissButton = { OutlinedButton(onClick = { viewModel.rejectIncomingTransfer(request.requestId) }) { Text(stringResource(R.string.reject)) } }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                    val scale = if (currentIconState == ShareIconState.RECEIVING_ACTIVE) pulsatingScale else 1f
                    val alpha = if (currentIconState == ShareIconState.RECEIVING_ACTIVE) pulsatingAlpha else 1f

                    Box(modifier = Modifier.graphicsLayer { rotationZ = rotationValue; scaleX = scale; scaleY = scale; this.alpha = alpha }) {
                        ExpressiveShapeBackground(iconSize = 120.dp, color = starColor, forcedShape = targetShape)
                    }
                    Image(
                        painter = painterResource(id = R.drawable.ic_share_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp).graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
                        colorFilter = ColorFilter.tint(iconColor)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedContent(targetState = transferStatus, label = "status") { targetStatus ->
                    Text(text = targetStatus, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedVisibility(visible = isTransferActive && transferProgress != null) {
                    LinearWavyProgressIndicator(progress = { (transferProgress ?: 0) / 100f }, modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp))
                }
                AnimatedVisibility(visible = isTransferActive) {
                    Button(onClick = { viewModel.cancelTransfer() }) { Text(stringResource(R.string.cancel)) }
                }
            }

            val showButtons = !isReceivingActive && !isTransferActive && !isCleaningUp

            val topCardBottomRadius by animateDpAsState(
                targetValue = if (showButtons) 4.dp else 24.dp,
                label = "topCardRadius"
            )

            Card(
                onClick = {
                    if (!isTransferActive && !isCleaningUp) {
                        viewModel.setReceivingActive(!isReceivingActive)
                        view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = if (showButtons) 2.dp else 0.dp),
                enabled = !isTransferActive && !isCleaningUp,
                shape = RoundedCornerShape(
                    topStart = 24.dp, topEnd = 24.dp,
                    bottomStart = topCardBottomRadius, bottomEnd = topCardBottomRadius
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isTransferActive && !isCleaningUp) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.make_discoverable),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Switch(
                        checked = isReceivingActive,
                        onCheckedChange = { isChecked ->
                            viewModel.setReceivingActive(isChecked)
                            view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                        },
                        enabled = !isTransferActive && !isCleaningUp,
                    )
                }
            }

            AnimatedVisibility(
                visible = showButtons,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { viewModel.startDiscovery() },
                        enabled = !isDiscoveryActive && !isCleaningUp,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp,
                            bottomStart = 24.dp
                        )
                    ) {
                        Text(stringResource(R.string.find_devices))
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    ElevatedButton(
                        onClick = { viewModel.stopDiscovery() },
                        enabled = isDiscoveryActive && !isCleaningUp,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 24.dp
                        )
                    ) {
                        Text(stringResource(R.string.stop_search))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.devices))

            val showPlaceholder = !isReceivingActive && !isTransferActive && !isCleaningUp && (!isDiscoveryActive || (discoveredDevices.isEmpty() && !isDiscoveryActive))

            if (showPlaceholder) {
                Text(
                    text = if (isDiscoveryActive) stringResource(R.string.searching_for_devices) else stringResource(R.string.press_find_or_enable_receiving),
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                discoveredDevices.forEachIndexed { index, device ->
                    val position = when {
                        discoveredDevices.size == 1 -> CardPosition.SINGLE
                        index == 0 -> CardPosition.TOP
                        index == discoveredDevices.lastIndex -> CardPosition.BOTTOM
                        else -> CardPosition.MIDDLE
                    }
                    DeviceItem(
                        device = device,
                        isTransferActive = isTransferActive || isCleaningUp,
                        onClick = { selectedDevice ->
                            viewModel.setSelectedDevice(selectedDevice)
                            try {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            } catch (e: Exception) {
                                viewModel.clearSelectedDevice()
                                coroutineScope.launch { snackbarHostState.showSnackbar(message = context.getString(R.string.error_opening_file_picker, e.message ?: "unknown"), withDismissAction = true) }
                            }
                        },
                        position = position,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp + bottomSpacing))
        }
    }
}

@Composable
fun DeviceItem(
    device: DiscoveredDevice,
    isTransferActive: Boolean,
    onClick: (DiscoveredDevice) -> Unit,
    position: CardPosition,
) {
    val isEnabled = device.isResolved && !isTransferActive
    val summaryText = when {
        device.isResolving -> stringResource(R.string.resolving)
        device.isResolved -> "${device.ipAddress?.hostAddress}:${device.port}"
        else -> stringResource(R.string.waiting_for_resolution)
    }
    val titleText = device.name.ifBlank { stringResource(R.string.unknown_device) }

    CustomCardItem(
        position = position,
        icon = getDeviceIconRes(device.type),
        iconPainter = null,
        title = titleText,
        summary = summaryText,
        status = isEnabled,
        onClick = { if (isEnabled) onClick(device) },
    )
}

@Composable
private fun getDeviceIconRes(deviceType: String): Int {
    return when (deviceType) {
        Constants.VALUE_DEVICE_PHONE -> R.drawable.ic_device_24dp
        Constants.VALUE_DEVICE_WATCH -> R.drawable.ic_watch_24dp
        else -> R.drawable.ic_phone_android_24dp
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun checkAndRequestPermissions(context: Context, launcher: ActivityResultLauncher<Array<String>>, onGranted: () -> Unit) {
    val requiredPermissions = mutableListOf<String>()
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) requiredPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    if (requiredPermissions.isNotEmpty()) launcher.launch(requiredPermissions.toTypedArray()) else onGranted()
}