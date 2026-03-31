package com.example.timerapp.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

class DragDropState(
    val lazyListState: LazyListState,
    private val onDragStartedCallback: () -> Unit,
    private val onMoveCallback:        (Int, Int) -> Unit,
    private val onDragEndCallback:     () -> Unit,
    private val onDragCancelCallback:  () -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    var dragDelta by mutableFloatStateOf(0f)
        private set

    /** Позиція пальця у viewport — для автопрокрутки */
    var touchY by mutableFloatStateOf(0f)
        private set

    private var initialItemOffset = 0f

    val isDragging: Boolean get() = draggingItemIndex != null

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.let { item ->
                draggingItemIndex  = item.index
                initialItemOffset  = item.offset.toFloat()
                dragDelta          = 0f
                touchY             = offset.y
                onDragStartedCallback()
            }
    }

    fun onDrag(offset: Offset) {
        if (draggingItemIndex == null) return
        dragDelta += offset.y
        touchY    += offset.y
        checkForSwap()
    }

    /** Викликається після автопрокрутки — компенсує зміщення елементів */
    fun onAutoScroll(scrolledBy: Float) {
        if (draggingItemIndex == null) return
        initialItemOffset -= scrolledBy
        checkForSwap()
    }

    private fun checkForSwap() {
        val current = draggingItemIndex ?: return
        val currentAbsoluteY = initialItemOffset + dragDelta

        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                if (item.index == current) return@firstOrNull false
                // Threshold: swap тільки коли перетягнули за середину цільового елемента
                val midpoint = item.offset + item.size / 2
                if (item.index > current) {
                    currentAbsoluteY.toInt() > midpoint
                } else {
                    currentAbsoluteY.toInt() < midpoint
                }
            }
            ?.let { target ->
                initialItemOffset = target.offset.toFloat()
                dragDelta = currentAbsoluteY - initialItemOffset
                onMoveCallback(current, target.index)
                draggingItemIndex = target.index
            }
    }

    fun onDragEnd() {
        draggingItemIndex = null
        dragDelta = 0f
        touchY    = 0f
        onDragEndCallback()
    }

    fun onDragCancel() {
        draggingItemIndex = null
        dragDelta = 0f
        touchY    = 0f
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
