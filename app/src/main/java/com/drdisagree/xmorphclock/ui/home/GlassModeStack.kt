package com.drdisagree.xmorphclock.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.drdisagree.xmorphclock.ui.components.GlassSupport
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.liquid.DampedDragAnimation
import com.drdisagree.xmorphclock.ui.components.liquid.InteractiveHighlight
import com.drdisagree.xmorphclock.ui.theme.FlatSurface
import com.drdisagree.xmorphclock.ui.theme.LocalPalette
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.TabAccent
import com.drdisagree.xmorphclock.ui.theme.TabContainer
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
import kotlin.math.sign
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Composable
internal fun GlassModeStack(
    mode: PreviewMode,
    onModeChange: (PreviewMode) -> Unit,
    modifier: Modifier,
    row: () -> Float
) {
    val backdrop = LocalBackdrop.current
    val currentOnModeChange by rememberUpdatedState(onModeChange)
    val currentRow by rememberUpdatedState(row)
    val labelsBackdrop = rememberLayerBackdrop()
    val container = if (GlassSupport.full) TabContainer else FlatSurface
    val dark = LocalPalette.current.isDark
    val density = LocalDensity.current
    val count = PreviewMode.entries.size
    val last = (count - 1).toFloat()
    val lensSize = minOf(LENS_MAX, (SLOT_HEIGHT + SLOT_INSET * 2) * LENS_RATIO)
    val scope = rememberCoroutineScope()
    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(density) {
        derivedStateOf {
            val extent = with(density) { SLOT_HEIGHT.toPx() * count }
            val fraction = (offsetAnimation.value / extent).fastCoerceIn(-1f, 1f)
            with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
        }
    }
    var currentIndex by remember { mutableIntStateOf(mode.ordinal) }
    val animation = remember(scope, density) {
        val slotWidth = with(density) { SLOT_WIDTH.toPx() }
        val slotHeight = with(density) { SLOT_HEIGHT.toPx() }
        DampedDragAnimation(
            animationScope = scope,
            initialValue = mode.ordinal.toFloat(),
            valueRange = 0f..last,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = PRESSED_SCALE,
            onDragStarted = {},
            onDragStopped = {
                val target = targetValue.fastRoundToInt().fastCoerceIn(0, count - 1)
                currentIndex = target
                animateToValue(target.toFloat())
                scope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDrag = { _, dragAmount ->
                val t = currentRow().fastCoerceIn(0f, 1f)
                val along = lerp(dragAmount.y, dragAmount.x, t)
                updateValue((targetValue + along / lerp(slotHeight, slotWidth, t)).fastCoerceIn(0f, last))
                scope.launch { offsetAnimation.snapTo(offsetAnimation.value + along) }
            }
        )
    }
    LaunchedEffect(mode) { currentIndex = mode.ordinal }
    LaunchedEffect(animation) {
        snapshotFlow { currentIndex }
            .drop(1)
            .collectLatest { index ->
                animation.animateToValue(index.toFloat())
                currentOnModeChange(PreviewMode.entries[index])
            }
    }
    val active by remember(animation) {
        derivedStateOf { animation.pressProgress > 0f || abs(animation.value - currentIndex) > REST_EPSILON }
    }
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, labelsBackdrop)
    val highlight = remember(scope, density) {
        InteractiveHighlight(
            animationScope = scope,
            position = { _, _ -> with(density) { slotCenter(animation.value, currentRow()) } }
        )
    }
    val labelColor = OnInk
    val style = MaterialTheme.typography.labelLarge

    Box(
        modifier.graphicsLayer {
            val t = currentRow().fastCoerceIn(0f, 1f)
            translationX = panelOffset * t
            translationY = panelOffset * (1f - t)
        }
    ) {
        Box(
            Modifier
                .matchParentSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ModeContainerShape },
                    effects = {
                        if (GlassSupport.full) {
                            vibrancy()
                            blur(8.dp.toPx())
                            lens(lensSize.toPx(), lensSize.toPx())
                        }
                    },
                    layerBlock = {
                        val scale = lerp(1f, 1f + 16.dp.toPx() / size.maxDimension, animation.pressProgress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(container) }
                )
                .then(highlight.modifier)
        )
        ModeSlots(
            row = currentRow,
            selected = { animation.value },
            indicatorOnTop = true,
            indicator = {
                Box(
                    Modifier
                        .then(highlight.gestureModifier)
                        .then(animation.modifier)
                        .drawBackdrop(
                            backdrop = if (active) combinedBackdrop else labelsBackdrop,
                            shape = { Capsule() },
                            effects = {
                                if (GlassSupport.full) {
                                    val progress = animation.pressProgress
                                    lens(10.dp.toPx() * progress, 14.dp.toPx() * progress, chromaticAberration = true)
                                }
                            },
                            highlight = { Highlight.Default.copy(alpha = animation.pressProgress) },
                            shadow = { Shadow(alpha = animation.pressProgress) },
                            innerShadow = {
                                val progress = animation.pressProgress
                                InnerShadow(radius = 8.dp * progress, alpha = progress)
                            },
                            layerBlock = {
                                val t = currentRow().fastCoerceIn(0f, 1f)
                                val velocity = animation.velocity / 10f
                                val stretch = 1f / (1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f))
                                val squeeze = 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                                scaleX = animation.scaleX * lerp(squeeze, stretch, t)
                                scaleY = animation.scaleY * lerp(stretch, squeeze, t)
                            },
                            onDrawSurface = {
                                val progress = animation.pressProgress
                                drawRect(if (dark) Color.White.copy(alpha = DARK_PILL) else Color.Black.copy(alpha = LIGHT_PILL), alpha = 1f - progress)
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            }
                        )
                )
            }
        ) { entry ->
            Box(
                Modifier
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null, role = Role.Tab) { currentIndex = entry.ordinal },
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(entry.label), style = style, color = labelColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        ModeSlots(
            row = currentRow,
            selected = { animation.value },
            modifier = Modifier
                .matchParentSize()
                .clearAndSetSemantics {}
                .alpha(0f)
                .layerBackdrop(labelsBackdrop)
                .then(
                    if (active) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { ModeContainerShape },
                            effects = {
                                if (GlassSupport.full) {
                                    val progress = animation.pressProgress
                                    vibrancy()
                                    blur(8.dp.toPx())
                                    lens(lensSize.toPx() * progress, lensSize.toPx() * progress)
                                }
                            },
                            highlight = { Highlight.Default.copy(alpha = animation.pressProgress) },
                            onDrawSurface = { drawRect(container) }
                        )
                    } else {
                        Modifier
                    }
                )
                .then(highlight.modifier)
                .graphicsLayer(colorFilter = ColorFilter.tint(TabAccent))
        ) { entry ->
            Box(
                Modifier.graphicsLayer {
                    val scale = lerp(1f, LABEL_PRESS_SCALE, animation.pressProgress)
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(entry.label), style = style, color = labelColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private const val PRESSED_SCALE = 1.22f
private const val LABEL_PRESS_SCALE = 1.1f
private const val REST_EPSILON = 0.001f
private const val LENS_RATIO = 0.375f
private val LENS_MAX = 24.dp
private const val LIGHT_PILL = 0.08f
private const val DARK_PILL = 0.12f
