package com.drdisagree.xmorphclock.ui.editor

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.GlassContainer
import com.drdisagree.xmorphclock.ui.components.GlassIconButton
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.onTint

private val PillShape = RoundedCornerShape(50)

@Composable
fun EditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    fullscreen: Boolean,
    onCancel: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        GlassIconButton(
            R.drawable.ic_cross,
            onClick = onCancel,
            description = stringResource(R.string.action_cancel),
            modifier = Modifier.align(Alignment.CenterStart)
        )
        HistoryPill(canUndo, canRedo, onUndo, onRedo, Modifier.align(Alignment.Center))
        Row(
            Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Crossfade(fullscreen, animationSpec = tween(220), label = "fullscreen") { full ->
                GlassIconButton(
                    if (full) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen,
                    onClick = onToggleFullscreen,
                    description = stringResource(if (full) R.string.action_exit_fullscreen else R.string.action_fullscreen)
                )
            }
            GlassIconButton(
                R.drawable.ic_check,
                onClick = onSave,
                description = stringResource(R.string.action_save),
                tint = Accent,
                iconTint = onTint(Accent)
            )
        }
    }
}

@Composable
private fun HistoryPill(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassContainer(shape = PillShape, modifier = modifier) {
        Row(Modifier.padding(horizontal = 4.dp)) {
            HistoryButton(R.drawable.ic_undo, stringResource(R.string.action_undo), canUndo, onUndo)
            HistoryButton(R.drawable.ic_redo, stringResource(R.string.action_redo), canRedo, onRedo)
        }
    }
}

@Composable
private fun HistoryButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (enabled) 1f else DISABLED_ALPHA, tween(200), label = "history")
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .graphicsLayer { this.alpha = alpha },
        contentAlignment = Alignment.Center
    ) {
        AppIcon(icon, size = 20.dp)
    }
}

private const val DISABLED_ALPHA = 0.35f
