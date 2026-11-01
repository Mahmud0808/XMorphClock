package com.drdisagree.xmorphclock.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.GlassContainer

@Composable
fun PreviewActions(height: Dp, canEdit: Boolean, onAdd: () -> Unit, onFullscreen: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    GlassContainer(shape = RoundedCornerShape(50), modifier = modifier.height(height)) {
        Row(Modifier.padding(horizontal = 2.dp)) {
            ActionButton(R.drawable.ic_fullscreen, stringResource(R.string.action_fullscreen), height, true, onFullscreen)
            ActionButton(R.drawable.ic_plus, stringResource(R.string.action_add_element), height, true, onAdd)
            ActionButton(R.drawable.ic_edit, stringResource(R.string.action_edit), height, canEdit, onEdit)
        }
    }
}

@Composable
private fun ActionButton(icon: Int, description: String, size: Dp, enabled: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (enabled) 1f else DISABLED_ALPHA, tween(200), label = "previewAction")
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .graphicsLayer { this.alpha = alpha },
        contentAlignment = Alignment.Center
    ) {
        AppIcon(icon, size = ICON_SIZE)
    }
}

private val ICON_SIZE = 18.dp
private const val DISABLED_ALPHA = 0.4f
