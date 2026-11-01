package com.drdisagree.xmorphclock.ui.home

import android.graphics.RectF
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.domain.editor.FaceComposer
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.FaceAlign
import com.drdisagree.xmorphclock.domain.model.ItemBounds
import com.drdisagree.xmorphclock.domain.model.SnapResult
import com.drdisagree.xmorphclock.ui.theme.OnInk
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun BoxScope.ItemOverlay(
    extents: Map<String, RectF>,
    composition: Composition,
    selected: Int?,
    focused: String?,
    align: FaceAlign?,
    unit: Float,
    frameTop: Float,
    width: Float,
    dragging: Boolean,
    snap: SnapResult,
    onSelect: (Int?) -> Unit,
    onFocus: (Int, String?) -> Unit,
    onDrag: (Int, Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onAlign: (Int, FaceAlign) -> Unit,
    onRemove: (Int) -> Unit,
    frameLeft: Float,
    height: Float,
    onMeasured: (Map<Int, ItemBounds>, Float) -> Unit,
    resizable: Boolean = false,
    singles: Set<Int> = emptySet(),
    onResize: (Int, Float) -> Unit = { _, _ -> }
) {
    val density = LocalDensity.current
    val pad = with(density) { BOX_PAD.toPx() }
    val slack = with(density) { ITEM_TOUCH.toPx() }
    val layersOf = composition.items.associate { item ->
        item.id to extents.filterKeys { id -> if (item.clock) FaceComposer.isClockLayer(id) else FaceComposer.itemOf(id) == item.id }
    }
    val boxes = layersOf.mapNotNull { (id, layers) ->
        layers.values.union()?.let { id to RectF(it.left - pad, it.top - pad, it.right + pad, it.bottom + pad) }
    }.toMap()
    val measured = if (unit > 0f) layersOf.mapNotNull { (id, layers) ->
        layers.values.union()?.let { id to ItemBounds((it.left - frameLeft) / unit, (it.top - frameTop) / unit, (it.right - frameLeft) / unit, (it.bottom - frameTop) / unit) }
    }.toMap() else emptyMap()
    val maxY = if (unit > 0f) (height - frameTop) / unit else 0f
    LaunchedEffect(measured, maxY) { onMeasured(measured, maxY) }
    val currentBoxes by rememberUpdatedState(boxes)
    val currentLayers by rememberUpdatedState(layersOf)
    val currentSelected by rememberUpdatedState(selected)
    val currentSingles by rememberUpdatedState(singles)
    val currentFocused by rememberUpdatedState(focused)
    val currentUnit by rememberUpdatedState(unit)
    val currentSelect by rememberUpdatedState(onSelect)
    val currentFocus by rememberUpdatedState(onFocus)
    val currentDrag by rememberUpdatedState(onDrag)
    val currentDragEnd by rememberUpdatedState(onDragEnd)
    var dragItem by remember { mutableStateOf<Int?>(null) }
    val haptics = LocalHapticFeedback.current

    fun itemAt(position: Offset): Int? = currentBoxes.entries
        .filter { it.value.contains(position.x, position.y) }
        .minByOrNull { it.value.width() * it.value.height() }?.key

    Box(
        Modifier
            .matchParentSize()
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    val hit = itemAt(position)
                    when {
                        hit == null -> currentSelect(null)
                        hit != currentSelected -> currentSelect(hit)
                        hit in currentSingles -> Unit
                        else -> {
                            val stack = currentLayers[hit].orEmpty().entries
                                .filter { distance(it.value, position) <= slack }
                                .sortedBy { it.value.width() * it.value.height() }
                                .map { it.key }
                            val index = stack.indexOf(currentFocused)
                            currentFocus(hit, if (index < 0) stack.firstOrNull() else stack.getOrNull(index + 1))
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { position ->
                        dragItem = itemAt(position)
                        dragItem?.let {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (it != currentSelected) currentSelect(it)
                        }
                    },
                    onDragEnd = {
                        if (dragItem != null) currentDragEnd()
                        dragItem = null
                    },
                    onDragCancel = {
                        if (dragItem != null) currentDragEnd()
                        dragItem = null
                    }
                ) { change, amount ->
                    val id = dragItem ?: return@detectDragGesturesAfterLongPress
                    val scale = currentUnit
                    if (scale <= 0f) return@detectDragGesturesAfterLongPress
                    change.consume()
                    currentDrag(id, amount.x / scale, amount.y / scale)
                }
            }
    )
    var resizing by remember { mutableStateOf(false) }
    boxes.forEach { (id, rect) -> ItemBox(rect, id == selected && focused == null, dragging || resizing) }
    LaunchedEffect(snap.guideX, snap.guideY) {
        if (snap.guideX != null || snap.guideY != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    if (unit > 0f) {
        val guide = OnInk.copy(alpha = GUIDE_ALPHA)
        Canvas(Modifier.matchParentSize()) {
            val stroke = GUIDE_WIDTH.toPx()
            snap.guideX?.let { x ->
                val px = frameLeft + x * unit
                drawLine(guide, Offset(px, 0f), Offset(px, size.height), stroke)
            }
            snap.guideY?.let { y ->
                val py = frameTop + y * unit
                drawLine(guide, Offset(0f, py), Offset(size.width, py), stroke)
            }
        }
    }
    val focusRect = focused?.let { extents[it] }
    if (focusRect != null) FocusRing(focusRect)
    if (focused != null) return
    val box = selected?.let { boxes[it] } ?: return
    AlignPill(
        centerX = box.centerX(),
        top = box.bottom,
        current = align,
        alpha = 1f,
        onAlign = { onAlign(selected, it) }
    )
    RemoveButton(box.right, box.top, width, 1f) { onRemove(selected) }
    if (resizable) {
        ResizeHandle(
            box = box,
            onStart = {
                resizing = true
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onResize = { onResize(selected, it) },
            onEnd = {
                resizing = false
                onDragEnd()
            }
        )
    }
}

@Composable
private fun ItemBox(rect: RectF, selected: Boolean, instant: Boolean) {
    val density = LocalDensity.current
    val spec: AnimationSpec<Float> = if (instant) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val x by animateFloatAsState(rect.left, spec, label = "itemLeft")
    val y by animateFloatAsState(rect.top, spec, label = "itemTop")
    val boxWidth by animateFloatAsState(rect.width(), spec, label = "itemWidth")
    val boxHeight by animateFloatAsState(rect.height(), spec, label = "itemHeight")
    val border by animateColorAsState(OnInk.copy(alpha = if (selected) SELECTED_BORDER else IDLE_BORDER), label = "itemBorder")
    val fill by animateColorAsState(OnInk.copy(alpha = if (selected) SELECTED_FILL else 0f), label = "itemFill")
    val shape = RoundedCornerShape(with(density) { minOf(BOX_RADIUS.toPx(), minOf(boxWidth, boxHeight) * CORNER_RATIO).toDp() })
    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(with(density) { boxWidth.toDp() }, with(density) { boxHeight.toDp() })
            .clip(shape)
            .background(fill)
            .border(BORDER_WIDTH, border, shape)
    )
}

@Composable
private fun FocusRing(rect: RectF) {
    val density = LocalDensity.current
    val shape = RoundedCornerShape(ITEM_RADIUS)
    Box(
        Modifier
            .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
            .size(with(density) { rect.width().toDp() }, with(density) { rect.height().toDp() })
            .border(BORDER_WIDTH, OnInk.copy(alpha = SELECTED_BORDER), shape)
    )
}

private fun Collection<RectF>.union(): RectF? {
    if (isEmpty()) return null
    return RectF(minOf { it.left }, minOf { it.top }, maxOf { it.right }, maxOf { it.bottom })
}

private fun distance(rect: RectF, position: Offset): Float {
    val dx = maxOf(rect.left - position.x, 0f, position.x - rect.right)
    val dy = maxOf(rect.top - position.y, 0f, position.y - rect.bottom)
    return hypot(dx, dy)
}

private val BOX_PAD = 6.dp
private val GUIDE_WIDTH = 1.dp
private const val GUIDE_ALPHA = 0.7f
private val ITEM_TOUCH = 8.dp
private val ITEM_RADIUS = 14.dp
private val BOX_RADIUS = 18.dp
private val BORDER_WIDTH = 1.5.dp
private const val SELECTED_BORDER = 0.9f
private const val IDLE_BORDER = 0.22f
private const val SELECTED_FILL = 0.06f
