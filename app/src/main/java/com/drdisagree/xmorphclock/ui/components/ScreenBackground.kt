package com.drdisagree.xmorphclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.drdisagree.xmorphclock.domain.model.UiStyle
import com.drdisagree.xmorphclock.ui.theme.Ink
import com.drdisagree.xmorphclock.ui.theme.LocalUiStyle

@Composable
fun ScreenBackground(modifier: Modifier = Modifier) {
    if (LocalUiStyle.current == UiStyle.GLASS) {
        MeshBackground(modifier)
    } else {
        Box(modifier.background(Ink))
    }
}
