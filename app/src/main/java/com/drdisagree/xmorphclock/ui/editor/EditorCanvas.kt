package com.drdisagree.xmorphclock.ui.editor

import android.graphics.RectF
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.drdisagree.xmorphclock.domain.editor.ItemGroups
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.engine.ClockFaceView
import com.drdisagree.xmorphclock.engine.FaceFont
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.LocalClockGeometry
import com.drdisagree.xmorphclock.ui.components.LocalDepth
import com.drdisagree.xmorphclock.ui.components.LocalFaceData
import com.drdisagree.xmorphclock.ui.components.LocalWallpaper
import com.drdisagree.xmorphclock.ui.components.backdropFrame
import com.drdisagree.xmorphclock.ui.components.clockFrame
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.DarkPalette
import com.drdisagree.xmorphclock.ui.theme.Hairline
import com.drdisagree.xmorphclock.ui.theme.Ink
import com.drdisagree.xmorphclock.ui.theme.LocalPalette
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun EditorCanvas(
    face: ClockFace,
    selectedId: String?,
    large: Float,
    sleep: Float,
    playToken: Int,
    onSelect: (String?) -> Unit,
    onMove: (String, Float, Float) -> Unit,
    onResizeStart: (String) -> Pair<Float, Float>,
    onResize: (String, Float, Float) -> Unit,
    onTransformStart: (String) -> Pair<Float, Float>,
    onTransform: (String, Float, Float, Float, Float) -> Unit,
    onGestureEnd: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val layer = rememberLayerBackdrop()
    val wallpaper = LocalWallpaper.current
    val depth = LocalDepth.current
    val geometry = LocalClockGeometry.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val accent = Accent
    val shape = RoundedCornerShape(28.dp)
    var view by remember { mutableStateOf<ClockFaceView?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var guide by remember { mutableStateOf(false) }
    val currentSelected by rememberUpdatedState(selectedId)
    val currentSelect by rememberUpdatedState(onSelect)
    val currentMove by rememberUpdatedState(onMove)
    val currentResizeStart by rememberUpdatedState(onResizeStart)
    val currentResize by rememberUpdatedState(onResize)
    val currentResizable by rememberUpdatedState<(String) -> Boolean>({ id ->
        if (ItemGroups.isGroup(id)) return@rememberUpdatedState false
        face.layers.firstOrNull { it.id == id }?.let { layer ->
            !layer.isIcon && !layer.isShape && !layer.font.startsWith(SYMBOL_FONT) && FaceFont.hasWidthAxis(layer.font)
        } == true
    })
    val currentTransformStart by rememberUpdatedState(onTransformStart)
    val currentTransform by rememberUpdatedState(onTransform)
    val currentEnd by rememberUpdatedState(onGestureEnd)

    LaunchedEffect(face, selectedId, large, sleep) {
        repeat(3) {
            withFrameNanos { }
            tick++
        }
    }
    LaunchedEffect(playToken) {
        if (playToken > 0) view?.playChange()
    }

    BoxWithConstraints(
        modifier
            .clip(shape)
            .background(Ink)
            .border(1.dp, Hairline, shape)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val frame = geometry.frame(large)
        val insetPx = widthPx * frame.left
        val topPx = heightPx * frame.top
        val snapPx = with(density) { SNAP.toPx() }
        val handlePx = with(density) { HANDLE_TOUCH.toPx() }

        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(layer)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF1C2333), Ink)))
            )
            if (wallpaper != null) {
                Image(wallpaper, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = sleep))
            )
        }

        val data = LocalFaceData.current
        AndroidView(
            factory = { context -> ClockFaceView(context, face).also { view = it } },
            modifier = Modifier.clockFrame(geometry) { large },
            update = { clock ->
                if (clock.face != face) clock.editFace(face)
                clock.data = data
                clock.setLargeProgress(large)
                clock.setSleepProgress(sleep)
                clock.setAodProgress(sleep)
                val bitmap = wallpaper?.asAndroidBitmap()
                if (clock.tag !== bitmap) {
                    clock.tag = bitmap
                    clock.setBackdrop(bitmap)
                }
                clock.setDepth(depth?.bitmap, depth?.opacity ?: 0f)
                if (bitmap != null) {
                    clock.setBackdropFrame(backdropFrame(bitmap, widthPx, heightPx, geometry, large))
                }
            }
        )

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(insetPx, topPx, handlePx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val rects = rectsOf(view, insetPx, topPx)
                        val selectedRect = rectOf(rects, currentSelected)
                        var mode: Int
                        var id: String?
                        var start = RectF()
                        var axes = 0f to 0f
                        if (selectedRect != null &&
                            hypot(down.position.x - selectedRect.right, down.position.y - selectedRect.bottom) < handlePx
                        ) {
                            mode = if (currentResizable(currentSelected!!)) MODE_RESIZE else MODE_SCALE
                            id = currentSelected
                            start = RectF(selectedRect)
                            axes = if (mode == MODE_RESIZE) currentResizeStart(currentSelected!!) else currentTransformStart(currentSelected!!)
                        } else {
                            val hit = dragTarget(rects, down.position, currentSelected)
                            mode = if (hit != null) MODE_MOVE else MODE_NONE
                            id = hit ?: currentSelected
                            rectOf(rects, id)?.let { start = RectF(it) }
                        }
                        var total = Offset.Zero
                        var applied = Offset.Zero
                        var dragging = false
                        var transforming = false
                        var moved = false
                        var armed = mode == MODE_RESIZE || mode == MODE_SCALE
                        var base = 0f to 0f
                        var zoom = 1f
                        var turn = 0f
                        var snappedTurn = false
                        val slop = viewConfiguration.touchSlop
                        val longPress = viewConfiguration.longPressTimeoutMillis
                        while (true) {
                            val event = if (!armed && mode == MODE_MOVE && !moved && !transforming) {
                                val remaining = longPress - (SystemClock.uptimeMillis() - down.uptimeMillis)
                                withTimeoutOrNull(remaining.coerceAtLeast(0L)) { awaitPointerEvent() }
                            } else {
                                awaitPointerEvent()
                            }
                            if (event == null) {
                                val target = id ?: continue
                                armed = true
                                dragging = true
                                total = Offset.Zero
                                applied = Offset.Zero
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (target != currentSelected) currentSelect(target)
                                continue
                            }
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            val target = id ?: continue
                            val clockWidth = view?.width?.toFloat()?.takeIf { it > 0f } ?: continue
                            if (pressed.size >= 2) {
                                if (!transforming) {
                                    transforming = true
                                    guide = false
                                    if (target != currentSelected) currentSelect(target)
                                    base = currentTransformStart(target)
                                    zoom = 1f
                                    turn = 0f
                                }
                                zoom *= event.calculateZoom()
                                turn += event.calculateRotation()
                                var rotation = base.second + turn
                                val snap = abs(rotation) < ROTATION_SNAP
                                if (snap) rotation = 0f
                                if (snap != snappedTurn) {
                                    snappedTurn = snap
                                    if (snap) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                val pan = event.calculatePan()
                                currentTransform(target, base.first * zoom, rotation, pan.x / clockWidth, pan.y / clockWidth)
                                event.changes.forEach { it.consume() }
                                continue
                            }
                            if (transforming) continue
                            val change = pressed.first()
                            total += change.positionChange()
                            if (!armed) {
                                if (total.getDistance() > slop) moved = true
                                continue
                            }
                            change.consume()
                            if (!dragging && total.getDistance() > slop) dragging = true
                            if (!dragging) continue
                            when (mode) {
                                MODE_MOVE -> {
                                    val center = start.centerX() - insetPx
                                    var dx = total.x
                                    val snapped = abs(center + dx - clockWidth / 2f) < snapPx
                                    if (snapped) dx = clockWidth / 2f - center
                                    if (snapped != guide) {
                                        guide = snapped
                                        if (snapped) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    val next = Offset(dx, total.y)
                                    val step = next - applied
                                    applied = next
                                    if (step != Offset.Zero) currentMove(target, step.x / clockWidth, step.y / clockWidth)
                                }

                                MODE_RESIZE -> {
                                    val widthFactor = ((start.width() + total.x) / start.width()).coerceAtLeast(MIN_FACTOR)
                                    val heightFactor = ((start.height() + total.y) / start.height()).coerceAtLeast(MIN_FACTOR)
                                    currentResize(target, axes.first * widthFactor, axes.second * heightFactor)
                                }

                                MODE_SCALE -> {
                                    val widthFactor = (start.width() + total.x) / start.width()
                                    val heightFactor = (start.height() + total.y) / start.height()
                                    val factor = ((widthFactor + heightFactor) / 2f).coerceAtLeast(MIN_FACTOR)
                                    currentTransform(target, axes.first * factor, axes.second, 0f, 0f)
                                }
                            }
                        }
                        guide = false
                        when {
                            dragging || transforming -> currentEnd()
                            !moved && !armed -> currentSelect(cycleTarget(rectsOf(view, insetPx, topPx), down.position, currentSelected))
                        }
                    }
                }
        ) {
            tick
            val rect = rectOf(rectsOf(view, insetPx, topPx), currentSelected)
            if (rect != null) {
                val pad = 6.dp.toPx()
                val radius = FRAME_RADIUS.toPx()
                val left = rect.left - pad
                val top = rect.top - pad
                val right = rect.right + pad
                val bottom = rect.bottom + pad
                val corner = radius.coerceAtMost(minOf(right - left, bottom - top) / 2f)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    cornerRadius = CornerRadius(corner),
                    style = Stroke(1.5.dp.toPx())
                )
                val reach = HANDLE_REACH.toPx().coerceAtMost(minOf(right - left, bottom - top) / 2f - corner).coerceAtLeast(0f)
                val handle = Path().apply {
                    moveTo(right, bottom - corner - reach)
                    lineTo(right, bottom - corner)
                    arcTo(Rect(right - corner * 2f, bottom - corner * 2f, right, bottom), 0f, 90f, false)
                    lineTo(right - corner - reach, bottom)
                }
                drawPath(
                    handle,
                    Color.White,
                    style = Stroke(HANDLE_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            if (guide) {
                val x = insetPx + (view?.width ?: 0) / 2f
                drawLine(accent, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            }
        }
        CompositionLocalProvider(LocalBackdrop provides layer, LocalPalette provides DarkPalette) {
            overlay()
        }
    }
}

private fun rectsOf(view: ClockFaceView?, dx: Float, dy: Float): Map<String, RectF> =
    view?.layerRects().orEmpty().mapValues { RectF(it.value).apply { offset(dx, dy) } }

private fun hitsAt(rects: Map<String, RectF>, position: Offset): List<String> =
    rects.entries.reversed()
        .filter { (_, rect) -> rect.reaches(position, HIT_SLOP) }
        .sortedBy { (_, rect) -> if (rect.reaches(position, 0f)) 0 else 1 }
        .map { it.key }

private fun RectF.reaches(position: Offset, slop: Float): Boolean =
    position.x >= left - slop && position.x <= right + slop && position.y >= top - slop && position.y <= bottom + slop

private fun cycleTarget(rects: Map<String, RectF>, position: Offset, selected: String?): String? {
    val hits = hitsAt(rects, position).sortedBy { id -> rects.getValue(id).let { it.width() * it.height() } }
    val first = hits.firstOrNull() ?: return null
    val key = ItemGroups.keyOf(first)
    val members = hits.filter { ItemGroups.belongs(key, it) }
    return when {
        selected == null -> key
        ItemGroups.isGroup(selected) -> if (selected == key) members.first() else key
        !ItemGroups.belongs(key, selected) -> key
        else -> members.getOrNull(members.indexOf(selected) + 1) ?: key
    }
}

private fun rectOf(rects: Map<String, RectF>, id: String?): RectF? {
    id ?: return null
    if (!ItemGroups.isGroup(id)) return rects[id]
    val members = rects.filterKeys { ItemGroups.belongs(id, it) }.values
    if (members.isEmpty()) return null
    return RectF(members.minOf { it.left }, members.minOf { it.top }, members.maxOf { it.right }, members.maxOf { it.bottom })
}

private fun dragTarget(rects: Map<String, RectF>, position: Offset, selected: String?): String? {
    val hits = hitsAt(rects, position)
    if (selected != null && selected in hits) return selected
    if (ItemGroups.isGroup(selected) && rectOf(rects, selected)?.reaches(position, HIT_SLOP) == true) return selected
    val first = hits.minByOrNull { id -> rects.getValue(id).let { it.width() * it.height() } } ?: return null
    return if (selected != null && !ItemGroups.isGroup(selected) && ItemGroups.belongs(ItemGroups.keyOf(first), selected)) first else ItemGroups.keyOf(first)
}

private const val MODE_NONE = 0
private const val HIT_SLOP = 12f
private const val MODE_MOVE = 1
private const val MODE_RESIZE = 2
private const val MODE_SCALE = 3
private const val MIN_FACTOR = 0.15f
private val SNAP = 6.dp
private const val ROTATION_SNAP = 3f
private val HANDLE_TOUCH = 28.dp
private val FRAME_RADIUS = 16.dp
private val HANDLE_REACH = 3.dp
private val HANDLE_WIDTH = 5.dp

private const val SYMBOL_FONT = "res:symbols"
