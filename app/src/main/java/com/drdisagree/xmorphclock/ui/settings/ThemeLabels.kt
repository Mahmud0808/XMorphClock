package com.drdisagree.xmorphclock.ui.settings

import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.ThemeMode

internal fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
