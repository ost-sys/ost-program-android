package com.ost.application.ui.screen.share

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.ost.application.R
import java.util.UUID
import kotlin.math.log10
import kotlin.math.pow

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
object LiveUpdateNotificationManager {

    private lateinit var notificationManager: NotificationManager
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID_LIVE_UPDATES,
            "Live Transfer Updates",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Channel for live file transfer progress updates."
        }
        notificationManager.createNotificationChannel(channel)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showPreparing() = showNotification(NotificationState.Preparing)

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showConnecting() = showNotification(NotificationState.Connecting)

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showWaitingForAcceptance() = showNotification(NotificationState.WaitingForAcceptance)

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showTransferring(progress: Int, uris: List<Uri>, totalSize: Long, isSending: Boolean) {
        showNotification(NotificationState.Transferring(progress, uris, totalSize, isSending))
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showCompleted(uris: List<Uri>, isSending: Boolean) {
        showNotification(NotificationState.Completed(uris, isSending))
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showCancelled(message: String?) = showNotification(NotificationState.Cancelled(message))

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showFailed(errorMessage: String) = showNotification(NotificationState.Failed(errorMessage))

    private fun showNotification(state: NotificationState) {
        if (!::notificationManager.isInitialized) return
        val builder = buildNotificationForState(state)
        notificationManager.notify(Constants.NOTIFICATION_ID_TRANSFER, builder.build())
    }

    private fun buildNotificationForState(state: NotificationState): NotificationCompat.Builder {
        val cancelIntent = Intent(appContext, ShareService::class.java).apply { action = Constants.ACTION_CANCEL_TRANSFER }
        val cancelPendingIntent = PendingIntent.getService(
            appContext, 0, cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = when (state) {
            is NotificationState.Preparing -> buildBaseNotification(isSending = true)
                .setContentTitle("Preparing files")
                .setProgress(100, 0, true)
            is NotificationState.Connecting -> buildBaseNotification(isSending = true)
                .setContentTitle("Connecting to peer")
                .setProgress(100, 0, true)
            is NotificationState.WaitingForAcceptance -> buildBaseNotification(isSending = true)
                .setContentTitle("Waiting for receiver to accept")
                .setProgress(100, 0, true)
            is NotificationState.Transferring -> {
                val title = if (state.isSending) "Sending ${state.uris.size} files" else "Receiving ${state.uris.size} files"
                val currentBytes = (state.progress / 100.0 * state.totalSize).toLong()

                val progressStyle = buildRichProgressStyle(state.progress)
                progressStyle.setProgressTrackerIcon(
                    IconCompat.createWithResource(
                        appContext,
                        if (state.isSending) R.drawable.ic_upload_file_24dp else R.drawable.ic_download_24dp
                    )
                )
                progressStyle.setProgress(state.progress)

                buildBaseNotification(isSending = state.isSending)
                    .setContentTitle(title)
                    .setContentText("${currentBytes.formatFileSize()} of ${state.totalSize.formatFileSize()}")
                    .setStyle(progressStyle)
            }
            is NotificationState.Completed -> {
                val title = if (state.isSending) "Files sent" else "Files received"
                val contentText = if (state.uris.size == 1) {
                    state.uris.first().lastPathSegment ?: "File"
                } else {
                    "${state.uris.size} files processed successfully"
                }
                val notificationBuilder = buildBaseNotification(isOngoing = false, isSending = state.isSending)
                    .setContentTitle(title)
                    .setContentText(contentText)
                    .setProgress(0, 0, false)
                    .setTimeoutAfter(10000)
                    .setAutoCancel(true)

                if (!state.isSending && state.uris.isNotEmpty()) {
                    val fileUri = state.uris.first()
                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, appContext.contentResolver.getType(fileUri))
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        appContext, UUID.randomUUID().hashCode(), openIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notificationBuilder.addAction(R.drawable.ic_open_in_new_24dp, "Open File", openPendingIntent)
                }
                notificationBuilder
            }
            is NotificationState.Cancelled -> buildBaseNotification(isOngoing = false)
                .setContentTitle("Transfer cancelled")
                .setContentText(state.message ?: "Cancelled by user")
                .setProgress(0, 0, false)
                .setTimeoutAfter(5000)
                .setAutoCancel(true)
            is NotificationState.Failed -> buildBaseNotification(isOngoing = false)
                .setContentTitle("Transfer failed")
                .setContentText(state.errorMessage)
                .setProgress(0, 0, false)
                .setTimeoutAfter(10000)
                .setAutoCancel(true)
        }

        if (state is NotificationState.Connecting || state is NotificationState.WaitingForAcceptance || state is NotificationState.Transferring) {
            builder.addAction(R.drawable.ic_cancel_24dp, "Cancel", cancelPendingIntent)
        } else {
            builder.clearActions()
        }

        return builder
    }

    private fun buildBaseNotification(isOngoing: Boolean = true, isSending: Boolean? = null): NotificationCompat.Builder {
        val smallIcon = when (isSending) {
            true -> R.drawable.ic_upload_file_24dp
            false -> R.drawable.ic_download_24dp
            null -> R.drawable.ic_share_24dp
        }
        return NotificationCompat.Builder(appContext, Constants.NOTIFICATION_CHANNEL_ID_LIVE_UPDATES)
            .setSmallIcon(smallIcon)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setOngoing(isOngoing)
            .setRequestPromotedOngoing(isOngoing)
    }

    private fun buildRichProgressStyle(progress: Int): NotificationCompat.ProgressStyle {
        val pointColor = Color.valueOf(236f / 255f, 183f / 255f, 255f / 255f, 1f).toArgb()
        val segmentColor = Color.valueOf(134f / 255f, 247f / 255f, 250f / 255f, 1f).toArgb()

        val progressStyle = NotificationCompat.ProgressStyle().setProgressSegments(
            List(4) { NotificationCompat.ProgressStyle.Segment(25).setColor(segmentColor) }
        )

        val points = mutableListOf<NotificationCompat.ProgressStyle.Point>()
        if (progress >= 25) points.add(NotificationCompat.ProgressStyle.Point(25).setColor(pointColor))
        if (progress >= 50) points.add(NotificationCompat.ProgressStyle.Point(50).setColor(pointColor))
        if (progress >= 75) points.add(NotificationCompat.ProgressStyle.Point(75).setColor(pointColor))
        if (progress >= 100) points.add(NotificationCompat.ProgressStyle.Point(100).setColor(pointColor))
        if (points.isNotEmpty()) {
            progressStyle.setProgressPoints(points)
        }
        return progressStyle
    }

    private fun Long.formatFileSize(): String {
        if (this <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
        val unitIndex = digitGroups.coerceIn(0, units.size - 1)
        val sizeInUnit = this / 1024.0.pow(unitIndex.toDouble())
        return if (unitIndex == 0) {
            String.format(java.util.Locale.US, "%d", this.toInt())
        } else {
            String.format(java.util.Locale.US, "%.1f", sizeInUnit)
        } + " " + units[unitIndex]
    }

    private sealed class NotificationState {
        object Preparing : NotificationState()
        object Connecting : NotificationState()
        object WaitingForAcceptance : NotificationState()
        data class Transferring(val progress: Int, val uris: List<Uri>, val totalSize: Long, val isSending: Boolean) : NotificationState()
        data class Completed(val uris: List<Uri>, val isSending: Boolean) : NotificationState()
        data class Cancelled(val message: String?) : NotificationState()
        data class Failed(val errorMessage: String) : NotificationState()
    }
}