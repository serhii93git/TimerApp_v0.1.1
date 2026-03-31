package com.example.timerapp.ui

import android.widget.NumberPicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.timerapp.R
import com.example.timerapp.data.TimerEntity
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimerSheet(
    editingTimer: TimerEntity?,
    onDismiss:    () -> Unit,
    onSave:       (title: String, durationMs: Long, endTimeMs: Long, isLooping: Boolean) -> Unit
) {
    val titleAddText     = stringResource(R.string.add_timer_title)
    val titleEditText    = stringResource(R.string.edit_timer_title)
    val tabCountdown     = stringResource(R.string.tab_countdown)
    val tabExact         = stringResource(R.string.tab_exact_time)
    val labelTitle       = stringResource(R.string.timer_title_label)
    val labelDays        = stringResource(R.string.days)
    val labelHours       = stringResource(R.string.hours)
    val labelMinutes     = stringResource(R.string.minutes)
    val labelSeconds     = stringResource(R.string.seconds)
    val labelDay         = stringResource(R.string.day)
    val labelMonth       = stringResource(R.string.month)
    val labelYear        = stringResource(R.string.year)
    val labelHour        = stringResource(R.string.hour)
    val labelMinute      = stringResource(R.string.minute)
    val labelLoop        = stringResource(R.string.loop_timer)
    val saveText         = stringResource(R.string.action_save)
    val cancelText       = stringResource(R.string.action_cancel)
    val errDuration      = stringResource(R.string.error_invalid_duration)
    val errPastTime      = stringResource(R.string.error_past_time)

    val now = Calendar.getInstance()
    val months = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

    var timerName   by rememberSaveable { mutableStateOf(editingTimer?.title ?: "") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isLooping   by rememberSaveable { mutableStateOf(editingTimer?.isLooping ?: false) }
    var errorText   by rememberSaveable { mutableStateOf("") }

    var pickDays    by rememberSaveable { mutableIntStateOf(0) }
    var pickHours   by rememberSaveable { mutableIntStateOf(0) }
    var pickMinutes by rememberSaveable { mutableIntStateOf(0) }
    var pickSeconds by rememberSaveable { mutableIntStateOf(0) }

    var exactDay    by rememberSaveable { mutableIntStateOf(now.get(Calendar.DAY_OF_MONTH)) }
    var exactMonth  by rememberSaveable { mutableIntStateOf(now.get(Calendar.MONTH) + 1) }
    var exactYear   by rememberSaveable { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var exactHour   by rememberSaveable { mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var exactMinute by rememberSaveable { mutableIntStateOf(now.get(Calendar.MINUTE)) }

    LaunchedEffect(editingTimer) {
        editingTimer?.let { t ->
            val totalSec = t.durationMs / 1000L
            pickDays    = (totalSec / 86400L).toInt()
            pickHours   = ((totalSec % 86400L) / 3600L).toInt()
            pickMinutes = ((totalSec % 3600L) / 60L).toInt()
            pickSeconds = (totalSec % 60L).toInt()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text  = if (editingTimer != null) titleEditText else titleAddText,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value         = timerName,
                onValueChange = { timerName = it },
                label         = { Text(labelTitle) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            Spacer(Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text(tabCountdown, modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text(tabExact, modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "1m", 5 to "5m", 10 to "10m", 30 to "30m").forEach { (mins, label) ->
                            AssistChip(onClick = {
                                var newMins = pickMinutes + mins
                                var newHours = pickHours
                                if (newMins >= 60) { newHours += newMins / 60; newMins %= 60 }
                                var newDays = pickDays
                                if (newHours >= 24) { newDays += newHours / 24; newHours %= 24 }
                                pickMinutes = newMins
                                pickHours   = newHours.coerceAtMost(23)
                                pickDays    = newDays.coerceAtMost(364)
                            }, label = { Text("+$label") })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WheelPicker(labelDays,    0,  364, pickDays)    { pickDays    = it }
                        WheelPicker(labelHours,   0,   23, pickHours)   { pickHours   = it }
                        WheelPicker(labelMinutes, 0,   59, pickMinutes) { pickMinutes = it }
                        WheelPicker(labelSeconds, 0,   59, pickSeconds) { pickSeconds = it }
                    }
                }
                1 -> {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WheelPicker(labelDay,   1, 31, exactDay)   { exactDay   = it }
                        WheelPicker(labelMonth, 1, 12, exactMonth, displayedValues = months) { exactMonth = it }
                        WheelPicker(labelYear,  now.get(Calendar.YEAR), now.get(Calendar.YEAR) + 10, exactYear) { exactYear = it }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier             = Modifier.fillMaxWidth(),
                        horizontalArrangement= Arrangement.Center,
                        verticalAlignment    = Alignment.CenterVertically
                    ) {
                        WheelPicker(labelHour,   0, 23, exactHour)   { exactHour   = it }
                        Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 8.dp))
                        WheelPicker(labelMinute, 0, 59, exactMinute) { exactMinute = it }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(labelLoop, modifier = Modifier.weight(1f))
                Switch(checked = isLooping, onCheckedChange = { isLooping = it })
            }

            if (errorText.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(cancelText) }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick  = {
                        errorText = ""
                        val name = timerName.ifBlank { "Timer" }
                        if (selectedTab == 0) {
                            val totalSecs = pickDays * 86400L + pickHours * 3600L + pickMinutes * 60L + pickSeconds
                            if (totalSecs <= 0L) { errorText = errDuration; return@Button }
                            val durationMs = totalSecs * 1000L
                            val endTimeMs  = System.currentTimeMillis() + durationMs
                            onSave(name, durationMs, endTimeMs, isLooping)
                        } else {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, exactYear)
                                set(Calendar.MONTH, exactMonth - 1)
                                set(Calendar.DAY_OF_MONTH, exactDay)
                                set(Calendar.HOUR_OF_DAY, exactHour)
                                set(Calendar.MINUTE, exactMinute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val endTimeMs = cal.timeInMillis
                            if (endTimeMs <= System.currentTimeMillis()) { errorText = errPastTime; return@Button }
                            val durationMs = endTimeMs - System.currentTimeMillis()
                            onSave(name, durationMs, endTimeMs, isLooping)
                        }
                    }
                ) { Text(saveText) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun WheelPicker(
    label:           String,
    minVal:          Int,
    maxVal:          Int,
    value:           Int,
    displayedValues: Array<String>? = null,
    onValueChange:   (Int) -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface.let {
        android.graphics.Color.argb(
            (it.alpha * 255).toInt(),
            (it.red * 255).toInt(),
            (it.green * 255).toInt(),
            (it.blue * 255).toInt()
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AndroidView(
            factory  = { ctx ->
                NumberPicker(ctx).apply {
                    wrapSelectorWheel = true
                    this.displayedValues = null
                    this.minValue = minVal
                    this.maxValue = maxVal
                    if (displayedValues != null) this.displayedValues = displayedValues
                    this.value = value.coerceIn(minVal, maxVal)
                    setOnValueChangedListener { _, _, newVal -> onValueChange(newVal) }
                    // Колір тексту для темної теми
                    try {
                        val field = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
                        field.isAccessible = true
                        (field.get(this) as? android.graphics.Paint)?.color = textColor
                        // Оновити колір для дочірніх EditText
                        for (i in 0 until childCount) {
                            (getChildAt(i) as? android.widget.EditText)?.setTextColor(textColor)
                        }
                    } catch (_: Exception) { }
                    invalidate()
                }
            },
            update   = { picker ->
                picker.displayedValues = null
                picker.minValue = minVal
                picker.maxValue = maxVal
                if (displayedValues != null) picker.displayedValues = displayedValues
                if (picker.value != value) picker.value = value.coerceIn(minVal, maxVal)
                // Оновити колір при зміні теми
                try {
                    val field = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
                    field.isAccessible = true
                    (field.get(picker) as? android.graphics.Paint)?.color = textColor
                    for (i in 0 until picker.childCount) {
                        (picker.getChildAt(i) as? android.widget.EditText)?.setTextColor(textColor)
                    }
                } catch (_: Exception) { }
                picker.invalidate()
            },
            modifier = Modifier.height(120.dp)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}