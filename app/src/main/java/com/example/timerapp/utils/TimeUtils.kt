package com.example.timerapp.utils

object TimeUtils {

    fun formatMillis(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val days    = totalSeconds / 86400L
        val hours   = (totalSeconds % 86400L) / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (days > 0) {
            "%dd %02d:%02d:%02d".format(days, hours, minutes, seconds)
        } else {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        }
    }

    fun formatMillisShort(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val hours   = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}
