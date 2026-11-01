package com.drdisagree.xmorphclock.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.drdisagree.xmorphclock.ui.components.liquid.InteractiveHighlight
import com.drdisagree.xmorphclock.ui.theme.FlatSurface
import com.drdisagree.xmorphclock.ui.theme.LocalPalette
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun GlassMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    top: Dp,
    end: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = LocalPalette.current.isDark
    val container = if (dark) Color(0xFF121212).copy(alpha = 0.45f) else Color(0xFFFAFAFA).copy(alpha = 0.7f)
    val flat = FlatSurface
    val animationScope = rememberCoroutineScope()
    val highlight = remember(animationScope) { InteractiveHighlight(animationScope) }
    BackHandler(enabled = visible, onBack = onDismiss)
    if (visible) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = null, indication = null, onClick = onDismiss)
        )
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = top, end = end), contentAlignment = Alignment.TopEnd) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)) + scaleIn(tween(320, easing = Emphasized), initialScale = 0.86f, transformOrigin = TransformOrigin(1f, 0f)),
            exit = fadeOut(tween(140)) + scaleOut(tween(200), targetScale = 0.92f, transformOrigin = TransformOrigin(1f, 0f))
        ) {
            Column(
                Modifier
                    .width(MENU_WIDTH)
                    .drawBackdrop(
                        backdrop = LocalBackdrop.current,
                        shape = { MenuShape },
                        effects = {
                            if (GlassSupport.full) {
                                colorControls(brightness = if (dark) 0f else 0.2f, saturation = 1.5f)
                                blur(if (dark) 8.dp.toPx() else 16.dp.toPx())
                                lens(16.dp.toPx(), 32.dp.toPx(), depthEffect = true)
                            }
                        },
                        highlight = { Highlight.Default.copy(alpha = RIM_ALPHA) },
                        layerBlock = {
                            val progress = highlight.pressProgress
                            val scale = lerp(1f, 1f + PRESS_GROW.toPx() / size.height, progress)
                            val maxOffset = size.minDimension
                            val offset = highlight.offset
                            translationX = maxOffset * tanh(DRAG_FOLLOW * offset.x / maxOffset)
                            translationY = maxOffset * tanh(DRAG_FOLLOW * offset.y / maxOffset)
                            val stretch = PRESS_GROW.toPx() / size.minDimension
                            val angle = atan2(offset.y, offset.x)
                            scaleX = scale + stretch * abs(cos(angle) * offset.x / size.maxDimension)
                            scaleY = scale + stretch * abs(sin(angle) * offset.y / size.maxDimension)
                        },
                        onDrawSurface = {
                            if (!GlassSupport.full) drawRect(flat)
                            drawRect(container)
                        }
                    )
                    .clickable(interactionSource = null, indication = null) {}
                    .then(highlight.modifier)
                    .then(highlight.gestureModifier)
                    .padding(vertical = 8.dp),
                content = content
            )
        }
    }
}

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val MenuShape = RoundedCornerShape(28.dp)
private val MENU_WIDTH = 232.dp
private const val RIM_ALPHA = 0.5f
private const val DRAG_FOLLOW = 0.05f
private val PRESS_GROW = 6.dp
