package com.drdisagree.xmorphclock.ui.home

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
internal fun ResizeHandle(box: RectF, onStart: () -> Unit, onResize: (Float) -> Unit, onEnd: () -> Unit) {
    val density = LocalDensity.current
    val touch = with(density) { HANDLE_TOUCH.toPx() }
    val currentBox by rememberUpdatedState(box)
    val currentResize by rememberUpdatedState(onResize)
    val currentStart by rememberUpdatedState(onStart)
    val currentEnd by rememberUpdatedState(onEnd)
    Canvas(
        Modifier
            .offset { IntOffset((box.right - touch / 2f).roundToInt(), (box.bottom - touch / 2f).roundToInt()) }
            .size(HANDLE_TOUCH)
            .pointerInput(Unit) {
                var center = Offset.Zero
                var origin = Offset.Zero
                var start = 1f
                var pointer = Offset.Zero
                detectDragGestures(
                    onDragStart = { position ->
                        val rect = currentBox
                        center = Offset(rect.centerX(), rect.centerY())
                        origin = Offset(rect.right - touch / 2f, rect.bottom - touch / 2f)
                        pointer = origin + position
                        start = hypot(pointer.x - center.x, pointer.y - center.y).coerceAtLeast(1f)
                        currentStart()
                    },
                    onDragEnd = { currentEnd() },
                    onDragCancel = { currentEnd() }
                ) { change, amount ->
                    change.consume()
                    pointer += amount
                    currentResize(hypot(pointer.x - center.x, pointer.y - center.y) / start)
                }
            }
    ) {
        val side = minOf(box.width(), box.height())
        val corner = minOf(CORNER.toPx(), side * CORNER_RATIO)
        val reach = minOf(REACH.toPx(), side * REACH_RATIO)
        val stroke = (STROKE.toPx() * (side / FULL_SIDE.toPx())).coerceIn(MIN_STROKE.toPx(), STROKE.toPx())
        val right = size.width / 2f
        val bottom = size.height / 2f
        val handle = Path().apply {
            moveTo(right, bottom - corner - reach)
            lineTo(right, bottom - corner)
            arcTo(Rect(right - corner * 2f, bottom - corner * 2f, right, bottom), 0f, 90f, false)
            lineTo(right - corner - reach, bottom)
        }
        drawPath(handle, Color.White, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private val HANDLE_TOUCH = 44.dp
private val CORNER = 18.dp
private val REACH = 10.dp
private val STROKE = 3.dp
private val MIN_STROKE = 2.dp
private val FULL_SIDE = 120.dp
internal const val CORNER_RATIO = 0.25f
private const val REACH_RATIO = 0.12f
