package com.drdisagree.xmorphclock.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.drdisagree.xmorphclock.ui.components.AppIcon
import kotlin.math.roundToInt

@Composable
internal fun RemoveButton(x: Float, y: Float, width: Float, alpha: Float, beside: Float? = null, onClick: () -> Unit) {
    val density = LocalDensity.current
    val size = with(density) { REMOVE_SIZE.toPx() }
    val margin = with(density) { REMOVE_MARGIN.toPx() }
    val spec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val gap = with(density) { REMOVE_GAP.toPx() }
    val targetX = (if (beside != null) x + gap else x - size * REMOVE_INSET).coerceAtMost(width - size - margin).coerceAtLeast(margin)
    val targetY = (if (beside != null) beside - size / 2f else y - size * REMOVE_OUTSET).coerceAtLeast(margin)
    val left by animateFloatAsState(targetX, spec, label = "removeX")
    val top by animateFloatAsState(targetY, spec, label = "removeY")
    Box(
        Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(REMOVE_SIZE)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(REMOVE_RADIUS))
            .background(RemoveFill)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.action_remove), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppIcon(R.drawable.ic_minus, tint = RemoveInk, size = REMOVE_ICON)
    }
}

private val REMOVE_SIZE = 22.dp
private val REMOVE_ICON = 14.dp
private val REMOVE_RADIUS = 7.dp
private val RemoveFill = Color(0xFFE9E9EB)
private val RemoveInk = Color(0xFF1C1C1E)
private val REMOVE_MARGIN = 6.dp
private val REMOVE_GAP = 6.dp
private const val REMOVE_INSET = 0.8f
private const val REMOVE_OUTSET = 0.2f
