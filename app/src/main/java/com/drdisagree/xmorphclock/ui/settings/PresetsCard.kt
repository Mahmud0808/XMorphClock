package com.drdisagree.xmorphclock.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.FitText
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted

@Composable
fun PresetsCard(ownCount: Int, onExport: () -> Unit, onImport: () -> Unit) {
    GlassCard {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
            Text(stringResource(R.string.section_presets), style = MaterialTheme.typography.titleSmall, color = OnInk)
            AnimatedContent(
                targetState = ownCount,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                label = "presetsCount"
            ) { count ->
                Text(
                    if (count == 0) stringResource(R.string.presets_none) else pluralStringResource(R.plurals.presets_count, count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnInkMuted
                )
            }
        }
        Row(
            Modifier.padding(TILE_INSET),
            horizontalArrangement = Arrangement.spacedBy(TILE_INSET)
        ) {
            ActionTile(
                icon = R.drawable.ic_export,
                title = stringResource(R.string.action_export),
                summary = stringResource(R.string.export_summary),
                enabled = ownCount > 0,
                onClick = onExport,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                icon = R.drawable.ic_import,
                title = stringResource(R.string.action_import),
                summary = stringResource(R.string.import_summary),
                enabled = true,
                onClick = onImport,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionTile(
    @DrawableRes icon: Int,
    title: String,
    summary: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) PRESSED_SCALE else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tilePress"
    )
    val alpha by animateFloatAsState(if (enabled) 1f else DISABLED_ALPHA, tween(360), label = "tileEnabled")
    val badge by animateColorAsState(if (enabled) Accent else OnInk.copy(alpha = IDLE_BADGE), tween(360), label = "tileBadge")
    val glyph by animateColorAsState(if (enabled) Color.White else OnInk, tween(360), label = "tileGlyph")
    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(WellShape)
            .background(OnInk.copy(alpha = WELL_FILL))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .drawBehind { drawCircle(badge) },
            contentAlignment = Alignment.Center
        ) {
            AppIcon(icon, tint = glyph, size = 22.dp)
        }
        Spacer(Modifier.height(24.dp))
        FitText(title, style = MaterialTheme.typography.titleSmall, color = OnInk)
        Spacer(Modifier.height(2.dp))
        Text(summary, style = MaterialTheme.typography.bodySmall, color = OnInkMuted, minLines = 2, maxLines = 2)
    }
}

private val TILE_INSET = 12.dp
private val WellShape = RoundedCornerShape(16.dp)
private const val WELL_FILL = 0.06f
private const val IDLE_BADGE = 0.08f
private const val DISABLED_ALPHA = 0.45f
private const val PRESSED_SCALE = 0.96f
