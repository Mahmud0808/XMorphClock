package com.drdisagree.xmorphclock.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.drdisagree.xmorphclock.domain.model.ClockFrame
import com.drdisagree.xmorphclock.domain.model.ClockGeometry

val DefaultClockGeometry = ClockGeometry(
    large = ClockFrame(0.06f, 0.1f, 0.88f),
    small = ClockFrame(0.06f, 0.07f, 0.88f),
    aspect = 9f / 20f
)

val LocalClockGeometry = staticCompositionLocalOf { DefaultClockGeometry }
