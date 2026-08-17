package com.ost.application.explorer.pdfreader
import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.legere.pdfiumandroid.PdfDocument
import io.legere.pdfiumandroid.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
data class OutlineItem(
    val title: String,
    val pageIndex: Int,
    val depth: Int
)
data class PdfReaderUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val filePath: String = "",
    val fileName: String = "",
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val currentBitmap: Bitmap? = null,
    val zoom: Float = 2.0f,
    val showExitDialog: Boolean = false,
    val showPageIndicator: Boolean = false,
    val showUi: Boolean = true,
    val outline: List<OutlineItem> = emptyList(),
    val showOutline: Boolean = false
)
class PdfReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("pdf_reader_prefs", 0)
    private val pdfiumCore = PdfiumCore(application)
    private val _uiState = MutableStateFlow(PdfReaderUiState())
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var hideUiJob: Job? = null
    companion object {
        private const val TAG = "PdfReaderViewModel"
        const val MIN_ZOOM = 1.0f
        const val MAX_ZOOM = 5.0f
        const val ZOOM_STEP = 0.5f
        private const val PREF_PAGE_PREFIX = "page_"
        private const val PREF_ZOOM_PREFIX = "zoom_"
        private const val RENDER_WIDTH = 1440
        private const val UI_HIDE_DELAY_MS = 3000L
    }
    fun loadDocument(filePath: String) {
        val file = File(filePath)
        loadDocumentInternal(
            documentKey = filePath,
            fileName = file.name,
            openDescriptor = {
                if (!file.exists()) null
                else ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        )
    }
    fun loadDocument(uri: android.net.Uri) {
        val application = getApplication<Application>()
        loadDocumentInternal(
            documentKey = uri.toString(),
            fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "PDF",
            openDescriptor = { application.contentResolver.openFileDescriptor(uri, "r") }
        )
    }
    private fun loadDocumentInternal(
        documentKey: String,
        fileName: String,
        openDescriptor: () -> ParcelFileDescriptor?
    ) {
        val application = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, filePath = documentKey) }
            withContext(Dispatchers.IO) {
                try {
                    closeRenderer()
                    val descriptor = openDescriptor()
                    if (descriptor == null) {
                        _uiState.update {
                            it.copy(isLoading = false, error = application.getString(com.ost.application.R.string.pdf_file_not_found))
                        }
                        return@withContext
                    }
                    fileDescriptor = descriptor
                    pdfRenderer = PdfRenderer(fileDescriptor!!)
                    val totalPages = pdfRenderer!!.pageCount
                    val savedPage = prefs.getInt(PREF_PAGE_PREFIX + documentKey, 0)
                        .coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                    val savedZoom = prefs.getFloat(PREF_ZOOM_PREFIX + documentKey, 2.0f)
                        .coerceIn(MIN_ZOOM, MAX_ZOOM)
                    val outline = runCatching { openDescriptor() }.getOrNull()
                        ?.let { parseOutline(it) } ?: emptyList()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            fileName = fileName,
                            totalPages = totalPages,
                            currentPage = savedPage,
                            zoom = savedZoom,
                            outline = outline
                        )
                    }
                    renderPage(savedPage)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading PDF", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = application.getString(
                                com.ost.application.R.string.pdf_open_error,
                                e.localizedMessage ?: e.javaClass.simpleName
                            )
                        )
                    }
                }
            }
        }
        scheduleHideUi()
    }
    private fun parseOutline(descriptor: ParcelFileDescriptor): List<OutlineItem> {
        val result = mutableListOf<OutlineItem>()
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = pdfiumCore.newDocument(descriptor)
            val bookmarks = pdfiumCore.getTableOfContents(pdfDocument)
            flattenBookmarks(bookmarks, depth = 0, result = result)
        } catch (e: Exception) {
            Log.w(TAG, "Outline parsing failed: ${e.message}")
        } finally {
            pdfDocument?.let { runCatching { pdfiumCore.closeDocument(it) } }
        }
        return result
    }
    private fun flattenBookmarks(
        bookmarks: List<PdfDocument.Bookmark>,
        depth: Int,
        result: MutableList<OutlineItem>
    ) {
        for (bookmark in bookmarks) {
            val title = bookmark.title ?: continue
            if (title.isBlank()) continue
            val pageIndex = bookmark.pageIdx.toInt().coerceAtLeast(0)
            result.add(OutlineItem(title = title.trim(), pageIndex = pageIndex, depth = depth))
            if (bookmark.children.isNotEmpty()) {
                flattenBookmarks(bookmark.children, depth + 1, result)
            }
        }
    }
    fun goToPage(page: Int) {
        val state = _uiState.value
        val target = page.coerceIn(0, state.totalPages - 1)
        if (target == state.currentPage) return
        viewModelScope.launch {
            _uiState.update { it.copy(currentPage = target, showPageIndicator = true) }
            withContext(Dispatchers.IO) { renderPage(target) }
            delay(2000)
            _uiState.update { it.copy(showPageIndicator = false) }
        }
    }
    fun setZoom(zoom: Float) {
        _uiState.update { it.copy(zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)) }
    }
    fun resetZoom() {
        _uiState.update { it.copy(zoom = MIN_ZOOM) }
    }
    fun onRotaryZoom(scrollPixels: Float) {
        val current = _uiState.value.zoom
        val delta = -scrollPixels / 200f
        val newZoom = (current + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)
        _uiState.update { it.copy(zoom = newZoom) }
    }
    fun showExitDialog() {
        _uiState.update { it.copy(showExitDialog = true) }
    }
    fun dismissExitDialog() {
        _uiState.update { it.copy(showExitDialog = false) }
        showUiAndScheduleHide()
    }
    fun showOutline() {
        hideUiJob?.cancel()
        _uiState.update { it.copy(showOutline = true) }
    }
    fun dismissOutline() {
        _uiState.update { it.copy(showOutline = false) }
        showUiAndScheduleHide()
    }
    fun navigateToOutlineItem(item: OutlineItem) {
        _uiState.update { it.copy(showOutline = false) }
        goToPage(item.pageIndex)
        showUiAndScheduleHide()
    }
    fun onScreenTap() {
        val currentlyVisible = _uiState.value.showUi
        hideUiJob?.cancel()
        _uiState.update { it.copy(showUi = !currentlyVisible) }
        if (!currentlyVisible) scheduleHideUi()
    }
    fun saveProgress() {
        val state = _uiState.value
        if (state.filePath.isNotEmpty() && state.totalPages > 0) {
            prefs.edit()
                .putInt(PREF_PAGE_PREFIX + state.filePath, state.currentPage)
                .putFloat(PREF_ZOOM_PREFIX + state.filePath, state.zoom)
                .apply()
        }
    }
    private fun showUiAndScheduleHide() {
        hideUiJob?.cancel()
        _uiState.update { it.copy(showUi = true) }
        scheduleHideUi()
    }
    private fun scheduleHideUi() {
        hideUiJob?.cancel()
        hideUiJob = viewModelScope.launch {
            delay(UI_HIDE_DELAY_MS)
            _uiState.update { it.copy(showUi = false) }
        }
    }
    private fun renderPage(pageIndex: Int) {
        val renderer = pdfRenderer ?: return
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return
        try {
            val page = renderer.openPage(pageIndex)
            val pageWidth = page.width.takeIf { it > 0 } ?: 595
            val pageHeight = page.height.takeIf { it > 0 } ?: 842
            val aspectRatio = pageHeight.toFloat() / pageWidth.toFloat()
            val bitmapWidth = RENDER_WIDTH.coerceAtMost(8192)
            val bitmapHeight = (bitmapWidth * aspectRatio).toInt().coerceAtMost(8192)
            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            _uiState.update { it.copy(currentBitmap = bitmap) }
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering page $pageIndex", e)
        }
    }
    private fun closeRenderer() {
        try {
            pdfRenderer?.close(); fileDescriptor?.close()
        } catch (_: Exception) {
        }
        pdfRenderer = null; fileDescriptor = null
    }
    override fun onCleared() {
        hideUiJob?.cancel()
        saveProgress()
        closeRenderer()
        super.onCleared()
    }
}
class PdfReaderViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfReaderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PdfReaderViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
