package com.ost.application.explorer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.ost.application.R
import com.ost.application.util.ConfimationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class EditorDialogState(val message: String, val isError: Boolean)

class TextEditorActivity : ComponentActivity() {

    private var filePath: String? = null
    private val fileContent = mutableStateOf<String?>(null)
    private val isLoading = mutableStateOf(true)
    private val dialogState = mutableStateOf<EditorDialogState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filePath = intent.getStringExtra("filePath")

        if (filePath == null) {
            Log.e("TextEditor", "File path is missing!")
            dialogState.value = EditorDialogState("File path missing", true)
        } else {
            loadInitialContent()
        }

        setContent {
            MaterialTheme {
                TextEditorScreen()
            }
        }
    }

    private fun loadInitialContent() {
        isLoading.value = true
        lifecycleScope.launch {
            val result = readFileContentAsync(filePath)
            if (result.isSuccess) {
                fileContent.value = result.getOrNull() ?: ""
            } else {
                Log.e("TextEditor", "Error reading file", result.exceptionOrNull())
                fileContent.value = ""
                dialogState.value = EditorDialogState(
                    result.exceptionOrNull()?.localizedMessage ?: "Error reading file",
                    true
                )
            }
            isLoading.value = false
        }
    }

    private suspend fun readFileContentAsync(path: String?): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                path?.let {
                    val file = File(it)
                    if (file.exists() && file.canRead()) {
                        Result.success(file.readText())
                    } else {
                        Result.failure(IOException("File not found or cannot be read: $path"))
                    }
                } ?: Result.failure(IllegalArgumentException("File path is null"))
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: SecurityException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(IOException("Generic error reading file", e))
            }
        }
    }

    private suspend fun saveFileContentAsync(path: String?, content: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            if (path == null) return@withContext Result.failure(IllegalArgumentException("File path is null"))
            try {
                val file = File(path)
                file.parentFile?.mkdirs()
                FileOutputStream(file).use {
                    it.write(content.toByteArray())
                }
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: SecurityException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(IOException("Generic error saving file", e))
            }
        }
    }

    @Composable
    fun TextEditorScreen() {
        val currentContent by fileContent
        val isCurrentlyLoading by isLoading
        val currentDialog by dialogState
        val editorTextState = remember { mutableStateOf("") }

        LaunchedEffect(currentContent) {
            editorTextState.value = currentContent ?: ""
        }

        val focusRequester = remember { FocusRequester() }
        val scrollState = rememberScrollState()
        LocalContext.current
        val focusManager = LocalFocusManager.current
        var isSaving by remember { mutableStateOf(false) }

        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(VignettePosition.Top) }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isCurrentlyLoading) {
                    CircularProgressIndicator()
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicTextField(
                            value = editorTextState.value,
                            onValueChange = { editorTextState.value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                                .verticalScroll(scrollState)
                                .padding(8.dp),
                            textStyle = TextStyle(
                                color = MaterialTheme.colors.onSurface,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Left,
                                fontFamily = FontFamily(Font(R.font.consola))
                            )
                        )

                        Button(
                            modifier = Modifier.size(ButtonDefaults.DefaultButtonSize),
                            enabled = !isSaving,
                            onClick = {
                                if (filePath != null) {
                                    isSaving = true
                                    focusManager.clearFocus()
                                    lifecycleScope.launch {
                                        val result = saveFileContentAsync(filePath, editorTextState.value)
                                        isSaving = false
                                        if (result.isSuccess) {
                                            dialogState.value = EditorDialogState("Saved successfully", false)
                                        } else {
                                            Log.e("TextEditor", "Error saving file", result.exceptionOrNull())
                                            dialogState.value = EditorDialogState(
                                                result.exceptionOrNull()?.localizedMessage ?: "Error saving file",
                                                true
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_save_24dp),
                                        contentDescription = "Save"
                                    )
                                }
                            }
                        }

                        LaunchedEffect(Unit) {
                            if(!isCurrentlyLoading) {
                                focusRequester.requestFocus()
                            }
                        }
                    }
                }
            }

            currentDialog?.let { state ->
                val iconRes = if (state.isError) R.drawable.ic_error_24dp else R.drawable.ic_check_circle_24dp
                ConfimationDialog(
                    message = state.message,
                    iconResId = iconRes,
                    onDismiss = { dialogState.value = null }
                )
            }
        }
    }
}