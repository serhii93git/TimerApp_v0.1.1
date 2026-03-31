package com.example.timerapp.ui

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
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

    private var mediaPlayer: MediaPlayer? = null
    private val activityScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

        val timerId    = intent.getIntExtra(TimerForegroundService.EXTRA_TIMER_ID, -1)
        val timerTitle = intent.getStringExtra("TIMER_TITLE") ?: getString(R.string.app_name)

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

                        Spacer(Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = {
                                    stopAlarm()
                                    if (timerId != -1) {
                                        activityScope.launch {
                                            val dao   = AppDatabase.getInstance(this@TimerRingingActivity).timerDao()
                                            val timer = dao.getById(timerId)
                                            timer?.let { t ->
                                                val now    = System.currentTimeMillis()
                                                val newEnd = now + t.durationMs
                                                dao.update(t.copy(endTimeMs = newEnd, state = TimerState.RUNNING, remainingMs = 0L))
                                                AlarmScheduler.schedule(this@TimerRingingActivity, timerId, newEnd)
                                                val startIntent = android.content.Intent(
                                                    this@TimerRingingActivity,
                                                    TimerForegroundService::class.java
                                                ).apply {
                                                    action = TimerForegroundService.ACTION_START
                                                    putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
                                                }
                                                startForegroundService(startIntent)
                                            }
                                            NotificationHelper.cancelCompletionNotification(this@TimerRingingActivity, timerId)
                                        }
                                    }
                                    finish()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6ECFAA))
                            ) {
                                Text("\u21BA $restartText")
                            }

                            Button(
                                onClick = {
                                    stopAlarm()
                                    if (timerId != -1) {
                                        activityScope.launch {
                                            val dao = AppDatabase.getInstance(this@TimerRingingActivity).timerDao()
                                            dao.markStopped(timerId)
                                            NotificationHelper.cancelCompletionNotification(this@TimerRingingActivity, timerId)
                                        }
                                    }
                                    finish()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9C84D4)
                                )
                            ) {
                                Text(dismissText, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
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
