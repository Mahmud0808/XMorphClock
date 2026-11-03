package com.drdisagree.xmorphclock.ui.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.engine.ClockFaceView
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
    configure: (ClockFaceView) -> Unit = {}
) {
    val data = LocalFaceData.current
    AndroidView(
        factory = { context -> ClockFaceView(context, face).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO } },
        modifier = if (fit) modifier.fitInside(centered) else modifier,
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
