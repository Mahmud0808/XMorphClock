package com.drdisagree.xmorphclock.ui.home

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.FaceAlign
import com.drdisagree.xmorphclock.ui.components.AppIcon
import kotlin.math.roundToInt

@Composable
internal fun AlignPill(centerX: Float, top: Float, current: FaceAlign?, alpha: Float, onAlign: (FaceAlign) -> Unit) {
    val density = LocalDensity.current
    val width = with(density) { (ALIGN_BUTTON * FaceAlign.entries.size + ALIGN_PAD * 2).toPx() }
    val gap = with(density) { ALIGN_GAP.toPx() }
    val spec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val x by animateFloatAsState(centerX - width / 2f, spec, label = "alignX")
    val y by animateFloatAsState(top + gap, spec, label = "alignY")
    Row(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .graphicsLayer { this.alpha = alpha }
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
                    .clickable(role = Role.RadioButton) { onAlign(align) },
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

private val ALIGN_BUTTON = 26.dp
private val ALIGN_ICON = 14.dp
private val ALIGN_PAD = 2.dp
private val ALIGN_GAP = 10.dp
private const val ALIGN_FILL = 0.45f
private const val ALIGN_IDLE = 0.6f
private const val ALIGN_SELECTED = 0.22f
