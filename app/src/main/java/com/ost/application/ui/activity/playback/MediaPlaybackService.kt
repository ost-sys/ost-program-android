package com.ost.application.ui.activity.playback

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.audiofx.HapticGenerator
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.session.MediaButtonReceiver
import com.ost.application.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.media.app.NotificationCompat as MediaNotificationCompat

const val NOTIFICATION_CHANNEL_ID = "haptic_music_playback_channel"
const val NOTIFICATION_ID = 101
const val ACTION_BROADCAST_STATE = "com.ost.application.BROADCAST_STATE"
const val EXTRA_IS_PLAYING = "isPlaying"
const val EXTRA_CURRENT_POSITION = "currentPosition"
const val EXTRA_DURATION = "duration"
const val EXTRA_IS_HAPTIC_AVAILABLE = "isHapticAvailable"
const val EXTRA_MEDIA_URI = "com.ost.application.EXTRA_MEDIA_URI"
const val ACTION_REQUEST_STATE = "com.ost.application.ACTION_REQUEST_STATE"
private const val MEDIA_SERVICE_TAG = "MediaPlaybackService"

@ExperimentalMaterial3ExpressiveApi
class MediaPlaybackService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var hapticGenerator: HapticGenerator? = null
    private var mediaSession: MediaSessionCompat? = null
    private var currentMediaUri: Uri? = null
    private var isHapticAvailableOnDevice: Boolean = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var playbackUpdateJob: Job? = null
    private var currentPlaybackState: Int = PlaybackStateCompat.STATE_NONE
    private var currentPlaybackPosition: Int = 0
    private var currentPlaybackDuration: Int = 0
    private var isHapticEnabled: Boolean = true
    private var isServiceForeground = false

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() { startPlayback() }
        override fun onPause() { pausePlayback() }
        override fun onStop() { stopPlayback(true) }
        override fun onSeekTo(pos: Long) {
            mediaPlayer?.let {
                val newPosition = pos.coerceIn(0, currentPlaybackDuration.toLong())
                it.seekTo(newPosition.toInt())
                currentPlaybackPosition = newPosition.toInt()
                updateMediaSessionAndNotification()
            }
        }
        override fun onRewind() { onSeekTo(currentPlaybackPosition - 5000L) }
        override fun onFastForward() { onSeekTo(currentPlaybackPosition + 5000L) }
        override fun onSkipToPrevious() { onRewind() }
        override fun onSkipToNext() { onFastForward() }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isHapticAvailableOnDevice = HapticGenerator.isAvailable()
        }
        val prefs = getSharedPreferences("HapticPlayerPrefs", MODE_PRIVATE)
        isHapticEnabled = prefs.getBoolean("isHapticEnabled", true)

        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mediaSession = MediaSessionCompat(applicationContext, "HapticMusicSession", mediaButtonReceiver, null).apply {
            setCallback(mediaSessionCallback)
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            mediaSession?.let { MediaButtonReceiver.handleIntent(it, intent) }
        }
        when (intent?.action) {
            ACTION_PLAY -> mediaSessionCallback.onPlay()
            ACTION_PAUSE -> mediaSessionCallback.onPause()
            ACTION_STOP, ACTION_STOP_SERVICE_FROM_NOTIFICATION -> mediaSessionCallback.onStop()
            ACTION_SEEK_TO -> mediaSessionCallback.onSeekTo(intent.getLongExtra(EXTRA_SEEK_POSITION, 0L))
            ACTION_REWIND_5 -> mediaSessionCallback.onRewind()
            ACTION_FORWARD_5 -> mediaSessionCallback.onFastForward()
            ACTION_TOGGLE_HAPTIC -> {
                isHapticEnabled = intent.getBooleanExtra(EXTRA_HAPTIC_ENABLED, true)
                hapticGenerator?.setEnabled(isHapticEnabled)
                updateMediaSessionAndNotification()
            }
            ACTION_SET_SOURCE_AND_PLAY -> {
                startForeground(NOTIFICATION_ID, buildNotification(isLoading = true))
                isServiceForeground = true
                val uri = getUriFromIntent(intent)
                val hapticEnabled = intent.getBooleanExtra(EXTRA_HAPTIC_ENABLED, true)
                if (uri != null) {
                    initializeMediaPlayer(uri, hapticEnabled, autostart = true)
                } else {
                    Log.e(MEDIA_SERVICE_TAG, "Received SET_SOURCE_AND_PLAY command with null URI.")
                    stopSelf()
                }
            }
            ACTION_SET_SOURCE_PREPARE -> {
                if (!isServiceForeground) {
                    startForeground(NOTIFICATION_ID, buildNotification(isLoading = true))
                    isServiceForeground = true
                }
                val uri = getUriFromIntent(intent)
                val hapticEnabled = intent.getBooleanExtra(EXTRA_HAPTIC_ENABLED, true)
                if (uri != null) {
                    initializeMediaPlayer(uri, hapticEnabled, autostart = false)
                } else {
                    Log.e(MEDIA_SERVICE_TAG, "Received SET_SOURCE_PREPARE command with null URI.")
                    stopSelf()
                }
            }
            ACTION_REQUEST_STATE -> {
                sendPlaybackStateBroadcast()
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(MEDIA_SERVICE_TAG, "onTaskRemoved: Stopping service completely.")
        stopPlayback(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        Log.d(MEDIA_SERVICE_TAG, "Service onDestroy.")
        super.onDestroy()
        releaseResources(true)
        isServiceForeground = false
        serviceScope.cancel()
    }

    @SuppressLint("NewApi")
    private fun initializeMediaPlayer(uri: Uri, hapticEnabled: Boolean, autostart: Boolean) {
        if (mediaPlayer != null && currentMediaUri == uri) {
            Log.d(MEDIA_SERVICE_TAG, "MediaPlayer already initialized for this URI. Skipping re-initialization.")
            if (autostart && mediaPlayer?.isPlaying == false) {
                startPlayback()
            } else {
                sendPlaybackStateBroadcast()
            }
            return
        }

        releaseResources(fullyRelease = false)
        currentMediaUri = uri
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(applicationContext, uri)
                setOnPreparedListener { mp ->
                    currentPlaybackDuration = mp.duration
                    Log.d(MEDIA_SERVICE_TAG, "MediaPlayer prepared. Duration: $currentPlaybackDuration")
                    val audioSessionId = mp.audioSessionId
                    if (isHapticAvailableOnDevice) {
                        hapticGenerator = HapticGenerator.create(audioSessionId)
                        hapticGenerator?.setEnabled(hapticEnabled)
                    }
                    mediaSession?.setMetadata(getTrackMetadataForNotification(applicationContext, uri))
                    currentPlaybackState = PlaybackStateCompat.STATE_PAUSED
                    updateMediaSessionAndNotification()
                    if (autostart) startPlayback()
                }
                setOnCompletionListener {
                    Log.d(MEDIA_SERVICE_TAG, "Playback completed.")
                    stopPlayback(false)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(MEDIA_SERVICE_TAG, "MediaPlayer error: what=$what, extra=$extra. Stopping service.")
                    releaseResources(true)
                    stopSelf()
                    false
                }
                prepareAsync()
            } catch (e: Exception) {
                Log.e(MEDIA_SERVICE_TAG, "Error initializing MediaPlayer with URI: $uri. Stopping service.", e)
                releaseResources(true)
                stopSelf()
            }
        }
    }

    private fun startPlayback() {
        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
            currentPlaybackState = PlaybackStateCompat.STATE_PLAYING
            if (!isServiceForeground) {
                startForeground(NOTIFICATION_ID, buildNotification())
                isServiceForeground = true
            } else {
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, buildNotification())
            }
            startPlaybackStateUpdater()
            Log.d(MEDIA_SERVICE_TAG, "Playback started.")
        } else if (mediaPlayer?.isPlaying == true) {
            Log.d(MEDIA_SERVICE_TAG, "Playback already active, skipping start command.")
        } else {
            Log.w(MEDIA_SERVICE_TAG, "MediaPlayer is null or not prepared, cannot start playback.")
        }
    }

    private fun pausePlayback() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            currentPlaybackState = PlaybackStateCompat.STATE_PAUSED
            playbackUpdateJob?.cancel()
            updateMediaSessionAndNotification()
            Log.d(MEDIA_SERVICE_TAG, "Playback paused.")
        }
    }

    private fun stopPlayback(killService: Boolean) {
        if (mediaPlayer != null) {
            mediaPlayer?.pause()
            mediaPlayer?.seekTo(0)
            currentPlaybackPosition = 0
            currentPlaybackState = PlaybackStateCompat.STATE_STOPPED
            playbackUpdateJob?.cancel()
            updateMediaSessionAndNotification()
            Log.d(MEDIA_SERVICE_TAG, "Playback stopped. Kill service: $killService")

            if (killService) {
                releaseResources(true)
                stopSelf()
            }
        }
    }

    private fun startPlaybackStateUpdater() {
        playbackUpdateJob?.cancel()
        playbackUpdateJob = serviceScope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                currentPlaybackPosition = mediaPlayer?.currentPosition ?: 0
                updateMediaSessionAndNotification()
                delay(200)
            }
            Log.d(MEDIA_SERVICE_TAG, "Playback update job cancelled or finished.")
        }
    }

    private fun releaseResources(fullyRelease: Boolean) {
        playbackUpdateJob?.cancel()
        serviceScope.coroutineContext.cancelChildren()
        hapticGenerator?.release()
        hapticGenerator = null
        mediaPlayer?.release()
        mediaPlayer = null
        currentMediaUri = if (fullyRelease) null else currentMediaUri
        if (fullyRelease) {
            mediaSession?.release()
            mediaSession = null
            if (isServiceForeground) {
                stopForeground(true)
            }
            isServiceForeground = false
            Log.d(MEDIA_SERVICE_TAG, "Resources fully released.")
        } else {
            Log.d(MEDIA_SERVICE_TAG, "Resources partially released (MediaSession kept).")
        }
    }

    private fun updateMediaSessionAndNotification() {
        val actions = (PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_REWIND or
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT)

        val stateBuilder = PlaybackStateCompat.Builder().setActions(actions)
        stateBuilder.setState(currentPlaybackState, currentPlaybackPosition.toLong(), 1.0f, SystemClock.elapsedRealtime())
        mediaSession?.setPlaybackState(stateBuilder.build())
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, buildNotification())
        sendPlaybackStateBroadcast()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Haptic Playback", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun buildNotification(isLoading: Boolean = false): Notification {
        val isPlaying = currentPlaybackState == PlaybackStateCompat.STATE_PLAYING
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play_circle_24dp)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, PlaybackActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .setDeleteIntent(PendingIntent.getService(this, 0,
                Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_STOP_SERVICE_FROM_NOTIFICATION },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (isLoading) {
            builder.setContentTitle("Loading...")
        } else {
            val mediaMetadata = mediaSession?.controller?.metadata
            builder
                .setContentTitle(mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "No Title")
                .setContentText(mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "No Artist")
                .setLargeIcon(mediaMetadata?.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
                .addAction(R.drawable.ic_fast_rewind_24dp, "Rewind", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_REWIND))
                .addAction(if (isPlaying) R.drawable.ic_pause_circle_24dp else R.drawable.ic_play_circle_24dp, if (isPlaying) "Pause" else "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))
                .addAction(R.drawable.ic_fast_forward_24dp, "Forward", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_FAST_FORWARD))
                .addAction(R.drawable.ic_skip_previous_24dp, "Previous Track", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
                .addAction(R.drawable.ic_skip_next_24dp, "Next Track", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
                .addAction(if (isHapticEnabled) R.drawable.ic_vibration_24dp else R.drawable.ic_volume_off_24dp, "Haptic", buildHapticTogglePendingIntent())
                .setStyle(MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(3, 1, 4)
                )
        }
        return builder.build()
    }

    private fun buildHapticTogglePendingIntent(): PendingIntent {
        val toggleIntent = Intent(this, MediaPlaybackService::class.java).apply {
            action = ACTION_TOGGLE_HAPTIC
            putExtra(EXTRA_HAPTIC_ENABLED, !isHapticEnabled)
        }
        return PendingIntent.getService(this, 123, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getUriFromIntent(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_URI)
        }
    }

    private fun getTrackMetadataForNotification(context: Context, uri: Uri): MediaMetadataCompat {
        val builder = MediaMetadataCompat.Builder()
        try {
            val retriever = MediaMetadataRetriever()
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                } ?: run {
                    Log.e(MEDIA_SERVICE_TAG, "Failed to open file descriptor for MediaMetadataRetriever for notification metadata on $uri.")
                    return builder.build()
                }
            } catch (e: Exception) {
                Log.e(MEDIA_SERVICE_TAG, "Failed to set data source for retriever on $uri for notification metadata", e)
                return builder.build()
            }

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val albumArtBytes = retriever.embeddedPicture

            builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title ?: uri.lastPathSegment?.substringBeforeLast("."))
            builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist ?: "Unknown Artist")
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)

            if (albumArtBytes != null) {
                val bitmap = BitmapFactory.decodeByteArray(albumArtBytes, 0, albumArtBytes.size)
                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            }
            retriever.release()
        } catch (e: Exception) {
            Log.e(MEDIA_SERVICE_TAG, "Failed to retrieve metadata for notification from $uri (outer catch)", e)
        }
        return builder.build()
    }

    private fun sendPlaybackStateBroadcast() {
        val intent = Intent(ACTION_BROADCAST_STATE).apply {
            putExtra(EXTRA_IS_PLAYING, (currentPlaybackState == PlaybackStateCompat.STATE_PLAYING))
            putExtra(EXTRA_CURRENT_POSITION, currentPlaybackPosition)
            putExtra(EXTRA_DURATION, currentPlaybackDuration)
            putExtra(EXTRA_IS_HAPTIC_AVAILABLE, isHapticAvailableOnDevice)
            putExtra(EXTRA_MEDIA_URI, currentMediaUri)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        const val ACTION_PLAY = "com.ost.application.ACTION_PLAY"
        const val ACTION_PAUSE = "com.ost.application.ACTION_PAUSE"
        const val ACTION_STOP = "com.ost.application.ACTION_STOP"
        const val ACTION_SEEK_TO = "com.ost.application.ACTION_SEEK_TO"
        const val ACTION_REWIND_5 = "com.ost.application.ACTION_REWIND_5"
        const val ACTION_FORWARD_5 = "com.ost.application.ACTION_FORWARD_5"
        const val ACTION_TOGGLE_HAPTIC = "com.ost.application.ACTION_TOGGLE_HAPTIC"
        const val ACTION_SET_SOURCE_AND_PLAY = "com.ost.application.ACTION_SET_SOURCE_AND_PLAY"
        const val ACTION_SET_SOURCE_PREPARE = "com.ost.application.ACTION_SET_SOURCE_PREPARE"
        const val ACTION_STOP_SERVICE_FROM_NOTIFICATION = "com.ost.application.ACTION_STOP_SERVICE_FROM_NOTIFICATION"
        const val EXTRA_URI = "com.ost.application.EXTRA_URI"
        const val EXTRA_SEEK_POSITION = "com.ost.application.EXTRA_SEEK_POSITION"
        const val EXTRA_HAPTIC_ENABLED = "com.ost.application.EXTRA_HAPTIC_ENABLED"

        fun requestState(context: Context) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply { action = ACTION_REQUEST_STATE }
            context.startService(intent)
        }

        fun startServiceWithUri(context: Context, uri: Uri, hapticEnabled: Boolean) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = ACTION_SET_SOURCE_AND_PLAY
                putExtra(EXTRA_URI, uri)
                putExtra(EXTRA_HAPTIC_ENABLED, hapticEnabled)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }
        fun prepareService(context: Context, uri: Uri, hapticEnabled: Boolean) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = ACTION_SET_SOURCE_PREPARE
                putExtra(EXTRA_URI, uri)
                putExtra(EXTRA_HAPTIC_ENABLED, hapticEnabled)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }
        fun sendCommand(context: Context, action: String) {
            context.startService(Intent(context, MediaPlaybackService::class.java).apply { this.action = action })
        }
        fun sendSeekCommand(context: Context, position: Long) {
            context.startService(Intent(context, MediaPlaybackService::class.java).apply {
                action = ACTION_SEEK_TO
                putExtra(EXTRA_SEEK_POSITION, position)
            })
        }
        fun sendRewindCommand(context: Context) = sendCommand(context, ACTION_REWIND_5)
        fun sendForwardCommand(context: Context) = sendCommand(context, ACTION_FORWARD_5)
        fun sendHapticToggleCommand(context: Context, enabled: Boolean) {
            context.startService(Intent(context, MediaPlaybackService::class.java).apply {
                action = ACTION_TOGGLE_HAPTIC
                putExtra(EXTRA_HAPTIC_ENABLED, enabled)
            })
        }
    }
}