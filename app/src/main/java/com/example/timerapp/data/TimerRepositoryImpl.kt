package com.example.timerapp.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepositoryImpl @Inject constructor(
    private val dao: TimerDao
) : TimerRepository {

    override val timers: Flow<List<TimerEntity>> = dao.getAllTimers()

    override suspend fun insert(timer: TimerEntity): Long = dao.insert(timer)
    override suspend fun update(timer: TimerEntity) = dao.update(timer)
    override suspend fun delete(timer: TimerEntity) = dao.delete(timer)
    override suspend fun getAll(): List<TimerEntity> = dao.getAll()
    override suspend fun getById(id: Int): TimerEntity? = dao.getById(id)
    override suspend fun markStopped(id: Int) = dao.markStopped(id)
    override suspend fun markPaused(id: Int, remaining: Long) = dao.markPaused(id, remaining)
    override suspend fun markResumed(id: Int, newEnd: Long) = dao.markResumed(id, newEnd)
    override suspend fun updateOrderIndex(id: Int, orderIndex: Int) = dao.updateOrderIndex(id, orderIndex)
}
