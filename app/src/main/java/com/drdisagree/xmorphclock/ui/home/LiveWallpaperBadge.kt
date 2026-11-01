package com.drdisagree.xmorphclock.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.ui.components.GlassIconButton
import com.drdisagree.xmorphclock.ui.theme.Warning

@Composable
fun LiveWallpaperBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassIconButton(
        R.drawable.ic_alert,
        onClick = onClick,
        modifier = modifier,
        size = LIVE_BADGE_SIZE,
        description = stringResource(R.string.live_wallpaper_badge),
        iconTint = Warning
    )
}

val LIVE_BADGE_SIZE = 28.dp
