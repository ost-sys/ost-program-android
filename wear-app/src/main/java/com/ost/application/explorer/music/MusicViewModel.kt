@file:OptIn(ExperimentalHorologistApi::class)

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.model.Media
import com.google.android.horologist.media.ui.state.PlayerViewModel
import com.ost.application.explorer.music.TAG
import kotlinx.coroutines.launch

@SuppressLint("UnsafeOptInUsageError")
class MusicViewModel(
    player: Player,
    private val playerRepository: PlayerRepositoryImpl
) : PlayerViewModel(playerRepository) {

    init {
        Log.d(TAG, "MusicViewModel: Initializing...")
        viewModelScope.launch {
            playerRepository.connect(player) { Log.d(TAG,"PlayerRepository connect callback triggered.") }
            Log.d(TAG,"MusicViewModel: connect coroutine launched.")
        }
    }

    fun setMediaUri(uri: String, title: String?, artist: String?) {
        Log.d(TAG,"MusicViewModel: setMediaUri called.")
        viewModelScope.launch {
            Log.d(TAG,"MusicViewModel: viewModelScope.launch in setMediaUri started.")
            Log.d(TAG,"MusicViewModel: Setting media in repository - Title: $title, Artist: $artist, Uri: $uri")
            try {
                playerRepository.setMedia(
                    Media(id = uri, uri = uri, title = title ?: "Unknown Title", artist = artist ?: "Unknown Artist")
                )
                Log.d(TAG,"MusicViewModel: setMedia call completed (via repository).")
            } catch (e: Exception) {
                Log.e(TAG,"MusicViewModel: Error calling playerRepository.setMedia: ${e.message}", e)
            }
            Log.d(TAG,"MusicViewModel: viewModelScope.launch in setMediaUri finished.")
        }
        Log.d(TAG,"MusicViewModel: setMediaUri call finished (after launching coroutine).")
    }
}