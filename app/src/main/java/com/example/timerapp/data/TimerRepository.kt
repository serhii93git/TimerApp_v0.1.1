package com.example.timerapp.data

import kotlinx.coroutines.flow.Flow

interface TimerRepository {
    val timers: Flow<List<TimerEntity>>
    suspend fun insert(timer: TimerEntity): Long
    suspend fun update(timer: TimerEntity)
    suspend fun delete(timer: TimerEntity)
    suspend fun getAll(): List<TimerEntity>
    suspend fun getById(id: Int): TimerEntity?
    suspend fun markStopped(id: Int)
    suspend fun markPaused(id: Int, remaining: Long)
    suspend fun markResumed(id: Int, newEnd: Long)
    suspend fun updateOrderIndex(id: Int, orderIndex: Int)
    suspend fun getNextOrderIndex(): Int
    suspend fun deleteAndReindex(timer: TimerEntity)
}
