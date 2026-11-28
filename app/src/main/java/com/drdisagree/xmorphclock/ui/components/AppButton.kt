package com.drdisagree.xmorphclock.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.domain.model.UiStyle
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidButton
import com.drdisagree.xmorphclock.ui.theme.LocalUiStyle
import com.drdisagree.xmorphclock.ui.theme.onTint
import com.kyant.backdrop.Backdrop

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalBackdrop.current,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    height: Dp = 48.dp,
    horizontalPadding: Dp = 20.dp,
    content: @Composable RowScope.() -> Unit
) {
    if (LocalUiStyle.current == UiStyle.GLASS) {
        LiquidButton(onClick, backdrop, modifier, tint, surfaceColor, height, horizontalPadding, content)
        return
    }
    val padding = PaddingValues(horizontal = horizontalPadding)
    if (tint.isSpecified) {
        Button(
            onClick = onClick,
            modifier = modifier.height(height),
            colors = ButtonDefaults.buttonColors(containerColor = tint, contentColor = onTint(tint)),
            contentPadding = padding,
            content = content
        )
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier.height(height),
            colors = if (surfaceColor.isSpecified) ButtonDefaults.filledTonalButtonColors(containerColor = surfaceColor) else ButtonDefaults.filledTonalButtonColors(),
            contentPadding = padding,
            content = content
        )
    }
}
