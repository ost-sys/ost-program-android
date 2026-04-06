package com.ost.application.explorer

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.FailDialog
import com.ost.application.util.SuccessDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class RenameDialogState(val message: String, val isError: Boolean)

data class RenameUiState(
    val originalFileName: String,
    val newName: String,
    val isBusy: Boolean,
    val currentDialog: RenameDialogState?,
    val isRenameButtonEnabled: Boolean
)

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

    private class RenameViewModel(application: Application, private val originalFilePath: String) : ViewModel() {

        private val originalFile = File(originalFilePath)
        private val appContext: Context = application.applicationContext

        private val _uiState = MutableStateFlow(
            RenameUiState(
                originalFileName = originalFile.name,
                newName = originalFile.name,
                isBusy = false,
                currentDialog = null,
                isRenameButtonEnabled = false
            )
        )
        val uiState: StateFlow<RenameUiState> = _uiState.asStateFlow()

        private val _renameResultEvent = Channel<Boolean>()
        val renameResultEvent = _renameResultEvent.receiveAsFlow()

        init {
            if (!originalFile.exists()) {
                viewModelScope.launch {
                    _renameResultEvent.send(false)
                }
            } else {
                updateRenameButtonState(originalFile.name)
            }
        }

        fun onNewNameChanged(newName: String) {
            val filteredName = newName.trim().filter { it != '/' && it != '\\' }
            _uiState.value = _uiState.value.copy(newName = filteredName)
            updateRenameButtonState(filteredName)
        }

        private fun updateRenameButtonState(newName: String) {
            val isEnabled = newName.isNotBlank() && newName != _uiState.value.originalFileName
            _uiState.value = _uiState.value.copy(isRenameButtonEnabled = isEnabled)
        }

        fun onRenameAttempt() {
            val currentNewName = _uiState.value.newName

            if (currentNewName.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    currentDialog = RenameDialogState(appContext.getString(R.string.new_name_cannot_be_empty), true)
                )
                return
            }
            if (currentNewName == _uiState.value.originalFileName) {
                _uiState.value = _uiState.value.copy(
                    currentDialog = RenameDialogState(appContext.getString(R.string.name_is_the_same), false)
                )
                return
            }

            _uiState.value = _uiState.value.copy(isBusy = true)
            viewModelScope.launch {
                val result = renameFileAsync(originalFile, currentNewName)
                _uiState.value = _uiState.value.copy(isBusy = false)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        currentDialog = RenameDialogState(appContext.getString(R.string.renamed_successfully), false)
                    )
                    delay(1500)
                    _uiState.value = _uiState.value.copy(currentDialog = null)
                    _renameResultEvent.send(true)
                } else {
                    Log.e("RenameViewModel", "Failed to rename", result.exceptionOrNull())
                    _uiState.value = _uiState.value.copy(
                        currentDialog = RenameDialogState(
                            result.exceptionOrNull()?.localizedMessage
                                ?: appContext.getString(R.string.failed_to_rename),
                            true
                        )
                    )
                }
            }
        }

        private suspend fun renameFileAsync(file: File, newName: String): Result<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    val parentDir = file.parentFile
                    if (parentDir == null) {
                        Result.failure(IOException(appContext.getString(R.string.cannot_get_parent_directory)))
                    } else {
                        val newFile = File(parentDir, newName)
                        if (newFile.exists()) {
                            Result.failure(IOException(appContext.getString(R.string.file_with_the_new_name_already_exists)))
                        } else {
                            if (file.renameTo(newFile)) {
                                Result.success(Unit)
                            } else {
                                Result.failure(IOException(appContext.getString(R.string.rename_failed_check_permissions_or_storage_state)))
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    Result.failure(e)
                } catch (e: Exception) {
                    Result.failure(IOException(appContext.getString(R.string.generic_error_renaming_file), e))
                }
            }
        }

        fun onDialogDismissed() {
            _uiState.value = _uiState.value.copy(currentDialog = null)
        }

        fun onCancelClicked() {
            viewModelScope.launch {
                _renameResultEvent.send(false)
            }
        }
    }
    private val renameViewModel: RenameViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(RenameViewModel::class.java)) {
                    val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                        ?: throw IllegalArgumentException("Missing file path for RenameViewModel")
                    @Suppress("UNCHECKED_CAST")
                    return RenameViewModel(application, filePath) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        if (filePath == null) {
            Log.e("RenameActivity", "Missing file path in Intent")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        val originalFile = File(filePath)
        if (!originalFile.exists()) {
            Log.e("RenameActivity", "Original file does not exist: $filePath")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        setContent {
            OSTToolsTheme {
                val uiState by renameViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    renameViewModel.renameResultEvent.collect { success ->
                        setResult(if (success) RESULT_OK else RESULT_CANCELED)
                        finish()
                    }
                }

                RenameScreen(
                    uiState = uiState,
                    onNewNameChanged = renameViewModel::onNewNameChanged,
                    onRenameClick = renameViewModel::onRenameAttempt,
                    onCancelClick = renameViewModel::onCancelClicked,
                    onDialogDismiss = renameViewModel::onDialogDismissed
                )
            }
        }
    }

    @Composable
    private fun RenameScreen(
        uiState: RenameUiState,
        onNewNameChanged: (String) -> Unit,
        onRenameClick: () -> Unit,
        onCancelClick: () -> Unit,
        onDialogDismiss: () -> Unit
    ) {
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current

        AppScaffold(
            timeText = { TimeText() }
        ) {
            val listState = rememberScalingLazyListState()
            ScreenScaffold(
                scrollState = listState,
                edgeButton = {
                    EdgeButton (
                        onClick = {
                            focusManager.clearFocus()
                            onRenameClick()
                        },
                        enabled = !uiState.isBusy && uiState.isRenameButtonEnabled,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (uiState.isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(painter = painterResource(R.drawable.ic_check_circle_24dp), contentDescription = null)
                            }
                        }
                    }
                }
            ) {
                ScalingLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 28.dp)
                        .focusRequester(focusRequester),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    anchorType = ScalingLazyListAnchorType.ItemCenter,
                    ) {
                    item {
                        Button(
                            onClick = onCancelClick,
                            enabled = !uiState.isBusy,
                            colors = ButtonDefaults.filledTonalButtonColors(),
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_cancel_24dp), contentDescription = null)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item {
                        Text(
                            text = uiState.originalFileName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        BasicTextField(
                            value = uiState.newName,
                            onValueChange = onNewNameChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true
                        )
                        LaunchedEffect(Unit) {
                            delay(100)
                            focusRequester.requestFocus()
                        }
                    }

                }

                uiState.currentDialog?.let { state ->
                    val iconRes = if (state.isError) R.drawable.ic_error_24dp else R.drawable.ic_check_circle_24dp
                    if (state.isError) {
                        FailDialog(
                            message = state.message,
                            iconResId = iconRes,
                            onDismiss = onDialogDismiss,
                            showDialog = true
                        )
                    } else {
                        SuccessDialog(
                            message = state.message,
                            iconResId = iconRes,
                            onDismiss = onDialogDismiss,
                            showDialog = true
                        )
                    }
                }
            }
        }
    }

    @Preview(showBackground = true, device = "id:wearos_xl_round")
    @Composable
    private fun PreviewRenameScreen() {
        MaterialTheme {
            RenameScreen(
                uiState = RenameUiState(
                    originalFileName = "my_file.txt",
                    newName = "my_new_file.txt",
                    isBusy = false,
                    currentDialog = null,
                    isRenameButtonEnabled = true
                ),
                onNewNameChanged = {},
                onRenameClick = {},
                onCancelClick = {},
                onDialogDismiss = {}
            )
        }
    }
}