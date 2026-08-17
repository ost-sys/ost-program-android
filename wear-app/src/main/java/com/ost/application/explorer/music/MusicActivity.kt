@file:OptIn(ExperimentalHorologistApi::class)
package com.ost.application.explorer.music
import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
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
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.ost.application.theme.OSTToolsTheme
internal const val MUSIC_ACTIVITY_TAG = "MusicActivityLog"
private const val SEEK_INCREMENT_MS = 10000L
class MusicActivity : ComponentActivity() {
    companion object {
        const val LAUNCH_MODE_EXTRA = "launch_mode"
        const val MODE_FULL_PLAYER = "mode_full"
        const val MODE_SINGLE_FILE = "mode_single"
    }
    private var player: Player? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var musicUri: Uri? = null
    private var launchMode by mutableStateOf(MODE_SINGLE_FILE)
    private var pendingAction: (() -> Unit)? = null
    private val musicViewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val activePlayer = player ?: throw IllegalStateException("Player not initialized for ViewModel")
            if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                return MusicViewModel(activePlayer, PlayerRepositoryImpl()) as T
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
                pendingAction?.invoke()
                pendingAction = null
            } else {
                Log.w(MUSIC_ACTIVITY_TAG, "Permission denied, finishing activity.")
                finish()
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incomingIntent = intent
        val action = incomingIntent.action
        val data = incomingIntent.data
        val modeExtra = incomingIntent.getStringExtra(LAUNCH_MODE_EXTRA)
        val musicPath = incomingIntent.getStringExtra("musicPath")
        when {
            modeExtra == MODE_FULL_PLAYER || (action == Intent.ACTION_MAIN && data == null) -> {
                startActivity(Intent(this, LibraryActivity::class.java))
                finish()
                return
            }
            (action == Intent.ACTION_VIEW && data != null) || (modeExtra == MODE_SINGLE_FILE && data != null) -> {
                launchMode = MODE_SINGLE_FILE
                musicUri = data
            }
            musicPath != null -> {
                launchMode = MODE_SINGLE_FILE
                musicUri = "file://$musicPath".toUri()
            }
            else -> {
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
                actionToPerform()
            }
            else -> {
                pendingAction = actionToPerform
                requestPermissionLauncher.launch(permission)
            }
        }
    }
    @androidx.annotation.OptIn(UnstableApi::class)
    private fun initializePlayerAndSetContent() {
        if (launchMode == MODE_SINGLE_FILE && musicUri == null) {
            Log.e(MUSIC_ACTIVITY_TAG, "Attempted to launch in single file mode but no URI was provided.")
            finish()
            return
        }
        val future = MediaController.Builder(
            this,
            SessionToken(this, ComponentName(this, PlaybackService::class.java))
        ).buildAsync()
        controllerFuture = future
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull()
            if (controller == null) {
                Log.e(MUSIC_ACTIVITY_TAG, "Failed to connect MediaController")
                finish()
                return@addListener
            }
            player = controller
            setContent {
                OSTToolsTheme {
                    MainPlayerScreen(
                        launchMode = launchMode,
                        singleTrackUri = musicUri,
                        singleTrackViewModel = musicViewModel,
                        volumeViewModel = volumeViewModel,
                        context = this
                    )
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }
    @SuppressLint("ServiceCast")
    fun createVolumeViewModel(): VolumeViewModel {
        val audioRepository = SystemAudioRepository.fromContext(applicationContext)
        val vibrator: Vibrator = applicationContext.getSystemService(VIBRATOR_SERVICE) as? Vibrator
            ?: throw IllegalStateException("Vibrator service not found.")
        return VolumeViewModel(
            volumeRepository = audioRepository,
            audioOutputRepository = audioRepository,
            vibrator = vibrator,
            onCleared = { audioRepository.close() }
        )
    }
    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        player = null
    }
}
