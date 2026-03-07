package com.ost.application.explorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.MaterialTheme

import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

class ImageActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imagePath = intent.getStringExtra("imagePath")

        if (imagePath == null) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                ImageViewerScreen(
                    imagePath = imagePath,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
fun ImageViewerScreen(imagePath: String, onDismiss: () -> Unit) {
    androidx.wear.compose.material.SwipeToDismissBox(
        onDismissed = onDismiss,
        hasBackground = false
    ) {
        ZoomableImage(imagePath = imagePath)
    }
}

@Composable
fun ZoomableImage(imagePath: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale == 1f) {
                        offset = Offset.Zero
                    } else {
                        offset += pan
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(imagePath))
                .crossfade(true)
                .build(),
            contentDescription = "Просмотр изображения",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}