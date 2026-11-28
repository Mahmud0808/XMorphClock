package com.drdisagree.xmorphclock.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.domain.model.UiStyle
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTabs
import com.drdisagree.xmorphclock.ui.theme.LocalUiStyle
import com.drdisagree.xmorphclock.ui.theme.TabAccent
import com.kyant.backdrop.Backdrop

@Composable
fun AppTabs(
    selectedIndex: Int,
    onSelected: (index: Int) -> Unit,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalBackdrop.current,
    height: Dp = 64.dp,
    accent: Color = TabAccent,
    adaptive: Boolean = false,
    enabled: (index: Int) -> Boolean = { true },
    content: @Composable RowScope.() -> Unit
) {
    if (LocalUiStyle.current == UiStyle.GLASS) {
        LiquidTabs(selectedIndex, onSelected, backdrop, tabsCount, modifier, height, accent, adaptive, enabled, content)
    } else {
        ExpressiveTabs(selectedIndex, tabsCount, modifier, height, content)
    }
}
