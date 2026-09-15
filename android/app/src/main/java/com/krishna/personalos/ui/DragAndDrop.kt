package com.krishna.personalos.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal class WorkspaceDragState {
    data class Target(val category: String, val bounds: Rect, val drop: (String, Boolean) -> Unit)
    val targets = mutableMapOf<String, Target>()
    var source by mutableStateOf<String?>(null)
        private set
    var category by mutableStateOf("")
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    var target by mutableStateOf<String?>(null)
        private set
    fun start(id: String, type: String, point: Offset) { source = id; category = type; pointer = point; refresh() }
    fun move(delta: Offset) { pointer += delta; refresh() }
    fun refresh() {
        target = targets.filter { (id, item) -> id != source && item.category == category && item.bounds.contains(pointer) }
            .minByOrNull { it.value.bounds.width * it.value.bounds.height }?.key
    }
    fun finish() {
        refresh()
        val id = source
        val destination = targets[target]
        val after = destination?.let { pointer.y > it.bounds.center.y } ?: false
        cancel()
        if (id != null && destination != null) destination.drop(id, after)
    }
    fun cancel() { source = null; target = null; category = "" }
}

internal val LocalWorkspaceDrag = staticCompositionLocalOf<WorkspaceDragState?> { null }

/** Geometry is measured in root coordinates so nested sections share a drop space. */
internal fun Modifier.workspaceDropTarget(state: WorkspaceDragState?, id: String, category: String, onDrop: (String, Boolean) -> Unit): Modifier = composed {
    if (state == null) return@composed this
    val latestDrop by rememberUpdatedState(onDrop)
    val color = MaterialTheme.colorScheme.primary
    DisposableEffect(state, id) { onDispose { state.targets.remove(id) } }
    this.testTag("drop-target-$id").onGloballyPositioned { state.targets[id] = WorkspaceDragState.Target(category, it.boundsInRoot()) { source, after -> latestDrop(source, after) }; if (state.source != null) state.refresh() }
        .drawBehind {
            if (state.target == id) {
                drawRect(color.copy(alpha = .12f))
                val bounds = state.targets[id]?.bounds
                val y = if (bounds != null && state.pointer.y > bounds.center.y) size.height else 0f
                drawLine(color, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
            }
        }
}

@Composable
internal fun WorkspaceDragHandle(id: String, category: String, label: String, enabled: Boolean = true, state: WorkspaceDragState? = LocalWorkspaceDrag.current) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val feedback = LocalHapticFeedback.current
    val container = LocalPinnableContainer.current
    var pin by remember { mutableStateOf<PinnableContainer.PinnedHandle?>(null) }
    DisposableEffect(state, id) { onDispose { pin?.release(); pin = null; if (state?.source == id) state.cancel() } }
    Box(Modifier.size(44.dp).onGloballyPositioned { bounds = it.boundsInRoot() }
        .semantics { contentDescription = "Drag $label" }
        .pointerInput(state, id, enabled) {
            try {
            if (state != null && enabled) detectDragGesturesAfterLongPress(
                onDragStart = { offset -> pin?.release(); pin = container?.pin(); feedback.performHapticFeedback(HapticFeedbackType.LongPress); state.start(id, category, bounds.topLeft + offset) },
                onDrag = { change, delta -> change.consume(); state.move(delta) },
                onDragEnd = { state.finish(); pin?.release(); pin = null }, onDragCancel = { state.cancel(); pin?.release(); pin = null },
            )
            } finally { pin?.release(); pin = null; if (state?.source == id) state.cancel() }
        }, contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.DragIndicator, null, tint = if (state?.source == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else .4f))
    }
}

@Composable
internal fun WorkspaceDragAutoScroll(state: WorkspaceDragState, bounds: Rect, horizontal: Boolean = false, scroll: suspend (Float) -> Unit) {
    val currentBounds by rememberUpdatedState(bounds)
    val currentScroll by rememberUpdatedState(scroll)
    val edge = with(LocalDensity.current) { 56.dp.toPx() }
    val speed = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(state.source) {
        while (state.source != null) {
            withFrameNanos { }
            val area = currentBounds
            if (area == Rect.Zero) continue
            val position = if (horizontal) state.pointer.x else state.pointer.y
            val start = if (horizontal) area.left else area.top
            val end = if (horizontal) area.right else area.bottom
            val amount = when { position < start + edge -> -speed; position > end - edge -> speed; else -> 0f }
            if (amount != 0f) { currentScroll(amount); state.refresh() }
        }
    }
}
