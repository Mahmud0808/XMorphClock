package com.drdisagree.xmorphclock.ui.components

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.drdisagree.xmorphclock.domain.model.UiStyle
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidSlider
import com.drdisagree.xmorphclock.ui.theme.LocalUiStyle
import com.kyant.backdrop.Backdrop

@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalBackdrop.current,
    onValueChangeFinished: (() -> Unit)? = null
) {
    if (LocalUiStyle.current == UiStyle.GLASS) {
        LiquidSlider(value, onValueChange, valueRange, backdrop, modifier, onValueChangeFinished)
    } else {
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = modifier, onValueChangeFinished = onValueChangeFinished)
    }
}
