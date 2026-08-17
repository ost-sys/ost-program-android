package com.ost.application.explorer
import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.ost.application.core.share.Constants
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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.rememberRevealState
import androidx.wear.tooling.preview.devices.WearDevices
import com.ost.application.R
import com.ost.application.explorer.music.MusicActivity
import com.ost.application.explorer.pdfreader.PdfReaderActivity
import com.ost.application.share.ShareActivity
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.FailDialog
import com.ost.application.util.ThumbnailCache
import com.ost.application.util.SuccessDialog
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
data class FileDialogInfo(
    val message: String,
    val isError: Boolean = false,
    val actionIconResId: Int = R.drawable.ic_check_circle_24dp
)
enum class ClipboardOperation { COPY, CUT }
data class ClipboardState(val files: Set<File>, val operation: ClipboardOperation)
class FileExplorerActivity : ComponentActivity() {
    private val rootPath = Environment.getExternalStorageDirectory().absolutePath
    private val currentPath = mutableStateOf(rootPath)
    private val explorerPrefs by lazy { getSharedPreferences("explorer_prefs", MODE_PRIVATE) }
    private val showHiddenFiles = mutableStateOf(false)
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
                Log.e("Permission", "Not all permissions granted.")
                dialogInfo.value = FileDialogInfo(getString(R.string.storage_permissions_denied), isError = true)
                loadFiles(currentPath.value)
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHiddenFiles.value = explorerPrefs.getBoolean("show_hidden", false)
        setContent { FileManagerApp() }
        checkAndRequestPermissions()
    }
    private fun setShowHiddenFiles(show: Boolean) {
        showHiddenFiles.value = show
        explorerPrefs.edit().putBoolean("show_hidden", show).apply()
        loadFiles(currentPath.value)
    }
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else if (!Environment.isExternalStorageManager()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            Log.d("Permission", "MANAGE_EXTERNAL_STORAGE granted.")
        }
        if (permissionsToRequest.isNotEmpty()) requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        else {
            Log.d("Permission", "Permissions already granted or not needed.")
            loadFiles(currentPath.value)
        }
    }
    private fun showActionsForFile(file: File) { _showActionsDialogForFile.value = file }
    private fun dismissActionsDialog() { _showActionsDialogForFile.value = null }
    private fun clearClipboard() { _clipboardState.value = null }
    private fun copyFile(file: File) {
        if (file.exists()) {
            _clipboardState.value = ClipboardState(files = setOf(file.absoluteFile), operation = ClipboardOperation.COPY)
            dialogInfo.value = FileDialogInfo(
                message = getString(R.string.copied_file, file.name),
                isError = false,
                actionIconResId = R.drawable.ic_copy_24dp
            )
            dismissActionsDialog()
        } else {
            dialogInfo.value = FileDialogInfo(getString(R.string.error_file_not_found), isError = true)
            dismissActionsDialog()
        }
    }
    private fun cutFile(file: File) {
        if (file.exists()) {
            _clipboardState.value = ClipboardState(files = setOf(file.absoluteFile), operation = ClipboardOperation.CUT)
            dialogInfo.value = FileDialogInfo(
                message = getString(R.string.cut_file, file.name),
                isError = false,
                actionIconResId = R.drawable.ic_cut_24dp
            )
            dismissActionsDialog()
        } else {
            dialogInfo.value = FileDialogInfo(getString(R.string.error_file_not_found), isError = true)
            dismissActionsDialog()
        }
    }
    private fun deleteFile(file: File) {
        dismissActionsDialog()
        lifecycleScope.launch {
            val (deleted, message) = deleteFileOrDirInternal(file)
            dialogInfo.value = FileDialogInfo(
                message = message,
                isError = !deleted,
                actionIconResId = R.drawable.ic_delete_24dp
            )
            if (deleted) loadFiles(currentPath.value)
        }
    }
    private fun shareFile(context: Context, file: File) {
        dismissActionsDialog()
        if (file.isDirectory) {
            dialogInfo.value = FileDialogInfo(getString(R.string.cannot_share_folders), isError = true)
            return
        }
        if (!file.exists()) {
            dialogInfo.value = FileDialogInfo(getString(R.string.error_file_not_found), isError = true)
            return
        }
        try {
            val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val uris = ArrayList<Uri>().apply { add(fileUri) }
            val intent = Intent(context, ShareActivity::class.java).apply {
                action = "com.ost.application.action.SEND_FILES"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error sharing ${file.name}", e)
            dialogInfo.value = FileDialogInfo(getString(R.string.error_preparing_file_for_sharing), isError = true)
        }
    }

    private fun shareFileLocalSend(context: Context, file: File) {
        dismissActionsDialog()
        if (file.isDirectory) {
            dialogInfo.value = FileDialogInfo(getString(R.string.cannot_share_folders), isError = true)
            return
        }
        if (!file.exists()) {
            dialogInfo.value = FileDialogInfo(getString(R.string.error_file_not_found), isError = true)
            return
        }
        try {
            val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val uris = ArrayList<Uri>().apply { add(fileUri) }
            val intent = Intent().apply {
                action = "com.ost.application.action.LOCALSEND_SEND_FILES"
                setClassName(context.packageName, "com.ost.application.share.LocalSendActivity")
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error sharing via LocalSend ${file.name}", e)
            dialogInfo.value = FileDialogInfo(getString(R.string.error_preparing_file_for_sharing), isError = true)
        }
    }
    private fun renameFile(file: File) {
        dismissActionsDialog()
        renameFileLauncher.launch(file.absolutePath)
    }
    private fun pasteFiles() {
        val state = _clipboardState.value
        if (state == null || state.files.isEmpty()) {
            dialogInfo.value = FileDialogInfo(getString(R.string.clipboard_is_empty), isError = true)
            return
        }
        val destinationPath = currentPath.value
        val filesToProcess = state.files
        val operation = state.operation
        lifecycleScope.launch {
            var successCount = 0
            var errorCount = 0
            withContext(Dispatchers.IO) {
                filesToProcess.forEach { sourceFile ->
                    if (!sourceFile.exists()) {
                        errorCount++
                        return@forEach
                    }
                    var targetFile = File(destinationPath, sourceFile.name)
                    if (targetFile.exists()) {
                        if (operation == ClipboardOperation.CUT &&
                            sourceFile.absolutePath == targetFile.absolutePath) {
                            errorCount++
                            return@forEach
                        }
                        val baseName = targetFile.nameWithoutExtension
                        val extension = targetFile.extension.let { if (it.isNotEmpty()) ".$it" else "" }
                        var counter = 1
                        while (targetFile.exists() && counter <= 1000) {
                            targetFile = File(destinationPath, "$baseName ($counter)$extension")
                            counter++
                        }
                        if (counter > 1000) {
                            errorCount++
                            return@forEach
                        }
                    }
                    try {
                        when (operation) {
                            ClipboardOperation.COPY -> {
                                if (sourceFile.isDirectory) sourceFile.copyRecursively(targetFile, overwrite = false)
                                else sourceFile.copyTo(targetFile, overwrite = false)
                            }
                            ClipboardOperation.CUT -> {
                                if (!sourceFile.renameTo(targetFile)) {
                                    if (sourceFile.isDirectory) sourceFile.copyRecursively(targetFile, overwrite = false)
                                    else sourceFile.copyTo(targetFile, overwrite = false)
                                    if (targetFile.exists()) {
                                        if (!deleteRecursively(sourceFile))
                                            throw IOException("Failed to delete source after copy during CUT")
                                    } else {
                                        throw IOException("Copy failed during CUT operation")
                                    }
                                }
                            }
                        }
                        successCount++
                    } catch (e: Exception) {
                        Log.e(Constants.TAG, "[PasteAction] Error: ${sourceFile.name} → ${targetFile.name}", e)
                        errorCount++
                        if (targetFile.exists()) deleteRecursively(targetFile)
                    }
                }
            }
            if (operation == ClipboardOperation.CUT && errorCount == 0) clearClipboard()
            val isError = errorCount > 0
            val message = when {
                errorCount == 0 -> getString(R.string.paste_success, successCount)
                successCount == 0 -> getString(R.string.paste_failed, errorCount)
                else -> getString(R.string.paste_partial, successCount, errorCount)
            }
            dialogInfo.value = FileDialogInfo(
                message = message,
                isError = isError,
                actionIconResId = R.drawable.ic_paste_24dp
            )
            loadFiles(currentPath.value)
        }
    }
    @Composable
    fun FileManagerApp() {
        val listState = rememberScalingLazyListState()
        val files = fileList.collectAsState().value
        val currentDialogInfo by dialogInfo
        val currentActionsDialogFile by _showActionsDialogForFile
        var showExplorerSettings by remember { mutableStateOf(false) }
        OSTToolsTheme {
            if (showExplorerSettings) {
                ExplorerSettingsDialog(
                    showHidden = showHiddenFiles.value,
                    onShowHiddenChange = { setShowHiddenFiles(it) },
                    onDismissRequest = { showExplorerSettings = false }
                )
            }
            AppScaffold(timeText = { TimeText() }) {
                ScreenScaffold(
                    scrollState = listState,
                    contentPadding = PaddingValues(10.dp),
                    edgeButton = {
                        EdgeButton(onClick = { showExplorerSettings = true }) {
                            Icon(painterResource(R.drawable.ic_settings_24dp), "Settings")
                        }
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FileList(
                            modifier = Modifier.fillMaxSize(),
                            path = currentPath.value,
                            files = files,
                            listState = listState,
                            showDialog = { msg, isErr ->
                                dialogInfo.value = FileDialogInfo(msg, isErr)
                            },
                            onPathChange = { newPath ->
                                currentPath.value = newPath
                                loadFiles(newPath)
                            },
                            onCreate = { name, isDirectory ->
                                lifecycleScope.launch {
                                    val (created, message) = createNewFileOrDir(currentPath.value, name, isDirectory)
                                    dialogInfo.value = FileDialogInfo(
                                        message = message,
                                        isError = !created,
                                        actionIconResId = R.drawable.ic_add_24dp
                                    )
                                    if (created) loadFiles(currentPath.value)
                                }
                            },
                            onShowActionsRequest = ::showActionsForFile,
                            onDeleteSwipe = { fileToDelete ->
                                lifecycleScope.launch {
                                    val (deleted, message) = deleteFileOrDirInternal(fileToDelete)
                                    dialogInfo.value = FileDialogInfo(
                                        message = message,
                                        isError = !deleted,
                                        actionIconResId = R.drawable.ic_delete_24dp
                                    )
                                    if (deleted) loadFiles(currentPath.value)
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
                                } else false
                            }
                        )
                        AnimatedVisibility(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            visible = showPasteButton,
                            enter = slideInVertically { it / 2 } + fadeIn(),
                            exit = slideOutVertically { it / 2 } + fadeOut()
                        ) {
                            EdgeButton(onClick = { pasteFiles() }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_paste_24dp),
                                    contentDescription = "Paste",
                                    modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                                )
                            }
                        }
                    }
                }
            }
            currentActionsDialogFile?.let { fileToShowActionsFor ->
                FileActionsDialog(
                    file = fileToShowActionsFor,
                    onDismissRequest = ::dismissActionsDialog,
                    onCopy = { copyFile(fileToShowActionsFor) },
                    onCut = { cutFile(fileToShowActionsFor) },
                    onRename = { renameFile(fileToShowActionsFor) },
                    onDelete = { deleteFile(fileToShowActionsFor) },
                    onShare = { shareFile(this, fileToShowActionsFor) },
                    onShareLocalSend = { shareFileLocalSend(this, fileToShowActionsFor) }
                )
            }
            currentDialogInfo?.let { info ->
                if (info.isError) {
                    FailDialog(
                        message = info.message,
                        iconResId = R.drawable.ic_error_24dp,
                        onDismiss = { dialogInfo.value = null },
                        showDialog = true
                    )
                } else {
                    SuccessDialog(
                        actionIconResId = info.actionIconResId,
                        onDismiss = { dialogInfo.value = null },
                        showDialog = true
                    )
                }
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
                try { focusRequester.requestFocus() }
                catch (e: Exception) { Log.w("FileListFocus", "RequestFocus failed: $e") }
            }
        }
        Crossfade(targetState = path, label = "FileListTransition") { currentDisplayPath ->
            ScalingLazyColumn(
                modifier = modifier,
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
                            colors = ButtonDefaults.filledTonalButtonColors(),
                            enabled = currentDisplayPath != rootPath && !isActionDialogVisible,
                            onClick = { if (!onNavigateBack()) (context as? FileExplorerActivity)?.finish() }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back_24dp),
                                contentDescription = "Back",
                                modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            enabled = !isActionDialogVisible,
                            onClick = { showNewFileDialog.value = true },
                            shape = RoundedCornerShape(16.dp)
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
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                } else {
                    items(files.size, key = { index -> files[index].absolutePath }) { index ->
                        val file = files[index]
                        val position = when {
                            files.size == 1 -> CardPosition.SINGLE
                            index == 0 -> CardPosition.TOP
                            index == files.lastIndex -> CardPosition.BOTTOM
                            else -> CardPosition.MIDDLE
                        }
                        val (itemType, itemIcon) = remember(file.isDirectory, file.extension) {
                            getItemTypeAndIcon(file)
                        }
                        val summaryState = remember { mutableStateOf<String?>("...") }
                        LaunchedEffect(file.absolutePath, file.lastModified(), file.length()) {
                            summaryState.value = withContext(Dispatchers.IO) {
                                try {
                                    if (file.isDirectory) formatFolderSize(file)
                                    else formatFileSize(file.length())
                                } catch (e: Exception) {
                                    Log.e("FileSizeCalc", "Error for ${file.name}", e)
                                    if (file.isDirectory) "N/A" else formatFileSize(file.length())
                                }
                            }
                        }
                        val lastModifiedText = remember(file.lastModified()) { formatLastModified(file.lastModified()) }
                        val thumbnailState = remember(file.absolutePath) { mutableStateOf<android.graphics.Bitmap?>(null) }
                        LaunchedEffect(file.absolutePath, file.lastModified(), file.length()) {
                            thumbnailState.value = ThumbnailCache.load(context, file)
                        }
                        CardItem(
                            title = file.name,
                            summary = summaryState.value,
                            position = position,
                            itemType = itemType,
                            itemIcon = itemIcon,
                            time = lastModifiedText,
                            thumbnail = thumbnailState.value,
                            onOpenFile = {
                                if (file.isDirectory) {
                                    if (file.canRead()) onPathChange(file.absolutePath)
                                    else showDialog(getString(R.string.cannot_read_folder), true)
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
                    if (name.isBlank()) showDialog(context.getString(R.string.name_cannot_be_empty), true)
                    else {
                        onCreate(name, isDirectory)
                        showNewFileDialog.value = false
                    }
                }
            )
        }
    }
    private fun getItemTypeAndIcon(file: File): Pair<String, Int> {
        val ext = file.extension.lowercase(Locale.ROOT)
        return when {
            file.isDirectory -> getString(R.string.folder) to R.drawable.ic_folder_24dp
            ext in setOf("png", "jpg", "jpeg", "gif", "bmp", "webp", "heic") ->
                getString(R.string.image) to R.drawable.ic_image_24dp
            ext in setOf("mp4", "avi", "mkv", "webm", "mov") ->
                getString(R.string.video) to R.drawable.ic_video_24dp
            ext in setOf("mp3", "m4a", "wav", "ogg", "aac", "flac") ->
                getString(R.string.music) to R.drawable.ic_music_24dp
            ext == "apk" -> "APK" to R.drawable.ic_apk_24dp
            ext in setOf("txt", "json", "xml", "log", "csv", "prop") ->
                getString(R.string.document) to R.drawable.ic_document_file_24dp
            ext in setOf("zip", "rar", "7z", "tar", "gz") ->
                getString(R.string.archive) to R.drawable.ic_folder_zip_24dp
            ext == "pdf" -> "PDF" to R.drawable.ic_picture_as_pdf_24dp
            else -> getString(R.string.file) to R.drawable.ic_draft_24dp
        }
    }
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun NewFileDialog(onDismissRequest: () -> Unit, onCreate: (String, Boolean) -> Unit) {
        var name by remember { mutableStateOf("") }
        var isDirectory by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        Dialog(onDismissRequest = onDismissRequest) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.new_file))
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.filter { char -> char != '/' && char != '\\' } },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onCreate(name, isDirectory); keyboardController?.hide() }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                LaunchedEffect(Unit) {
                    delay(200)
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
                Spacer(modifier = Modifier.height(8.dp))
                SwitchButton(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    checked = isDirectory,
                    onCheckedChange = { isDirectory = it },
                    label = { Text(stringResource(R.string.folder)) },
                    icon = { Icon(painterResource(R.drawable.ic_folder_24dp), "folder") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(onClick = onDismissRequest, colors = ButtonDefaults.filledTonalButtonColors()) {
                        Icon(painter = painterResource(R.drawable.ic_cancel_24dp), contentDescription = "Cancel")
                    }
                    Spacer(modifier = Modifier.size(6.dp))
                    Button(onClick = { onCreate(name, isDirectory) }, shape = RoundedCornerShape(16.dp)) {
                        Icon(painter = painterResource(R.drawable.ic_add_24dp), contentDescription = "Create")
                    }
                }
            }
        }
    }
    private suspend fun createNewFileOrDir(path: String, name: String, isDirectory: Boolean): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            val safeName = name.filter { it != '/' && it != '\\' }
            if (safeName.isBlank()) return@withContext false to getString(R.string.name_cannot_be_empty)
            val file = File(path, safeName)
            try {
                if (file.exists()) {
                    false to getString(R.string.already_exists, safeName)
                } else {
                    val success = if (isDirectory) file.mkdirs() else file.createNewFile()
                    val typeStr = if (isDirectory) getString(R.string.folder) else getString(R.string.file)
                    val typeStrS = if (isDirectory) getString(R.string.folder_s) else getString(R.string.file_s)
                    if (success) true to getString(R.string.folder_file_created, typeStr, safeName)
                    else false to getString(R.string.failed_to_create, typeStrS)
                }
            } catch (e: IOException) {
                Log.e(Constants.TAG, "Error creating '$safeName'", e)
                false to getString(R.string.error_msg, e.localizedMessage ?: getString(R.string.cannot_create))
            } catch (e: SecurityException) {
                Log.e(Constants.TAG, "Security error creating '$safeName'", e)
                false to getString(R.string.permission_denied)
            }
        }
    }
    fun loadFiles(path: String) {
        lifecycleScope.launch {
            val currentFile = File(path)
            if (!currentFile.exists() || !currentFile.canRead()) {
                Log.e(Constants.TAG, "Cannot load: '$path'")
                dialogInfo.value = FileDialogInfo(getString(R.string.cannot_access, currentFile.name), isError = true)
                if (path != rootPath) {
                    val parent = currentFile.parentFile
                    val fallback = if (parent != null && parent.canRead()) parent.absolutePath else rootPath
                    currentPath.value = fallback
                    loadFiles(fallback)
                } else {
                    _fileList.value = emptyList()
                }
                return@launch
            }
            _fileList.value = withContext(Dispatchers.IO) {
                try {
                    val files = currentFile.listFiles()
                    if (files == null) {
                        withContext(Dispatchers.Main.immediate) {
                            dialogInfo.value = FileDialogInfo(getString(R.string.error_reading_folder, "null"), isError = true)
                        }
                        emptyList()
                    } else {
                        files.toList()
                            .filter { showHiddenFiles.value || !it.name.startsWith(".") }
                            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))
                    }
                } catch (e: Exception) {
                    Log.e(Constants.TAG, "Error listing files for: $path", e)
                    withContext(Dispatchers.Main.immediate) {
                        dialogInfo.value = FileDialogInfo(getString(R.string.error_reading_folder, e.localizedMessage), isError = true)
                    }
                    emptyList()
                }
            }
        }
    }
    @Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
    @Composable
    fun DefaultPreview() { FileManagerApp() }
    private suspend fun deleteFileOrDirInternal(file: File): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val deleted = deleteRecursively(file)
                val message = if (deleted) getString(R.string.deleted_n, file.name)
                else getString(R.string.failed_to_delete_n, file.name)
                deleted to message
            } catch (e: Exception) {
                Log.e(Constants.TAG, "Error deleting: ${file.absolutePath}", e)
                false to getString(R.string.error_deleting_e, e.localizedMessage ?: getString(R.string.unknown_error))
            }
        }
    }
    private fun deleteRecursively(fileOrDirectory: File): Boolean {
        return try {
            if (fileOrDirectory.isDirectory) {
                val children = fileOrDirectory.listFiles()
                if (children == null) {
                    Log.w(Constants.TAG, "listFiles returned null for: ${fileOrDirectory.absolutePath}")
                    return false
                }
                for (child in children) {
                    if (!deleteRecursively(child)) return false
                }
            }
            fileOrDirectory.delete()
        } catch (e: SecurityException) {
            Log.e(Constants.TAG, "SecurityException deleting ${fileOrDirectory.absolutePath}", e)
            false
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Exception deleting ${fileOrDirectory.absolutePath}", e)
            false
        }
    }
    private suspend fun formatFolderSize(folder: File): String {
        return try { formatFileSize(getFolderSize(folder, depth = 0)) }
        catch (e: Exception) { Log.e(Constants.TAG, "Error calculating size for ${folder.name}", e); "N/A" }
    }
    private suspend fun getFolderSize(folder: File, depth: Int): Long = withContext(Dispatchers.IO) {
        if (depth > 20) {
            Log.w(Constants.TAG, "getFolderSize: max depth at ${folder.absolutePath}")
            return@withContext 0L
        }
        var totalSize = 0L
        try {
            folder.listFiles()?.forEach { file ->
                totalSize += if (file.isDirectory) getFolderSize(file, depth + 1)
                else try { file.length() } catch (e: Exception) { 0L }
            }
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error listing files in ${folder.name}", e)
        }
        totalSize
    }
    @SuppressLint("DefaultLocale")
    private fun formatFileSize(size: Long): String {
        if (size < 0) return "N/A"
        if (size == 0L) return "0 B"
        val units = arrayOf(getString(R.string.b), getString(R.string.kb),
            getString(R.string.mb), getString(R.string.gb), getString(R.string.tb))
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val sizeInUnit = size / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) String.format("%d %s", size.toInt(), units[digitGroups])
        else String.format("%.1f %s", sizeInUnit, units[digitGroups])
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
    private fun isSameDay(cal1: Calendar, cal2: Calendar) =
        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    private fun isYesterday(today: Calendar, date: Calendar): Boolean {
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(yesterday, date)
    }
    fun openFile(context: Context, file: File, showDialog: (message: String, isError: Boolean) -> Unit) {
        if (file.extension.lowercase(Locale.ROOT) == "apk") {
            installApk(context, file) { msg, isErr -> showDialog(msg, isErr) }
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mimeType = android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(file.extension.lowercase(Locale.ROOT))
                ?: context.contentResolver.getType(uri)
                ?: "*/*"
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(viewIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Log.e(Constants.TAG, "ActivityNotFoundException for: ${file.name}", e)
            showDialog(getString(R.string.no_app_installed_to_open_this_file_type), true)
        } catch (e: IllegalArgumentException) {
            Log.e(Constants.TAG, "Error getting Uri for ${file.name}", e)
            showDialog(getString(R.string.error_accessing_file_e, e.localizedMessage), true)
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error opening file: ${file.name}", e)
            showDialog(getString(R.string.error_opening_file_e, e.localizedMessage), true)
        }
    }
    private val renameFileLauncher = registerForActivityResult(RenameContract()) { success ->
        if (success) loadFiles(currentPath.value)
    }
    @SuppressLint("WearRecents")
    private fun installApk(context: Context, file: File, showDialog: (message: String, isError: Boolean) -> Unit) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: IllegalArgumentException) {
            Log.e(Constants.TAG, "Error getting Uri for APK ${file.name}", e)
            showDialog(getString(R.string.error_accessing_apk_e, e.localizedMessage), true)
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error installing APK ${file.name}", e)
            showDialog(getString(R.string.failed_to_start_package_installer), true)
        }
    }
}
@Composable
fun ExplorerSettingsDialog(
    showHidden: Boolean,
    onShowHiddenChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        val listState = rememberScalingLazyListState()
        AppScaffold(timeText = { TimeText() }) {
            ScreenScaffold(
                scrollState = listState,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    anchorType = ScalingLazyListAnchorType.ItemCenter
                ) {
                    item { ListHeader { Text(stringResource(R.string.settings)) } }
                    item {
                        SwitchButton(
                            checked = showHidden,
                            onCheckedChange = onShowHiddenChange,
                            label = { Text(stringResource(R.string.show_hidden_files)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun CardItem(
    title: String,
    summary: String?,
    itemType: String,
    itemIcon: Int,
    time: String,
    position: CardPosition,
    thumbnail: android.graphics.Bitmap? = null,
    onOpenFile: () -> Unit,
    onDeleteSwipe: () -> Unit,
    onShowActionsRequest: () -> Unit
) {
    val revealState = rememberRevealState()
    val largeCornerRadius = 24.dp
    val smallCornerRadius = 4.dp
    val shape = when (position) {
        CardPosition.TOP -> RoundedCornerShape(topStart = largeCornerRadius, topEnd = largeCornerRadius, bottomStart = smallCornerRadius, bottomEnd = smallCornerRadius)
        CardPosition.MIDDLE -> RoundedCornerShape(smallCornerRadius)
        CardPosition.BOTTOM -> RoundedCornerShape(topStart = smallCornerRadius, topEnd = smallCornerRadius, bottomStart = largeCornerRadius, bottomEnd = largeCornerRadius)
        CardPosition.SINGLE -> RoundedCornerShape(largeCornerRadius)
    }
    SwipeToReveal(
        modifier = Modifier.fillMaxWidth(),
        revealState = revealState,
        primaryAction = {
            PrimaryActionButton(
                icon = { Icon(painter = painterResource(R.drawable.ic_delete_24dp), "Delete") },
                text = { Text(stringResource(R.string.delete)) },
                onClick = { onDeleteSwipe() }
            )
        },
        secondaryAction = {
            SecondaryActionButton(
                onClick = onShowActionsRequest,
                icon = { Icon(painter = painterResource(R.drawable.ic_more_vert_24dp), contentDescription = "Actions") }
            )
        },
        content = {
            Box(modifier = Modifier.fillMaxWidth().clip(shape)) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.85f),
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Black.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )
                }
                AppCard(
                    onClick = onOpenFile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    colors = if (thumbnail != null) {
                        CardDefaults.cardColors(
                            containerColor = Color.Transparent,
                            appNameColor = Color.White.copy(alpha = 0.85f),
                            titleColor = Color.White,
                            contentColor = Color.White.copy(alpha = 0.85f),
                            timeColor = Color.White.copy(alpha = 0.7f)
                        )
                    } else {
                        CardDefaults.cardColors()
                    },
                    appName = { Text(itemType, maxLines = 1) },
                    appImage = {
                        Icon(
                            painter = painterResource(id = itemIcon),
                            contentDescription = itemType,
                            modifier = Modifier.size(20.dp),
                            tint = if (thumbnail != null) Color.White else MaterialTheme.colorScheme.primary
                        )
                    },
                    title = { Text(title, maxLines = 2) },
                    time = { Text(time) }
                ) {
                    summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1) }
                }
            }
        },
        onSwipePrimaryAction = { onDeleteSwipe() }
    )
}
@Composable
fun FileActionsDialog(
    file: File,
    onDismissRequest: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onShareLocalSend: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismissRequest) {
        LaunchedEffect(Unit) {
            delay(50)
            try { focusRequester.requestFocus() }
            catch (e: Exception) { Log.w("DialogFocus", "RequestFocus failed: $e") }
        }
        AppScaffold {
            val listState = rememberScalingLazyListState()
            ScreenScaffold(
                scrollState = listState,
                contentPadding = PaddingValues(10.dp),
                edgeButton = {
                    EdgeButton(onClick = onDismissRequest) {
                        Icon(painter = painterResource(R.drawable.ic_cancel_24dp), contentDescription = null)
                    }
                }
            ) {
                ScalingLazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background),
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
                    item { ActionChip(iconResId = R.drawable.ic_copy_24dp, label = stringResource(R.string.copy), position = CardPosition.TOP, onClick = onCopy) }
                    item { ActionChip(iconResId = R.drawable.ic_cut_24dp, label = stringResource(R.string.cut), position = CardPosition.MIDDLE, onClick = onCut) }
                    item { ActionChip(iconResId = R.drawable.ic_edit_24dp, label = stringResource(R.string.rename), position = CardPosition.MIDDLE, onClick = onRename) }
                    if (!file.isDirectory) {
                        item { ActionChip(iconResId = R.drawable.ic_share_24dp, label = stringResource(R.string.share), position = CardPosition.MIDDLE, onClick = onShare) }
                        item { ActionChip(iconResId = R.drawable.ic_share_24dp, label = "LocalSend", position = CardPosition.MIDDLE, onClick = onShareLocalSend) }
                    }
                    item { ActionChip(iconResId = R.drawable.ic_delete_24dp, label = stringResource(R.string.delete), onClick = onDelete, position = CardPosition.BOTTOM, isDestructive = true) }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}
@Composable
private fun ActionChip(
    iconResId: Int,
    label: String,
    position: CardPosition,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val largeCornerRadius = 24.dp
    val smallCornerRadius = 4.dp
    val shape = when (position) {
        CardPosition.TOP -> RoundedCornerShape(topStart = largeCornerRadius, topEnd = largeCornerRadius, bottomStart = smallCornerRadius, bottomEnd = smallCornerRadius)
        CardPosition.MIDDLE -> RoundedCornerShape(smallCornerRadius)
        CardPosition.BOTTOM -> RoundedCornerShape(topStart = smallCornerRadius, topEnd = smallCornerRadius, bottomStart = largeCornerRadius, bottomEnd = largeCornerRadius)
        CardPosition.SINGLE -> RoundedCornerShape(largeCornerRadius)
    }
    Chip(
        modifier = Modifier.fillMaxWidth(),
        icon = { Icon(painterResource(id = iconResId), contentDescription = null) },
        label = { Text(label) },
        shape = shape,
        onClick = onClick,
        colors = if (isDestructive) ChipDefaults.primaryChipColors(
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) else ChipDefaults.secondaryChipColors()
    )
}
