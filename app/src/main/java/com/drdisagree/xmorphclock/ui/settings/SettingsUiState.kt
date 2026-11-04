package com.drdisagree.xmorphclock.ui.settings

import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.DepthSettings
import com.drdisagree.xmorphclock.domain.model.DepthStatus
import com.drdisagree.xmorphclock.domain.model.FacePreset
import com.drdisagree.xmorphclock.domain.model.ThemeMode
import com.drdisagree.xmorphclock.domain.model.WeatherState

data class SettingsUiState(
    val clockEnabled: Boolean = true,
    val burnInProtection: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val weather: WeatherState = WeatherState(),
    val locationNotFound: Boolean = false,
    val depth: DepthSettings = DepthSettings(),
    val depthStatus: DepthStatus = DepthStatus.OFF,
    val ownPresets: List<FacePreset> = emptyList(),
    val ownPreviews: Map<Int, ClockFace> = emptyMap(),
    val loaded: Boolean = false
)
