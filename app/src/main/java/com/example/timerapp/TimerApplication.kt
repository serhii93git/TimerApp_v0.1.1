package com.example.timerapp

import android.app.Application
import com.example.timerapp.utils.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TimerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Create notification channels early so receivers/services can use them immediately
        NotificationHelper.createChannels(this)
    }
}
