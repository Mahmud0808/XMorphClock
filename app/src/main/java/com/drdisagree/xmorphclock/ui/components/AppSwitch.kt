package com.drdisagree.xmorphclock.ui.components

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.drdisagree.xmorphclock.domain.model.UiStyle
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidToggle
import com.drdisagree.xmorphclock.ui.theme.LocalUiStyle
import com.kyant.backdrop.Backdrop

@Composable
fun AppSwitch(
    selected: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalBackdrop.current
) {
    if (LocalUiStyle.current == UiStyle.GLASS) {
        LiquidToggle(selected, onSelect, backdrop, modifier)
    } else {
        Switch(checked = selected, onCheckedChange = onSelect, modifier = modifier)
    }
}
