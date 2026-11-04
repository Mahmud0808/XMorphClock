package com.drdisagree.xmorphclock.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.drdisagree.xmorphclock.ui.theme.Negative
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import com.drdisagree.xmorphclock.ui.theme.Warning

@Composable
fun toneTextColor(tone: StatusTone): Color = when (tone) {
    StatusTone.WARNING -> Warning
    StatusTone.ERROR -> Negative
    else -> OnInkMuted
}
