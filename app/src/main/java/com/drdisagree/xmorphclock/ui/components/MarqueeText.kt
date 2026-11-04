package com.drdisagree.xmorphclock.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MarqueeText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var boxWidth by remember { mutableIntStateOf(0) }
    var textWidth by remember { mutableIntStateOf(0) }
    val overflowing = boxWidth in 1 until textWidth
    val gap = with(density) { GAP.toPx() }
    val edge = with(density) { EDGE.toPx() }
    val speed = with(density) { VELOCITY.toPx() }
    val distance = textWidth + gap
    val offset = remember { Animatable(0f) }
    val endFade by animateFloatAsState(if (overflowing) 1f else 0f, tween(FADE_MS), label = "marqueeEnd")
    LaunchedEffect(overflowing, textWidth, gap) {
        offset.snapTo(0f)
        if (!overflowing) return@LaunchedEffect
        delay(INITIAL_DELAY_MS)
        while (true) {
            offset.animateTo(distance, tween((distance / speed * MILLIS).toInt(), easing = LinearEasing))
            offset.snapTo(0f)
            delay(REPEAT_DELAY_MS)
        }
    }
    Box(
        modifier
            .onSizeChanged { boxWidth = it.width }
            .clipToBounds()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val length = edge.coerceAtMost(size.width / 2f)
                val travelled = offset.value
                val startFade = if (overflowing) (minOf(travelled, distance - travelled) / length).coerceIn(0f, 1f) else 0f
                val leading = if (rtl) endFade else startFade
                val trailing = if (rtl) startFade else endFade
                if (leading > 0f) {
                    drawRect(
                        Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 1f - leading), Color.Black), 0f, length),
                        size = Size(length, size.height),
                        blendMode = BlendMode.DstIn
                    )
                }
                if (trailing > 0f) {
                    drawRect(
                        Brush.horizontalGradient(listOf(Color.Black, Color.Black.copy(alpha = 1f - trailing)), size.width - length, size.width),
                        topLeft = Offset(size.width - length, 0f),
                        size = Size(length, size.height),
                        blendMode = BlendMode.DstIn
                    )
                }
            },
        contentAlignment = if (overflowing) Alignment.CenterStart else Alignment.Center
    ) {
        Row(
            Modifier
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .graphicsLayer { translationX = if (rtl) offset.value else -offset.value }
        ) {
            Text(
                text,
                style = style,
                color = color,
                maxLines = 1,
                softWrap = false,
                onTextLayout = { textWidth = it.size.width }
            )
            if (overflowing) {
                Spacer(Modifier.width(GAP))
                Text(text, style = style, color = color, maxLines = 1, softWrap = false)
            }
        }
    }
}

private const val FADE_MS = 240
private const val INITIAL_DELAY_MS = 1_200L
private const val REPEAT_DELAY_MS = 1_800L
private const val MILLIS = 1_000f
private val EDGE = 10.dp
private val GAP = 28.dp
private val VELOCITY = 26.dp
