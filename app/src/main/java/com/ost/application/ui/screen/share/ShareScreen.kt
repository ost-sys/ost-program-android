@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.ost.application.ui.screen.share

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.R
import com.ost.application.utils.CustomCardItem
import com.ost.application.utils.SectionTitle

@Composable
fun ShareScreen(
    modifier: Modifier = Modifier,
    viewModel: ShareViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val discoveredDevices by viewModel.discoveredDevices.observeAsState(initial = emptyList())
    val transferStatus by viewModel.transferStatus.observeAsState(initial = "Idle.")
    val transferProgress by viewModel.transferProgress.observeAsState(initial = null)
    val isTransferActive by viewModel.isTransferActive.observeAsState(initial = false)
    val isReceivingActive by viewModel.isReceivingActive.observeAsState(initial = false)
    val isDiscoveryActive by viewModel.isDiscoveryActive.observeAsState(initial = false)

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            Log.i(Constants.TAG, "File selected: $selectedUri")
            viewModel.sendFileToSelectedDevice(selectedUri)
        } ?: run {
            Log.i(Constants.TAG, "File selection cancelled.")
            viewModel.clearSelectedDevice()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(Constants.TAG, "ShareScreen: Permission result received: $permissions")
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Log.i(Constants.TAG, "All critical permissions granted via launcher.")
            viewModel.handlePermissionsGranted()
        } else {
            Log.w(Constants.TAG, "Not all critical permissions granted.")
        }
    }

    LaunchedEffect(key1 = true) {
        NotificationHelper.createNotificationChannel(context.applicationContext)
        checkAndRequestPermissions(context, requestPermissionLauncher) {
            viewModel.handlePermissionsGranted()
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d(Constants.TAG, "ShareScreen: ON_RESUME.")
                    if (!isReceivingActive && !isTransferActive) {
                        viewModel.startDiscovery()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d(Constants.TAG, "ShareScreen: ON_PAUSE.")
                    if (isDiscoveryActive && !isReceivingActive) {
                        viewModel.stopDiscovery()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d(Constants.TAG, "ShareScreen: ON_DESTROY lifecycle event.")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Log.d(Constants.TAG, "ShareScreen: onDispose.")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_share_24dp),
                contentDescription = stringResource(R.string.share_files),
                modifier = Modifier.size(80.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = transferStatus,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedVisibility(visible = isTransferActive && transferProgress != null) {
                LinearProgressIndicator(
                    progress = { (transferProgress ?: 0) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .padding(horizontal = 32.dp)
                )
            }
            AnimatedVisibility(visible = isTransferActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.cancelTransfer() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }

        ElevatedCard(
            onClick = {
                if (!isTransferActive) {
                    viewModel.setReceivingActive(!isReceivingActive)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp, start = 5.dp, end = 5.dp),
            enabled = !isTransferActive,
            elevation = CardDefaults.cardElevation(
                defaultElevation = 5.dp
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
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
                    },
                    enabled = !isTransferActive
                )
            }
        }


        SectionTitle(stringResource(R.string.devices))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 5.dp)
        ) {
            if (discoveredDevices.isEmpty() && !isReceivingActive) {
                item {
                    Text(
                        text = if (isDiscoveryActive) stringResource(R.string.searching_for_devices) else stringResource(R.string.searching_or_enable_receiving),
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(discoveredDevices, key = { it.id }) { device ->
                    DeviceItem(
                        device = device,
                        isTransferActive = isTransferActive,
                        onClick = { selectedDevice ->
                            viewModel.setSelectedDevice(selectedDevice)
                            try {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            } catch (e: Exception) {
                                Log.e(Constants.TAG, "Error launching file picker", e)
                                viewModel.clearSelectedDevice()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: DiscoveredDevice,
    isTransferActive: Boolean,
    onClick: (DiscoveredDevice) -> Unit
) {
    val isEnabled = device.isResolved && !isTransferActive
    val summaryText = when {
        device.isResolving -> stringResource(R.string.resolving)
        device.isResolved -> "${device.ipAddress?.hostAddress}:${device.port}"
        else -> stringResource(R.string.waiting_for_resolution)
    }
    val titleText = device.name.ifBlank { stringResource(R.string.unknown_device) }

    CustomCardItem(
        icon = getDeviceIconRes(device.type),
        iconPainter = null,
        title = titleText,
        summary = summaryText,
        status = isEnabled,
        onClick = {
            if (isEnabled) {
                onClick(device)
            }
        }
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

private fun checkAndRequestPermissions(
    context: Context,
    launcher: ActivityResultLauncher<Array<String>>,
    onGranted: () -> Unit
) {
    val requiredPermissions = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (requiredPermissions.isNotEmpty()) {
        Log.d(Constants.TAG, "Requesting permissions: ${requiredPermissions.joinToString()}")
        launcher.launch(requiredPermissions.toTypedArray())
    } else {
        Log.i(Constants.TAG, "Necessary permissions already granted.")
        onGranted()
    }
}