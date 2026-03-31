package com.example.timerapp.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timerapp.data.TimerEntity
import com.example.timerapp.data.TimerRepository
import com.example.timerapp.data.TimerState
import com.example.timerapp.service.TimerForegroundService
import com.example.timerapp.utils.AlarmScheduler
import com.example.timerapp.utils.PrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val repo: TimerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _sortOrder    = MutableStateFlow(PrefsManager.loadSortOrder(context))
    private val _isDragging   = MutableStateFlow(false)
    private val _dragSnapshot = MutableStateFlow<List<TimerEntity>?>(null)

    val uiState: StateFlow<TimerUiState> = combine(
        repo.timers, _sortOrder, _isDragging, _dragSnapshot
    ) { list, order, dragging, snapshot ->
        val displayList = if (dragging && snapshot != null) {
            snapshot
        } else {
            when (order) {
                SortOrder.CUSTOM    -> list
                SortOrder.TIME_ASC  -> list.sortedBy {
                    if (it.isRunning) it.endTimeMs - System.currentTimeMillis()
                    else it.remainingMs
                }
                SortOrder.TIME_DESC -> list.sortedByDescending {
                    if (it.isRunning) it.endTimeMs - System.currentTimeMillis()
                    else it.remainingMs
                }
            }
        }
        TimerUiState(
            timers        = displayList,
            sortOrder     = order,
            isDragEnabled = (order == SortOrder.CUSTOM)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState())

    private val _showAddSheet = MutableStateFlow(false)
    private val _editingTimer = MutableStateFlow<TimerEntity?>(null)
    private val _showSettings = MutableStateFlow(false)

    val showAddSheet: StateFlow<Boolean>      = _showAddSheet
    val editingTimer: StateFlow<TimerEntity?> = _editingTimer
    val showSettings: StateFlow<Boolean>      = _showSettings

    // ─── Drag Lifecycle (Three-layer gate) ───────────────────────────

    fun onDragStarted() {
        _isDragging.value   = true
        _dragSnapshot.value = uiState.value.timers.toList()
    }

    fun onReorder(from: Int, to: Int) {
        val current = _dragSnapshot.value?.toMutableList() ?: return
        if (from !in current.indices || to !in current.indices) return
        val item = current.removeAt(from)
        current.add(to, item)
        _dragSnapshot.value = current
    }

    fun persistOrder() {
        val snapshot = _dragSnapshot.value ?: return
        viewModelScope.launch {
            snapshot.forEachIndexed { index, timer ->
                repo.updateOrderIndex(timer.id, index)
            }
            _isDragging.value   = false
            _dragSnapshot.value = null
        }
    }

    fun cancelDrag() {
        _isDragging.value   = false
        _dragSnapshot.value = null
    }

    // ─── Timer CRUD ──────────────────────────────────────────────────

    fun addTimer(title: String, durationMs: Long, endTimeMs: Long, isLooping: Boolean) {
        viewModelScope.launch {
            val currentCount = repo.getAll().size
            val timer = TimerEntity(
                title       = title,
                durationMs  = durationMs,
                endTimeMs   = endTimeMs,
                state       = TimerState.RUNNING,
                remainingMs = 0L,
                orderIndex  = currentCount,
                isLooping   = isLooping
            )
            val id = repo.insert(timer)
            AlarmScheduler.schedule(context, id.toInt(), endTimeMs)
            startService(ACTION_START, id.toInt())
        }
    }

    fun updateTimer(id: Int, title: String, durationMs: Long, endTimeMs: Long, isLooping: Boolean) {
        viewModelScope.launch {
            val existing = repo.getById(id) ?: return@launch
            AlarmScheduler.cancel(context, id)
            val updated = existing.copy(
                title       = title,
                durationMs  = durationMs,
                endTimeMs   = endTimeMs,
                state       = TimerState.RUNNING,
                remainingMs = 0L,
                isLooping   = isLooping
            )
            repo.update(updated)
            AlarmScheduler.schedule(context, id, endTimeMs)
            startService(ACTION_START, id)
        }
    }

    fun deleteTimer(timer: TimerEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancel(context, timer.id)
            stopService(timer.id)
            repo.delete(timer)
        }
    }

    fun pauseTimer(timerId: Int)  { startService(ACTION_PAUSE, timerId) }
    fun resumeTimer(timerId: Int) { startService(ACTION_RESUME, timerId) }
    fun stopTimer(timerId: Int)   { startService(ACTION_STOP, timerId) }

    // ─── Sort ────────────────────────────────────────────────────────

    fun setSortOrder(order: SortOrder) {
        PrefsManager.saveSortOrder(context, order)
        _sortOrder.value = order
    }

    // ─── Sheet visibility ────────────────────────────────────────────

    fun showAddSheet(editing: TimerEntity? = null) {
        _editingTimer.value = editing
        _showAddSheet.value = true
    }

    fun hideAddSheet() {
        _showAddSheet.value = false
        _editingTimer.value = null
    }

    fun showSettings()  { _showSettings.value = true }
    fun hideSettings()  { _showSettings.value = false }

    // ─── Service helpers ─────────────────────────────────────────────

    private val ACTION_START  = TimerForegroundService.ACTION_START
    private val ACTION_PAUSE  = TimerForegroundService.ACTION_PAUSE
    private val ACTION_RESUME = TimerForegroundService.ACTION_RESUME
    private val ACTION_STOP   = TimerForegroundService.ACTION_STOP_TIMER

    private fun startService(action: String, timerId: Int) {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            this.action = action
            putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
        }
        context.startForegroundService(intent)
    }

    private fun stopService(timerId: Int) {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_STOP_TIMER
            putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
        }
        context.startForegroundService(intent)
    }
}
