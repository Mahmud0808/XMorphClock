package com.drdisagree.xmorphclock.ui.home

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

@Composable
internal fun ModeSlots(
    row: () -> Float,
    selected: () -> Float,
    modifier: Modifier = Modifier,
    indicatorOnTop: Boolean = false,
    indicator: (@Composable () -> Unit)? = null,
    label: @Composable (PreviewMode) -> Unit
) {
    Layout(
        modifier = modifier,
        content = {
            indicator?.invoke()
            PreviewMode.entries.forEach { label(it) }
        }
    ) { measurables, _ ->
        val slotWidth = SLOT_WIDTH.roundToPx()
        val slotHeight = SLOT_HEIGHT.roundToPx()
        val inset = SLOT_INSET.roundToPx()
        val count = PreviewMode.entries.size
        val t = row().coerceIn(0f, 1f)
        val slot = Constraints.fixed(slotWidth, slotHeight)
        val placeables = measurables.map { it.measure(slot) }
        val pill = if (indicator != null) placeables.first() else null
        val labels = if (indicator != null) placeables.drop(1) else placeables
        fun x(index: Float): Int = inset + lerp(0f, index * slotWidth, t).roundToInt()
        fun y(index: Float): Int = inset + lerp(index * slotHeight, 0f, t).roundToInt()
        val width = lerp(slotWidth.toFloat(), (slotWidth * count).toFloat(), t).roundToInt() + inset * 2
        val height = lerp((slotHeight * count).toFloat(), slotHeight.toFloat(), t).roundToInt() + inset * 2
        layout(width, height) {
            val index = selected()
            if (!indicatorOnTop) pill?.place(x(index), y(index))
            labels.forEachIndexed { position, item -> item.place(x(position.toFloat()), y(position.toFloat())) }
            if (indicatorOnTop) pill?.place(x(index), y(index))
        }
    }
}

internal fun Density.slotCenter(index: Float, row: Float): Offset {
    val t = row.coerceIn(0f, 1f)
    val slotWidth = SLOT_WIDTH.toPx()
    val slotHeight = SLOT_HEIGHT.toPx()
    val inset = SLOT_INSET.toPx()
    return Offset(inset + lerp(0f, index * slotWidth, t) + slotWidth / 2f, inset + lerp(index * slotHeight, 0f, t) + slotHeight / 2f)
}

internal val SLOT_WIDTH = 76.dp
internal val SLOT_HEIGHT = 40.dp
internal val SLOT_INSET = 4.dp
internal const val MODE_DISABLED_ALPHA = 0.38f
internal val ModeContainerShape = RoundedCornerShape(SLOT_HEIGHT / 2 + SLOT_INSET)
