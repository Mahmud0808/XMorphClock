package com.drdisagree.xmorphclock.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class Palette(
    val isDark: Boolean,
    val ink: Color,
    val onInk: Color,
    val onInkMuted: Color,
    val onInkFaint: Color,
    val container: Color,
    val containerHigh: Color,
    val hairline: Color,
    val accent: Color,
    val flat: Color,
    val tabContainer: Color,
    val toggleTrack: Color,
    val toggleOn: Color,
    val mesh: List<Color>
)
