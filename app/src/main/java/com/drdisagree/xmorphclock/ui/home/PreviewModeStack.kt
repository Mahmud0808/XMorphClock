package com.drdisagree.xmorphclock.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.drdisagree.xmorphclock.domain.model.UiStyle
import com.drdisagree.xmorphclock.ui.theme.LocalUiStyle

@Composable
internal fun PreviewModeStack(
    mode: PreviewMode,
    onModeChange: (PreviewMode) -> Unit,
    modifier: Modifier = Modifier,
    row: () -> Float = { 0f },
    smallEnabled: Boolean = true
) {
    if (LocalUiStyle.current == UiStyle.GLASS) {
        GlassModeStack(mode, onModeChange, modifier, row, smallEnabled)
        return
    }
    val colors = MaterialTheme.colorScheme
    val selected by animateFloatAsState(mode.ordinal.toFloat(), MaterialTheme.motionScheme.fastSpatialSpec(), label = "mode")
    ModeSlots(
        row = row,
        selected = { selected },
        modifier = modifier
            .clip(ModeContainerShape)
            .background(colors.surfaceContainerHighest),
        indicator = { Box(Modifier.clip(CircleShape).background(colors.secondaryContainer)) }
    ) { entry ->
        val enabled = smallEnabled || entry != PreviewMode.SMALL
        val color = if (entry == mode) colors.onSecondaryContainer else colors.onSurface
        Box(
            Modifier
                .clip(CircleShape)
                .clickable(enabled = enabled, role = Role.Tab) { onModeChange(entry) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(entry.label),
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) color else color.copy(alpha = MODE_DISABLED_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
