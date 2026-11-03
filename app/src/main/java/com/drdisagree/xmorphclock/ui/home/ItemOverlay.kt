package com.drdisagree.xmorphclock.ui.home

import android.graphics.RectF
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.domain.editor.FaceComposer
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.ItemBounds
import com.drdisagree.xmorphclock.ui.theme.OnInk
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun BoxScope.ItemOverlay(
    extents: Map<String, RectF>,
    composition: Composition,
    selected: Int?,
    focused: String?,
    unit: Float,
    frameTop: Float,
    onSelect: (Int?) -> Unit,
    onFocus: (Int, String?) -> Unit,
    frameLeft: Float,
    height: Float,
    onMeasured: (Map<Int, ItemBounds>, Float) -> Unit,
    singles: Set<Int> = emptySet()
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
    val currentSelect by rememberUpdatedState(onSelect)
    val currentFocus by rememberUpdatedState(onFocus)

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
    )
    boxes.forEach { (id, rect) -> ItemBox(rect, id == selected && focused == null) }
    focused?.let { extents[it] }?.let { FocusRing(it) }
}

@Composable
private fun ItemBox(rect: RectF, selected: Boolean) {
    val density = LocalDensity.current
    val spec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
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
private val ITEM_TOUCH = 8.dp
private val ITEM_RADIUS = 14.dp
private val BOX_RADIUS = 18.dp
private val BORDER_WIDTH = 1.5.dp
private const val SELECTED_BORDER = 0.9f
private const val IDLE_BORDER = 0.22f
private const val SELECTED_FILL = 0.06f
private const val CORNER_RATIO = 0.25f
