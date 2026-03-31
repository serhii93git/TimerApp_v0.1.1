package com.example.timerapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.timerapp.data.AppDatabase
import com.example.timerapp.data.TimerState
import com.example.timerapp.service.TimerForegroundService
import com.example.timerapp.utils.AlarmScheduler
import kotlinx.coroutines.*

/**
 * Reschedules all active timers after device reboot.
 * Without this receiver, the RECEIVE_BOOT_COMPLETED permission is wasted
 * and all running timers are silently lost on every reboot.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).timerDao()
                val timers = dao.getAll()
                val now = System.currentTimeMillis()

                timers.forEach { timer ->
                    when (timer.state) {
                        TimerState.RUNNING -> {
                            if (timer.endTimeMs > now) {
                                // Timer still has time left — reschedule alarm + start service
                                AlarmScheduler.schedule(context, timer.id, timer.endTimeMs)
                                val startIntent = Intent(context, TimerForegroundService::class.java).apply {
                                    action = TimerForegroundService.ACTION_START
                                    putExtra(TimerForegroundService.EXTRA_TIMER_ID, timer.id)
                                }
                                context.startForegroundService(startIntent)
                            } else {
                                // Timer expired while device was off — mark finished
                                dao.markStopped(timer.id)
                            }
                        }
                        TimerState.PAUSED -> {
                            // Paused timers retain their remainingMs — no alarm needed
                        }
                        TimerState.FINISHED -> {
                            // Nothing to do
                        }
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}
