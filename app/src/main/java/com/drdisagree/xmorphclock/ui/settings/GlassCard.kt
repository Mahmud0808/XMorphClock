package com.drdisagree.xmorphclock.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.glass
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

internal val TileShape = RoundedCornerShape(28.dp)

@Composable
internal fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    val surface = rememberLayerBackdrop()
    Column(
        Modifier
            .fillMaxWidth()
            .glass(LocalBackdrop.current, TileShape, exported = surface)
            .clip(TileShape)
    ) {
        CompositionLocalProvider(LocalBackdrop provides surface) { content() }
    }
}
