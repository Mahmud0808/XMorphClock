package com.drdisagree.xmorphclock.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidButton
import com.drdisagree.xmorphclock.ui.components.liquid.rememberAdaptiveLuminance
import com.drdisagree.xmorphclock.ui.theme.FlatSurface
import com.drdisagree.xmorphclock.ui.theme.Negative
import com.drdisagree.xmorphclock.ui.theme.Positive
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight

private val NotificationShape = RoundedCornerShape(26.dp)

@Composable
fun LockNotification(
    title: String,
    body: String,
    ok: Boolean?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    val surface = rememberLayerBackdrop()
    val luminance = rememberAdaptiveLuminance()
    val content = luminance.contentColor
    val flat = FlatSurface
    Row(
        modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = LocalBackdrop.current,
                shape = { NotificationShape },
                effects = {
                    if (GlassSupport.full) {
                        luminance.apply(this)
                        lens(16.dp.toPx(), 32.dp.toPx(), depthEffect = true)
                    }
                },
                highlight = { Highlight.Default.copy(alpha = RIM_ALPHA) },
                exportedBackdrop = surface,
                onDrawSurface = { if (!GlassSupport.full) drawRect(flat) },
                onDrawBackdrop = { drawBackdrop ->
                    drawBackdrop()
                    luminance.record(this, drawBackdrop)
                }
            )
            .animateContentSize(tween(320))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val badge by animateColorAsState(
            when (ok) {
                true -> Positive.copy(alpha = 0.22f)
                false -> Negative.copy(alpha = 0.22f)
                null -> content.copy(alpha = 0.12f)
            },
            tween(360),
            label = "badge"
        )
        Box(
            Modifier
                .size(38.dp)
                .drawBehind { drawCircle(badge) },
            contentAlignment = Alignment.Center
        ) {
            Crossfade(ok, animationSpec = tween(320), label = "icon") { state ->
                AppIcon(
                    when (state) {
                        true -> R.drawable.ic_check
                        false -> R.drawable.ic_cross
                        null -> R.drawable.ic_faces
                    },
                    tint = when (state) {
                        true -> Positive
                        false -> Negative
                        null -> content
                    },
                    size = 18.dp
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.notification_header),
                style = MaterialTheme.typography.labelMedium,
                color = content.copy(alpha = 0.5f)
            )
            Text(title, style = MaterialTheme.typography.titleMedium, color = content)
            AnimatedContent(
                targetState = body,
                transitionSpec = {
                    fadeIn(tween(260, delayMillis = 60)) togetherWith fadeOut(tween(140)) using SizeTransform(clip = false)
                },
                label = "body"
            ) { text ->
                Text(text, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.72f))
            }
        }
        var lastLabel by remember { mutableStateOf(actionLabel) }
        if (actionLabel != null) lastLabel = actionLabel
        AnimatedVisibility(
            visible = actionLabel != null,
            enter = fadeIn(tween(260)) + scaleIn(tween(320), initialScale = 0.8f) + expandHorizontally(tween(320)),
            exit = fadeOut(tween(160)) + scaleOut(tween(240), targetScale = 0.8f) + shrinkHorizontally(tween(280))
        ) {
            LiquidButton(onAction, surface, height = 34.dp, horizontalPadding = 14.dp) {
                Text(lastLabel.orEmpty(), style = MaterialTheme.typography.labelLarge, color = content)
            }
        }
    }
}

private const val RIM_ALPHA = 0.5f
