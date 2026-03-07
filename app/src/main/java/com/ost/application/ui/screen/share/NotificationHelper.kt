package com.ost.application.ui.screen.share

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ost.application.R
import kotlin.math.log10
import kotlin.math.pow

object NotificationHelper {

    fun createAppNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val transferChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notifications_for_file_transfer_progress_and_status)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        val completionChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID_COMPLETION,
            context.getString(R.string.notification_channel_name_finished),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description_finished)
        }

        val incomingChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID_INCOMING,
            context.getString(R.string.notification_channel_name_incoming_files),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description_incoming_files)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(listOf(transferChannel, completionChannel, incomingChannel))
    }

    fun buildForegroundServiceNotification(context: Context, contentText: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_share_24dp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showIncomingFileConfirmationNotification(
        context: Context,
        requestId: String,
        fileNames: List<String>,
        totalSize: Long,
        senderDeviceName: String
    ) {
        val title = context.getString(R.string.notif_incoming_files_title)
        val contentText = context.getString(
            R.string.notif_incoming_files_details,
            senderDeviceName,
            fileNames.joinToString(", "),
            totalSize.formatFileSize(context)
        )

        val acceptIntent = Intent(context, ShareService::class.java).apply {
            action = Constants.ACTION_ACCEPT_RECEIVE
            putExtra(Constants.EXTRA_REQUEST_ID, requestId)
        }
        val acceptPendingIntent = PendingIntent.getService(
            context,
            requestId.hashCode() + 1,
            acceptIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val rejectIntent = Intent(context, ShareService::class.java).apply {
            action = Constants.ACTION_REJECT_RECEIVE
            putExtra(Constants.EXTRA_REQUEST_ID, requestId)
        }
        val rejectPendingIntent = PendingIntent.getService(
            context,
            requestId.hashCode() + 2,
            rejectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID_INCOMING)
            .setSmallIcon(R.drawable.ic_share_24dp)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_check_circle_24dp, context.getString(R.string.accept), acceptPendingIntent)
            .addAction(R.drawable.ic_cancel_24dp, context.getString(R.string.reject), rejectPendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(Constants.NOTIFICATION_ID_INCOMING_FILE, builder.build())
    }

    fun cancelNotification(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    fun Long.formatFileSize(context: Context): String {
        if (this < 0) return "?? B"
        if (this == 0L) return "0 ${context.getString(R.string.b)}"
        val units = arrayOf(
            context.getString(R.string.b),
            context.getString(R.string.kb),
            context.getString(R.string.mb),
            context.getString(R.string.gb),
            context.getString(R.string.tb)
        )
        val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
        val unitIndex = digitGroups.coerceAtMost(units.size - 1).coerceAtLeast(0)
        val sizeInUnit = this / 1024.0.pow(unitIndex.toDouble())
        return (if (unitIndex == 0) String.format(java.util.Locale.US, "%d", this.toInt())
        else String.format(java.util.Locale.US, "%.1f", sizeInUnit)) + " " + units[unitIndex]
    }
}