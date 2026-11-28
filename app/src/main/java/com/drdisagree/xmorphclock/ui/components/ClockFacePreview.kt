package com.drdisagree.xmorphclock.ui.components

import android.graphics.RectF
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.engine.ClockFaceView
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun ClockFacePreview(
    face: ClockFace,
    modifier: Modifier = Modifier,
    largeProgress: () -> Float = { 1f },
    sleepProgress: () -> Float = { 0f },
    aodProgress: () -> Float = { 0f },
    centered: Boolean = true,
    fit: Boolean = true,
    crop: Boolean = false,
    configure: (ClockFaceView) -> Unit = {}
) {
    val data = LocalFaceData.current
    val content = remember { mutableStateOf<Rect?>(null) }
    val sized = if (fit) modifier.fitInside(centered) else modifier
    AndroidView(
        factory = { context ->
            ClockFaceView(context, face).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                if (crop) addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ -> content.track(view as ClockFaceView) }
            }
        },
        modifier = if (crop) sized.cropToContent(content) else sized,
        update = { view ->
            if (view.face != face) view.face = face
            view.data = data
            view.setLargeProgress(largeProgress())
            view.setSleepProgress(sleepProgress())
            view.setAodProgress(aodProgress())
            configure(view)
        }
    )
}

private fun MutableState<Rect?>.track(view: ClockFaceView) {
    val bounds = RectF()
    val next = if (view.contentBounds(bounds, tight = true)) Rect(bounds.left, bounds.top, bounds.right, bounds.bottom) else null
    if (value != next) value = next
}

private fun Modifier.cropToContent(content: MutableState<Rect?>): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val bounds = content.value
    if (bounds == null) {
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    } else {
        val top = floor(bounds.top).toInt().coerceIn(0, placeable.height)
        val bottom = ceil(bounds.bottom).toInt().coerceIn(top, placeable.height)
        layout(placeable.width, bottom - top) { placeable.place(0, -top) }
    }
}

private fun Modifier.fitInside(centered: Boolean): Modifier = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val natural = measurable.measure(Constraints(minWidth = width, maxWidth = width))
    val overflow = constraints.hasBoundedHeight && natural.height > constraints.maxHeight && natural.height > 0
    val placeable = if (overflow) {
        val fitted = (width * constraints.maxHeight.toFloat() / natural.height).roundToInt().coerceAtLeast(1)
        measurable.measure(Constraints.fixedWidth(fitted))
    } else {
        natural
    }
    layout(width, placeable.height.coerceAtMost(constraints.maxHeight)) {
        placeable.place(if (centered) (width - placeable.width) / 2 else 0, 0)
    }
}
