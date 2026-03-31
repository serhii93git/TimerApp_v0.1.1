package com.example.timerapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.timerapp.data.AppDatabase
import com.example.timerapp.service.TimerForegroundService
import com.example.timerapp.ui.TimerRingingActivity
import com.example.timerapp.utils.NotificationHelper
import kotlinx.coroutines.*

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getIntExtra(TimerForegroundService.EXTRA_TIMER_ID, -1)
        if (timerId == -1) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao   = AppDatabase.getInstance(context).timerDao()
                val timer = dao.getById(timerId) ?: return@launch
                dao.markStopped(timerId)

                withContext(Dispatchers.Main) {
                    val ringingIntent = Intent(context, TimerRingingActivity::class.java).apply {
                        putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
                        putExtra("TIMER_TITLE", timer.title)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(ringingIntent)
                }
                NotificationHelper.showCompletionNotification(context, timerId, timer.title)
            } finally {
                result.finish()
            }
        }
    }
}