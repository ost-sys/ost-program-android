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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val originalFileName: String = "",
    val newName: String = "",
    val isBusy: Boolean = false,
    val currentDialog: RenameDialogState? = null,
    val isRenameButtonEnabled: Boolean = false
)
class RenameContract : ActivityResultContract<String, Boolean>() {
    override fun createIntent(context: Context, input: String): Intent =
        Intent(context, RenameActivity::class.java).apply {
            putExtra(RenameActivity.EXTRA_FILE_PATH, input)
        }
    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}
class RenameActivity : ComponentActivity() {
    companion object {
        const val EXTRA_FILE_PATH = "file_path"
    }
    private class RenameViewModel(
        application: Application,
        private val originalFilePath: String
    ) : ViewModel() {
        private val appContext: Context = application.applicationContext
        private val originalFile = File(originalFilePath)
        private val _uiState = MutableStateFlow(
            RenameUiState(
                originalFileName = originalFile.name,
                newName = originalFile.name,
            )
        )
        val uiState: StateFlow<RenameUiState> = _uiState.asStateFlow()
        private val _resultEvent = Channel<Boolean>(Channel.BUFFERED)
        val resultEvent = _resultEvent.receiveAsFlow()
        init {
            if (!originalFile.exists()) {
                viewModelScope.launch { _resultEvent.send(false) }
            } else {
                refreshButtonState(originalFile.name)
            }
        }
        fun onNewNameChanged(newName: String) {
            val filtered = newName.filter { it != '/' && it != '\\' }
            _uiState.value = _uiState.value.copy(newName = filtered)
            refreshButtonState(filtered)
        }
        private fun refreshButtonState(name: String) {
            _uiState.value = _uiState.value.copy(
                isRenameButtonEnabled = name.isNotBlank() && name != _uiState.value.originalFileName
            )
        }
        fun onRenameAttempt() {
            val newName = _uiState.value.newName.trim()
            if (newName.isBlank()) {
                setDialog(appContext.getString(R.string.new_name_cannot_be_empty), isError = true)
                return
            }
            if (newName == _uiState.value.originalFileName) {
                setDialog(appContext.getString(R.string.name_is_the_same), isError = false)
                return
            }
            _uiState.value = _uiState.value.copy(isBusy = true)
            viewModelScope.launch {
                val result = renameFile(originalFile, newName)
                _uiState.value = _uiState.value.copy(isBusy = false)
                if (result.isSuccess) {
                    setDialog(appContext.getString(R.string.renamed_successfully), isError = false)
                    delay(1500)
                    _uiState.value = _uiState.value.copy(currentDialog = null)
                    _resultEvent.send(true)
                } else {
                    Log.e("RenameViewModel", "Rename failed", result.exceptionOrNull())
                    setDialog(
                        result.exceptionOrNull()?.localizedMessage
                            ?: appContext.getString(R.string.failed_to_rename),
                        isError = true
                    )
                }
            }
        }
        fun onDialogDismissed() {
            _uiState.value = _uiState.value.copy(currentDialog = null)
        }
        fun onCancelClicked() {
            viewModelScope.launch { _resultEvent.send(false) }
        }
        private fun setDialog(message: String, isError: Boolean) {
            _uiState.value = _uiState.value.copy(
                currentDialog = RenameDialogState(message, isError)
            )
        }
        private suspend fun renameFile(file: File, newName: String): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val parent = file.parentFile
                        ?: return@withContext Result.failure(
                            IOException(appContext.getString(R.string.cannot_get_parent_directory))
                        )
                    val dest = File(parent, newName)
                    when {
                        dest.exists() -> Result.failure(
                            IOException(appContext.getString(R.string.file_with_the_new_name_already_exists))
                        )
                        file.renameTo(dest) -> Result.success(Unit)
                        else -> Result.failure(
                            IOException(appContext.getString(R.string.rename_failed_check_permissions_or_storage_state))
                        )
                    }
                } catch (e: SecurityException) {
                    Result.failure(e)
                } catch (e: Exception) {
                    Result.failure(IOException(appContext.getString(R.string.generic_error_renaming_file), e))
                }
            }
    }
    private val viewModel: RenameViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val path = intent.getStringExtra(EXTRA_FILE_PATH)
                    ?: throw IllegalArgumentException("Missing $EXTRA_FILE_PATH")
                return RenameViewModel(application, path) as T
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        if (filePath == null || !File(filePath).exists()) {
            Log.e("RenameActivity", "Invalid or missing file path: $filePath")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        setContent {
            OSTToolsTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) {
                    viewModel.resultEvent.collect { success ->
                        setResult(if (success) RESULT_OK else RESULT_CANCELED)
                        finish()
                    }
                }
                RenameScreen(
                    uiState = uiState,
                    onNewNameChanged = viewModel::onNewNameChanged,
                    onRenameClick = viewModel::onRenameAttempt,
                    onCancelClick = viewModel::onCancelClicked,
                    onDialogDismiss = viewModel::onDialogDismissed
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
        val listState = rememberScalingLazyListState()
        AppScaffold(timeText = { TimeText() }) {
            ScreenScaffold(scrollState = listState) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ScalingLazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        anchorType = ScalingLazyListAnchorType.ItemCenter
                    ) {
                        item {
                            Text(
                                text = uiState.originalFileName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            BasicTextField(
                                value = uiState.newName,
                                onValueChange = onNewNameChanged,
                                enabled = !uiState.isBusy,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        onRenameClick()
                                    }
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                            LaunchedEffect(Unit) {
                                delay(100)
                                focusRequester.requestFocus()
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = onCancelClick,
                            enabled = !uiState.isBusy,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cancel_24dp),
                                contentDescription = "Cancel"
                            )
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        FilledIconButton(
                            onClick = {
                                focusManager.clearFocus()
                                onRenameClick()
                            },
                            enabled = uiState.isRenameButtonEnabled && !uiState.isBusy,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (uiState.isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_circle_24dp),
                                    contentDescription = "Rename"
                                )
                            }
                        }
                    }
                    uiState.currentDialog?.let { state ->
                        if (state.isError) {
                            FailDialog(
                                message = state.message,
                                iconResId = R.drawable.ic_error_24dp,
                                onDismiss = onDialogDismiss,
                                showDialog = true
                            )
                        } else {
                            SuccessDialog(
                                actionIconResId = R.drawable.ic_edit_24dp,
                                onDismiss = onDialogDismiss,
                                showDialog = true
                            )
                        }
                    }
                }
            }
        }
    }
    @Preview(showBackground = true, device = "id:wearos_small_round")
    @Composable
    private fun PreviewRenameScreen() {
        OSTToolsTheme {
            RenameScreen(
                uiState = RenameUiState(
                    originalFileName = "my_document.txt",
                    newName = "my_document_v2.txt",
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
