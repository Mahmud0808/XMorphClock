package com.drdisagree.xmorphclock.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun GlassContainer(
    shape: Shape,
    modifier: Modifier = Modifier,
    inner: Modifier = Modifier,
    blurRadius: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val surface = rememberLayerBackdrop()
    Column(
        modifier
            .glass(LocalBackdrop.current, shape, blurRadius = blurRadius, exported = surface)
            .then(inner)
    ) {
        CompositionLocalProvider(LocalBackdrop provides surface) {
            content()
        }
    }
}
