package com.drdisagree.xmorphclock.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.domain.model.UiStyle
import com.drdisagree.xmorphclock.ui.theme.LocalUiStyle
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.onTint

@Composable
fun GlassIconButton(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    description: String? = null,
    tint: Color = Color.Unspecified,
    iconTint: Color = OnInk,
    iconModifier: Modifier = Modifier
) {
    if (LocalUiStyle.current != UiStyle.GLASS) {
        val buttonModifier = modifier
            .size(size)
            .semantics { if (description != null) contentDescription = description }
        val iconColor = if (iconTint == OnInk) MaterialTheme.colorScheme.onSurfaceVariant else iconTint
        if (tint.isSpecified) {
            FilledIconButton(onClick, buttonModifier, colors = IconButtonDefaults.filledIconButtonColors(containerColor = tint)) {
                AppIcon(icon, modifier = iconModifier, tint = onTint(tint), size = size * ICON_RATIO)
            }
        } else {
            FilledIconButton(onClick, buttonModifier, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                AppIcon(icon, modifier = iconModifier, tint = iconColor, size = size * ICON_RATIO)
            }
        }
        return
    }
    AppButton(
        onClick = onClick,
        backdrop = LocalBackdrop.current,
        modifier = modifier
            .size(size)
            .semantics {
                role = Role.Button
                if (description != null) contentDescription = description
            },
        tint = tint,
        height = size,
        horizontalPadding = 0.dp
    ) {
        AppIcon(icon, modifier = iconModifier, tint = iconTint, size = size * ICON_RATIO)
    }
}

private const val ICON_RATIO = 0.45f
