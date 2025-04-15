@file:OptIn(ExperimentalHorologistApi::class, UnstableApi::class)

package com.ost.application.explorer.music

import MusicViewModel
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.wear.compose.material.MaterialTheme
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl

internal const val TAG = "MusicActivityLog"

class MusicActivity : ComponentActivity() {

    companion object {
        const val LAUNCH_MODE_EXTRA = "launch_mode"
        const val MODE_FULL_PLAYER = "mode_full"
        const val MODE_SINGLE_FILE = "mode_single"
    }

    private lateinit var player: ExoPlayer
    private var musicUri: Uri? = null
    private var launchMode by mutableStateOf(MODE_SINGLE_FILE)
    private var pendingAction: (() -> Unit)? = null

    private val musicViewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (!::player.isInitialized) { throw IllegalStateException("Player not initialized for ViewModel") }
            if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                return MusicViewModel(player, PlayerRepositoryImpl()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
    private val musicViewModel: MusicViewModel by viewModels { musicViewModelFactory }

    private val volumeViewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VolumeViewModel::class.java)) { return createVolumeViewModel() as T }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
    private val volumeViewModel: VolumeViewModel by viewModels { volumeViewModelFactory }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i(TAG, "Permission Granted!")
                pendingAction?.invoke(); pendingAction = null
            } else {
                Log.w(TAG, "Permission Denied!"); finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started.")

        val incomingIntent = intent
        val action = incomingIntent.action
        val data = incomingIntent.data
        val modeExtra = incomingIntent.getStringExtra(LAUNCH_MODE_EXTRA)

        Log.d(TAG, "Intent action: $action, data: $data, modeExtra: $modeExtra")

        if (modeExtra == MODE_FULL_PLAYER || (action == Intent.ACTION_MAIN && data == null)) {
            launchMode = MODE_FULL_PLAYER
            Log.i(TAG, "Launch mode determined: FULL PLAYER")
            musicUri = null
        } else if (action == Intent.ACTION_VIEW && data != null) {
            launchMode = MODE_SINGLE_FILE
            musicUri = data
            Log.i(TAG, "Launch mode determined: SINGLE FILE (from VIEW action), Uri: $musicUri")
        } else {
            val musicPath = incomingIntent.getStringExtra("musicPath")
            if (musicPath != null) {
                launchMode = MODE_SINGLE_FILE
                musicUri = "file://$musicPath".toUri()
                Log.i(TAG, "Launch mode determined: SINGLE FILE (from path extra), Uri: $musicUri")
            } else {
                Log.w(TAG, "Could not determine launch mode or file. Defaulting to FULL PLAYER.")
                launchMode = MODE_FULL_PLAYER
                musicUri = null
            }
        }

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val actionToPerform = { initializePlayerAndSetContent() }
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Permission already granted.")
                actionToPerform()
            }
            else -> {
                Log.i(TAG, "Requesting permission: $permission")
                pendingAction = actionToPerform
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun initializePlayerAndSetContent() {
        if (launchMode == MODE_SINGLE_FILE && musicUri == null) {
            Log.e(TAG, "Error: musicUri is null in SINGLE FILE mode during initialization.")
            finish(); return
        }
        Log.i(TAG, "Initializing Player and UI for mode: $launchMode")

        player = ExoPlayer.Builder(this)
            .setSeekForwardIncrementMs(10000L)
            .setSeekBackIncrementMs(10000L)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { Log.i(TAG, "PlayerListener: >>> onMediaItemTransition - ID: ${mediaItem?.mediaId}, reason: $reason") }
                    override fun onPlayerError(error: PlaybackException) { Log.e(TAG, "PlayerListener: >>> onPlayerError - Code: ${error.errorCodeName}, Msg: ${error.message}", error); error.cause?.let { Log.e(TAG, ">>> Cause: ${it.message}", it) } }
                    override fun onPlaybackStateChanged(playbackState: Int) { val stateString = when(playbackState){ /*...*/ else -> {}
                    }; Log.i(TAG, "PlayerListener: >>> onPlaybackStateChanged - $stateString") }
                    override fun onIsPlayingChanged(isPlaying: Boolean) { Log.i(TAG, "PlayerListener: >>> onIsPlayingChanged - $isPlaying") }
                })
            }

        setContent {
            MaterialTheme {
                MainPlayerScreen(
                    launchMode = launchMode,
                    singleTrackUri = musicUri,
                    singleTrackViewModel = musicViewModel,
                    volumeViewModel = volumeViewModel,
                    context = this
                )
            }
        }
        Log.i(TAG, "Player and UI Initialized for mode: $launchMode")
    }

    @SuppressLint("ServiceCast")
    fun createVolumeViewModel(): VolumeViewModel {
        val audioRepository = SystemAudioRepository.fromContext(applicationContext)
        val vibrator: Vibrator = applicationContext.getSystemService(Vibrator::class.java)
            ?: throw IllegalStateException("Vibrator service not found.")
        Log.i(TAG, "Vibrator service obtained.")
        return VolumeViewModel(
            volumeRepository = audioRepository, audioOutputRepository = audioRepository,
            vibrator = vibrator,
            onCleared = { Log.i(TAG, "VolumeViewModel cleared."); audioRepository.close() }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::player.isInitialized) { player.release() }
        Log.i(TAG, "MusicActivity onDestroy: Player released.")
    }
}