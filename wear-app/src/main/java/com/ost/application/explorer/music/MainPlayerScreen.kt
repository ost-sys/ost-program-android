@file:OptIn(ExperimentalHorologistApi::class)

package com.ost.application.explorer.music

import MusicViewModel
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumeScreen
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.media.ui.components.PodcastControlButtons
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.state.PlayerUiController
import com.google.android.horologist.media.ui.state.PlayerUiState
import com.google.android.horologist.media.ui.state.model.MediaUiModel
import com.ost.application.R

private const val TAG_SCREENS = "PlayerScreens"

@Composable
fun MainPlayerScreen(
    launchMode: String,
    singleTrackUri: Uri?,
    singleTrackViewModel: MusicViewModel,
    volumeViewModel: VolumeViewModel,
    context: Context
) {
    when (launchMode) {
        MusicActivity.Companion.MODE_SINGLE_FILE -> {
            Log.d(TAG_SCREENS, "Rendering SINGLE FILE player UI")
            if (singleTrackUri != null) {
                MusicPlayerScreen(
                    context = context,
                    uri = singleTrackUri,
                    musicViewModel = singleTrackViewModel,
                    volumeViewModel = volumeViewModel
                )
            } else {
                Log.e(TAG_SCREENS, "Error: singleTrackUri is null in MODE_SINGLE_FILE UI")
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: No music file specified.")
                }
            }
        }
        MusicActivity.Companion.MODE_FULL_PLAYER -> {
            Log.d(TAG_SCREENS, "Rendering FULL PLAYER UI")
            FullPlayerScreen(
                musicViewModel = singleTrackViewModel,
                volumeViewModel = volumeViewModel,
                context = context
            )
        }
        else -> {
            Log.e(TAG_SCREENS, "Error: Unknown launch mode: $launchMode")
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
        Log.d(TAG_SCREENS, "LaunchedEffect (Single): Starting metadata retrieval for $uri")
        val retriever = MediaMetadataRetriever()
        var fetchedTitle: String? = null
        var fetchedArtist: String? = null
        try {
            retriever.setDataSource(context, uri)
            fetchedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            fetchedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val artBytes = retriever.embeddedPicture
            albumArtBitmap = artBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            Log.i(TAG_SCREENS, "Metadata retrieved (Single): Title='${fetchedTitle}', Artist='${fetchedArtist}', Has Art=${albumArtBitmap != null}")

            Log.d(TAG_SCREENS, "LaunchedEffect (Single): Calling setMediaUri with: Title='${fetchedTitle}', Artist='${fetchedArtist}'")
            musicViewModel.setMediaUri(uri.toString(), fetchedTitle, fetchedArtist)

        } catch (e: Exception) {
            Log.e(TAG_SCREENS, "Error loading metadata (Single): ${e.message}", e)
            albumArtBitmap = null
            Log.d(TAG_SCREENS, "LaunchedEffect (Single): Calling setMediaUri due to error")
            musicViewModel.setMediaUri(uri.toString(), "Error", "Metadata Error")
        } finally {
            try { retriever.release() } catch (e: Exception) { Log.e(TAG_SCREENS, "Error releasing retriever (Single)", e) }
            Log.d(TAG_SCREENS, "LaunchedEffect (Single): Metadata retriever released.")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        timeText = { TimeText() }
    ) {
        if (showVolumeScreen) {
            Log.d(TAG_SCREENS,"Displaying VolumeScreen (Single)")
            Dialog(onDismissRequest = { showVolumeScreen = false }) {
                VolumeScreen(volumeViewModel = volumeViewModel)
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
                        Brush.linearGradient(colors = listOf(Color(0xFF303030), Color(0xFF101010)))
                    )
                )
            }

            PlayerScreen(
                modifier = Modifier.padding(horizontal = 5.dp),
                playerViewModel = musicViewModel,
                volumeViewModel = volumeViewModel,

                mediaDisplay = { playerUiState: PlayerUiState ->
                    SideEffect {
                        val mediaUiModel = playerUiState.media
                        val mediaType = mediaUiModel?.javaClass?.name ?: "null"
                        val uiTitle = (mediaUiModel as? MediaUiModel.Ready)?.title
                        val uiSubtitle = (mediaUiModel as? MediaUiModel.Ready)?.subtitle
                        Log.d(TAG_SCREENS, "DEBUG mediaDisplay recomposition (Single): mediaUiModel type = $mediaType, UI title = $uiTitle, UI subtitle = $uiSubtitle")
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val readyModel = playerUiState.media as? MediaUiModel.Ready
                        if (readyModel != null) {
                            Text( text = readyModel.title, color = Color.White, style = MaterialTheme.typography.button.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text( text = readyModel.subtitle, color = Color.LightGray, style = MaterialTheme.typography.caption1, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center )
                        } else {
                            Text( text = "Media loading...", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.button.copy(fontSize = 15.sp), textAlign = TextAlign.Center )
                            Text( text = "(Waiting for player/state)", color = Color.Gray, style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center )
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
                        Button(
                            onClick = { Log.d(TAG_SCREENS,"Volume button clicked (Single)"); showVolumeScreen = true },
                            modifier = Modifier.size(24.dp),
                            colors = ButtonDefaults.buttonColors( backgroundColor = Color.Transparent)
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

    Column(modifier = Modifier.fillMaxSize()) {

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
                            style = MaterialTheme.typography.button.copy(fontSize = 15.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = readyModel.subtitle,
                            style = MaterialTheme.typography.caption1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = Color.LightGray
                        )
                    } else {
                        Text(
                            "Трек не выбран",
                            style = MaterialTheme.typography.body1,
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
                var showVolumeScreen by remember { mutableStateOf(false) }
                Button(
                    onClick = { showVolumeScreen = true },
                    modifier = Modifier.size(24.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)
                ) {
                    Icon(painter = painterResource(R.drawable.ic_volume_up_24dp), contentDescription = "Volume")
                }
            }
        )
    }
}