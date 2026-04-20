package com.example.timerapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timer: TimerEntity): Long

    @Update
    suspend fun update(timer: TimerEntity)

    @Delete
    suspend fun delete(timer: TimerEntity)

    @Query("SELECT * FROM timers ORDER BY orderIndex ASC")
    fun getAllTimers(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers ORDER BY orderIndex ASC")
    suspend fun getAll(): List<TimerEntity>

    @Query("SELECT * FROM timers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): TimerEntity?

    @Query("UPDATE timers SET state = 'FINISHED', remainingMs = 0 WHERE id = :id")
    suspend fun markStopped(id: Int)

    @Query("UPDATE timers SET state = 'PAUSED', remainingMs = :remaining WHERE id = :id")
    suspend fun markPaused(id: Int, remaining: Long)

    @Query("UPDATE timers SET state = 'RUNNING', endTimeMs = :newEnd, remainingMs = 0 WHERE id = :id")
    suspend fun markResumed(id: Int, newEnd: Long)

    @Query("UPDATE timers SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Int, orderIndex: Int)

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM timers")
    suspend fun getNextOrderIndex(): Int

    @Transaction
    suspend fun deleteAndReindex(timer: TimerEntity) {
        delete(timer)
        val remaining = getAll()
        remaining.forEachIndexed { index, t ->
            if (t.orderIndex != index) updateOrderIndex(t.id, index)
        }
    }
}
