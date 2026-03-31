package com.example.timerapp.di

import com.example.timerapp.data.TimerRepository
import com.example.timerapp.data.TimerRepositoryImpl
import com.example.timerapp.data.TimerDao
import com.example.timerapp.data.AppDatabase
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindRepository(impl: TimerRepositoryImpl): TimerRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
            AppDatabase.getInstance(context)

        @Provides
        @Singleton
        fun provideTimerDao(db: AppDatabase): TimerDao = db.timerDao()
    }
}
