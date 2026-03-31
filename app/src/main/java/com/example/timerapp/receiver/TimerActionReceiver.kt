package com.example.timerapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.timerapp.data.AppDatabase
import com.example.timerapp.data.TimerState
import com.example.timerapp.service.TimerForegroundService
import com.example.timerapp.utils.AlarmScheduler
import com.example.timerapp.utils.NotificationHelper
import com.example.timerapp.widget.TimerGlanceWidget
import kotlinx.coroutines.*

class TimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getIntExtra(TimerForegroundService.EXTRA_TIMER_ID, -1)
        if (timerId == -1) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).timerDao()
                when (intent.action) {
                    "ACTION_DISMISS" -> {
                        dao.markStopped(timerId)
                        AlarmScheduler.cancel(context, timerId)
                        NotificationHelper.cancelCompletionNotification(context, timerId)
                        TimerGlanceWidget.updateState(context)
                    }
                    "ACTION_RESTART" -> {
                        val timer = dao.getById(timerId) ?: return@launch
                        val now    = System.currentTimeMillis()
                        val newEnd = now + timer.durationMs
                        dao.update(timer.copy(endTimeMs = newEnd, state = TimerState.RUNNING, remainingMs = 0L))
                        AlarmScheduler.schedule(context, timerId, newEnd)
                        NotificationHelper.cancelCompletionNotification(context, timerId)

                        val startIntent = Intent(context, TimerForegroundService::class.java).apply {
                            action = TimerForegroundService.ACTION_START
                            putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
                        }
                        context.startForegroundService(startIntent)
                        TimerGlanceWidget.updateState(context)
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}
