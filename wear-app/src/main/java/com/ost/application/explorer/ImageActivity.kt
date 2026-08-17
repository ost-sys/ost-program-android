package com.ost.application.explorer
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ost.application.R
import com.ost.application.theme.OSTToolsTheme
import kotlinx.coroutines.delay
import java.io.File
class ImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model: Any? = intent.getStringExtra("imagePath")?.let { File(it) } ?: intent.data
        if (model == null) {
            finish()
            return
        }
        setContent {
            OSTToolsTheme {
                ImageViewerScreen(
                    model = model,
                    onDismiss = { finish() }
                )
            }
        }
    }
}
@Composable
fun ImageViewerScreen(model: Any, onDismiss: () -> Unit) {
    val swipeState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = swipeState,
        onDismissed = onDismiss,
        backgroundKey = "bg",
        contentKey = "content",
        hasBackground = false
    ) {
        ZoomableImageViewer(model = model)
    }
}
@Composable
fun ZoomableImageViewer(model: Any) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(2000)
            controlsVisible = false
        }
    }
    val controlsAlpha by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "controls_alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 6f)
                    val zoomChange = newScale / scale
                    val centroidFromCenter = centroid - Offset(size.width / 2f, size.height / 2f)
                    val unclamped = centroidFromCenter - (centroidFromCenter - offset) * zoomChange + pan
                    scale = newScale
                    val maxX = size.width * (newScale - 1f) / 2f
                    val maxY = size.height * (newScale - 1f) / 2f
                    offset = if (newScale == 1f) Offset.Zero else Offset(
                        unclamped.x.coerceIn(-maxX, maxX),
                        unclamped.y.coerceIn(-maxY, maxY)
                    )
                    controlsVisible = true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val gifCapableLoader = remember {
            ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                    else add(GifDecoder.Factory())
                }
                .build()
        }
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            imageLoader = gifCapableLoader,
            contentDescription = "Image viewer",
            contentScale = ContentScale.Fit,
            onState = { state ->
                isLoading = state is AsyncImagePainter.State.Loading
                isError = state is AsyncImagePainter.State.Error
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 2.dp
            )
        }
        if (isError) {
            Icon(
                painter = painterResource(R.drawable.ic_error_24dp),
                contentDescription = "Failed to load image",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        }
        if (scale > 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp)
                    .alpha(controlsAlpha),
                contentAlignment = Alignment.BottomCenter
            ) {
                IconButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_zoom_out_map_24dp),
                        contentDescription = "Reset zoom",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
