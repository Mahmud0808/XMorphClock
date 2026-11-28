package com.drdisagree.xmorphclock.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.drdisagree.xmorphclock.domain.model.UiStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun XMorphClockTheme(dark: Boolean = isSystemInDarkTheme(), style: UiStyle = UiStyle.EXPRESSIVE, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dynamic = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    val expressive = style == UiStyle.EXPRESSIVE
    val target = if (expressive) paletteOf(dynamic, dark) else if (dark) DarkPalette else LightPalette
    val palette = target.copy(
        ink = animated(target.ink),
        onInk = animated(target.onInk),
        onInkMuted = animated(target.onInkMuted),
        onInkFaint = animated(target.onInkFaint),
        container = animated(target.container),
        containerHigh = animated(target.containerHigh),
        hairline = animated(target.hairline),
        accent = animated(target.accent),
        flat = animated(target.flat),
        tabContainer = animated(target.tabContainer),
        toggleTrack = animated(target.toggleTrack),
        toggleOn = animated(target.toggleOn),
        mesh = target.mesh.map { animated(it) }
    )
    val scheme = if (expressive) {
        dynamic
    } else {
        dynamic.copy(
            background = palette.ink,
            surface = palette.ink,
            onBackground = palette.onInk,
            onSurface = palette.onInk
        )
    }
    CompositionLocalProvider(LocalPalette provides palette, LocalUiStyle provides style) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            motionScheme = if (expressive) MotionScheme.expressive() else MotionScheme.standard(),
            content = content
        )
    }
}

private fun paletteOf(scheme: ColorScheme, dark: Boolean): Palette = Palette(
    isDark = dark,
    ink = scheme.surface,
    onInk = scheme.onSurface,
    onInkMuted = scheme.onSurfaceVariant,
    onInkFaint = scheme.outline,
    container = scheme.surfaceContainer,
    containerHigh = scheme.surfaceContainerHigh,
    hairline = scheme.outlineVariant,
    accent = scheme.primary,
    flat = scheme.surfaceContainerHighest,
    tabContainer = scheme.surfaceContainerHigh,
    toggleTrack = scheme.surfaceContainerHighest,
    toggleOn = scheme.primary,
    mesh = List(MESH_STOPS) { scheme.surface }
)

@Composable
private fun animated(color: Color): Color = animateColorAsState(color, tween(THEME_FADE), label = "palette").value

private const val THEME_FADE = 450
private const val MESH_STOPS = 4
