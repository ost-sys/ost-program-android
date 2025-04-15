package com.ost.application.explorer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.ost.application.R
import com.ost.application.util.ConfimationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class RenameDialogState(val message: String, val isError: Boolean)

class RenameContract : ActivityResultContract<String, Boolean>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(context, RenameActivity::class.java).apply {
            putExtra(RenameActivity.EXTRA_FILE_PATH, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}


class RenameActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
    }

    private var originalFile: File? = null
    private val dialogState = mutableStateOf<RenameDialogState?>(null)
    private val isRenaming = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        if (filePath == null) {
            Log.e("RenameActivity", "Missing file path in Intent")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        originalFile = File(filePath)

        if (originalFile?.exists() == false) {
            Log.e("RenameActivity", "Original file does not exist: $filePath")
            setResult(RESULT_CANCELED)
            finish()
            return
        }


        setContent {
            MaterialTheme {
                RenameScreen(
                    file = originalFile!!,
                    currentDialog = dialogState.value,
                    isBusy = isRenaming.value,
                    onDismissDialog = { dialogState.value = null },
                    onRenameAttempt = { newName ->
                        performRename(originalFile!!, newName)
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    private fun performRename(fileToRename: File, newNameRaw: String) {
        val newName = newNameRaw.trim().filter { it != '/' && it != '\\' }

        if (newName.isBlank()) {
            dialogState.value = RenameDialogState(getString(R.string.new_name_cannot_be_empty), true)
            return
        }
        if (newName == fileToRename.name) {
            dialogState.value = RenameDialogState(getString(R.string.name_is_the_same), false)
            return
        }

        isRenaming.value = true
        lifecycleScope.launch {
            val result = renameFileAsync(fileToRename, newName)
            isRenaming.value = false

            if (result.isSuccess) {
                dialogState.value = RenameDialogState(getString(R.string.renamed_successfully), false)
                kotlinx.coroutines.delay(1500)

                if(dialogState.value?.isError == false) { dialogState.value = null }

                setResult(RESULT_OK)
                finish()
            } else {
                Log.e("RenameActivity", "Failed to rename", result.exceptionOrNull())
                dialogState.value = RenameDialogState(
                    result.exceptionOrNull()?.localizedMessage ?: getString(R.string.failed_to_rename),
                    true
                )
            }
        }
    }

    private suspend fun renameFileAsync(file: File, newName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val parentDir = file.parentFile
                if (parentDir == null) {
                    Result.failure(IOException(getString(R.string.cannot_get_parent_directory)))
                } else {
                    val newFile = File(parentDir, newName)
                    if (newFile.exists()) {
                        Result.failure(IOException(getString(R.string.file_with_the_new_name_already_exists)))
                    } else {
                        if (file.renameTo(newFile)) {
                            Result.success(Unit)
                        } else {
                            Result.failure(IOException(getString(R.string.rename_failed_check_permissions_or_storage_state)))
                        }
                    }
                }
            } catch (e: SecurityException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(IOException(getString(R.string.generic_error_renaming_file), e))
            }
        }
    }

    @Composable
    fun RenameScreen(
        file: File,
        currentDialog: RenameDialogState?,
        isBusy: Boolean,
        onDismissDialog: () -> Unit,
        onRenameAttempt: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        var newNameState by remember { mutableStateOf(file.name) }
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current

        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(VignettePosition.TopAndBottom) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Rename", style = MaterialTheme.typography.title3)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.caption1,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                BasicTextField(
                    value = newNameState,
                    onValueChange = { newNameState = it.filter { char -> char != '/' && char != '\\' } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .border(1.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colors.onBackground,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true
                )

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(100)
                    focusRequester.requestFocus()
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onCancel,
                        enabled = !isBusy,
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_cancel_24dp), contentDescription = "Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onRenameAttempt(newNameState)
                        },
                        enabled = !isBusy && newNameState.isNotBlank() && newNameState != file.name,
                        modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if(isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(painter = painterResource(R.drawable.ic_check_circle_24dp), contentDescription = "Rename")
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
                    onDismiss = onDismissDialog
                )
            }
        }
    }
}