package com.ost.application.explorer
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import com.ost.application.R
import com.ost.application.theme.OSTToolsTheme
import kotlinx.coroutines.delay
class VideoActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoUri = intent.getStringExtra("videoPath")?.let { "file://$it".toUri() }
            ?: intent.data
            ?: run {
                finish()
                return
            }
        setContent {
            OSTToolsTheme {
                VideoPlayerScreen(videoUri = videoUri)
            }
        }
    }
}
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(videoUri: android.net.Uri) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }
    val playerView = remember {
        PlayerView(context).apply {
            player = exoPlayer
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            keepScreenOn = true
        }
    }
    val audioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    var videoProgress by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isEnded by remember { mutableStateOf(false) }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                isEnded = state == Player.STATE_ENDED
                if (isEnded) controlsVisible = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying && !isEnded) {
            delay(3000)
            controlsVisible = false
        }
    }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val duration = exoPlayer.duration
            if (duration > 0L) {
                videoProgress = exoPlayer.currentPosition.toFloat() / duration.toFloat()
            }
            delay(500)
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { playerView },
            modifier = Modifier.fillMaxSize()
        )
        CircularProgressIndicator(
            progress = { videoProgress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 4.dp,
            colors = androidx.wear.compose.material3.ProgressIndicatorDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                trackColor = Color.Transparent
            )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(controlsAlpha)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    SmallControlButton(
                        iconRes = R.drawable.ic_volume_down_24dp,
                        contentDescription = "Volume down",
                        onClick = {
                            currentVolume = (currentVolume - 1).coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                            controlsVisible = true
                        }
                    )
                    IconButton(
                        onClick = {
                            when {
                                isEnded -> {
                                    exoPlayer.seekTo(0)
                                    exoPlayer.play()
                                    isEnded = false
                                }
                                exoPlayer.isPlaying -> exoPlayer.pause()
                                else -> exoPlayer.play()
                            }
                            controlsVisible = true
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        val iconRes = when {
                            isEnded -> R.drawable.ic_replay_24dp
                            isPlaying -> R.drawable.ic_pause_24dp
                            else -> R.drawable.ic_play_24dp
                        }
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    SmallControlButton(
                        iconRes = R.drawable.ic_volume_up_24dp,
                        contentDescription = "Volume up",
                        onClick = {
                            currentVolume = (currentVolume + 1).coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                            controlsVisible = true
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun SmallControlButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
