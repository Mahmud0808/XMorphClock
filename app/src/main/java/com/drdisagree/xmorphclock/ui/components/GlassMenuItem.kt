package com.drdisagree.xmorphclock.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInk

@Composable
fun GlassMenuItem(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    badge: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppIcon(icon, tint = OnInk, size = 20.dp)
        Text(label, style = MaterialTheme.typography.titleSmall, color = OnInk, modifier = Modifier.weight(1f))
        AnimatedVisibility(visible = badge, enter = fadeIn(tween(200)) + scaleIn(tween(260)), exit = fadeOut(tween(160)) + scaleOut(tween(200))) {
            StatusDot(Accent, size = 7.dp)
        }
    }
}
