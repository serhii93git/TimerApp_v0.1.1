package com.example.timerapp.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

/**
 * Manages drag-and-drop gesture state for LazyColumn.
 *
 * FIX: Uses delta-based offset tracking. The original code set draggingItemOffset
 * to item.offset (absolute viewport position), causing the dragged card to
 * visually jump by its own y-position on drag start. Now we track:
 *   - initialItemOffset: absolute position for hit-testing
 *   - dragDelta: accumulated gesture delta for visual offset (starts at 0)
 */
class DragDropState(
    val lazyListState: LazyListState,
    private val onDragStartedCallback: () -> Unit,
    private val onMoveCallback:        (Int, Int) -> Unit,
    private val onDragEndCallback:     () -> Unit,
    private val onDragCancelCallback:  () -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    /** Visual offset delta from the item's natural position (starts at 0). */
    var dragDelta by mutableFloatStateOf(0f)
        private set

    /** Absolute viewport offset of the dragged item — used only for hit-testing. */
    private var initialItemOffset = 0f

    val isDragging: Boolean get() = draggingItemIndex != null

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.let { item ->
                draggingItemIndex  = item.index
                initialItemOffset  = item.offset.toFloat()
                dragDelta          = 0f
                onDragStartedCallback()
            }
    }

    fun onDrag(offset: Offset) {
        val current = draggingItemIndex ?: return
        dragDelta += offset.y
        // Hit-test using absolute position
        val currentAbsoluteY = initialItemOffset + dragDelta
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                currentAbsoluteY.toInt() in item.offset..(item.offset + item.size) &&
                item.index != current
            }
            ?.let { target ->
                // Recalibrate: item swapped, so reset initial to target's position
                initialItemOffset = target.offset.toFloat()
                dragDelta = currentAbsoluteY - initialItemOffset
                onMoveCallback(current, target.index)
                draggingItemIndex = target.index
            }
    }

    fun onDragEnd() {
        draggingItemIndex = null
        dragDelta = 0f
        onDragEndCallback()
    }

    fun onDragCancel() {
        draggingItemIndex = null
        dragDelta = 0f
        onDragCancelCallback()
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onDragStarted: () -> Unit,
    onMove:        (Int, Int) -> Unit,
    onDragEnd:     () -> Unit,
    onDragCancel:  () -> Unit
): DragDropState = remember(lazyListState) {
    DragDropState(
        lazyListState        = lazyListState,
        onDragStartedCallback = onDragStarted,
        onMoveCallback        = onMove,
        onDragEndCallback     = onDragEnd,
        onDragCancelCallback  = onDragCancel
    )
}
