package com.drdisagree.xmorphclock.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.domain.model.UiStyle
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTab
import com.drdisagree.xmorphclock.ui.theme.LocalUiStyle

@Composable
fun RowScope.AppTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (LocalUiStyle.current == UiStyle.GLASS) {
        LiquidTab(onClick, modifier, content)
        return
    }
    Column(
        modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(role = Role.Tab, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}
