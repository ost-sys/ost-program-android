package com.ost.application.ui.activity.playback

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.audiofx.HapticGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.ost.application.R
import com.ost.application.ui.activity.animation.RadialGradientView
import com.ost.application.ui.theme.OSTToolsTheme

private const val MAIN_ACTIVITY_TAG = "MainActivity"

data class TrackMetadata(
    val title: String,
    val artist: String?,
    val albumArt: Bitmap?,
    val primaryColor: Color,
    val secondaryColor: Color
)

@ExperimentalMaterial3ExpressiveApi
class PlaybackActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(MAIN_ACTIVITY_TAG, "Notification permission granted.")
            } else {
                Log.w(MAIN_ACTIVITY_TAG, "Notification permission denied. App may not show foreground notifications.")
            }
        }

    private var activitySelectedFileUri: Uri? by mutableStateOf(null)
    private var activityTrackMetadata: TrackMetadata? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        handleIncomingIntent(intent)

        setContent {
            OSTToolsTheme {
                HapticMusicScreen(
                    selectedFileUri = activitySelectedFileUri,
                    trackMetadata = activityTrackMetadata,
                    onUriSelected = { uri, metadata ->
                        activitySelectedFileUri = uri
                        activityTrackMetadata = metadata
                        Log.d(MAIN_ACTIVITY_TAG, "onUriSelected callback: URI updated to $uri, metadata updated to ${metadata?.title}")
                    },
                    defaultPrimaryColor = MaterialTheme.colorScheme.primary,
                    defaultTertiaryColor = MaterialTheme.colorScheme.tertiary,
                    hapticAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) HapticGenerator.isAvailable() else false
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        MediaPlaybackService.requestState(this)
    }

    private fun handleIncomingIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri: Uri = intent.data!!
            Log.d(MAIN_ACTIVITY_TAG, "Received ACTION_VIEW Intent with URI: $uri")

            val contentResolver = applicationContext.contentResolver
            val isMediaStoreUri = uri.scheme == "content" && uri.authority?.contains("media", ignoreCase = true) == true

            if (uri.scheme == "content" && (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) && !isMediaStoreUri) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    Log.d(MAIN_ACTIVITY_TAG, "Took persistable URI permission for non-MediaStore URI: $uri (from ACTION_VIEW)")
                } catch (e: SecurityException) {
                    Log.e(MAIN_ACTIVITY_TAG, "Failed to take persistable URI permission for non-MediaStore URI $uri from ACTION_VIEW. This might prevent re-opening after app restart.", e)
                    activitySelectedFileUri = null
                    activityTrackMetadata = null
                    return
                } catch (e: Exception) {
                    Log.e(MAIN_ACTIVITY_TAG, "Unexpected error trying to take persistable URI permission for $uri from ACTION_VIEW", e)
                    activitySelectedFileUri = null
                    activityTrackMetadata = null
                    return
                }
            } else if (isMediaStoreUri) {
                Log.d(MAIN_ACTIVITY_TAG, "URI is from MediaStore: $uri. Relying on READ_MEDIA_AUDIO permission.")
            } else if (uri.scheme == "file") {
                Log.d(MAIN_ACTIVITY_TAG, "URI is a file path: $uri. Relying on storage permissions.")
            }

            activitySelectedFileUri = uri
            val prefs = getSharedPreferences("HapticPlayerPrefs", MODE_PRIVATE)
            val isHapticEnabled = prefs.getBoolean("HapticPlayerPrefs", true)

            MediaPlaybackService.startServiceWithUri(this, uri, isHapticEnabled)
        }
    }
}

@ExperimentalMaterial3ExpressiveApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapticMusicScreen(
    selectedFileUri: Uri?,
    trackMetadata: TrackMetadata?,
    onUriSelected: (Uri?, TrackMetadata?) -> Unit,
    defaultPrimaryColor: Color,
    defaultTertiaryColor: Color,
    hapticAvailable: Boolean
) {
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences("HapticPlayerPrefs", Context.MODE_PRIVATE) }
    var isHapticEnabled by remember { mutableStateOf(prefs.getBoolean("isHapticEnabled", true)) }

    var currentSelectedFileUri by remember(selectedFileUri) { mutableStateOf(selectedFileUri) }
    var currentTrackMetadata by remember(trackMetadata) { mutableStateOf(trackMetadata) }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var showFileInfoDialog by remember { mutableStateOf(false) }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION

            try {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                Log.d(MAIN_ACTIVITY_TAG, "Persistable URI permission granted for: $uri (from SAF)")
            } catch (e: SecurityException) {
                Log.e(MAIN_ACTIVITY_TAG, "Failed to take persistable URI permission for $uri from SAF. This might prevent re-opening after app restart.", e)
                return@rememberLauncherForActivityResult
            } catch (e: Exception) {
                Log.e(MAIN_ACTIVITY_TAG, "Unexpected error trying to take persistable URI permission for $uri from SAF.", e)
                return@rememberLauncherForActivityResult
            }

            val newMetadata = getTrackMetadata(context, uri, defaultPrimaryColor, defaultTertiaryColor)
            onUriSelected(uri, newMetadata)

            prefs.edit {
                putString("lastTrackUri", uri.toString())
            }
            MediaPlaybackService.startServiceWithUri(context, uri, isHapticEnabled)
        }
    }

    LaunchedEffect(currentSelectedFileUri) {
        if (currentSelectedFileUri != null && currentTrackMetadata == null) {
            try {
                context.contentResolver.openInputStream(currentSelectedFileUri!!)?.close()
                val newMetadata = getTrackMetadata(context, currentSelectedFileUri!!, defaultPrimaryColor, defaultTertiaryColor)
                onUriSelected(currentSelectedFileUri, newMetadata)
                val isHapticEnabledForService = prefs.getBoolean("isHapticEnabled", true)
                MediaPlaybackService.prepareService(context, currentSelectedFileUri!!, isHapticEnabledForService)
            } catch (e: Exception) {
                Log.e(MAIN_ACTIVITY_TAG, "Failed to get metadata or prepare service for URI: $currentSelectedFileUri", e)
                prefs.edit {
                    remove("lastTrackUri")
                }
                onUriSelected(null, null)
            }
        }
    }

    val playbackStateReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_BROADCAST_STATE) {
                    isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                    currentPosition = intent.getIntExtra(EXTRA_CURRENT_POSITION, 0)
                    val newDuration = intent.getIntExtra(EXTRA_DURATION, 0)
                    if (duration != newDuration) {
                        duration = newDuration
                    }

                    val receivedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(EXTRA_MEDIA_URI, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(EXTRA_MEDIA_URI)
                    }

                    if (receivedUri != null && receivedUri != currentSelectedFileUri) {
                        context?.let {
                            val newMetadata = getTrackMetadata(it, receivedUri, defaultPrimaryColor, defaultTertiaryColor)
                            onUriSelected(receivedUri, newMetadata)
                            prefs.edit {
                                putString("lastTrackUri", receivedUri.toString())
                            }
                        }
                    } else if (receivedUri == null && currentSelectedFileUri != null) {
                        onUriSelected(null, null)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter(ACTION_BROADCAST_STATE)
        LocalBroadcastManager.getInstance(context).registerReceiver(playbackStateReceiver, filter)
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(playbackStateReceiver)
        }
    }

    if (showFileInfoDialog && currentTrackMetadata != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Track Information") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Title: ${currentTrackMetadata?.title}")
                    Text("Artist: ${currentTrackMetadata?.artist ?: "Unknown"}")
                    Text("Duration: ${formatTime(duration)}")
                    Text("Haptic Feedback: ${if (hapticAvailable) "Available" else "Not Available"}")
                }
            },
            confirmButton = { TextButton(onClick = { }) { Text("OK") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(
            metadata = currentTrackMetadata,
            isPlaying = isPlaying,
            defaultPrimary = defaultPrimaryColor,
            defaultTertiary = defaultTertiaryColor
        )

        val dimmingAlpha by animateFloatAsState(
            targetValue = if (isPlaying || currentTrackMetadata == null) 0.2f else 0.6f,
            animationSpec = tween(durationMillis = 500),
            label = "DimmingAlphaAnimation"
        )
        Spacer(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimmingAlpha))
        )

        Column(
            modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())

            HapticInfo(isAvailable = hapticAvailable)

            Spacer(modifier = Modifier.weight(1f))
            PlayerContent(
                metadata = currentTrackMetadata,
                currentPosition = currentPosition,
                duration = duration,
                onSeek = { newPosition -> MediaPlaybackService.sendSeekCommand(context, newPosition) }
            )
            Spacer(modifier = Modifier.height(32.dp))
            PlayerControls(
                isPlaying = isPlaying,
                isPlayerActive = currentSelectedFileUri != null,
                isHapticToggleVisible = hapticAvailable,
                isHapticEnabled = isHapticEnabled,
                onPlayPauseClick = {
                    val action = if (isPlaying) MediaPlaybackService.ACTION_PAUSE else MediaPlaybackService.ACTION_PLAY
                    MediaPlaybackService.sendCommand(context, action)
                },
                onRewindClick = { MediaPlaybackService.sendRewindCommand(context) },
                onForwardClick = { MediaPlaybackService.sendForwardCommand(context) },
                onFileInfoClick = { },
                onHapticToggle = {
                    val newState = !isHapticEnabled
                    isHapticEnabled = newState
                    prefs.edit {
                        putBoolean("isHapticEnabled", newState)
                    }
                    MediaPlaybackService.sendHapticToggleCommand(context, newState)
                }
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) }
            ) {
                Text(
                    text = if (currentSelectedFileUri == null) "Choose a Song" else "Choose Another Song",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PlayerContent(
    metadata: TrackMetadata?, currentPosition: Int, duration: Int, onSeek: (Long) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(currentPosition.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(currentPosition) {
        if (!isDragging) {
            sliderPosition = currentPosition.toFloat()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxWidth(0.75f).aspectRatio(1f)
        ) {
            AsyncImage(
                model = metadata?.albumArt,
                contentDescription = "Album Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.ic_music_note_24dp),
                placeholder = painterResource(id = R.drawable.ic_music_note_24dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = metadata?.title ?: "Select a song",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = Color.White
        )
        if (metadata?.artist != null) {
            Text(
                text = metadata.artist,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = sliderPosition,
                onValueChange = { newValue ->
                    isDragging = true
                    sliderPosition = newValue
                },
                onValueChangeFinished = {
                    isDragging = false
                    onSeek(sliderPosition.toLong())
                },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                enabled = duration > 0,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = formatTime(currentPosition), style = MaterialTheme.typography.bodySmall, color = Color.White)
                Text(text = formatTime(duration), style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        }
    }
}

@Composable
fun PlayerControls(
    isPlaying: Boolean, isPlayerActive: Boolean, isHapticToggleVisible: Boolean, isHapticEnabled: Boolean,
    onPlayPauseClick: () -> Unit, onRewindClick: () -> Unit, onForwardClick: () -> Unit, onFileInfoClick: () -> Unit, onHapticToggle: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        val iconColor = Color.White.copy(alpha = 0.8f)
        val activeIconColor = MaterialTheme.colorScheme.primary

        IconButton(onClick = onFileInfoClick, enabled = isPlayerActive) { Icon(Icons.Default.Info, contentDescription = "File Info", tint = iconColor) }
        IconButton(onClick = onRewindClick, enabled = isPlayerActive) { Icon(Icons.Default.Replay5, contentDescription = "Rewind 5 seconds", modifier = Modifier.size(36.dp), tint = Color.White) }
        FilledIconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(72.dp),
            enabled = isPlayerActive,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Crossfade(targetState = isPlaying, label = "PlayPauseIconAnimation") { playing ->
                val icon = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow
                Icon(imageVector = icon, contentDescription = if (playing) "Pause" else "Play", modifier = Modifier.size(42.dp))
            }
        }
        IconButton(onClick = onForwardClick, enabled = isPlayerActive) { Icon(Icons.Default.Forward5, contentDescription = "Forward 5 seconds", modifier = Modifier.size(36.dp), tint = Color.White) }
        if (isHapticToggleVisible) {
            IconButton(onClick = onHapticToggle) {
                val icon = if (isHapticEnabled) Icons.Filled.Vibration else Icons.Outlined.Vibration
                val tint = if (isHapticEnabled) activeIconColor else iconColor
                Icon(icon, contentDescription = "Toggle Haptics", tint = tint)
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
fun AnimatedBackground(metadata: TrackMetadata?, isPlaying: Boolean, defaultPrimary: Color, defaultTertiary: Color) {
    val primaryColor = metadata?.primaryColor ?: defaultPrimary
    val tertiaryColor = metadata?.secondaryColor ?: defaultTertiary
    val secondaryColor = Color.Transparent
    val startAnimation = metadata != null && isPlaying

    RadialGradientBackground(
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        tertiaryColor = tertiaryColor,
        startAnimation = startAnimation,
        alignType = RadialGradientView.AlignType.LTR,
        showArc = false,
        gradientPatternResId = R.raw.radial_gradient
    )
}

@Composable
fun RadialGradientBackground(
    modifier: Modifier = Modifier, startAnimation: Boolean, primaryColor: Color, secondaryColor: Color, tertiaryColor: Color,
    alignType: RadialGradientView.AlignType, showArc: Boolean, gradientPatternResId: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val radialGradientView = remember { RadialGradientView(context) }

    DisposableEffect(lifecycleOwner) {
        radialGradientView.init(alignType, gradientPatternResId)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (radialGradientView.isAnimating) radialGradientView.startAnimation()
                Lifecycle.Event.ON_STOP -> radialGradientView.stopAnimation()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            radialGradientView.releaseResources()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { radialGradientView },
        update = { view ->
            view.setColors(primaryColor.toArgb(), secondaryColor.toArgb(), tertiaryColor.toArgb())
            view.setArcShow(showArc)
            if (startAnimation && !view.isAnimating) {
                view.startAnimation()
            } else if (!startAnimation && view.isAnimating) {
                view.stopAnimation()
            }
        }
    )
}

@Composable
fun HapticInfo(isAvailable: Boolean) {
    val hapticColor = Color.White.copy(alpha = 0.8f)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Icon(imageVector = Icons.Default.Vibration, contentDescription = "Haptic Feedback", tint = hapticColor)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = if (isAvailable) "Haptic feedback available" else "Haptic feedback not available", style = MaterialTheme.typography.bodyMedium, color = hapticColor)
    }
}

fun getTrackMetadata(context: Context, uri: Uri, defaultPrimary: Color, defaultTertiary: Color): TrackMetadata? {
    return try {
        val retriever = MediaMetadataRetriever()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
            } ?: run {
                Log.e(MAIN_ACTIVITY_TAG, "Failed to open file descriptor for MediaMetadataRetriever on $uri. Check URI access.")
                return null
            }
        } catch (e: Exception) {
            Log.e(MAIN_ACTIVITY_TAG, "Failed to set data source for MediaMetadataRetriever on $uri", e)
            return null
        }

        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val albumArtBytes = retriever.embeddedPicture
        val albumArt: Bitmap? = if (albumArtBytes != null) BitmapFactory.decodeByteArray(albumArtBytes, 0, albumArtBytes.size) else null

        var primaryColor: Color? = null
        var secondaryColor: Color? = null
        if (albumArt != null) {
            val palette = Palette.from(albumArt).generate()
            palette.dominantSwatch?.rgb?.let { primaryColor = Color(it) }
            palette.vibrantSwatch?.rgb?.let { secondaryColor = Color(it) }
        }

        retriever.release()
        val metadata = TrackMetadata(
            title = title ?: uri.lastPathSegment?.substringBeforeLast(".") ?: "Unknown Title",
            artist = artist ?: "Unknown Artist",
            albumArt = albumArt,
            primaryColor = primaryColor ?: defaultPrimary,
            secondaryColor = secondaryColor ?: defaultTertiary
        )
        Log.d(MAIN_ACTIVITY_TAG, "Successfully retrieved metadata for $uri: Title='${metadata.title}', Artist='${metadata.artist}'")
        return metadata
    } catch (e: Exception) {
        Log.e(MAIN_ACTIVITY_TAG, "Failed to retrieve metadata for $uri (outer catch)", e)
        null
    }
}

fun formatTime(millis: Int): String {
    val minutes = millis / 1000 / 60
    val seconds = (millis / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}