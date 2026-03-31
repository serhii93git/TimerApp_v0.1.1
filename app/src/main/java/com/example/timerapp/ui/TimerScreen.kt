package com.example.timerapp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.timerapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val showAdd      by viewModel.showAddSheet.collectAsStateWithLifecycle()
    val editTimer    by viewModel.editingTimer.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()

    val noTimersText = stringResource(R.string.no_timers)
    val settingsText = stringResource(R.string.settings)
    val addTimerText = stringResource(R.string.action_add)

    val lazyListState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = lazyListState,
        onDragStarted = { viewModel.onDragStarted() },
        onMove        = { from, to -> viewModel.onReorder(from, to) },
        onDragEnd     = { viewModel.persistOrder() },
        onDragCancel  = { viewModel.cancelDrag() }
    )

    // Автопрокрутка LazyColumn коли палець біля верхнього/нижнього краю
    LaunchedEffect(dragDropState.isDragging) {
        if (!dragDropState.isDragging) return@LaunchedEffect
        val scrollThreshold = 60f
        while (dragDropState.isDragging) {
            val layoutInfo = lazyListState.layoutInfo
            val viewportStart = layoutInfo.viewportStartOffset.toFloat()
            val viewportEnd   = layoutInfo.viewportEndOffset.toFloat()
            val touchY = dragDropState.touchY

            val scrollSpeed = when {
                touchY < viewportStart + scrollThreshold ->
                    -(viewportStart + scrollThreshold - touchY).coerceIn(0f, scrollThreshold) * 1.5f
                touchY > viewportEnd - scrollThreshold ->
                    (touchY - viewportEnd + scrollThreshold).coerceIn(0f, scrollThreshold) * 1.5f
                else -> 0f
            }

            if (scrollSpeed != 0f) {
                lazyListState.scroll(MutatePriority.PreventUserInput) {
                    val scrolled = scrollBy(scrollSpeed)
                    dragDropState.onAutoScroll(scrolled)
                }
            }
            delay(16L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { viewModel.showSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = settingsText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { viewModel.showAddSheet() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = addTimerText, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.timers.isEmpty()) {
                Text(
                    text      = noTimersText,
                    modifier  = Modifier.align(Alignment.Center),
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    state               = lazyListState,
                    modifier            = Modifier
                        .fillMaxSize()
                        .dragContainer(dragDropState),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = uiState.timers,
                        key   = { _, timer -> timer.id }
                    ) { index, timer ->
                        val isDraggingThisItem = dragDropState.draggingItemIndex == index
                        val elevation by animateDpAsState(
                            targetValue = if (isDraggingThisItem) 8.dp else 2.dp,
                            label       = "card_elevation"
                        )
                        DraggableItem(dragDropState = dragDropState, index = index) { isDragging ->
                            TimerCard(
                                timer         = timer,
                                isDragEnabled = uiState.isDragEnabled,
                                elevation     = elevation,
                                onPause       = { viewModel.pauseTimer(timer.id) },
                                onResume      = { viewModel.resumeTimer(timer.id) },
                                onStop        = { viewModel.stopTimer(timer.id) },
                                onEdit        = { viewModel.showAddSheet(timer) },
                                onDelete      = { viewModel.deleteTimer(timer) },
                                onDragStart   = { /* handled by gesture */ }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddTimerSheet(
            editingTimer = editTimer,
            onDismiss    = { viewModel.hideAddSheet() },
            onSave       = { title, durationMs, endTimeMs, isLooping ->
                if (editTimer != null) {
                    viewModel.updateTimer(editTimer!!.id, title, durationMs, endTimeMs, isLooping)
                } else {
                    viewModel.addTimer(title, durationMs, endTimeMs, isLooping)
                }
                viewModel.hideAddSheet()
            }
        )
    }

    if (showSettings) {
        SettingsSheet(
            currentOrder  = uiState.sortOrder,
            onOrderChange = { viewModel.setSortOrder(it) },
            onDismiss     = { viewModel.hideSettings() }
        )
    }
}

fun Modifier.dragContainer(dragDropState: DragDropState): Modifier {
    return this.pointerInput(dragDropState) {
        detectDragGesturesAfterLongPress(
            onDragStart  = { offset -> dragDropState.onDragStart(offset) },
            onDrag       = { change, dragAmount ->
                change.consume()
                dragDropState.onDrag(dragAmount)
            },
            onDragEnd    = { dragDropState.onDragEnd() },
            onDragCancel = { dragDropState.onDragCancel() }
        )
    }
}

@Composable
fun DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val isDragging = dragDropState.draggingItemIndex == index
    // FIX: Use dragDelta (relative) instead of draggingItemOffset.y (absolute)
    val offsetY = if (isDragging) dragDropState.dragDelta else 0f
    Box(
        modifier = Modifier.offset { androidx.compose.ui.unit.IntOffset(0, offsetY.toInt()) }
    ) {
        content(isDragging)
    }
}
