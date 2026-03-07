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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
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
import com.ost.application.util.ConfirmationDialog
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

// --- Data Classes (могут быть и вложенными, но здесь оставлены на верхнем уровне файла для читаемости) ---
data class RenameDialogState(val message: String, val isError: Boolean)

data class RenameUiState(
    val originalFileName: String,
    val newName: String,
    val isBusy: Boolean,
    val currentDialog: RenameDialogState?,
    val isRenameButtonEnabled: Boolean
)
// ---------------------------------------------------------------------------------------------------


// --- ActivityResultContract (остается без изменений) ---
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
// ---------------------------------------------------------------------------------------------------


class RenameActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
    }

    // --- ViewModel (вложенный класс) ---
    // Вложенный ViewModel получает Application Context через конструктор,
    // чтобы иметь доступ к строковым ресурсам через getApplication().
    private class RenameViewModel(application: Application, private val originalFilePath: String) : ViewModel() {

        private val originalFile = File(originalFilePath)
        private val appContext: Context = application.applicationContext // Сохраняем контекст для доступа к ресурсам

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
            // Проверка оригинального файла при инициализации ViewModel
            if (!originalFile.exists()) {
                // Если файл не существует, сразу отправляем событие об отмене.
                // Activity сама обработает это событие и завершится с RESULT_CANCELED.
                viewModelScope.launch {
                    _renameResultEvent.send(false)
                }
            } else {
                updateRenameButtonState(originalFile.name) // Иначе, инициализируем состояние кнопки
            }
        }

        fun onNewNameChanged(newName: String) {
            // Фильтруем недопустимые символы для имен файлов
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
                    // Диалог успеха скрывается автоматически через 1.5 сек, затем Activity завершается
                    delay(1500)
                    _uiState.value = _uiState.value.copy(currentDialog = null)
                    _renameResultEvent.send(true) // Сообщаем Activity о успехе
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
                _renameResultEvent.send(false) // Сообщаем Activity об отмене
            }
        }
    }
    // ---------------------------------------------------------------------------------------------------

    // --- ViewModel Factory (вложенный объект) ---
    // Фабрика для ViewModel, чтобы передать ей Application и filePath
    private val renameViewModel: RenameViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(RenameViewModel::class.java)) {
                    val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                    // На этом этапе filePath уже был проверен в onCreate,
                    // но для безопасности можно добавить еще одну проверку
                    if (filePath == null) {
                        throw IllegalArgumentException("Missing file path for RenameViewModel")
                    }
                    @Suppress("UNCHECKED_CAST")
                    return RenameViewModel(application, filePath) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
    // ---------------------------------------------------------------------------------------------------


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // **Важно:** Проверяем filePath до инициализации ViewModel,
        // так как ViewModelProvider.Factory не может напрямую завершить Activity.
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
            MaterialTheme {
                // Собираем состояние UI из ViewModel
                val uiState by renameViewModel.uiState.collectAsStateWithLifecycle()

                // Отслеживаем одноразовые события (например, завершение Activity)
                LaunchedEffect(Unit) {
                    renameViewModel.renameResultEvent.collect { success ->
                        setResult(if (success) RESULT_OK else RESULT_CANCELED)
                        finish()
                    }
                }

                // Отображаем Compose UI
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


    // --- Composable UI (вложенная функция) ---
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
                Text(
                    text = stringResource(R.string.rename), // Заголовок
                    style = MaterialTheme.typography.title3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.originalFileName,
                    style = MaterialTheme.typography.caption1,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                BasicTextField(
                    value = uiState.newName,
                    onValueChange = onNewNameChanged,
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

                // Запрашиваем фокус после небольшой задержки для надежности
                LaunchedEffect(Unit) {
                    delay(100)
                    focusRequester.requestFocus()
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onCancelClick,
                        enabled = !uiState.isBusy,
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_cancel_24dp), contentDescription = null)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus() // Убираем фокус с текстового поля перед операцией
                            onRenameClick()
                        },
                        enabled = !uiState.isBusy && uiState.isRenameButtonEnabled,
                        modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
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
            }

            uiState.currentDialog?.let { state ->
                val iconRes = if (state.isError) R.drawable.ic_error_24dp else R.drawable.ic_check_circle_24dp
                ConfirmationDialog(
                    message = state.message,
                    iconResId = iconRes,
                    onDismiss = onDialogDismiss,
                    showDialog = true
                )
            }
        }
    }

    @Preview(showBackground = true)
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
    // ---------------------------------------------------------------------------------------------------
}