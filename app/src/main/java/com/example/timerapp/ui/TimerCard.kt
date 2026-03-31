package com.example.timerapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timerapp.R
import com.example.timerapp.data.TimerEntity
import com.example.timerapp.data.TimerState
import com.example.timerapp.utils.TimeUtils

@Composable
fun TimerCard(
    timer:         TimerEntity,
    isDragEnabled: Boolean,
    elevation:     Dp,
    onPause:       () -> Unit,
    onResume:      () -> Unit,
    onStop:        () -> Unit,
    onEdit:        () -> Unit,
    onDelete:      () -> Unit,
    onDragStart:   () -> Unit
) {
    val pauseLabel   = stringResource(R.string.action_pause)
    val resumeLabel  = stringResource(R.string.action_resume)
    val stopLabel    = stringResource(R.string.action_stop)
    val editLabel    = stringResource(R.string.action_edit)
    val deleteLabel  = stringResource(R.string.action_delete)
    val stoppedLabel = stringResource(R.string.timer_stopped)
    val pausedLabel  = stringResource(R.string.timer_paused)
    val dragLabel    = stringResource(R.string.drag_handle)

    var displayTime by remember(timer.id, timer.state, timer.endTimeMs, timer.remainingMs) {
        mutableStateOf(
            when (timer.state) {
                TimerState.RUNNING  -> TimeUtils.formatMillis((timer.endTimeMs - System.currentTimeMillis()).coerceAtLeast(0L))
                TimerState.PAUSED   -> TimeUtils.formatMillis(timer.remainingMs)
                TimerState.FINISHED -> stoppedLabel
            }
        )
    }

    LaunchedEffect(timer.id, timer.state, timer.endTimeMs) {
        if (timer.state == TimerState.RUNNING) {
            while (true) {
                val now       = System.currentTimeMillis()
                val remaining = (timer.endTimeMs - now).coerceAtLeast(0L)
                displayTime   = TimeUtils.formatMillis(remaining)
                if (remaining == 0L) {
                    displayTime = stoppedLabel
                    break
                }
                val drift = now % 1_000L
                kotlinx.coroutines.delay(if (drift == 0L) 1_000L else 1_000L - drift)
            }
        } else if (timer.state == TimerState.PAUSED) {
            displayTime = TimeUtils.formatMillis(timer.remainingMs)
        } else {
            displayTime = stoppedLabel
        }
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDragEnabled) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_drag_handle),
                        contentDescription = dragLabel,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp)
                    )
                }

                Row(
                    modifier          = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = timer.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (timer.isPaused) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text     = pausedLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    if (timer.isLooping) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            painter            = painterResource(R.drawable.ic_loop),
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.secondary,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded         = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text    = { Text(editLabel) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text    = { Text(deleteLabel) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text  = displayTime,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize   = 36.sp
                ),
                color = when (timer.state) {
                    TimerState.RUNNING  -> MaterialTheme.colorScheme.primary
                    TimerState.PAUSED   -> MaterialTheme.colorScheme.tertiary
                    TimerState.FINISHED -> MaterialTheme.colorScheme.outline
                }
            )

            Spacer(Modifier.height(12.dp))

            when (timer.state) {
                TimerState.RUNNING -> {
                    OutlinedButton(onClick = onPause) {
                        Icon(painterResource(R.drawable.ic_pause), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(pauseLabel)
                    }
                }
                TimerState.PAUSED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onResume) {
                            Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(resumeLabel)
                        }
                        OutlinedButton(onClick = onStop) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stopLabel)
                        }
                    }
                }
                TimerState.FINISHED -> { /* No buttons */ }
            }
        }
    }
}
