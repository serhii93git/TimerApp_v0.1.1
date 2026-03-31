package com.example.timerapp.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [TimerEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(TimerStateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun timerDao(): TimerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timer_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

class TimerStateConverter {
    @TypeConverter
    fun fromTimerState(state: TimerState): String = state.name

    @TypeConverter
    fun toTimerState(value: String): TimerState = TimerState.valueOf(value)
}
