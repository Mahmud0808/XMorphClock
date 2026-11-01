package com.drdisagree.xmorphclock.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.drdisagree.xmorphclock.domain.model.ClockGeometry
import kotlin.math.roundToInt

fun Modifier.clockFrame(geometry: ClockGeometry, largeProgress: () -> Float): Modifier = layout { measurable, constraints ->
    val frame = geometry.frame(largeProgress())
    val width = (constraints.maxWidth * frame.width).roundToInt().coerceAtLeast(1)
    val placeable = measurable.measure(Constraints(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(
            (constraints.maxWidth * frame.left).roundToInt(),
            (constraints.maxHeight * frame.top).roundToInt()
        )
    }
}
