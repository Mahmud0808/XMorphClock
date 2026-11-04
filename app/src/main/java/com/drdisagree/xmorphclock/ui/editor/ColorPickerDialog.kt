package com.drdisagree.xmorphclock.ui.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.FaceGradient
import com.drdisagree.xmorphclock.ui.components.DialogButton
import com.drdisagree.xmorphclock.ui.components.GlassDialogFrame
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val FieldShape = RoundedCornerShape(20.dp)
private val BarShape = RoundedCornerShape(50)
private val HueStops = List(7) { Color.hsv(it * 60f % 360f, 1f, 1f) }

@Composable
fun ColorPickerDialog(visible: Boolean, initial: String, resolve: (String) -> Int, onApply: (String) -> Unit, onDismiss: () -> Unit, allowGradient: Boolean = true) {
    var gradient by remember { mutableStateOf(false) }
    var stops by remember { mutableStateOf(listOf(Hsv(0f, 0f, 1f), Hsv(0f, 0f, 1f))) }
    var active by remember { mutableIntStateOf(0) }
    var angle by remember { mutableFloatStateOf(DEFAULT_ANGLE) }
    LaunchedEffect(visible, initial) {
        if (!visible) return@LaunchedEffect
        val parsed = FaceGradient.parse(initial)
        gradient = allowGradient && parsed != null
        active = 0
        angle = parsed?.angle ?: DEFAULT_ANGLE
        stops = parsed?.stops?.take(2)?.map { Hsv.of(resolve(it)) } ?: Hsv.of(resolve(initial)).let { start ->
            listOf(start, start.copy(hue = (start.hue + PAIR_HUE_SHIFT) % 360f, saturation = start.saturation.coerceAtLeast(PAIR_SATURATION)))
        }
    }
    val current = stops[if (gradient) active else 0]
    fun update(hsv: Hsv) {
        stops = stops.toMutableList().also { it[if (gradient) active else 0] = hsv }
    }
    val colors = stops.map { Color(it.argb) }
    GlassDialogFrame(visible, onDismiss) {
        Text(
            stringResource(R.string.color_custom),
            style = MaterialTheme.typography.titleLarge,
            color = OnInk,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 12.dp)
        )
        if (allowGradient) Row(
            Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextTile(stringResource(R.string.color_solid), !gradient, { gradient = false })
            TextTile(stringResource(R.string.color_gradient), gradient, { gradient = true })
        }
        SaturationValueField(
            hue = current.hue,
            saturation = current.saturation,
            value = current.value,
            onChange = { s, v -> update(current.copy(saturation = s, value = v)) },
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(FIELD_HEIGHT)
        )
        HueBar(
            hue = current.hue,
            onChange = { update(current.copy(hue = it)) },
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 16.dp)
                .fillMaxWidth()
                .height(BAR_HEIGHT)
        )
        AlphaBar(
            color = Color(current.copy(alpha = 1f).argb),
            alpha = current.alpha,
            onChange = { update(current.copy(alpha = it)) },
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 12.dp)
                .fillMaxWidth()
                .height(BAR_HEIGHT)
        )
        AnimatedContent(
            targetState = gradient,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
            label = "pickerMode"
        ) { isGradient ->
            Row(
                Modifier
                    .padding(start = 28.dp, end = 28.dp, top = 16.dp)
                    .fillMaxWidth()
                    .height(STOP_SIZE),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isGradient) {
                    colors.forEachIndexed { index, color ->
                        StopDot(color, index == active, { active = index })
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(BarShape)
                            .drawBehind {
                                checker(CHECKER_CELL.toPx())
                                drawRect(angledBrush(colors, angle, size.width, size.height))
                            }
                    )
                } else {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .drawBehind {
                                checker(CHECKER_CELL.toPx())
                                drawCircle(colors.first())
                            }
                    )
                    Text(hexOf(stops.first()), style = MaterialTheme.typography.titleMedium, color = OnInkMuted)
                }
            }
        }
        AnimatedVisibility(
            visible = gradient,
            enter = fadeIn(tween(240)) + expandVertically(tween(320)),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(280))
        ) {
            Column(Modifier.padding(top = 12.dp)) {
                Row(Modifier.padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.gradient_angle),
                        style = MaterialTheme.typography.labelLarge,
                        color = OnInkMuted,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${angle.roundToInt()}°", style = MaterialTheme.typography.labelLarge, color = OnInk)
                }
                RangeSlider(angle, 0f..FULL_ANGLE, { angle = it })
            }
        }
        Row(
            Modifier
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DialogButton(stringResource(R.string.action_cancel), primary = false, onClick = onDismiss, modifier = Modifier.weight(1f))
            DialogButton(
                stringResource(R.string.action_apply),
                primary = true,
                onClick = {
                    onApply(if (gradient) FaceGradient(angle, stops.map(::hexOf)).encode() else hexOf(stops.first()))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StopDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    val ring by animateColorAsState(if (selected) Color.White else Color.Transparent, tween(200), label = "stopRing")
    Box(
        Modifier
            .size(STOP_SIZE)
            .clip(CircleShape)
            .drawBehind {
                checker(CHECKER_CELL.toPx())
                drawCircle(color, size.minDimension / 2f - THUMB_STROKE.toPx())
                drawCircle(ring, size.minDimension / 2f - THUMB_STROKE.toPx() / 2f, style = Stroke(THUMB_STROKE.toPx()))
            }
            .clickable(role = Role.RadioButton, onClick = onClick)
    )
}

private fun angledBrush(colors: List<Color>, angle: Float, width: Float, height: Float): Brush {
    val radians = Math.toRadians(angle.toDouble())
    val dx = cos(radians).toFloat()
    val dy = sin(radians).toFloat()
    val half = (abs(width * dx) + abs(height * dy)) / 2f
    val center = Offset(width / 2f, height / 2f)
    return Brush.linearGradient(colors, center - Offset(dx * half, dy * half), center + Offset(dx * half, dy * half))
}

private fun hexOf(hsv: Hsv): String = String.format(Locale.ROOT, "#%08X", hsv.argb)

private fun DrawScope.checker(cell: Float) {
    drawRect(CheckerLight)
    var row = 0
    var y = 0f
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else cell
        while (x < size.width) {
            drawRect(CheckerDark, Offset(x, y), Size(cell, cell))
            x += cell * 2f
        }
        y += cell
        row++
    }
}

@Composable
private fun AlphaBar(color: Color, alpha: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    fun update(x: Float, width: Int) {
        onChange((x / width.coerceAtLeast(1)).coerceIn(0f, 1f))
    }
    Box(
        modifier
            .clip(BarShape)
            .pointerInput(Unit) {
                detectTapGestures { update(it.x, size.width) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    update(change.position.x, size.width)
                }
            }
            .drawBehind {
                checker(CHECKER_CELL.toPx())
                drawRect(Brush.horizontalGradient(listOf(color.copy(alpha = 0f), color)))
                val radius = size.height / 2f
                val x = (alpha * size.width).coerceIn(radius, size.width - radius)
                drawCircle(color.copy(alpha = alpha), radius - THUMB_STROKE.toPx(), Offset(x, radius))
                drawCircle(Color.White, radius - THUMB_STROKE.toPx() / 2f, Offset(x, radius), style = Stroke(THUMB_STROKE.toPx()))
            }
    )
}

@Composable
private fun SaturationValueField(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    fun update(position: Offset, width: Int, height: Int) {
        onChange(
            (position.x / width.coerceAtLeast(1)).coerceIn(0f, 1f),
            1f - (position.y / height.coerceAtLeast(1)).coerceIn(0f, 1f)
        )
    }
    Box(
        modifier
            .clip(FieldShape)
            .pointerInput(Unit) {
                detectTapGestures { update(it, size.width, size.height) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    update(change.position, size.width, size.height)
                }
            }
            .drawBehind {
                drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val center = Offset(saturation * size.width, (1f - value) * size.height)
                drawCircle(Color.hsv(hue, saturation, value), THUMB_RADIUS.toPx(), center)
                drawCircle(Color.White, THUMB_RADIUS.toPx(), center, style = Stroke(THUMB_STROKE.toPx()))
            }
    )
}

@Composable
private fun HueBar(hue: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    fun update(x: Float, width: Int) {
        onChange((x / width.coerceAtLeast(1)).coerceIn(0f, 1f) * 360f % 360f)
    }
    Box(
        modifier
            .clip(BarShape)
            .pointerInput(Unit) {
                detectTapGestures { update(it.x, size.width) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    update(change.position.x, size.width)
                }
            }
            .drawBehind {
                drawRoundRect(Brush.horizontalGradient(HueStops), cornerRadius = CornerRadius(size.height / 2f))
                val radius = size.height / 2f
                val x = (hue / 360f * size.width).coerceIn(radius, size.width - radius)
                drawCircle(Color.hsv(hue, 1f, 1f), radius - THUMB_STROKE.toPx(), Offset(x, radius))
                drawCircle(Color.White, radius - THUMB_STROKE.toPx() / 2f, Offset(x, radius), style = Stroke(THUMB_STROKE.toPx()))
            }
    )
}

private val FIELD_HEIGHT = 200.dp
private val STOP_SIZE = 36.dp
private const val DEFAULT_ANGLE = 0f
private const val FULL_ANGLE = 360f
private const val PAIR_HUE_SHIFT = 40f
private const val PAIR_SATURATION = 0.5f
private val BAR_HEIGHT = 28.dp
private val THUMB_RADIUS = 11.dp
private val THUMB_STROKE = 3.dp
private val CHECKER_CELL = 5.dp
private val CheckerLight = Color(0xFF8A8A8A)
private val CheckerDark = Color(0xFF5E5E5E)
