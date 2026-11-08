package com.drdisagree.xmorphclock.xposed.systemui

internal enum class RomClockSetting(val key: String, val system: Boolean) {
    CLOCK_STYLE("lock_screen_custom_clock_style", false),
    LOCKSCREEN_WIDGETS("lockscreen_widgets_enabled", true)
}
