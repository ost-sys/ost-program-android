package com.ost.application.share
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.ost.application.R
import com.ost.application.core.share.Constants
import java.util.UUID
import kotlin.math.log10
import kotlin.math.pow
object NotificationHelper {
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val transferChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val completionChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID + "_finished",
                context.getString(R.string.notification_channel_name_finished),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description_finished)
            }
            val incomingChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID + "_incoming",
                context.getString(R.string.notification_channel_name_incoming_files),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description_incoming_files)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(transferChannel)
            notificationManager.createNotificationChannel(completionChannel)
            notificationManager.createNotificationChannel(incomingChannel)
        }
    }
    fun buildOngoingNotification(
        context: Context,
        notificationId: Int,
        targetActivity: Class<*>,
        title: String,
        contentText: String,
        statusText: String,
        iconRes: Int = R.drawable.ic_notification_24dp,
        progress: Int? = null,
        totalSize: Long = 0L
    ): NotificationCompat.Builder {
        createNotificationChannel(context)
        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(iconRes)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        if (progress != null && progress in 0..100) {
            builder.setProgress(100, progress, false)
        }

        val activityIntent = Intent(context, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val ongoingActivityStatus = if (progress != null && progress in 0..100) {
            if (totalSize > 0) {
                Status.Builder()
                    .addTemplate(
                        context.getString(
                            R.string.notif_progress_details,
                            (progress / 100.0 * totalSize).toLong().formatFileSize(context),
                            totalSize.formatFileSize(context),
                            progress
                        )
                    )
                    .build()
            } else {
                Status.Builder()
                    .addTemplate("$statusText ($progress%)")
                    .build()
            }
        } else {
            Status.Builder()
                .addTemplate(statusText)
                .build()
        }

        val ongoingActivity = OngoingActivity.Builder(
            context.applicationContext,
            notificationId,
            builder
        )
            .setStaticIcon(iconRes)
            .setTouchIntent(activityPendingIntent)
            .setStatus(ongoingActivityStatus)
            .setTitle(title)
            .build()

        ongoingActivity.apply(context.applicationContext)
        return builder
    }

    fun buildForegroundServiceNotification(context: Context, contentText: String): NotificationCompat.Builder {
        createNotificationChannel(context)
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification_24dp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
    }

    fun startOrUpdateOngoingActivity(
        context: Context,
        notificationId: Int,
        targetActivity: Class<*>,
        title: String,
        contentText: String,
        statusText: String,
        iconRes: Int = R.drawable.ic_notification_24dp,
        progress: Int? = null,
        totalSize: Long = 0L
    ) {
        val builder = buildOngoingNotification(
            context = context,
            notificationId = notificationId,
            targetActivity = targetActivity,
            title = title,
            contentText = contentText,
            statusText = statusText,
            iconRes = iconRes,
            progress = progress,
            totalSize = totalSize
        )
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    fun stopOngoingActivity(context: Context, notificationId: Int = Constants.NOTIFICATION_ID_SERVICE_FOREGROUND) {
        cancelNotification(context, notificationId)
    }
    fun showTransferNotification(context: Context, title: String, progress: Int, totalSize: Long, isSending: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24dp)
            .setContentTitle(title)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        if (isSending) {
            builder.setContentText(context.getString(R.string.notif_sending_progress, progress, totalSize.formatFileSize(context)))
        } else {
            builder.setContentText(context.getString(R.string.notif_receiving_progress, progress, totalSize.formatFileSize(context)))
        }
        val cancelIntent = Intent(context, WearShareService::class.java).apply {
            action = Constants.ACTION_CANCEL_TRANSFER
        }
        val cancelPendingIntent = PendingIntent.getService(
            context,
            0,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(R.drawable.ic_cancel_24dp, context.getString(R.string.cancel), cancelPendingIntent)
        notificationManager.notify(Constants.NOTIFICATION_ID_TRANSFER, builder.build())
    }
    fun showCompletionNotification(context: Context, title: String, success: Boolean, errorMessage: String? = null) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        context.getString(R.string.notification_channel_name_finished)
        val channelId = Constants.NOTIFICATION_CHANNEL_ID + "_finished"
        val contentText = if (success) context.getString(R.string.transfer_completed) else errorMessage ?: context.getString(R.string.transfer_failed)
        val icon = if (success) R.drawable.ic_check_circle_24dp else R.drawable.ic_error_24dp
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        notificationManager.notify(UUID.randomUUID().hashCode(), builder.build())
    }
    fun showIncomingFileConfirmationNotification(
        context: Context,
        requestId: String,
        fileNames: List<String>,
        totalSize: Long,
        senderDeviceName: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        context.getString(R.string.notification_channel_name_incoming_files)
        val channelId = Constants.NOTIFICATION_CHANNEL_ID + "_incoming"
        val title = context.getString(R.string.notif_incoming_files_title)
        val contentText = context.getString(R.string.notif_incoming_files_details, senderDeviceName, fileNames.joinToString(", "), totalSize.formatFileSize(context))
        val acceptIntent = Intent(context, WearShareService::class.java).apply {
            action = Constants.ACTION_ACCEPT_RECEIVE
            putExtra(Constants.EXTRA_REQUEST_ID, requestId)
        }
        val acceptPendingIntent = PendingIntent.getService(
            context,
            requestId.hashCode() + 1,
            acceptIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val rejectIntent = Intent(context, WearShareService::class.java).apply {
            action = Constants.ACTION_REJECT_RECEIVE
            putExtra(Constants.EXTRA_REQUEST_ID, requestId)
        }
        val rejectPendingIntent = PendingIntent.getService(
            context,
            requestId.hashCode() + 2,
            rejectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openActivityIntent = Intent(context, ShareActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openActivityPendingIntent = PendingIntent.getActivity(
            context,
            requestId.hashCode() + 3,
            openActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_24dp)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_check_circle_24dp, context.getString(R.string.accept), acceptPendingIntent)
            .addAction(R.drawable.ic_cancel_24dp, context.getString(R.string.reject), rejectPendingIntent)
            .addAction(R.drawable.ic_open_24dp, context.getString(R.string.open_activity_notification_button), openActivityPendingIntent)
            .setAutoCancel(true)
        notificationManager.notify(Constants.NOTIFICATION_ID_INCOMING_FILE, builder.build())
    }
    fun cancelNotification(context: Context, id: Int = Constants.NOTIFICATION_ID_TRANSFER) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
    }
    fun Long.formatFileSize(context: Context): String {
        if (this < 0) return "?? B"
        if (this == 0L) return "0 ${context.getString(R.string.b)}"
        val units = arrayOf(context.getString(R.string.b), context.getString(R.string.kb), context.getString(R.string.mb), context.getString(R.string.gb), context.getString(R.string.tb))
        val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
        val unitIndex = digitGroups.coerceAtMost(units.size - 1).coerceAtLeast(0)
        val sizeInUnit = this / 1024.0.pow(unitIndex.toDouble())
        return (if (unitIndex == 0) String.format(java.util.Locale.US, "%d", this.toInt())
        else String.format(java.util.Locale.US, "%.1f", sizeInUnit)) + " " + units[unitIndex]
    }
}
