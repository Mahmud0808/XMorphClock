package com.drdisagree.xmorphclock.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.domain.model.UiStyle
import com.drdisagree.xmorphclock.ui.theme.LocalUiStyle
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
    val level = LocalSurfaceLevel.current
    val colors = MaterialTheme.colorScheme
    val tonal = if (level == 0) colors.surfaceContainer else colors.surfaceContainerHighest
    val tint = if (LocalUiStyle.current == UiStyle.GLASS) null else ({ tonal })
    Column(
        modifier
            .glass(LocalBackdrop.current, shape, tint = tint, blurRadius = blurRadius, exported = surface)
            .then(inner)
    ) {
        CompositionLocalProvider(LocalBackdrop provides surface, LocalSurfaceLevel provides level + 1) {
            content()
        }
    }
}
