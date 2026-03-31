package com.example.timerapp.utils

import android.content.Context
import com.example.timerapp.ui.SortOrder

object PrefsManager {
    private const val PREFS_NAME = "timer_prefs"
    private const val KEY_SORT_ORDER = "sort_order"

    fun saveSortOrder(context: Context, order: SortOrder) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SORT_ORDER, order.name)
            .apply()
    }

    fun loadSortOrder(context: Context): SortOrder {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SORT_ORDER, SortOrder.CUSTOM.name)
        return try { SortOrder.valueOf(name ?: SortOrder.CUSTOM.name) }
        catch (e: IllegalArgumentException) { SortOrder.CUSTOM }
    }
}
