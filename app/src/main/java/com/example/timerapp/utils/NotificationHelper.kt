package com.example.timerapp.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.timerapp.R
import com.example.timerapp.receiver.TimerActionReceiver
import com.example.timerapp.service.TimerForegroundService
import com.example.timerapp.ui.TimerRingingActivity

object NotificationHelper {

    const val CHANNEL_PERSISTENT = "timer_persistent"
    const val CHANNEL_COMPLETION  = "timer_completion"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val persistentChannel = NotificationChannel(
            CHANNEL_PERSISTENT,
            context.getString(R.string.channel_persistent_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_persistent_desc)
            setSound(null, null)
        }

        val completionChannel = NotificationChannel(
            CHANNEL_COMPLETION,
            context.getString(R.string.channel_completion_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_completion_desc)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannel(persistentChannel)
        manager.createNotificationChannel(completionChannel)
    }

    fun buildPersistentNotification(
        context: Context,
        title: String,
        timeLeft: String,
        timerId: Int,
        isPaused: Boolean
    ): Notification {
        val pauseResumeIntent = Intent(context, TimerForegroundService::class.java).apply {
            action = if (isPaused) TimerForegroundService.ACTION_RESUME
                     else          TimerForegroundService.ACTION_PAUSE
            putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
        }
        val pauseResumePi = PendingIntent.getService(
            context, timerId * 10 + 1, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_STOP_TIMER
            putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
        }
        val stopPi = PendingIntent.getService(
            context, timerId * 10 + 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_PERSISTENT)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(timeLeft)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                if (isPaused) R.drawable.ic_play else R.drawable.ic_pause,
                if (isPaused) context.getString(R.string.action_resume)
                else          context.getString(R.string.action_pause),
                pauseResumePi
            )
            .addAction(R.drawable.ic_close, context.getString(R.string.action_stop), stopPi)
            .build()
    }

    fun showCompletionNotification(context: Context, timerId: Int, title: String) {
        val ringingIntent = Intent(context, TimerRingingActivity::class.java).apply {
            putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
            putExtra("TIMER_TITLE", title)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val ringingPi = PendingIntent.getActivity(
            context, timerId * 10 + 3, ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, TimerActionReceiver::class.java).apply {
            action = "ACTION_DISMISS"
            putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
        }
        val dismissPi = PendingIntent.getBroadcast(
            context, timerId * 10 + 4, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val restartIntent = Intent(context, TimerActionReceiver::class.java).apply {
            action = "ACTION_RESTART"
            putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
        }
        val restartPi = PendingIntent.getBroadcast(
            context, timerId * 10 + 5, restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETION)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.notification_completion_title))
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(ringingPi, true)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_close,   context.getString(R.string.action_dismiss), dismissPi)
            .addAction(R.drawable.ic_refresh, context.getString(R.string.action_restart), restartPi)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(timerId + 10000, notification)
    }

    fun cancelCompletionNotification(context: Context, timerId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(timerId + 10000)
    }

    fun showRestartedNotification(context: Context, timerId: Int, title: String, timeLeft: String) {
        val notification = buildPersistentNotification(context, title, timeLeft, timerId, false)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(timerId, notification)
    }
}
