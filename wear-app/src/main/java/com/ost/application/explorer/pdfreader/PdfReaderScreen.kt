package com.ost.application.explorer.pdfreader
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PdfReaderScreen(
    uiState: PdfReaderUiState,
    onPageChange: (Int) -> Unit,
    onZoomChange: (Float) -> Unit,
    onShowExitDialog: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onExitConfirmed: () -> Unit,
    onScreenTap: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onRotaryZoom: (Float) -> Unit = {},
    onShowOutline: () -> Unit = {},
    onDismissOutline: () -> Unit = {},
    onOutlineItemClick: (OutlineItem) -> Unit = {}
) {
    androidx.activity.compose.BackHandler { onShowExitDialog() }
    if (uiState.showExitDialog) {
        ExitConfirmDialog(
            currentPage = uiState.currentPage,
            onConfirm = onExitConfirmed,
            onDismiss = onDismissExitDialog
        )
        return
    }
    when {
        uiState.isLoading -> LoadingScreen()
        uiState.error != null -> ErrorScreen(uiState.error)
        uiState.showOutline -> PdfOutlineScreen(
            outline = uiState.outline,
            currentPage = uiState.currentPage,
            totalPages = uiState.totalPages,
            onItemClick = onOutlineItemClick,
            onDismiss = onDismissOutline
        )
        else -> PdfPageScreen(
            uiState = uiState,
            onNextPage = { onPageChange(uiState.currentPage + 1) },
            onPrevPage = { onPageChange(uiState.currentPage - 1) },
            onZoomChange = onZoomChange,
            onScreenTap = onScreenTap,
            onDoubleTap = onDoubleTap,
            onRotaryZoom = onRotaryZoom,
            onShowOutline = onShowOutline
        )
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PdfPageScreen(
    uiState: PdfReaderUiState,
    onNextPage: () -> Unit,
    onPrevPage: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onScreenTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onRotaryZoom: (Float) -> Unit,
    onShowOutline: () -> Unit
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val currentZoom by rememberUpdatedState(uiState.zoom)
    LaunchedEffect(uiState.currentPage) { offset = Offset.Zero }
    LaunchedEffect(uiState.zoom) {
        if (uiState.zoom <= PdfReaderViewModel.MIN_ZOOM) offset = Offset.Zero
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }
    LaunchedEffect(uiState.showExitDialog) {
        if (!uiState.showExitDialog) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                onRotaryZoom(event.verticalScrollPixels)
                true
            }
    ) {
        uiState.currentBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page ${uiState.currentPage + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput("tap") {
                        detectTapGestures(
                            onTap = { onScreenTap() },
                            onDoubleTap = { onDoubleTap() }
                        )
                    }
                    .pointerInput("transform") {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val newZoom = (currentZoom * gestureZoom)
                                .coerceIn(PdfReaderViewModel.MIN_ZOOM, PdfReaderViewModel.MAX_ZOOM)
                            onZoomChange(newZoom)
                            if (newZoom <= PdfReaderViewModel.MIN_ZOOM) {
                                offset = Offset.Zero
                            } else {
                                val maxX = (containerSize.width / 2f) * (newZoom - 1f)
                                val maxY = (containerSize.height / 2f) * (newZoom - 1f)
                                val newOff = offset + pan
                                offset = Offset(
                                    x = newOff.x.coerceIn(-maxX, maxX),
                                    y = newOff.y.coerceIn(-maxY, maxY)
                                )
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = uiState.zoom,
                        scaleY = uiState.zoom,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        PageIndicatorBadge(
            current = uiState.currentPage + 1,
            total = uiState.totalPages,
            zoom = uiState.zoom,
            hasOutline = uiState.outline.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp),
            onClick = onShowOutline
        )
        AnimatedVisibility(
            visible = uiState.showUi,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZoomButton(
                        iconRes = Icons.Rounded.ZoomOut,
                        contentDescription = "Zoom out",
                        enabled = uiState.zoom > PdfReaderViewModel.MIN_ZOOM,
                        onClick = {
                            onZoomChange(
                                (uiState.zoom - PdfReaderViewModel.ZOOM_STEP)
                                    .coerceAtLeast(PdfReaderViewModel.MIN_ZOOM)
                            )
                        }
                    )
                    ZoomButton(
                        iconRes = Icons.Rounded.ZoomIn,
                        contentDescription = "Zoom in",
                        enabled = uiState.zoom < PdfReaderViewModel.MAX_ZOOM,
                        onClick = {
                            onZoomChange(
                                (uiState.zoom + PdfReaderViewModel.ZOOM_STEP)
                                    .coerceAtMost(PdfReaderViewModel.MAX_ZOOM)
                            )
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 2.dp)
                ) {
                    NavButton(
                        iconRes = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Previous",
                        enabled = uiState.currentPage > 0,
                        onClick = onPrevPage
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp)
                ) {
                    NavButton(
                        iconRes = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Next",
                        enabled = uiState.currentPage < uiState.totalPages - 1,
                        onClick = onNextPage
                    )
                }
            }
        }
    }
}
@Composable
private fun PageIndicatorBadge(
    current: Int,
    total: Int,
    zoom: Float,
    hasOutline: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "$current / $total", color = Color.White, fontSize = 11.sp)
            Text(text = "·", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            Text(text = "×${"%.1f".format(zoom)}", color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp)
        }
    }
}
@Composable
private fun NavButton(
    iconRes: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        enabled = enabled,
    ) {
        Icon(
            imageVector = iconRes,
            contentDescription = contentDescription,
        )
    }
}
@Composable
private fun ZoomButton(
    iconRes: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = iconRes,
            contentDescription = contentDescription,
        )
    }
}
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.fillMaxSize())
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
@Composable
private fun ErrorScreen(error: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
@Composable
fun ExitConfirmDialog(
    currentPage: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.activity.compose.BackHandler { onDismiss() }
    AlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.AutoMirrored.Rounded.ExitToApp, "Exit")
        },
        title = { Text("Exit?") },
        text = {
            Text("When you reopen it, page ${currentPage + 1} will open")
        },
        dismissButton = {
            FilledTonalIconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Cancel, "Отмена")
            }
        },
        confirmButton = {
            FilledIconButton(onClick = onConfirm) {
                Icon(Icons.Rounded.CheckCircle, "OK")
            }
        }
    )
}
