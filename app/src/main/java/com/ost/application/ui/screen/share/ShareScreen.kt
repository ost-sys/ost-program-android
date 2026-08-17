@file:OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
package com.ost.application.ui.screen.share

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.core.share.Constants
import com.ost.application.core.share.DiscoveredDevice
import com.ost.application.ui.components.SectionTitle
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import kotlinx.coroutines.launch
import java.io.File

data class StagedFileInfo(
    val uri: Uri,
    val name: String,
    val size: Long
)

fun getUriDetails(context: Context, uri: Uri): StagedFileInfo {
    var name = "Selected Item"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
        }
    }
    return StagedFileInfo(uri, name, size)
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
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current

    val selectedProtocol    by viewModel.selectedProtocol.collectAsStateWithLifecycle()
    val isServiceRunning    by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val isServiceStuck      by viewModel.isServiceStuck.collectAsStateWithLifecycle()
    val discoveredDevices   by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val transferStatus      by viewModel.statusText.collectAsStateWithLifecycle()
    val transferProgress    by viewModel.transferProgress.collectAsStateWithLifecycle()
    val isTransferActive    by viewModel.isTransferActive.collectAsStateWithLifecycle()
    val isReceivingActive   by viewModel.isReceivingActive.collectAsStateWithLifecycle()
    val isDiscoveryActive   by viewModel.isDiscovering.collectAsStateWithLifecycle()
    val incomingRequest     by viewModel.incomingTransferRequest.collectAsStateWithLifecycle()
    val isCleaningUp        by viewModel.isCleaningUp.collectAsStateWithLifecycle()
    val lastReceivedFiles   by viewModel.lastReceivedFiles.collectAsStateWithLifecycle()
    val stagedUris          by viewModel.stagedUris.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var textToSend by remember { mutableStateOf("") }

    val stagedDetails = remember(stagedUris) {
        stagedUris.map { getUriDetails(context, it) }
    }
    val totalStagedSize = remember(stagedDetails) { stagedDetails.sumOf { it.size } }

    LaunchedEffect(Unit) {
        checkAndRequestPermissions(context,
            launcher = null,
            onGranted = { viewModel.handlePermissionsGranted() }
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.handlePermissionsGranted() }

    LaunchedEffect(Unit) {
        checkAndRequestPermissions(context,
            launcher = requestPermissionLauncher,
            onGranted = { viewModel.handlePermissionsGranted() }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasRequiredPermissions(context)) {
                    viewModel.handlePermissionsGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addStagedUris(uris)
        }
        showAddSheet = false
    }

    incomingRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { viewModel.rejectIncomingTransfer(request.requestId) },
            icon = { Icon(painterResource(R.drawable.ic_download_24dp), contentDescription = null) },
            title = { Text(stringResource(R.string.notif_incoming_files_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.notif_incoming_files_details,
                        request.senderDeviceName,
                        request.fileNames.size,
                        android.text.format.Formatter.formatFileSize(context, request.totalSize)
                    )
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.acceptIncomingTransfer(request.requestId) }) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.rejectIncomingTransfer(request.requestId) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text(stringResource(R.string.enter_text_to_send)) },
            text = {
                OutlinedTextField(
                    value = textToSend,
                    onValueChange = { textToSend = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textToSend.isNotBlank()) {
                            try {
                                val tempFile = File(context.cacheAreaDir(), "shared_text.txt")
                                tempFile.writeText(textToSend)
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    Constants.FILE_PROVIDER_AUTHORITY,
                                    tempFile
                                )
                                viewModel.addStagedUris(listOf(uri))
                            } catch (e: Exception) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Error adding text: ${e.message}")
                                }
                            }
                        }
                        showTextDialog = false
                        showAddSheet = false
                        textToSend = ""
                    }
                ) {
                    Text(stringResource(R.string.send))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_to_selection),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.what_do_you_want_to_add),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    SelectionOptionCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(painterResource(R.drawable.ic_upload_file_24dp), contentDescription = null, modifier = Modifier.size(32.dp)) },
                        label = stringResource(R.string.selection_file),
                        onClick = {
                            try {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            } catch (e: Exception) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Error opening file picker")
                                }
                            }
                        }
                    )

                    SelectionOptionCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(32.dp)) },
                        label = stringResource(R.string.selection_text),
                        onClick = {
                            showTextDialog = true
                        }
                    )

                    SelectionOptionCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(32.dp)) },
                        label = stringResource(R.string.selection_paste),
                        onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                try {
                                    val tempFile = File(context.cacheAreaDir(), "pasted_text.txt")
                                    tempFile.writeText(clipText)
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        Constants.FILE_PROVIDER_AUTHORITY,
                                        tempFile
                                    )
                                    viewModel.addStagedUris(listOf(uri))
                                    showAddSheet = false
                                } catch (e: Exception) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Error pasting text")
                                    }
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.logs_copied))
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = { showAddSheet = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.selection_title))
                    TextButton(onClick = {
                        viewModel.clearStagedUris()
                        showEditDialog = false
                    }) {
                        Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${stringResource(R.string.files_count, stagedUris.size)} • ${stringResource(R.string.size_total, android.text.format.Formatter.formatFileSize(context, totalStagedSize))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stagedDetails, key = { it.uri.toString() }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(painterResource(R.drawable.ic_upload_file_24dp), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(android.text.format.Formatter.formatFileSize(context, item.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { viewModel.removeStagedUri(item.uri) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    action = {
                        data.visuals.actionLabel?.let { label ->
                            Button(onClick = { data.performAction() }) { Text(label) }
                        }
                    },
                    containerColor = if (data.visuals.message.startsWith(stringResource(R.string.error_prefix)))
                        MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.inverseSurface,
                    contentColor = if (data.visuals.message.startsWith(stringResource(R.string.error_prefix)))
                        MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.inverseOnSurface
                ) { Text(text = data.visuals.message) }
            }
        }
    ) { innerPadding ->
        val bottomSpacing = LocalBottomSpacing.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 8.dp,
                bottom = 16.dp + bottomSpacing
            )
        ) {

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    ToggleButton(
                        checked = selectedProtocol == ShareProtocol.OST_SHARE,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            viewModel.setProtocol(ShareProtocol.OST_SHARE)
                        },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                        Text(text = stringResource(R.string.protocol_ost_share), maxLines = 1)
                    }

                    ToggleButton(
                        checked = selectedProtocol == ShareProtocol.LOCALSEND,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            viewModel.setProtocol(ShareProtocol.LOCALSEND)
                        },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_device_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                        Text(text = stringResource(R.string.protocol_localsend), maxLines = 1)
                    }
                }
            }

            if (isServiceStuck) {
                item {
                    ServiceControlCard(
                        isServiceRunning = isServiceRunning,
                        isServiceStuck = isServiceStuck,
                        isCleaningUp = isCleaningUp,
                        onToggle = { on ->
                            view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                            if (on) viewModel.startService() else viewModel.stopService()
                        },
                        onRestart = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            viewModel.restartService()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (stagedUris.isNotEmpty()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.selection_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (stagedUris.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearStagedUris() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }

                        if (stagedUris.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showAddSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.add_to_selection))
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.files_count, stagedUris.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.size_total, android.text.format.Formatter.formatFileSize(context, totalStagedSize)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(stagedDetails) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_upload_file_24dp),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showEditDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.edit_action))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { showAddSheet = true },
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.add_action))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SectionTitle(stringResource(R.string.nearby_devices))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                if (isDiscoveryActive) viewModel.stopDiscovery() else viewModel.startDiscovery()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_refresh_24dp),
                                contentDescription = "Refresh",
                                tint = if (isDiscoveryActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                                viewModel.setReceivingActive(!isReceivingActive)
                            }
                        ) {
                            Icon(
                                painter = painterResource(if (isReceivingActive) R.drawable.ic_visibility_24dp else R.drawable.ic_visibility_off_24dp),
                                contentDescription = "Visibility",
                                tint = if (isReceivingActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                                if (isServiceRunning) viewModel.stopService() else viewModel.startService()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_power_new_24dp),
                                contentDescription = "Service",
                                tint = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            if (discoveredDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_no_wifi_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.troubleshoot_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(discoveredDevices) { index, device ->
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
                            if (stagedUris.isNotEmpty()) {
                                viewModel.sendFilesToSelectedDevice(stagedUris)
                                viewModel.clearStagedUris()
                            } else {
                                showAddSheet = true
                            }
                        },
                        position = position
                    )
                }
            }

            if (isTransferActive) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (transferProgress != null) {
                                CircularWavyProgressIndicator(
                                    progress = { (transferProgress ?: 0) / 100f },
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Text(
                                text = transferStatus,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.cancelTransfer() }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                }
            }

            if (lastReceivedFiles.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionTitle(stringResource(R.string.share_received_files))
                }

                itemsIndexed(lastReceivedFiles) { index, file ->
                    val position = when {
                        lastReceivedFiles.size == 1 -> CardPosition.SINGLE
                        index == 0 -> CardPosition.TOP
                        index == lastReceivedFiles.lastIndex -> CardPosition.BOTTOM
                        else -> CardPosition.MIDDLE
                    }
                    ReceivedFileItem(
                        file = file,
                        position = position,
                        onClick = {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                Constants.FILE_PROVIDER_AUTHORITY,
                                file
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "*/*")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(android.content.Intent.createChooser(intent, "Open file"))
                            } catch (e: Exception) {}
                        },
                        onDelete = { viewModel.deleteReceivedFile(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectionOptionCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun Context.cacheAreaDir(): File {
    val dir = File(cacheDir, "shared_text")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

@Composable
private fun ServiceControlCard(
    isServiceRunning: Boolean,
    isServiceStuck: Boolean,
    isCleaningUp: Boolean,
    onToggle: (Boolean) -> Unit,
    onRestart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceStuck)
                MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.share_service),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when {
                        isCleaningUp -> stringResource(R.string.service_stuck)
                        isServiceStuck -> stringResource(R.string.service_stuck)
                        isServiceRunning -> stringResource(R.string.service_running)
                        else -> stringResource(R.string.service_stopped)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isServiceStuck) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isServiceStuck) {
                ElevatedButton(onClick = onRestart) {
                    Text(stringResource(R.string.restart_service))
                }
            } else {
                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = onToggle,
                    enabled = !isCleaningUp
                )
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: DiscoveredDevice,
    isTransferActive: Boolean,
    onClick: (DiscoveredDevice) -> Unit,
    position: CardPosition
) {
    val deviceIconRes = when {
        device.deviceType.contains("tablet", ignoreCase = true) -> R.drawable.ic_tablet_24dp
        device.deviceType.contains("desktop", ignoreCase = true) || device.deviceType.contains("pc", ignoreCase = true) || device.deviceType.contains("mac", ignoreCase = true) -> R.drawable.ic_desktop_windows_24dp
        device.deviceType.contains("watch", ignoreCase = true) || device.deviceType.contains("wear", ignoreCase = true) -> R.drawable.ic_watch_24dp
        else -> R.drawable.ic_mobile_24dp
    }
    CustomCardItem(
        title = device.name,
        summary = "${device.host ?: device.ipAddress?.hostAddress ?: ""} • ${device.deviceType}",
        icon = deviceIconRes,
        position = position,
        onClick = { if (!isTransferActive) onClick(device) }
    )
}

@Composable
fun ReceivedFileItem(
    file: java.io.File,
    position: CardPosition,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = if (position == CardPosition.BOTTOM || position == CardPosition.SINGLE) 0.dp else 2.dp),
        shape = when (position) {
            CardPosition.TOP -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
            CardPosition.MIDDLE -> RoundedCornerShape(4.dp)
            CardPosition.BOTTOM -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
            CardPosition.SINGLE -> RoundedCornerShape(24.dp)
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_download_24dp), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(android.text.format.Formatter.formatFileSize(LocalContext.current, file.length()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun hasRequiredPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}

private fun checkAndRequestPermissions(
    context: Context,
    launcher: ActivityResultLauncher<Array<String>>?,
    onGranted: () -> Unit
) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val allGranted = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    if (allGranted) {
        onGranted()
    } else {
        launcher?.launch(permissions)
    }
}
