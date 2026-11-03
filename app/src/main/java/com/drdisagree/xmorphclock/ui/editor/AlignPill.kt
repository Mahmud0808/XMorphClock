package com.drdisagree.xmorphclock.ui.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.FaceAlign
import com.drdisagree.xmorphclock.ui.components.AppIcon
import kotlin.math.roundToInt

@Composable
internal fun AlignPill(centerX: Float, top: Float, width: Float, height: Float, current: FaceAlign?, visible: Boolean, onAlign: (FaceAlign) -> Unit) {
    val density = LocalDensity.current
    val pillWidth = with(density) { (ALIGN_BUTTON * FaceAlign.entries.size + ALIGN_PAD * 2).toPx() }
    val pillHeight = with(density) { (ALIGN_BUTTON + ALIGN_PAD * 2).toPx() }
    val gap = with(density) { ALIGN_GAP.toPx() }
    val margin = with(density) { ALIGN_MARGIN.toPx() }
    val x = (centerX - pillWidth / 2f).coerceAtMost(width - pillWidth - margin).coerceAtLeast(margin)
    val y = (top + gap).coerceAtMost(height - pillHeight - margin).coerceAtLeast(margin)
    val spec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val shown by animateFloatAsState(if (visible) 1f else 0f, spec, label = "alignShown")
    if (shown <= 0f) return
    Row(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .graphicsLayer {
                alpha = shown
                scaleX = ALIGN_START + (1f - ALIGN_START) * shown
                scaleY = ALIGN_START + (1f - ALIGN_START) * shown
            }
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = ALIGN_FILL))
            .padding(ALIGN_PAD)
    ) {
        FaceAlign.entries.forEach { align ->
            val tint by animateColorAsState(if (current == align) Color.White else Color.White.copy(alpha = ALIGN_IDLE), label = "alignTint")
            val fill by animateColorAsState(if (current == align) Color.White.copy(alpha = ALIGN_SELECTED) else Color.Transparent, label = "alignFill")
            Box(
                Modifier
                    .size(ALIGN_BUTTON)
                    .clip(CircleShape)
                    .background(fill)
                    .clickable(enabled = visible, role = Role.RadioButton, onClickLabel = stringResource(alignLabel(align))) { onAlign(align) },
                contentAlignment = Alignment.Center
            ) {
                AppIcon(alignIcon(align), tint = tint, size = ALIGN_ICON)
            }
        }
    }
}

private fun alignIcon(align: FaceAlign): Int = when (align) {
    FaceAlign.START -> R.drawable.ic_align_start
    FaceAlign.CENTER -> R.drawable.ic_align_center
    FaceAlign.END -> R.drawable.ic_align_end
}

private fun alignLabel(align: FaceAlign): Int = when (align) {
    FaceAlign.START -> R.string.align_start
    FaceAlign.CENTER -> R.string.align_center
    FaceAlign.END -> R.string.align_end
}

private val ALIGN_BUTTON = 26.dp
private val ALIGN_ICON = 14.dp
private val ALIGN_PAD = 2.dp
private val ALIGN_GAP = 10.dp
private val ALIGN_MARGIN = 6.dp
private const val ALIGN_FILL = 0.45f
private const val ALIGN_IDLE = 0.6f
private const val ALIGN_SELECTED = 0.22f
private const val ALIGN_START = 0.6f
