package com.ost.application.explorer.pdfreader
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ost.application.theme.OSTToolsTheme
class PdfReaderActivity : ComponentActivity() {
    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
    }
    private val viewModel: PdfReaderViewModel by viewModels {
        PdfReaderViewModelFactory(application)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val dataUri = intent.data
        when {
            filePath != null -> viewModel.loadDocument(filePath)
            dataUri != null -> viewModel.loadDocument(dataUri)
            else -> {
                Log.e("PdfReaderActivity", "No file path or data URI provided")
                finish()
                return
            }
        }
        setContent {
            OSTToolsTheme {
                val uiState by viewModel.uiState.collectAsState()
                PdfReaderScreen(
                    uiState = uiState,
                    onPageChange = { page -> viewModel.goToPage(page) },
                    onZoomChange = { zoom -> viewModel.setZoom(zoom) },
                    onExitConfirmed = { finish() },
                    onShowExitDialog = { viewModel.showExitDialog() },
                    onDismissExitDialog = { viewModel.dismissExitDialog() },
                    onScreenTap = { viewModel.onScreenTap() },
                    onDoubleTap = { viewModel.resetZoom() },
                    onRotaryZoom = { scrollPixels -> viewModel.onRotaryZoom(scrollPixels) },
                    onShowOutline = {
                        android.util.Log.d("PdfReader", "onShowOutline called, outline size=${viewModel.uiState.value.outline.size}")
                        viewModel.showOutline()
                        android.util.Log.d("PdfReader", "showOutline=${viewModel.uiState.value.showOutline}")
                    },
                    onDismissOutline = { viewModel.dismissOutline() },
                    onOutlineItemClick = { item -> viewModel.navigateToOutlineItem(item) }
                )
            }
        }
    }
    override fun onStop() {
        super.onStop()
        viewModel.saveProgress()
    }
}
