@file:OptIn(ExperimentalHorologistApi::class)

package com.ost.application.explorer.music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.audio.ui.material3.VolumeScreen
import com.google.android.horologist.media.ui.material3.components.PodcastControlButtons
import com.google.android.horologist.media.ui.material3.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.state.PlayerUiController
import com.google.android.horologist.media.ui.state.PlayerUiState
import com.google.android.horologist.media.ui.state.model.MediaUiModel
import com.ost.application.R

@Composable
fun MainPlayerScreen(
    launchMode: String,
    singleTrackUri: Uri?,
    singleTrackViewModel: MusicViewModel,
    volumeViewModel: VolumeViewModel,
    context: Context
) {
    when (launchMode) {
        MusicActivity.MODE_SINGLE_FILE -> {
            if (singleTrackUri != null) {
                MusicPlayerScreen(
                    context = context,
                    uri = singleTrackUri,
                    musicViewModel = singleTrackViewModel,
                    volumeViewModel = volumeViewModel
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: No music file specified.")
                }
            }
        }
        MusicActivity.MODE_FULL_PLAYER -> {
            FullPlayerScreen(
                musicViewModel = singleTrackViewModel,
                volumeViewModel = volumeViewModel,
                context = context
            )
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: Unknown launch mode.")
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun MusicPlayerScreen(
    context: Context,
    uri: Uri,
    musicViewModel: MusicViewModel,
    volumeViewModel: VolumeViewModel
) {
    var albumArtBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showVolumeScreen by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        val retriever = MediaMetadataRetriever()
        var fetchedTitle: String? = null
        var fetchedArtist: String? = null
        try {
            retriever.setDataSource(context, uri)
            fetchedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            fetchedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val artBytes = retriever.embeddedPicture
            albumArtBitmap = artBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

            musicViewModel.setMediaUri(uri.toString(), fetchedTitle, fetchedArtist)

        } catch (e: Exception) {
            Log.e("MusicPlayerScreen", "Error retrieving media metadata", e)
            albumArtBitmap = null
            musicViewModel.setMediaUri(uri.toString(), "Error", "Metadata Error")
        } finally {
            try { retriever.release() } catch (e: Exception) { Log.e("MusicPlayerScreen", "Error releasing MediaMetadataRetriever", e) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        timeText = { TimeText() }
    ) {
        if (showVolumeScreen) {
            Dialog(onDismissRequest = { showVolumeScreen = false }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    VolumeScreen(volumeViewModel = volumeViewModel)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (albumArtBitmap != null) {
                Image(
                    bitmap = albumArtBitmap!!.asImageBitmap(),
                    contentDescription = "Album Art Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.radialGradient(colors = listOf(Color(0xFF303030), Color(0xFF101010)))
                    )
                )
            }

            PlayerScreen(
                modifier = Modifier.padding(horizontal = 5.dp),
                playerViewModel = musicViewModel,
                volumeViewModel = volumeViewModel,

                mediaDisplay = { playerUiState: PlayerUiState ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val readyModel = playerUiState.media as? MediaUiModel.Ready
                        if (readyModel != null) {
                            Text( text = readyModel.title, color = Color.White, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text( text = readyModel.subtitle, color = Color.LightGray, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center )
                        } else {
                            Text( text = "Media loading...", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), textAlign = TextAlign.Center )
                            Text( text = "(Waiting for player/state)", color = Color.Gray, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center )
                        }
                    }
                },

                controlButtons = { playerController: PlayerUiController, playerUiState: PlayerUiState ->
                    PodcastControlButtons( playerController = playerController, playerUiState = playerUiState )
                },
                buttons = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showVolumeScreen = true },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon( painter = painterResource(R.drawable.ic_volume_up_24dp), contentDescription = "Adjust Volume" )
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun FullPlayerScreen(
    musicViewModel: MusicViewModel,
    volumeViewModel: VolumeViewModel,
    context: Context
) {
    var showVolumeScreen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        timeText = { TimeText() }
    ) {
        if (showVolumeScreen) {
            Dialog(onDismissRequest = { showVolumeScreen = false }) {
                VolumeScreen(volumeViewModel = volumeViewModel)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(colors = listOf(Color(0xFF303030), Color(0xFF101010)))
            )
        ) {
            PlayerScreen(
                playerViewModel = musicViewModel,
                volumeViewModel = volumeViewModel,

                mediaDisplay = { currentUiState: PlayerUiState ->
                    val readyModel = currentUiState.media as? MediaUiModel.Ready
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (readyModel != null) {
                            Text(
                                text = readyModel.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = readyModel.subtitle,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = Color.LightGray
                            )
                        } else {
                            Text(
                                "No track selected",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }
                },

                controlButtons = { playerController: PlayerUiController, currentUiState: PlayerUiState ->
                    PodcastControlButtons(
                        playerController = playerController,
                        playerUiState = currentUiState
                    )
                },

                buttons = { currentUiState: PlayerUiState ->
                    IconButton(
                        onClick = { showVolumeScreen = true },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_volume_up_24dp), contentDescription = "Volume")
                    }
                }
            )
        }
    }
}