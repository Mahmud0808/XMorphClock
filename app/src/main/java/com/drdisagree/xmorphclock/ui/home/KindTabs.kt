package com.drdisagree.xmorphclock.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTab
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTabs
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted

@Composable
fun KindTabs(selected: FaceKind, onSelect: (FaceKind) -> Unit, height: Dp, modifier: Modifier = Modifier) {
    LiquidTabs(
        selectedIndex = selected.ordinal,
        onSelected = { onSelect(FaceKind.entries[it]) },
        backdrop = LocalBackdrop.current,
        tabsCount = FaceKind.entries.size,
        height = height,
        modifier = modifier
    ) {
        FaceKind.entries.forEach { kind ->
            val label = stringResource(kindLabel(kind))
            val tint by animateColorAsState(if (kind == selected) OnInk else OnInkMuted, label = "kindTint")
            LiquidTab(onClick = { onSelect(kind) }, modifier = Modifier.semantics { contentDescription = label }) {
                AppIcon(kindIcon(kind), tint = tint, size = ICON_SIZE)
            }
        }
    }
}

private fun kindIcon(kind: FaceKind): Int = when (kind) {
    FaceKind.CLOCK -> R.drawable.ic_kind_clock
    FaceKind.WEATHER -> R.drawable.ic_kind_weather
    FaceKind.WIDGET -> R.drawable.ic_kind_widgets
}

private val ICON_SIZE = 18.dp
