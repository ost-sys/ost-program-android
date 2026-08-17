@file:OptIn(ExperimentalHorologistApi::class)
package com.ost.application.explorer.music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.audio.ui.material3.VolumeScreen
import com.google.android.horologist.media.ui.state.PlayerViewModel
import com.ost.application.R
import com.ost.application.component.player.OSTMediaPlayerScreen

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
                    Text(stringResource(R.string.error_no_music_file))
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
                Text(stringResource(R.string.error_unknown_launch_mode))
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
    val errorStr = stringResource(R.string.error)
    val unknownTitleStr = stringResource(R.string.unknown_title)
    val unknownArtistStr = stringResource(R.string.unknown_artist)

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
            musicViewModel.setMediaUri(
                uri.toString(),
                fetchedTitle ?: unknownTitleStr,
                fetchedArtist ?: unknownArtistStr
            )
        } catch (e: Exception) {
            Log.e("MusicPlayerScreen", "Error retrieving media metadata", e)
            albumArtBitmap = null
            musicViewModel.setMediaUri(uri.toString(), errorStr, errorStr)
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
            OSTMediaPlayerScreen(
                playerViewModel = musicViewModel,
                volumeViewModel = volumeViewModel,
                artBitmap = albumArtBitmap,
                onVolumeClick = { showVolumeScreen = true },
                fallbackTitle = stringResource(R.string.media_loading),
                fallbackSubtitle = stringResource(R.string.waiting_for_player)
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
        OSTMediaPlayerScreen(
            playerViewModel = musicViewModel,
            volumeViewModel = volumeViewModel,
            artBitmap = null,
            onVolumeClick = { showVolumeScreen = true },
            fallbackTitle = stringResource(R.string.no_track_selected)
        )
    }
}
