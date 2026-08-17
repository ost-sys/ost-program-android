package com.ost.application.explorer
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.FailDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
private val EDITABLE_EXTENSIONS = setOf(
    "txt", "md", "markdown", "log", "json", "xml", "yaml", "yml",
    "toml", "ini", "cfg", "conf", "properties", "csv", "tsv",
    "html", "htm", "css", "js", "ts", "kt", "java", "py", "rb",
    "sh", "bash", "zsh", "bat", "ps1", "c", "cpp", "h", "hpp",
    "rs", "go", "swift", "dart", "sql", "gradle", "kts", "gitignore"
)
private const val MAX_EDITABLE_BYTES = 1L * 1024 * 1024
private const val PREVIEW_BYTES = 128 * 1024
data class EditorDialogState(val message: String, val isError: Boolean)

private sealed class DocumentSource {
    abstract fun displayName(context: Context): String
    abstract fun sizeBytes(context: Context): Long
    abstract fun read(context: Context, maxBytes: Int?): String
    abstract fun write(context: Context, content: String)
    abstract fun isWritable(context: Context): Boolean
    class PathSource(val file: File) : DocumentSource() {
        override fun displayName(context: Context) = file.name
        override fun sizeBytes(context: Context) = file.length()
        override fun read(context: Context, maxBytes: Int?): String {
            if (!file.exists() || !file.canRead()) throw IOException("File not found or unreadable")
            return if (maxBytes == null) file.readText()
            else file.inputStream().use { String(it.readNBytesCompat(maxBytes)) }
        }
        override fun write(context: Context, content: String) {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { it.write(content.toByteArray()) }
        }
        override fun isWritable(context: Context) = !file.exists() || file.canWrite()
    }
    class UriSource(val uri: Uri) : DocumentSource() {
        override fun displayName(context: Context): String {
            runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) {
                        val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return it.getString(idx)
                    }
                }
            }
            return uri.lastPathSegment ?: "document"
        }
        override fun sizeBytes(context: Context): Long = runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        }.getOrDefault(-1L)
        override fun read(context: Context, maxBytes: Int?): String {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open document")
            return stream.use {
                if (maxBytes == null) it.readBytes().toString(Charsets.UTF_8)
                else String(it.readNBytesCompat(maxBytes))
            }
        }
        override fun write(context: Context, content: String) {
            val stream = context.contentResolver.openOutputStream(uri, "wt")
                ?: throw IOException("Cannot write document")
            stream.use { it.write(content.toByteArray()) }
        }
        override fun isWritable(context: Context): Boolean = runCatching {
            context.contentResolver.openOutputStream(uri, "wa")?.close()
            true
        }.getOrDefault(false)
    }
}
private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
    val buffer = ByteArray(max)
    var offset = 0
    while (offset < max) {
        val read = read(buffer, offset, max - offset)
        if (read == -1) break
        offset += read
    }
    return buffer.copyOf(offset)
}
class TextEditorActivity : ComponentActivity() {
    private var source: DocumentSource? = null
    private val fileContent = mutableStateOf<String?>(null)
    private val isLoading = mutableStateOf(true)
    private val isTruncatedPreview = mutableStateOf(false)
    private val isWritableSource = mutableStateOf(false)
    private val dialogState = mutableStateOf<EditorDialogState?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        source = intent.getStringExtra("filePath")?.let { DocumentSource.PathSource(File(it)) }
            ?: intent.data?.let { DocumentSource.UriSource(it) }
        if (source == null) {
            Log.e("TextEditor", "No file path or data URI supplied!")
            dialogState.value = EditorDialogState("File path missing", true)
            isLoading.value = false
        } else {
            loadContent()
        }
        setContent {
            OSTToolsTheme {
                TextEditorScreen()
            }
        }
    }
    private fun loadContent() {
        isLoading.value = true
        lifecycleScope.launch(Dispatchers.IO) {
            val src = source ?: return@launch
            try {
                val size = src.sizeBytes(this@TextEditorActivity)
                val tooLarge = size > MAX_EDITABLE_BYTES
                val text = src.read(this@TextEditorActivity, if (tooLarge) PREVIEW_BYTES else null)
                withContext(Dispatchers.Main) {
                    isTruncatedPreview.value = tooLarge
                    isWritableSource.value = !tooLarge && src.isWritable(this@TextEditorActivity)
                    fileContent.value = text
                    isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("TextEditor", "Error reading file", e)
                withContext(Dispatchers.Main) {
                    fileContent.value = ""
                    dialogState.value = EditorDialogState(e.localizedMessage ?: "Error reading file", true)
                    isLoading.value = false
                }
            }
        }
    }
    private suspend fun saveContent(content: String): Result<Unit> = withContext(Dispatchers.IO) {
        val src = source ?: return@withContext Result.failure(IllegalStateException("No source"))
        try {
            src.write(this@TextEditorActivity, content)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IOException("Error saving file", e))
        }
    }
    @Composable
    fun TextEditorScreen() {
        val content by fileContent
        val loading by isLoading
        val dialog by dialogState
        val truncated by isTruncatedPreview
        val writable by isWritableSource
        val name = remember { source?.displayName(this) ?: "" }
        val editable = !truncated && writable && File(name).extension.lowercase() in EDITABLE_EXTENSIONS
        val editorText = remember { mutableStateOf("") }
        var loadedText by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val scrollState = rememberScrollState()
        var isSaving by remember { mutableStateOf(false) }
        var showUnsavedDialog by remember { mutableStateOf(false) }
        val isDirty = editable && editorText.value != loadedText
        LaunchedEffect(content) {
            editorText.value = content ?: ""
            loadedText = content ?: ""
        }
        fun doSave(onDone: () -> Unit = {}) {
            isSaving = true
            focusManager.clearFocus()
            lifecycleScope.launch {
                val result = saveContent(editorText.value)
                isSaving = false
                if (result.isSuccess) {
                    loadedText = editorText.value
                    dialogState.value = EditorDialogState("Saved", false)
                    onDone()
                } else {
                    dialogState.value = EditorDialogState(
                        result.exceptionOrNull()?.localizedMessage ?: "Error saving file", true
                    )
                }
            }
        }
        BackHandler(enabled = isDirty && !showUnsavedDialog) {
            showUnsavedDialog = true
        }
        if (showUnsavedDialog) {
            Dialog(onDismissRequest = { showUnsavedDialog = false }) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.unsaved_changes),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        FilledTonalIconButton(onClick = {
                            showUnsavedDialog = false
                            finish()
                        }) {
                            Icon(painterResource(R.drawable.ic_delete_24dp), contentDescription = "Discard")
                        }
                        FilledIconButton(onClick = {
                            showUnsavedDialog = false
                            doSave { finish() }
                        }) {
                            Icon(painterResource(R.drawable.ic_save_24dp), contentDescription = "Save")
                        }
                    }
                }
            }
        }
        AppScaffold(timeText = { TimeText() }) {
            ScreenScaffold {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        loading -> {
                            CircularProgressIndicator()
                        }
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!editable) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_lock_24dp),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (truncated) stringResource(R.string.file_too_large_preview)
                                            else stringResource(R.string.read_only),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                BasicTextField(
                                    value = editorText.value,
                                    onValueChange = { if (editable) editorText.value = it },
                                    readOnly = !editable,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .border(
                                            width = 1.dp,
                                            color = if (editable)
                                                MaterialTheme.colorScheme.outline
                                            else
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        )
                                        .verticalScroll(scrollState)
                                        .padding(8.dp)
                                        .then(if (editable) Modifier.focusRequester(focusRequester) else Modifier),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    textStyle = TextStyle(
                                        color = if (editable)
                                            MaterialTheme.colorScheme.onSurface
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Start,
                                        fontFamily = FontFamily(Font(R.font.consola))
                                    )
                                )
                                if (editable) {
                                    FilledIconButton(
                                        onClick = { doSave() },
                                        enabled = !isSaving,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        if (isSaving) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_save_24dp),
                                                contentDescription = "Save"
                                            )
                                        }
                                    }
                                }
                                LaunchedEffect(loading) {
                                    if (!loading && editable) {
                                        focusRequester.requestFocus()
                                    }
                                }
                            }
                        }
                    }
                    dialog?.let { state ->
                        FailDialog(
                            message = state.message,
                            iconResId = if (state.isError) R.drawable.ic_error_24dp else R.drawable.ic_check_circle_24dp,
                            onDismiss = { dialogState.value = null },
                            showDialog = true
                        )
                    }
                }
            }
        }
    }
}
