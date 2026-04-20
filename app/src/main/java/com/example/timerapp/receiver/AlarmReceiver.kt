package com.example.timerapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.timerapp.service.TimerForegroundService
import com.example.timerapp.ui.TimerRingingActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getIntExtra(TimerForegroundService.EXTRA_TIMER_ID, -1)
        if (timerId == -1) return

        // Brief CPU wake so the FGS has time to come up and launch the alarm UI
        // before the system slides back into Doze.
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TimerApp:AlarmReceiver"
        )
        runCatching { wakeLock.acquire(10_000L) }

        try {
            // Direct attempt first: the FGS is almost certainly alive at this point
            // (the tick loop hasn't reached 0 yet) so the process has BAL exemption,
            // and launching synchronously from onReceive maximises the chance the
            // activity pops before the BAL grace window closes.
            runCatching {
                val ringingIntent = Intent(context, TimerRingingActivity::class.java).apply {
                    putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(ringingIntent)
            }

            // Also hand the completion off to the FGS so DB state, persistent
            // notification and the shade FSI notification are handled correctly.
            // Starting an FGS from an exact-alarm broadcast is on the Android 14
            // allowlist even during Doze.
            val svcIntent = Intent(context, TimerForegroundService::class.java).apply {
                action = TimerForegroundService.ACTION_TIMER_COMPLETE
                putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
            }
            context.startForegroundService(svcIntent)
        } finally {
            if (wakeLock.isHeld) runCatching { wakeLock.release() }
        }
    }
}