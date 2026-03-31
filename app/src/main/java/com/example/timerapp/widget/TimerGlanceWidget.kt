package com.example.timerapp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.timerapp.data.AppDatabase
import com.example.timerapp.data.TimerState
import com.example.timerapp.service.TimerForegroundService
import com.example.timerapp.ui.MainActivity
import com.example.timerapp.utils.TimeUtils

class TimerGlanceWidget : GlanceAppWidget() {

    companion object {
        val PREF_TIME     = stringPreferencesKey("widget_time")
        val PREF_TITLE    = stringPreferencesKey("widget_title")
        val PREF_TIMER_ID = intPreferencesKey("widget_timer_id")
        val PREF_RUNNING  = booleanPreferencesKey("widget_running")
        val PREF_PAUSED   = booleanPreferencesKey("widget_paused")

        suspend fun updateState(context: Context) {
            val dao     = AppDatabase.getInstance(context).timerDao()
            val timers  = dao.getAll()
            val active  = timers.firstOrNull {
                it.state == TimerState.RUNNING || it.state == TimerState.PAUSED
            }

            val displayTime  = when {
                active == null                      -> "--:--:--"
                active.state == TimerState.RUNNING  ->
                    TimeUtils.formatMillis((active.endTimeMs - System.currentTimeMillis()).coerceAtLeast(0L))
                active.state == TimerState.PAUSED   ->
                    TimeUtils.formatMillis(active.remainingMs)
                else                                -> "--:--:--"
            }

            val displayTitle  = active?.title ?: context.getString(com.example.timerapp.R.string.no_active_timer)
            val timerId       = active?.id ?: -1
            val isRunning     = active?.state == TimerState.RUNNING
            val isPaused      = active?.state == TimerState.PAUSED

            val manager = GlanceAppWidgetManager(context)
            val ids     = manager.getGlanceIds(TimerGlanceWidget::class.java)
            ids.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[PREF_TIME]     = displayTime
                        this[PREF_TITLE]    = displayTitle
                        this[PREF_TIMER_ID] = timerId
                        this[PREF_RUNNING]  = isRunning
                        this[PREF_PAUSED]   = isPaused
                    }
                }
                TimerGlanceWidget().update(context, glanceId)
            }
        }

        suspend fun requestUpdate(context: Context) = updateState(context)
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            WidgetContent(prefs, context)
        }
    }

    @Composable
    private fun WidgetContent(prefs: Preferences, context: Context) {
        val displayTime = prefs[PREF_TIME]     ?: "--:--:--"
        val title       = prefs[PREF_TITLE]    ?: context.getString(com.example.timerapp.R.string.no_active_timer)
        val timerId     = prefs[PREF_TIMER_ID] ?: -1
        val isRunning   = prefs[PREF_RUNNING]  ?: false
        val isPaused    = prefs[PREF_PAUSED]   ?: false

        val bgColor   = ColorProvider(Color(0xFF7B5EA7))
        val textColor = ColorProvider(Color.White)
        val btnBg     = ColorProvider(Color(0x40FFFFFF))

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier            = GlanceModifier.fillMaxSize().padding(12.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text  = title,
                    style = TextStyle(color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text  = displayTime,
                    style = TextStyle(color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                )

                if (timerId != -1 && (isRunning || isPaused)) {
                    Spacer(GlanceModifier.height(8.dp))
                    Row(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isRunning) {
                            Button(
                                text    = "\u23F8",
                                onClick = actionStartService(
                                    Intent(context, TimerForegroundService::class.java).apply {
                                        action = TimerForegroundService.ACTION_PAUSE
                                        putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
                                    },
                                    isForegroundService = true
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = btnBg,
                                    contentColor    = textColor
                                )
                            )
                        }
                        if (isPaused) {
                            Button(
                                text    = "\u25B6",
                                onClick = actionStartService(
                                    Intent(context, TimerForegroundService::class.java).apply {
                                        action = TimerForegroundService.ACTION_RESUME
                                        putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
                                    },
                                    isForegroundService = true
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = btnBg,
                                    contentColor    = textColor
                                )
                            )
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        Button(
                            text    = "\u23F9",
                            onClick = actionStartService(
                                Intent(context, TimerForegroundService::class.java).apply {
                                    action = TimerForegroundService.ACTION_STOP_TIMER
                                    putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
                                },
                                isForegroundService = true
                            ),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = btnBg,
                                contentColor    = textColor
                            )
                        )
                    }
                }
            }
        }
    }
}
