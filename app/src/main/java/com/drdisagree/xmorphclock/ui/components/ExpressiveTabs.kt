package com.drdisagree.xmorphclock.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
internal fun ExpressiveTabs(
    selectedIndex: Int,
    tabsCount: Int,
    modifier: Modifier,
    height: Dp,
    content: @Composable RowScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(colors.surfaceContainerHighest),
        contentAlignment = Alignment.CenterStart
    ) {
        val slot = (maxWidth - INSET * 2) / tabsCount.coerceAtLeast(1)
        val offset by animateDpAsState(slot * selectedIndex, MaterialTheme.motionScheme.fastSpatialSpec(), label = "tab")
        Box(
            Modifier
                .padding(INSET)
                .offset { IntOffset(offset.roundToPx(), 0) }
                .width(slot)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(colors.secondaryContainer)
        )
        CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(INSET),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

private val INSET = 4.dp
