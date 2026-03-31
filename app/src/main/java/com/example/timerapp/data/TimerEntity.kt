package com.example.timerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val durationMs: Long,
    val endTimeMs: Long,
    val state: TimerState = TimerState.FINISHED,
    val remainingMs: Long = 0L,
    val orderIndex: Int = 0,
    val isLooping: Boolean = false
) {
    val isRunning: Boolean get() = state == TimerState.RUNNING
    val isPaused: Boolean  get() = state == TimerState.PAUSED
    val isFinished: Boolean get() = state == TimerState.FINISHED
}
