package com.drdisagree.xmorphclock.domain.model

data class ClockSettings(
    val clockEnabled: Boolean,
    val faceStyle: Int,
    val burnInProtection: Boolean,
    val dynamicClock: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val uiStyle: UiStyle = UiStyle.EXPRESSIVE,
    val composition: Composition = Composition()
)
