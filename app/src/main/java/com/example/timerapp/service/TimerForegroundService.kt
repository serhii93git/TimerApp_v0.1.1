package com.example.timerapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.timerapp.data.AppDatabase
import com.example.timerapp.data.TimerEntity
import com.example.timerapp.data.TimerState
import com.example.timerapp.utils.AlarmScheduler
import com.example.timerapp.utils.HapticHelper
import com.example.timerapp.utils.NotificationHelper
import com.example.timerapp.utils.TimeUtils
import com.example.timerapp.widget.TimerGlanceWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {

    companion object {
        const val ACTION_START       = "ACTION_START"
        const val ACTION_STOP_TIMER  = "ACTION_STOP_TIMER"
        const val ACTION_PAUSE       = "ACTION_PAUSE"
        const val ACTION_RESUME      = "ACTION_RESUME"
        const val EXTRA_TIMER_ID     = "EXTRA_TIMER_ID"
        private const val NOTIF_ID   = 1001
    }

    @Inject lateinit var db: AppDatabase

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val tickJobs = mutableMapOf<Int, Job>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        val placeholder = NotificationHelper.buildPersistentNotification(
            this, getString(com.example.timerapp.R.string.app_name), "--:--:--", -1, false
        )
        startForeground(NOTIF_ID, placeholder)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timerId = intent?.getIntExtra(EXTRA_TIMER_ID, -1) ?: -1
        when (intent?.action) {
            ACTION_START       -> if (timerId != -1) startTickLoop(timerId)
            ACTION_STOP_TIMER  -> if (timerId != -1) stopTimer(timerId)
            ACTION_PAUSE       -> if (timerId != -1) pauseTimer(timerId)
            ACTION_RESUME      -> if (timerId != -1) resumeTimer(timerId)
        }
        return START_STICKY
    }

    private fun startTickLoop(timerId: Int) {
        tickJobs[timerId]?.cancel()
        tickJobs[timerId] = serviceScope.launch {
            while (isActive) {
                val timer = db.timerDao().getById(timerId) ?: break
                if (!timer.isRunning) break

                val now       = System.currentTimeMillis()
                val remaining = (timer.endTimeMs - now).coerceAtLeast(0L)

                updatePersistentNotification()
                TimerGlanceWidget.updateState(this@TimerForegroundService)

                if (remaining <= 0L) {
                    handleTimerComplete(timer)
                    break
                }

                val drift = System.currentTimeMillis() % 1_000L
                delay(if (drift == 0L) 1_000L else 1_000L - drift)
            }
            maybeStopSelf()
        }
    }

    private suspend fun handleTimerComplete(timer: TimerEntity) {
        if (timer.isLooping) {
            val now    = System.currentTimeMillis()
            val newEnd = now + timer.durationMs
            val updated = timer.copy(endTimeMs = newEnd, state = TimerState.RUNNING)
            db.timerDao().update(updated)
            AlarmScheduler.cancel(this, timer.id)
            AlarmScheduler.schedule(this, timer.id, newEnd)
            NotificationHelper.showRestartedNotification(
                this, timer.id, timer.title,
                TimeUtils.formatMillis(timer.durationMs)
            )
            startTickLoop(timer.id)
        } else {
            db.timerDao().markStopped(timer.id)
            AlarmScheduler.cancel(this, timer.id)

            NotificationHelper.showCompletionNotification(this, timer.id, timer.title)
            HapticHelper.vibrate(this)
        }
    }

    private fun stopTimer(timerId: Int) {
        serviceScope.launch {
            tickJobs[timerId]?.cancel()
            tickJobs.remove(timerId)
            db.timerDao().markStopped(timerId)
            AlarmScheduler.cancel(this@TimerForegroundService, timerId)
            TimerGlanceWidget.updateState(this@TimerForegroundService)
            maybeStopSelf()
        }
    }

    private fun pauseTimer(timerId: Int) {
        serviceScope.launch {
            tickJobs[timerId]?.cancel()
            tickJobs.remove(timerId)
            val timer = db.timerDao().getById(timerId) ?: return@launch
            val now   = System.currentTimeMillis()
            val remaining = (timer.endTimeMs - now).coerceAtLeast(0L)
            db.timerDao().markPaused(timerId, remaining)
            AlarmScheduler.cancel(this@TimerForegroundService, timerId)
            updatePersistentNotification()
            TimerGlanceWidget.updateState(this@TimerForegroundService)
        }
    }

    private fun resumeTimer(timerId: Int) {
        serviceScope.launch {
            val timer = db.timerDao().getById(timerId) ?: return@launch
            val now    = System.currentTimeMillis()
            val newEnd = now + timer.remainingMs
            db.timerDao().markResumed(timerId, newEnd)
            AlarmScheduler.schedule(this@TimerForegroundService, timerId, newEnd)
            startTickLoop(timerId)
        }
    }

    private suspend fun updatePersistentNotification() {
        val allTimers = db.timerDao().getAll()
        val now = System.currentTimeMillis()

        // Пріоритет: найближчий RUNNING таймер, потім перший PAUSED
        val soonestRunning = allTimers
            .filter { it.state == TimerState.RUNNING }
            .minByOrNull { it.endTimeMs }

        if (soonestRunning != null) {
            val remaining = (soonestRunning.endTimeMs - now).coerceAtLeast(0L)
            val notification = NotificationHelper.buildPersistentNotification(
                this, soonestRunning.title, TimeUtils.formatMillis(remaining),
                soonestRunning.id, isPaused = false
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(NOTIF_ID, notification)
        } else {
            val firstPaused = allTimers.firstOrNull { it.state == TimerState.PAUSED }
            if (firstPaused != null) {
                val notification = NotificationHelper.buildPersistentNotification(
                    this, firstPaused.title, TimeUtils.formatMillis(firstPaused.remainingMs),
                    firstPaused.id, isPaused = true
                )
                val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NOTIF_ID, notification)
            }
        }
    }

    private suspend fun maybeStopSelf() {
        val allTimers = db.timerDao().getAll()
        val hasActive = allTimers.any { it.state == TimerState.RUNNING || it.state == TimerState.PAUSED }
        if (!hasActive) {
            withContext(Dispatchers.Main) { stopSelf() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
