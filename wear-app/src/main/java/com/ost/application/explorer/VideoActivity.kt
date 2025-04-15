package com.ost.application.explorer

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import com.ost.application.R
import kotlinx.coroutines.delay

class VideoActivity : ComponentActivity() {

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoPath = intent.getStringExtra("videoPath") ?: ""

        setContent {
            if (videoPath.isNotEmpty()) {
                val context = LocalContext.current
                val videoUri = "file://$videoPath".toUri()

                val exoPlayer = remember(context) {
                    ExoPlayer.Builder(context).build().apply {
                        val mediaItem = MediaItem.fromUri(videoUri)
                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true
                    }
                }

                val playerView = remember(context) {
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = false
                        this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                }

                val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                var currentVolume by remember {
                    mutableStateOf(
                        audioManager.getStreamVolume(
                            AudioManager.STREAM_MUSIC
                        )
                    )
                }
                var videoProgress by remember { mutableFloatStateOf(0f) }
                var isPlaying by remember { mutableStateOf(true) }
                var controlsVisible by remember { mutableStateOf(true) }

                LaunchedEffect(controlsVisible, isPlaying) {
                    if (controlsVisible && isPlaying) {
                        delay(3000)
                        controlsVisible = false
                    }
                }

                val alpha: Float by animateFloatAsState(
                    targetValue = if (controlsVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = 500)
                )

                LaunchedEffect(exoPlayer) {
                    while (true) {
                        delay(1000)
                        if (exoPlayer.duration != 0L) {
                            videoProgress =
                                exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat()
                        }
                    }
                }

                DisposableEffect(key1 = exoPlayer) {
                    onDispose {
                        exoPlayer.release()
                    }
                }

                Box(
                    modifier = Modifier.Companion
                        .fillMaxSize()
                        .clickable {
                            controlsVisible = true
                        },
                    contentAlignment = Alignment.Companion.Center
                ) {
                    AndroidView(
                        factory = { playerView },
                        modifier = Modifier.Companion.fillMaxSize()
                    )

                    CircularProgressIndicator(
                        progress = videoProgress,
                        modifier = Modifier.Companion.fillMaxSize(),
                    )

                    Column(
                        modifier = Modifier.Companion
                            .fillMaxSize()
                            .padding(16.dp)
                            .alpha(alpha),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Companion.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.Companion.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.Companion.fillMaxWidth()
                        ) {
                            Button(
                                modifier = Modifier.Companion.size(24.dp),
                                colors = ButtonDefaults.secondaryButtonColors(),
                                onClick = {
                                    currentVolume = (currentVolume - 1)
                                        .coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        currentVolume,
                                        0
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_volume_down_24dp),
                                    contentDescription = "Volume Down",
                                )
                            }

                            Button(onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                    isPlaying = false
                                    controlsVisible =
                                        true
                                } else {
                                    if (exoPlayer.playbackState == ExoPlayer.STATE_ENDED) {
                                        exoPlayer.seekTo(0)
                                    }
                                    exoPlayer.play()
                                    isPlaying = true
                                    controlsVisible = true
                                }
                            }) {
                                Icon(
                                    painter = painterResource(
                                        id = if (exoPlayer.isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp
                                    ),
                                    contentDescription = if (exoPlayer.isPlaying) "Pause" else "Play",
                                    modifier = Modifier.Companion.size(48.dp)
                                )
                            }

                            Button(
                                modifier = Modifier.Companion.size(24.dp),
                                colors = ButtonDefaults.secondaryButtonColors(),
                                onClick = {
                                    currentVolume = (currentVolume + 1)
                                        .coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        currentVolume,
                                        0
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_volume_up_24dp),
                                    contentDescription = "Volume Up",
                                    modifier = Modifier.Companion.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}