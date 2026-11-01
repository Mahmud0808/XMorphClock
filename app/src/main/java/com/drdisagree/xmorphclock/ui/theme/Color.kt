package com.drdisagree.xmorphclock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val Positive = Color(0xFF30D158)
val Negative = Color(0xFFFF453A)
val Warning = Color(0xFFFFB800)

val Ink: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.ink

val OnInk: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.onInk

val OnInkMuted: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.onInkMuted

val OnInkFaint: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.onInkFaint

val Surface: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.container

val SurfaceHigh: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.containerHigh

val Hairline: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.hairline

val Accent: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.accent

val FlatSurface: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.flat

val TabAccent: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.accent

val TabContainer: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.tabContainer

val ToggleTrack: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.toggleTrack

val ToggleOn: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.toggleOn

val GlassTint: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.container

@Composable
@ReadOnlyComposable
fun onTint(tint: Color): Color = if (LocalPalette.current.isDark) Color.White else lerp(tint, Color.Black, ON_TINT_DEPTH)

private const val ON_TINT_DEPTH = 0.3f
