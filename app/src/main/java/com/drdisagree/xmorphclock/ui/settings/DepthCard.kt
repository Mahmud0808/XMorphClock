package com.drdisagree.xmorphclock.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.DepthSettings
import com.drdisagree.xmorphclock.domain.model.DepthStatus
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.StatusGlyph
import com.drdisagree.xmorphclock.ui.components.StatusTone
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidSlider
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidToggle
import com.drdisagree.xmorphclock.ui.components.toneTextColor
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import com.kyant.backdrop.backdrops.emptyBackdrop
import kotlin.math.roundToInt

@Composable
fun DepthCard(
    settings: DepthSettings,
    status: DepthStatus,
    onEnabledChange: (Boolean) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onWidgetsFrontChange: (Boolean) -> Unit,
    onRetry: () -> Unit
) {
    var opacity by remember(settings.opacity) { mutableFloatStateOf(settings.opacity) }
    GlassCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(interactionSource = null, indication = null, role = Role.Switch) { onEnabledChange(!settings.enabled) }
                .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.section_depth), style = MaterialTheme.typography.titleSmall, color = OnInk)
                AnimatedContent(
                    targetState = status,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                    label = "depthStatus"
                ) { current ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (current != DepthStatus.OFF && toneOf(current) == StatusTone.BUSY) StatusGlyph(StatusTone.BUSY, size = 12.dp)
                        Text(
                            stringResource(statusLabel(current)),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (current == DepthStatus.OFF) OnInkMuted else toneTextColor(toneOf(current))
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            LiquidToggle(settings.enabled, onEnabledChange, emptyBackdrop())
        }
        AnimatedVisibility(
            visible = status == DepthStatus.NO_SUBJECT || status == DepthStatus.UNAVAILABLE,
            enter = fadeIn(tween(240)) + expandVertically(tween(320)),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(280))
        ) {
            Box(Modifier.padding(start = 20.dp, bottom = 8.dp)) {
                Text(
                    stringResource(R.string.action_retry),
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onRetry)
                        .padding(vertical = 6.dp)
                )
            }
        }
        AnimatedVisibility(
            visible = settings.enabled,
            enter = fadeIn(tween(240)) + expandVertically(tween(320)),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(280))
        ) {
            Column(Modifier.padding(bottom = 12.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = null, indication = null, role = Role.Switch) { onWidgetsFrontChange(!settings.widgetsFront) }
                        .padding(start = 20.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.depth_widgets_front), style = MaterialTheme.typography.labelLarge, color = OnInk)
                        Text(stringResource(R.string.depth_widgets_front_summary), style = MaterialTheme.typography.bodySmall, color = OnInkMuted)
                    }
                    Spacer(Modifier.width(12.dp))
                    LiquidToggle(settings.widgetsFront, onWidgetsFrontChange, emptyBackdrop())
                }
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.depth_opacity),
                        style = MaterialTheme.typography.labelLarge,
                        color = OnInkMuted,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.percent_value, (opacity * PERCENT).roundToInt()),
                        style = MaterialTheme.typography.labelLarge,
                        color = OnInk
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidSlider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = MIN_OPACITY..1f,
                        backdrop = LocalBackdrop.current,
                        onValueChangeFinished = { onOpacityChange(opacity) }
                    )
                }
            }
        }
    }
}

private const val PERCENT = 100f
private const val MIN_OPACITY = 0.1f

private fun statusLabel(status: DepthStatus): Int = when (status) {
    DepthStatus.OFF -> R.string.depth_summary
    DepthStatus.WAITING -> R.string.depth_status_waiting
    DepthStatus.PREPARING -> R.string.depth_status_preparing
    DepthStatus.EXTRACTING -> R.string.depth_status_extracting
    DepthStatus.READY -> R.string.depth_status_ready
    DepthStatus.NO_SUBJECT -> R.string.depth_status_no_subject
    DepthStatus.UNAVAILABLE -> R.string.depth_status_unavailable
}

private fun toneOf(status: DepthStatus): StatusTone = when (status) {
    DepthStatus.READY -> StatusTone.OK
    DepthStatus.NO_SUBJECT, DepthStatus.UNAVAILABLE -> StatusTone.WARNING
    else -> StatusTone.BUSY
}
