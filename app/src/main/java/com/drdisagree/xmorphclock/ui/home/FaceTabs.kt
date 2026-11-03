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
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTab
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTabs
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted

@Composable
fun FaceTabs(selected: FaceTab, onSelect: (FaceTab) -> Unit, height: Dp, modifier: Modifier = Modifier) {
    LiquidTabs(
        selectedIndex = selected.ordinal,
        onSelected = { onSelect(FaceTab.entries[it]) },
        backdrop = LocalBackdrop.current,
        tabsCount = FaceTab.entries.size,
        height = height,
        modifier = modifier
    ) {
        FaceTab.entries.forEach { tab ->
            val label = stringResource(tabLabel(tab))
            val tint by animateColorAsState(if (tab == selected) OnInk else OnInkMuted, label = "tabTint")
            LiquidTab(onClick = { onSelect(tab) }, modifier = Modifier.semantics { contentDescription = label }) {
                AppIcon(tabIcon(tab), tint = tint, size = ICON_SIZE)
            }
        }
    }
}

private fun tabIcon(tab: FaceTab): Int = when (tab) {
    FaceTab.CLOCK -> R.drawable.ic_kind_clock
    FaceTab.WIDGETS -> R.drawable.ic_kind_widgets
    FaceTab.MINE -> R.drawable.ic_kind_mine
}

private val ICON_SIZE = 18.dp
