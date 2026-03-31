package com.example.timerapp.ui

import com.example.timerapp.data.TimerEntity

data class TimerUiState(
    val timers:        List<TimerEntity> = emptyList(),
    val sortOrder:     SortOrder         = SortOrder.CUSTOM,
    val isDragEnabled: Boolean           = true,
    val showAddSheet:  Boolean           = false,
    val editingTimer:  TimerEntity?      = null,
    val showSettings:  Boolean           = false,
    val pendingTimer:  PendingTimerData? = null
)

data class PendingTimerData(
    val title:      String,
    val durationMs: Long,
    val endTimeMs:  Long,
    val isLooping:  Boolean,
    val editId:     Int? = null
)

enum class SortOrder { CUSTOM, TIME_ASC, TIME_DESC }
