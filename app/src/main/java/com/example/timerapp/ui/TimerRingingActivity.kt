package com.example.timerapp.ui

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timerapp.R
import com.example.timerapp.data.AppDatabase
import com.example.timerapp.data.TimerState
import com.example.timerapp.service.TimerForegroundService
import com.example.timerapp.ui.theme.PastelTimerTheme
import com.example.timerapp.utils.AlarmScheduler
import com.example.timerapp.utils.NotificationHelper
import com.example.timerapp.utils.SoundHelper
import kotlinx.coroutines.*

class TimerRingingActivity : ComponentActivity() {

    companion object {
        // Process-wide set of timerIds the user has already dismissed. Blocks:
        //  1) late async launchRingingActivityNow() from the service coroutine
        //     (which is already in-flight when the user hits Dismiss) — otherwise
        //     it spawns a fresh Activity that restarts sound + vibration.
        //  2) Recents replaying the original intent after the task is gone.
        // Cleared via onNewRing() at each fresh alarm entry-point.
        private val dismissedIds: MutableSet<Int> =
            java.util.Collections.synchronizedSet(mutableSetOf())

        /** Called from AlarmReceiver / Service when a new alarm fires for this timer. */
        fun onNewRing(timerId: Int) { dismissedIds.remove(timerId) }
    }

    private var mediaPlayer: MediaPlayer? = null
    private val activityScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timerIdField: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON   or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val timerId = intent.getIntExtra(TimerForegroundService.EXTRA_TIMER_ID, -1)
        timerIdField = timerId

        // Already dismissed → don't re-ring. Happens when the service coroutine's
        // late launchRingingActivityNow() fires post-dismiss, or when Recents
        // replays the original task intent after we've finished.
        if (timerId != -1 && dismissedIds.contains(timerId)) {
            finishAndRemoveTask()
            return
        }

        // Title may be null when launched from onStartCommand before the DB fetch
        // finished (we prioritise launching the activity fast). Loaded async below.
        val timerTitleState = mutableStateOf(
            intent.getStringExtra("TIMER_TITLE") ?: getString(R.string.app_name)
        )
        // isLooping starts from intent extra (fast path); overridden from DB
        // shortly after. Controls whether the UI shows 2 or 3 buttons.
        val isLoopingState = mutableStateOf(
            intent.getBooleanExtra(EXTRA_IS_LOOPING, false)
        )

        if (timerId != -1) {
            activityScope.launch {
                val t = AppDatabase.getInstance(this@TimerRingingActivity)
                    .timerDao().getById(timerId)
                if (t != null) {
                    withContext(Dispatchers.Main) {
                        if (intent.getStringExtra("TIMER_TITLE") == null && t.title.isNotBlank()) {
                            timerTitleState.value = t.title
                        }
                        isLoopingState.value = t.isLooping
                    }
                }
            }
        }

        // Back-press mapping depends on whether this is a looping timer —
        // looping defaults to the non-destructive Skip (keeps the loop going),
        // non-looping defaults to Dismiss (the only sensible exit).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isLoopingState.value) skipCycle() else dismissRing()
            }
        })

        mediaPlayer = SoundHelper.createAlarmPlayer(this)
        mediaPlayer?.start()

        val pattern = longArrayOf(0L, 500L, 300L, 500L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }

        setContent {
            // FIX: Was bare MaterialTheme{} — now uses the app's PastelTimerTheme
            PastelTimerTheme {
                val timesUpText  = stringResource(R.string.times_up)
                val restartText  = stringResource(R.string.action_restart)
                val dismissText  = stringResource(R.string.action_dismiss)
                val skipText     = stringResource(R.string.action_skip)
                val stopLoopText = stringResource(R.string.action_stop_loop)
                val timerTitle by timerTitleState
                val isLooping   by isLoopingState

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1730)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "\u23F0", fontSize = 72.sp)

                        Text(
                            text       = timerTitle,
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )

                        Text(
                            text  = timesUpText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(Modifier.height(32.dp))

                        if (isLooping) {
                            // Looping: Restart (reset now), Skip (silence this
                            // cycle — next fires as scheduled), Stop Loop (kill
                            // future cycles). Stacked full-width for easy tap.
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier            = Modifier
                                    .padding(horizontal = 32.dp)
                                    .widthIn(max = 360.dp)
                                    .fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick  = { restartRing() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 64.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF6ECFAA)
                                    )
                                ) {
                                    Text("\u21BA $restartText", fontSize = 18.sp)
                                }

                                OutlinedButton(
                                    onClick  = { skipCycle() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 64.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White.copy(alpha = 0.85f)
                                    )
                                ) {
                                    Text(skipText, fontSize = 18.sp)
                                }

                                Button(
                                    onClick  = { dismissRing() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 64.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                                    colors   = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF9C84D4)
                                    )
                                ) {
                                    Text(stopLoopText, color = Color.White, fontSize = 18.sp)
                                }
                            }
                        } else {
                            // Non-looping: classic Restart + Dismiss, side-by-side.
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { restartRing() },
                                    modifier = Modifier
                                        .heightIn(min = 72.dp)
                                        .widthIn(min = 150.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6ECFAA))
                                ) {
                                    Text("\u21BA $restartText", fontSize = 20.sp)
                                }

                                Button(
                                    onClick = { dismissRing() },
                                    modifier = Modifier
                                        .heightIn(min = 72.dp)
                                        .widthIn(min = 150.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF9C84D4)
                                    )
                                ) {
                                    Text(dismissText, color = Color.White, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Full stop. For non-looping timers this is "Dismiss"; for looping ones it's
     * "Stop Loop". Either way: mark FINISHED, cancel any future alarm (critical
     * for loops — otherwise the next cycle keeps ringing), clear notifications,
     * and ask the service to clean up its tick loop + persistent notification.
     */
    private fun dismissRing() {
        val id = timerIdField
        if (id != -1) dismissedIds.add(id)
        stopAlarm()
        if (id != -1) {
            activityScope.launch {
                AppDatabase.getInstance(this@TimerRingingActivity).timerDao().markStopped(id)
                AlarmScheduler.cancel(this@TimerRingingActivity, id)
                NotificationHelper.cancelAllForTimer(this@TimerRingingActivity, id)
                // Hand off to the service to cancel the tick loop and update
                // the persistent/widget state. Idempotent with the work above.
                val stopIntent = Intent(
                    this@TimerRingingActivity,
                    TimerForegroundService::class.java
                ).apply {
                    action = TimerForegroundService.ACTION_STOP_TIMER
                    putExtra(TimerForegroundService.EXTRA_TIMER_ID, id)
                }
                runCatching { startForegroundService(stopIntent) }
            }
        }
        // Remove the task from Recents so tapping the app entry can't resurrect
        // the alarm screen via a replayed intent.
        finishAndRemoveTask()
    }

    /**
     * Looping-only: silence this cycle without touching the timer's state or the
     * scheduled next alarm. The loop continues and will ring again at its next
     * scheduled moment. dismissedIds prevents late launchRingingActivityNow()
     * calls from the service coroutine from re-ringing for the current cycle —
     * the next AlarmReceiver entry clears it via onNewRing().
     */
    private fun skipCycle() {
        val id = timerIdField
        if (id != -1) dismissedIds.add(id)
        stopAlarm()
        if (id != -1) {
            activityScope.launch {
                // Only the completion/loop-cycle heads-up could be up for this
                // cycle; the showRestartedNotification stays (loop is still live).
                NotificationHelper.cancelCompletionNotification(this@TimerRingingActivity, id)
            }
        }
        finishAndRemoveTask()
    }

    private fun restartRing() {
        val id = timerIdField
        stopAlarm()
        if (id != -1) {
            activityScope.launch {
                val dao   = AppDatabase.getInstance(this@TimerRingingActivity).timerDao()
                val timer = dao.getById(id)
                timer?.let { t ->
                    val now    = System.currentTimeMillis()
                    val newEnd = now + t.durationMs
                    dao.update(t.copy(endTimeMs = newEnd, state = TimerState.RUNNING, remainingMs = 0L))
                    AlarmScheduler.schedule(this@TimerRingingActivity, id, newEnd)
                    val startIntent = Intent(
                        this@TimerRingingActivity,
                        TimerForegroundService::class.java
                    ).apply {
                        action = TimerForegroundService.ACTION_START
                        putExtra(TimerForegroundService.EXTRA_TIMER_ID, id)
                    }
                    startForegroundService(startIntent)
                }
                NotificationHelper.cancelCompletionNotification(this@TimerRingingActivity, id)
            }
        }
        finishAndRemoveTask()
    }

    private fun stopAlarm() {
        try { mediaPlayer?.stop() } catch (_: Exception) {}
        try { mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator.cancel()
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.cancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        activityScope.cancel()
    }
}

// Optional intent extra: lets the launching path (service, notification) hint
// at the loop state so the UI doesn't briefly render the wrong button layout
// before the async DB read resolves. Falls back to DB if absent.
const val EXTRA_IS_LOOPING = "EXTRA_IS_LOOPING"
