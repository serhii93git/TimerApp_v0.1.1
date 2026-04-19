package com.example.timerapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.timerapp.data.AppDatabase
import com.example.timerapp.service.TimerForegroundService
import com.example.timerapp.utils.NotificationHelper
import kotlinx.coroutines.*

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getIntExtra(TimerForegroundService.EXTRA_TIMER_ID, -1)
        if (timerId == -1) return

        // Hold the CPU briefly so the notification + FSI are processed before Doze resumes.
        // startActivity() from a BroadcastReceiver is blocked on Android 10+, so we rely on
        // setFullScreenIntent inside the notification instead.
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TimerApp:AlarmReceiver"
        ).apply { acquire(10_000L) }

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao   = AppDatabase.getInstance(context).timerDao()
                val timer = dao.getById(timerId) ?: return@launch
                dao.markStopped(timerId)

                NotificationHelper.showCompletionNotification(context, timerId, timer.title)
            } finally {
                if (wakeLock.isHeld) runCatching { wakeLock.release() }
                result.finish()
            }
        }
    }
}