package com.example.timerapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.timerapp.service.TimerForegroundService

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

        // Exact-alarm broadcasts are on the FGS-start allowlist even in Doze,
        // so we hand the completion off to the service. Running inside the FGS
        // gives us BAL (Background Activity Launch) exemption, which is what
        // actually lets startActivity(ringingIntent) pop the full-screen
        // alarm UI regardless of lock/active state.
        val svcIntent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_TIMER_COMPLETE
            putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
        }
        try {
            context.startForegroundService(svcIntent)
        } finally {
            if (wakeLock.isHeld) runCatching { wakeLock.release() }
        }
    }
}