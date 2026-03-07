@file:OptIn(ExperimentalHorologistApi::class)

package com.ost.application.explorer.music

import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.model.Media
import com.google.android.horologist.media.ui.state.PlayerViewModel
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MusicViewModel(
    player: Player,
    private val playerRepository: PlayerRepositoryImpl
) : PlayerViewModel(playerRepository) {

    init {
        viewModelScope.launch {
            playerRepository.connect(player) { }
        }
    }

    fun setMediaUri(uri: String, title: String?, artist: String?) {
        viewModelScope.launch {
            try {
                playerRepository.setMedia(
                    Media(id = uri, uri = uri, title = title ?: "Unknown Title", artist = artist ?: "Unknown Artist")
                )
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error setting media URI: $uri", e)
            }
        }
    }
}