package com.drdisagree.xmorphclock.ui.components.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.drdisagree.xmorphclock.ui.components.GlassSupport
import com.drdisagree.xmorphclock.ui.theme.FlatSurface
import com.drdisagree.xmorphclock.ui.theme.LocalPalette
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.TabAccent
import com.drdisagree.xmorphclock.ui.theme.TabContainer
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Composable
fun LiquidTabs(
    selectedIndex: Int,
    onSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    accent: Color = TabAccent,
    adaptive: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val luminance = if (adaptive && AdaptiveLuminance.supported) rememberAdaptiveLuminance() else null
    val currentOnSelected by rememberUpdatedState(onSelected)
    val tabsBackdrop = rememberLayerBackdrop()
    val container = if (GlassSupport.full) TabContainer else FlatSurface
    val dark = LocalPalette.current.isDark
    val adaptivePill = luminance?.let { it.contentColor.copy(alpha = ADAPTIVE_PILL) }
    val selectedTint = luminance?.let { if (it.contentColor.luminance() < HALF) Color.White else Color.Black } ?: accent
    val indicatorHeight = height - 8.dp

    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        val maxWidth = constraints.maxWidth.toFloat()
        val tabWidth = with(density) { (maxWidth - 8.dp.toPx()) / tabsCount }
        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density, maxWidth) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / maxWidth).fastCoerceIn(-1f, 1f)
                with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember { mutableIntStateOf(selectedIndex) }
        val animation = remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIndex.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = target
                    animateToValue(target.toFloat())
                    animationScope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch { offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x) }
                }
            )
        }
        LaunchedEffect(selectedIndex) { currentIndex = selectedIndex }
        LaunchedEffect(animation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    animation.animateToValue(index.toFloat())
                    currentOnSelected(index)
                }
        }
        val highlight = remember(animationScope, tabWidth) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (animation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (animation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        if (GlassSupport.full) {
                            if (luminance != null) {
                                luminance.apply(this)
                            } else {
                                vibrancy()
                                blur(8.dp.toPx())
                            }
                            lens(24.dp.toPx(), 24.dp.toPx())
                        }
                    },
                    onDrawBackdrop = { drawBackdrop ->
                        drawBackdrop()
                        luminance?.record(this, drawBackdrop)
                    },
                    layerBlock = {
                        val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, animation.pressProgress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { if (luminance == null) drawRect(container) }
                )
                .then(highlight.modifier)
                .height(height)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides (luminance?.contentColor ?: if (adaptive) OnInk else LocalContentColor.current)) {
                content()
            }
        }

        CompositionLocalProvider(LocalLiquidTabScale provides { lerp(1f, 1.2f, animation.pressProgress) }) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            if (GlassSupport.full) {
                                val progress = animation.pressProgress
                                vibrancy()
                                blur(8.dp.toPx())
                                lens(24.dp.toPx() * progress, 24.dp.toPx() * progress)
                            }
                        },
                        highlight = { Highlight.Default.copy(alpha = animation.pressProgress) },
                        onDrawSurface = {
                            drawRect(container)
                            adaptivePill?.let { drawRect(it, alpha = 1f - animation.pressProgress) }
                        }
                    )
                    .then(highlight.modifier)
                    .height(indicatorHeight)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(selectedTint)),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX = if (isLtr) animation.value * tabWidth + panelOffset
                    else size.width - (animation.value + 1f) * tabWidth + panelOffset
                }
                .then(highlight.gestureModifier)
                .then(animation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        if (GlassSupport.full) {
                            val progress = animation.pressProgress
                            lens(10.dp.toPx() * progress, 14.dp.toPx() * progress, chromaticAberration = true)
                        }
                    },
                    highlight = { Highlight.Default.copy(alpha = animation.pressProgress) },
                    shadow = { Shadow(alpha = if (adaptivePill != null) max(animation.pressProgress, RESTING_SHADOW) else animation.pressProgress) },
                    innerShadow = {
                        val progress = animation.pressProgress
                        InnerShadow(radius = 8.dp * progress, alpha = progress)
                    },
                    layerBlock = {
                        val velocity = animation.velocity / 10f
                        scaleX = animation.scaleX / (1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f))
                        scaleY = animation.scaleY * (1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f))
                    },
                    onDrawSurface = {
                        val progress = animation.pressProgress
                        if (adaptivePill == null) {
                            drawRect(if (dark) Color.White.copy(alpha = DARK_PILL) else Color.Black.copy(alpha = LIGHT_PILL), alpha = 1f - progress)
                        }
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                )
                .height(indicatorHeight)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}

private const val HALF = 0.5f
private const val RESTING_SHADOW = 0.45f
private const val LIGHT_PILL = 0.08f
private const val DARK_PILL = 0.12f
private const val ADAPTIVE_PILL = 0.55f
