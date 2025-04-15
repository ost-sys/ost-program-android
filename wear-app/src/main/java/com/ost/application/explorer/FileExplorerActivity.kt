package com.ost.application.explorer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberRevealState
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.SwipeToRevealCard
import androidx.wear.compose.material.SwipeToRevealPrimaryAction
import androidx.wear.compose.material.SwipeToRevealSecondaryAction
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.tooling.preview.devices.WearDevices
import com.ost.application.R
import com.ost.application.explorer.music.MusicActivity
import com.ost.application.share.Constants
import com.ost.application.share.ShareActivity
import com.ost.application.util.ConfimationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

data class FileDialogInfo(val message: String, val isError: Boolean = false)

enum class ClipboardOperation { COPY, CUT }
data class ClipboardState(val files: Set<File>, val operation: ClipboardOperation)

class FileExplorerActivity : ComponentActivity() {

    private val rootPath = Environment.getExternalStorageDirectory().absolutePath
    private val currentPath = mutableStateOf(rootPath)
    private val _fileList = MutableStateFlow<List<File>>(emptyList())
    val fileList: StateFlow<List<File>> = _fileList.asStateFlow()
    private val dialogInfo = mutableStateOf<FileDialogInfo?>(null)

    private val _clipboardState = mutableStateOf<ClipboardState?>(null)
    val clipboardState: ClipboardState? by _clipboardState

    private val _showActionsDialogForFile = mutableStateOf<File?>(null)
    val showActionsDialogForFile: File? by _showActionsDialogForFile

    val showPasteButton: Boolean
        @Composable get() = _clipboardState.value != null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Log.i("Permission", "All required permissions granted.")
                loadFiles(currentPath.value)
            } else {
                Log.e("Permission", "Not all permissions granted. File listing might be incomplete.")
                dialogInfo.value = FileDialogInfo("Storage permissions denied", isError = true)
                loadFiles(currentPath.value)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FileManagerApp()
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else
            if (!Environment.isExternalStorageManager()) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            } else {
                Log.d("Permission", "MANAGE_EXTERNAL_STORAGE granted.")
            }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d("Permission", "Permissions already granted or not needed.")
            loadFiles(currentPath.value)
        }
    }

    private fun showActionsForFile(file: File) {
        _showActionsDialogForFile.value = file
    }

    private fun dismissActionsDialog() {
        _showActionsDialogForFile.value = null
    }

    private fun clearClipboard() {
        _clipboardState.value = null
    }

    private fun copyFile(file: File) {
        if (file.exists()) {
            _clipboardState.value = ClipboardState(files = setOf(file.absoluteFile), operation = ClipboardOperation.COPY)
            showDialog("Copied: ${file.name}", false)
            dismissActionsDialog()
        } else {
            showDialog("Error: File not found", true)
            dismissActionsDialog()
        }
    }

    private fun cutFile(file: File) {
        if (file.exists()) {
            _clipboardState.value = ClipboardState(files = setOf(file.absoluteFile), operation = ClipboardOperation.CUT)
            showDialog("Cut: ${file.name}", false)
            dismissActionsDialog()
        } else {
            showDialog("Error: File not found", true)
            dismissActionsDialog()
        }
    }

    private fun deleteFile(file: File) {
        dismissActionsDialog()
        lifecycleScope.launch {
            showDialog("Deleting: ${file.name}...", false)
            val (deleted, message) = deleteFileOrDirInternal(file)
            showDialog(message, isError = !deleted)
            if (deleted) {
                loadFiles(currentPath.value)
            }
        }
    }

    private fun shareFile(context: Context, file: File) {
        dismissActionsDialog()

        if (file.isDirectory) {
            showDialog("Cannot share folders.", true)
            return
        }

        Log.d(Constants.TAG, "Sharing file: ${file.name}")
        if (!file.exists()) {
            showDialog("Error: File not found", true)
            return
        }

        var fileUri: android.net.Uri? = null
        var errorOccurred = false
        try {
            fileUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: IllegalArgumentException) {
            Log.e(Constants.TAG, "Error getting URI for ${file.name}", e)
            errorOccurred = true
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Unexpected error getting URI for ${file.name}", e)
            errorOccurred = true
        }

        if (fileUri != null) {
            val uris = ArrayList<android.net.Uri>().apply { add(fileUri) }
            val intent = Intent(context, ShareActivity::class.java).apply {
                action = "com.ost.application.action.SEND_FILES"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(Constants.TAG, "Failed to start ShareActivity", e)
                errorOccurred = true
            }
        } else {
            errorOccurred = true
        }

        if (errorOccurred) {
            showDialog("Error preparing file for sharing.", true)
        }
    }

    private fun renameFile(file: File) {
        dismissActionsDialog()
        renameFileLauncher.launch(file.absolutePath)
    }

    private fun pasteFiles() {
        val state = _clipboardState.value
        if (state == null || state.files.isEmpty()) {
            showDialog("Clipboard is empty.", true)
            return
        }

        val destinationPath = currentPath.value
        val filesToProcess = state.files
        val operation = state.operation

        Log.d(Constants.TAG, "[PasteAction] Pasting ${filesToProcess.size} items (${operation}) into $destinationPath")

        lifecycleScope.launch {
            showDialog("Pasting...", false)

            var successCount = 0
            var errorCount = 0

            withContext(Dispatchers.IO) {
                filesToProcess.forEach { sourceFile ->
                    if (!sourceFile.exists()) {
                        Log.w(Constants.TAG, "[PasteAction] Source file doesn't exist: ${sourceFile.absolutePath}, skipping.")
                        errorCount++
                        return@forEach
                    }

                    val targetName = sourceFile.name
                    var targetFile = File(destinationPath, targetName)

                    var counter = 0
                    val baseName = targetFile.nameWithoutExtension
                    val extension = targetFile.extension.let { if (it.isNotEmpty()) ".$it" else "" }

                    while (targetFile.exists()) {
                        if (sourceFile.absolutePath == targetFile.absolutePath && operation == ClipboardOperation.CUT) {
                            Log.w(Constants.TAG, "[PasteAction] Attempting to CUT file onto itself: ${sourceFile.absolutePath}. Skipping.")
                            errorCount++
                            return@forEach
                        }
                        if (sourceFile.absolutePath == targetFile.absolutePath && operation == ClipboardOperation.COPY) {
                            counter++
                            val newName = "$baseName ($counter)$extension"
                            targetFile = File(destinationPath, newName)
                            Log.d(Constants.TAG, "[PasteAction] Copying file onto itself, creating copy: ${targetFile.name}")
                        } else {
                            counter++
                            val newName = "$baseName ($counter)$extension"
                            targetFile = File(destinationPath, newName)
                            Log.d(Constants.TAG, "[PasteAction] Name conflict, trying new name: ${targetFile.name}")
                        }

                        if (counter > 1000) {
                            Log.e(Constants.TAG, "[PasteAction] Too many conflicts for ${sourceFile.name}, skipping.")
                            errorCount++
                            return@forEach
                        }
                    }

                    try {
                        when (operation) {
                            ClipboardOperation.COPY -> {
                                if (sourceFile.isDirectory) {
                                    sourceFile.copyRecursively(targetFile, overwrite = false)
                                } else {
                                    sourceFile.copyTo(targetFile, overwrite = false)
                                }
                                Log.d(Constants.TAG, "[PasteAction] Copied ${sourceFile.name} to ${targetFile.name}")
                            }
                            ClipboardOperation.CUT -> {
                                if (sourceFile.renameTo(targetFile)) {
                                    Log.d(Constants.TAG, "[PasteAction] Moved ${sourceFile.name} to ${targetFile.name}")
                                } else {
                                    Log.w(Constants.TAG, "[PasteAction] renameTo failed for ${sourceFile.name}, trying copy+delete.")
                                    if (sourceFile.isDirectory) {
                                        sourceFile.copyRecursively(targetFile, overwrite = false)
                                    } else {
                                        sourceFile.copyTo(targetFile, overwrite = false)
                                    }
                                    if(targetFile.exists()) {
                                        if (deleteRecursively(sourceFile)) {
                                            Log.d(Constants.TAG, "[PasteAction] Source deleted after copy for CUT operation: ${sourceFile.name}")
                                        } else {
                                            Log.e(Constants.TAG, "[PasteAction] Failed to delete source after copy for CUT operation: ${sourceFile.name}")
                                            throw IOException("Failed to delete source after copy during CUT")
                                        }
                                    } else {
                                        throw IOException("Copy failed during CUT operation")
                                    }
                                }
                            }
                        }
                        successCount++
                    } catch (e: Exception) {
                        Log.e(Constants.TAG, "[PasteAction] Error processing ${sourceFile.name} -> ${targetFile.name}", e)
                        errorCount++
                        if (targetFile.exists()) {
                            deleteRecursively(targetFile)
                        }
                    }
                }
            }

            if (operation == ClipboardOperation.CUT && errorCount == 0) {
                clearClipboard()
            }

            val message = when {
                errorCount == 0 -> "${operation.name.lowercase().replaceFirstChar { it.uppercase() }} ${filesToProcess.size} item${if(filesToProcess.size > 1) "s" else ""}."
                successCount == 0 -> "Failed to ${operation.name.lowercase()} $errorCount item${if(errorCount > 1) "s" else ""}."
                else -> "${operation.name.lowercase().replaceFirstChar { it.uppercase() }} $successCount item${if(successCount > 1) "s" else ""}, failed for $errorCount."
            }
            showDialog(message, errorCount > 0)
            loadFiles(currentPath.value)
        }
    }

    private fun showDialog(message: String, isError: Boolean) {
        dialogInfo.value = FileDialogInfo(message, isError)
    }

    @Composable
    fun FileManagerApp() {
        val listState = rememberScalingLazyListState()
        val files = fileList.collectAsState().value
        val currentDialogInfo by dialogInfo
        clipboardState
        LocalContext.current
        val currentActionsDialogFile by _showActionsDialogForFile
        val actionsDialogListState = rememberScalingLazyListState()

        MaterialTheme {
            Scaffold(
                timeText = { TimeText() },
                positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FileList(
                        modifier = Modifier.fillMaxSize(),
                        path = currentPath.value,
                        files = files,
                        listState = listState,
                        showDialog = ::showDialog,
                        onPathChange = { newPath ->
                            currentPath.value = newPath
                            loadFiles(newPath)
                        },
                        onCreate = { name, isDirectory ->
                            lifecycleScope.launch {
                                val (created, message) = createNewFileOrDir(currentPath.value, name, isDirectory)
                                showDialog(message, isError = !created)
                                if (created) {
                                    loadFiles(currentPath.value)
                                }
                            }
                        },
                        onShowActionsRequest = ::showActionsForFile,
                        onDeleteSwipe = { fileToDelete ->
                            lifecycleScope.launch {
                                val (deleted, message) = deleteFileOrDirInternal(fileToDelete)
                                showDialog(message, isError = !deleted)
                                if (deleted) {
                                    loadFiles(currentPath.value)
                                }
                            }
                        },
                        isActionDialogVisible = currentActionsDialogFile != null,
                        onNavigateBack = {
                            val currentFile = File(currentPath.value)
                            val parentFile = currentFile.parentFile
                            if (parentFile != null && currentPath.value != rootPath && parentFile.canRead()) {
                                currentPath.value = parentFile.absolutePath
                                loadFiles(parentFile.absolutePath)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    AnimatedVisibility(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        visible = showPasteButton,
                        enter = slideInVertically { it / 2 } + fadeIn(),
                        exit = slideOutVertically { it / 2 } + fadeOut()
                    ) {
                        Button(
                            onClick = { pasteFiles() },
                            modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.primaryButtonColors()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_paste_24dp),
                                contentDescription = "Paste",
                                modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                            )
                        }
                    }

                }
            }

            currentActionsDialogFile?.let { fileToShowActionsFor ->
                FileActionsDialog(
                    listState = actionsDialogListState,
                    file = fileToShowActionsFor,
                    onDismissRequest = ::dismissActionsDialog,
                    onCopy = { copyFile(fileToShowActionsFor) },
                    onCut = { cutFile(fileToShowActionsFor) },
                    onRename = { renameFile(fileToShowActionsFor) },
                    onDelete = { deleteFile(fileToShowActionsFor) },
                    onShare = { shareFile(this, fileToShowActionsFor) }
                )
            }

            currentDialogInfo?.let { info ->
                val iconRes = if (info.isError) R.drawable.ic_error_24dp else R.drawable.ic_check_circle_24dp
                ConfimationDialog(
                    message = info.message,
                    iconResId = iconRes,
                    onDismiss = { dialogInfo.value = null }
                )
            }

            BackHandler(enabled = true) {
                if (currentActionsDialogFile != null) {
                    dismissActionsDialog()
                } else {
                    val currentFile = File(currentPath.value)
                    val parentFile = currentFile.parentFile
                    if (parentFile != null && currentPath.value != rootPath && parentFile.canRead()) {
                        currentPath.value = parentFile.absolutePath
                        loadFiles(parentFile.absolutePath)
                    } else {
                        finish()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @Composable
    fun FileList(
        modifier: Modifier = Modifier,
        path: String,
        files: List<File>,
        listState: ScalingLazyListState,
        showDialog: (message: String, isError: Boolean) -> Unit,
        onPathChange: (String) -> Unit,
        onCreate: (String, Boolean) -> Unit,
        onShowActionsRequest: (File) -> Unit,
        onDeleteSwipe: (File) -> Unit,
        isActionDialogVisible: Boolean,
        onNavigateBack: () -> Boolean
    ) {
        val focusRequester = remember { FocusRequester() }
        val context = LocalContext.current
        val showNewFileDialog = remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(path, isActionDialogVisible) {
            if (!isActionDialogVisible) {
                delay(100)
                try {
                    focusRequester.requestFocus()
                } catch (e: Exception) {
                    Log.w("FileListFocus", "RequestFocus failed: $e")
                }
            }
        }

        Crossfade(targetState = path, label = "FileListTransition") { currentDisplayPath ->
            ScalingLazyColumn(
                modifier = modifier
                    .focusRequester(focusRequester)
                    .onRotaryScrollEvent {
                        if (!isActionDialogVisible) {
                            coroutineScope.launch {
                                listState.scrollBy(it.verticalScrollPixels)
                            }
                            true
                        } else {
                            false
                        }
                    }
                    .focusable(),
                contentPadding = PaddingValues(top = 28.dp, bottom = 40.dp, start = 8.dp, end = 8.dp),
                state = listState
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                            colors = ButtonDefaults.secondaryButtonColors(),
                            enabled = currentDisplayPath != rootPath && !isActionDialogVisible,
                            onClick = {
                                if (!onNavigateBack()) {
                                    (context as? FileExplorerActivity)?.finish()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back_24dp),
                                contentDescription = "Back",
                                modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                            enabled = !isActionDialogVisible,
                            onClick = { showNewFileDialog.value = true }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_add_24dp),
                                contentDescription = "Add",
                                modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                            )
                        }
                    }
                }

                if (files.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.folder_is_empty),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.caption1
                        )
                    }
                } else {
                    items(files.size, key = { index -> files[index].absolutePath }) { index ->
                        val file = files[index]

                        val (itemType, itemIcon) = remember(file.isDirectory, file.extension) {
                            getItemTypeAndIcon(file)
                        }

                        val summaryState = remember { mutableStateOf<String?>("...") }
                        LaunchedEffect(file.absolutePath, file.lastModified(), file.length()) {
                            summaryState.value = withContext(Dispatchers.IO) {
                                try {
                                    if (file.isDirectory) {
                                        formatFolderSize(file)
                                    } else {
                                        formatFileSize(file.length())
                                    }
                                } catch (e: Exception) {
                                    Log.e("FileSizeCalc", "Error calculating size for ${file.name}", e)
                                    if (file.isDirectory) "N/A" else formatFileSize(file.length())
                                }
                            }
                        }

                        val lastModifiedText = remember(file.lastModified()) {
                            formatLastModified(file.lastModified())
                        }

                        CardItem(
                            file = file,
                            title = file.name,
                            summary = summaryState.value,
                            itemType = itemType,
                            itemIcon = itemIcon,
                            time = lastModifiedText,
                            onOpenFile = {
                                if (file.isDirectory) {
                                    if (file.canRead()) {
                                        onPathChange(file.absolutePath)
                                    } else {
                                        showDialog(getString(R.string.cannot_read_folder), true)
                                    }
                                } else {
                                    openFile(context, file) { msg, isErr -> showDialog(msg, isErr) }
                                }
                            },
                            onDeleteSwipe = { onDeleteSwipe(file) },
                            onShowActionsRequest = { onShowActionsRequest(file) }
                        )
                    }
                }
            }
        }

        if (showNewFileDialog.value) {
            NewFileDialog(
                onDismissRequest = { showNewFileDialog.value = false },
                onCreate = { name, isDirectory ->
                    if (name.isBlank()) {
                        showDialog(context.getString(R.string.name_cannot_be_empty), true)
                    } else {
                        onCreate(name, isDirectory)
                        showNewFileDialog.value = false
                    }
                }
            )
        }
    }

    private fun getItemTypeAndIcon(file: File): Pair<String, Int> {
        return when {
            file.isDirectory -> getString(R.string.folder) to R.drawable.ic_folder_24dp
            file.name.endsWith(".png", true) ||
                    file.name.endsWith(".jpg", true) ||
                    file.name.endsWith(".jpeg", true) ||
                    file.name.endsWith(".gif", true) ||
                    file.name.endsWith(".bmp", true) ||
                    file.name.endsWith(".webp", true)
                -> getString(R.string.image) to R.drawable.ic_image_24dp
            file.name.endsWith(".mp4", true) ||
                    file.name.endsWith(".avi", true) ||
                    file.name.endsWith(".mkv", true) ||
                    file.name.endsWith(".webm", true) ||
                    file.name.endsWith(".mov", true)
                -> getString(R.string.video) to R.drawable.ic_video_24dp
            file.name.endsWith(".mp3", true) ||
                    file.name.endsWith(".m4a", true) ||
                    file.name.endsWith(".wav", true) ||
                    file.name.endsWith(".ogg", true) ||
                    file.name.endsWith(".aac", true) ||
                    file.name.endsWith(".flac", true)
                -> getString(R.string.music) to R.drawable.ic_music_24dp
            file.name.endsWith(".apk", true) -> "APK" to R.drawable.ic_apk_24dp
            file.name.endsWith(".txt", true) ||
                    file.name.endsWith(".json", true) ||
                    file.name.endsWith(".xml", true) ||
                    file.name.endsWith(".log", true) ||
                    file.name.endsWith(".csv", true) ||
                    file.name.endsWith(".prop", true)
                -> getString(R.string.document) to R.drawable.ic_document_file_24dp
            file.name.endsWith(".zip", true) ||
                    file.name.endsWith(".rar", true) ||
                    file.name.endsWith(".7z", true) ||
                    file.name.endsWith(".tar", true) ||
                    file.name.endsWith(".gz", true)
                -> getString(R.string.archive) to R.drawable.ic_folder_zip_24dp
            else -> getString(R.string.file) to R.drawable.ic_draft_24dp
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun NewFileDialog(
        onDismissRequest: () -> Unit,
        onCreate: (String, Boolean) -> Unit
    ) {
        var name by remember { mutableStateOf("") }
        var isDirectory by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        Dialog(onDismissRequest = onDismissRequest) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(horizontal = 18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.new_file))
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.filter { char -> char != '/' && char != '\\' } },
                    textStyle = TextStyle(color = MaterialTheme.colors.onSurface, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onCreate(name, isDirectory)
                            keyboardController?.hide()
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colors.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .focusRequester(focusRequester),
                )

                LaunchedEffect(Unit) {
                    delay(200)
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }

                Spacer(modifier = Modifier.height(8.dp))
                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    checked = isDirectory,
                    onCheckedChange = { isDirectory = it },
                    label = { Text(stringResource(R.string.folder)) },
                    toggleControl = { Switch(checked = isDirectory) },
                    appIcon = { Icon(painter = painterResource(R.drawable.ic_folder_24dp), contentDescription = null)}
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { onCreate(name, isDirectory) },
                        modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_add_24dp), contentDescription = "Create")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_cancel_24dp), contentDescription = "Cancel")
                    }
                }
            }
        }
    }

    private suspend fun createNewFileOrDir(path: String, name: String, isDirectory: Boolean): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            val safeName = name.filter { it != '/' && it != '\\' }
            if (safeName.isBlank()) {
                return@withContext false to getString(R.string.name_cannot_be_empty)
            }
            val file = File(path, safeName)
            var success = false
            var message = ""
            try {
                if (file.exists()) {
                    message = getString(R.string.already_exists, safeName)
                } else {
                    success = if (isDirectory) {
                        file.mkdirs()
                    } else {
                        file.createNewFile()
                    }
                    message = if (success) {
                        getString(
                            R.string.folder_file_created,
                            if (isDirectory) getString(R.string.folder) else getString(R.string.file),
                            safeName
                        )
                    } else {
                        getString(
                            R.string.failed_to_create,
                            if (isDirectory) getString(R.string.folder_s) else getString(
                                R.string.file_s
                            )
                        )
                    }
                }
            } catch (e: IOException) {
                Log.e(Constants.TAG, "Error creating ${if (isDirectory) "folder" else "file"} '$safeName'", e)
                message = getString(R.string.error_msg, e.localizedMessage ?: getString(R.string.cannot_create))
                success = false
            } catch (e: SecurityException) {
                Log.e(Constants.TAG, "Security error creating ${if (isDirectory) "folder" else "file"} '$safeName'", e)
                message = getString(R.string.permission_denied)
                success = false
            }
            success to message
        }
    }

    fun loadFiles(path: String) {
        lifecycleScope.launch {
            val currentFile = File(path)
            var canLoad = true
            if (!currentFile.exists() || !currentFile.canRead()) {
                Log.e(Constants.TAG, "Cannot load files: Path '$path' does not exist or cannot be read.")
                showDialog(getString(R.string.cannot_access, currentFile.name), isError = true)

                if(path != rootPath) {
                    val parent = currentFile.parentFile
                    if (parent != null && parent.canRead()) {
                        currentPath.value = parent.absolutePath
                    } else {
                        currentPath.value = rootPath
                    }
                } else {
                    _fileList.value = emptyList()
                    canLoad = false
                }
                if(canLoad) loadFiles(currentPath.value)
                return@launch
            }

            _fileList.value = withContext(Dispatchers.IO) {
                try {
                    val files = currentFile.listFiles()
                    if (files == null) {
                        Log.w(Constants.TAG, "listFiles() returned null for path: $path")
                        withContext(Dispatchers.Main.immediate){
                            showDialog("Error reading folder contents", isError = true)
                        }
                        emptyList()
                    } else {
                        files.toList().sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))
                    }
                } catch (e: Exception) {
                    Log.e(Constants.TAG, "Error listing files for path: $path", e)
                    withContext(Dispatchers.Main.immediate) {
                        showDialog(getString(R.string.error_reading_folder, e.localizedMessage), isError = true)
                    }
                    emptyList()
                }
            }
        }
    }

    @Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
    @Composable
    fun DefaultPreview() {
        FileManagerApp()
    }

    private suspend fun deleteFileOrDirInternal(file: File): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            var deleted = false
            var message : String
            try {
                deleted = deleteRecursively(file)
                message = if (deleted) {
                    getString(R.string.deleted_n, file.name)
                } else {
                    getString(R.string.failed_to_delete_n, file.name)
                }
            } catch (e: Exception) {
                Log.e(Constants.TAG, "Error deleting file/folder: ${file.absolutePath}", e)
                message =
                    getString(R.string.error_deleting_e, e.localizedMessage ?: getString(R.string.unknown_error))
                deleted = false
            }
            deleted to message
        }
    }

    private fun deleteRecursively(fileOrDirectory: File): Boolean {
        try {
            if (fileOrDirectory.isDirectory) {
                val children = fileOrDirectory.listFiles()
                if (children != null) {
                    for (child in children) {
                        if (!deleteRecursively(child)) {
                            Log.w(Constants.TAG, "Failed to delete child: ${child.absolutePath}")
                            return false
                        }
                    }
                } else {
                    Log.w(Constants.TAG, "listFiles returned null for directory: ${fileOrDirectory.absolutePath}. Might be an access issue.")
                    return false
                }
            }
            return fileOrDirectory.delete()
        } catch (e: SecurityException) {
            Log.e(Constants.TAG, "SecurityException deleting ${fileOrDirectory.absolutePath}", e)
            return false
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Exception deleting ${fileOrDirectory.absolutePath}", e)
            return false
        }
    }

    private suspend fun formatFolderSize(folder: File): String {
        return try {
            val size = getFolderSize(folder)
            formatFileSize(size)
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error calculating size for ${folder.name}", e)
            "N/A"
        }
    }

    private suspend fun getFolderSize(folder: File): Long = withContext(Dispatchers.IO) {
        var totalSize: Long = 0
        try {
            val files = folder.listFiles()
            files?.forEach { file ->
                totalSize += if (file.isDirectory) {
                    getFolderSize(file)
                } else {
                    try { file.length() } catch (e: Exception) { 0L }
                }
            }
        } catch (e: Exception){
            Log.e(Constants.TAG, "Error listing files in ${folder.name}", e )
            totalSize = -1L
        }
        totalSize
    }

    @SuppressLint("DefaultLocale")
    private fun formatFileSize(size: Long): String {
        if (size < 0) return "N/A"
        if (size == 0L) return "0 B"
        val units = arrayOf(getString(R.string.b), getString(R.string.kb),
            getString(R.string.mb), getString(R.string.gb), getString(R.string.tb))
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt().coerceAtMost(units.size - 1)
        val safeDigitGroups = digitGroups.coerceAtLeast(0)
        val sizeInUnit = size / 1024.0.pow(safeDigitGroups.toDouble())

        return if (safeDigitGroups == 0) {
            String.format("%d %s", size.toInt(), units[safeDigitGroups])
        } else {
            String.format("%.1f %s", sizeInUnit, units[safeDigitGroups])
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun formatLastModified(lastModified: Long): String {
        val date = Date(lastModified)
        val today = Calendar.getInstance()
        val fileDate = Calendar.getInstance().apply { time = date }

        return when {
            isSameDay(today, fileDate) -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            isYesterday(today, fileDate) -> getString(R.string.yesterday)
            else -> SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(today: Calendar, date: Calendar): Boolean {
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(yesterday, date)
    }

    fun openFile(context: Context, file: File, showDialog: (message: String, isError: Boolean) -> Unit) {
        val intent : Intent? = when {
            file.name.endsWith(".apk", true) -> {
                installApk(context, file) { msg, isErr -> showDialog(msg, isErr) }
                null
            }

            file.name.endsWith(".txt", true) || file.name.endsWith(".json", true) || file.name.endsWith(".xml", true) || file.name.endsWith(".log", true) -> {
                Intent(context, TextEditorActivity::class.java).apply {
                    putExtra("filePath", file.absolutePath)
                }
            }

            file.name.endsWith(".png", true) || file.name.endsWith(".jpg", true) || file.name.endsWith(".jpeg", true) || file.name.endsWith(".gif", true) || file.name.endsWith(".bmp", true)-> {
                Intent(context, ImageActivity::class.java).apply {
                    putExtra("imagePath", file.absolutePath)
                }
            }

            file.name.endsWith(".mp4", true) || file.name.endsWith(".avi", true) || file.name.endsWith(".mkv", true) || file.name.endsWith(".webm", true) -> {
                Intent(context, VideoActivity::class.java).apply {
                    putExtra("videoPath", file.absolutePath)
                }
            }

            file.name.endsWith(".mp3", true) || file.name.endsWith(".m4a", true) || file.name.endsWith(".wav", true) || file.name.endsWith(".ogg", true) || file.name.endsWith(".aac", true)-> {
                Intent(context, MusicActivity::class.java).apply {
                    putExtra("musicPath", file.absolutePath)
                }
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
                    Log.e(Constants.TAG, "Error getting Uri for ${file.name}. Check FileProvider config.", e)
                    showDialog(getString(R.string.error_accessing_file_e, e.localizedMessage), true)
                    null
                }
                catch (e: Exception) {
                    Log.e(Constants.TAG, "Error creating generic intent for ${file.name}", e)
                    showDialog(getString(R.string.cannot_open_file_type), true)
                    null
                }
            }
        }
        try {
            intent?.let {
                Log.d(Constants.TAG, "Attempting to start activity for: ${file.name} with intent: $it")
                context.startActivity(it)
            }
        } catch (e: android.content.ActivityNotFoundException) {
            Log.e(Constants.TAG, "ActivityNotFoundException for opening file: ${file.name}", e)
            showDialog(getString(R.string.no_app_installed_to_open_this_file_type), true)
        }
        catch (e: Exception) {
            Log.e(Constants.TAG, "Generic error opening file: ${file.name}", e)
            showDialog(getString(R.string.error_opening_file_e, e.localizedMessage), true)
        }
    }

    private val renameFileLauncher = registerForActivityResult(RenameContract()) { success ->
        if (success) {
            Log.d(Constants.TAG, "Rename successful, reloading list.")
            loadFiles(currentPath.value)
        } else {
            Log.d(Constants.TAG, "Rename cancelled or failed.")
        }
    }

    @SuppressLint("WearRecents")
    private fun installApk(context: Context, file: File, showDialog: (message: String, isError: Boolean) -> Unit) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            Log.d(Constants.TAG, "Attempting to start installer for: ${file.name} with URI: $uri")
            context.startActivity(installIntent)
        } catch (e: IllegalArgumentException) {
            Log.e(Constants.TAG, "Error getting Uri for APK ${file.name}. Check FileProvider config.", e)
            showDialog(getString(R.string.error_accessing_apk_e, e.localizedMessage), true)
        }
        catch (e: Exception) {
            Log.e(Constants.TAG, "Error trying to install APK ${file.name}", e)
            showDialog(getString(R.string.failed_to_start_package_installer), true)
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class, ExperimentalWearFoundationApi::class)
@Composable
fun CardItem(
    file: File,
    title: String,
    summary: String?,
    itemType: String,
    itemIcon: Int,
    time: String,
    onOpenFile: () -> Unit,
    onDeleteSwipe: () -> Unit,
    onShowActionsRequest: () -> Unit
) {
    val revealState = rememberRevealState()

    SwipeToRevealCard(
        modifier = Modifier.fillMaxWidth(),
        revealState = revealState,
        primaryAction = {
            SwipeToRevealPrimaryAction(
                revealState = revealState,
                icon = { Icon(painter = painterResource(R.drawable.ic_delete_24dp), "Delete") },
                label = { Text(stringResource(R.string.delete)) },
                onClick = { onDeleteSwipe() }
            )
        },
        secondaryAction = {
            SwipeToRevealSecondaryAction(
                revealState = revealState,
                onClick = onShowActionsRequest
            ) {
                Icon(painter = painterResource(R.drawable.ic_more_vert_24dp), contentDescription = "Actions")
            }
        },
        onFullSwipe = { onDeleteSwipe() },
        content = {
            AppCard(
                onClick = onOpenFile,
                modifier = Modifier.fillMaxWidth(),
                appName = { Text(itemType, maxLines = 1) },
                appImage = {
                    Icon(
                        painter = painterResource(id = itemIcon),
                        contentDescription = itemType,
                        modifier = Modifier.size(20.dp)
                    )
                },
                title = { Text(title, maxLines = 2) },
                time = { Text(time) }
            ) {
                summary?.let { Text(it, style = MaterialTheme.typography.body2, maxLines = 1) }
            }
        }
    )
}

@Composable
fun FileActionsDialog(
    listState: ScalingLazyListState,
    file: File,
    onDismissRequest: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        LaunchedEffect(Unit) {
            delay(50)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Log.w("DialogFocus", "RequestFocus failed: $e")
            }
        }
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .background(MaterialTheme.colors.background)
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        listState.scrollBy(it.verticalScrollPixels)
                    }
                    true
                }
                .focusable(),
            state = listState,
            anchorType = ScalingLazyListAnchorType.ItemCenter,
        ) {
            item {
                ListHeader {
                    Text(
                        text = file.name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            item { ActionChip(iconResId = R.drawable.ic_copy_24dp, label = stringResource(R.string.copy), onClick = onCopy) }
            item { ActionChip(iconResId = R.drawable.ic_cut_24dp, label = stringResource(R.string.cut), onClick = onCut) }
            item { ActionChip(iconResId = R.drawable.ic_edit_24dp, label = stringResource(R.string.rename), onClick = onRename) }
            if (!file.isDirectory) {
                item { ActionChip(iconResId = R.drawable.ic_share_24dp, label = stringResource(R.string.share), onClick = onShare) }
            }
            item { ActionChip(iconResId = R.drawable.ic_delete_24dp, label = stringResource(R.string.delete), onClick = onDelete, isDestructive = true) }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Icon(painter = painterResource(R.drawable.ic_cancel_24dp), contentDescription = "Cancel")
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    iconResId: Int,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Chip(
        modifier = Modifier
            .fillMaxWidth(),
        icon = { Icon(painterResource(id = iconResId), contentDescription = null) },
        label = { Text(label) },
        onClick = onClick,
        colors = if (isDestructive) ChipDefaults.primaryChipColors(
            backgroundColor = MaterialTheme.colors.error,
            contentColor = MaterialTheme.colors.onError
        ) else ChipDefaults.secondaryChipColors()
    )
}