package com.drdisagree.xmorphclock.xposed.signal

enum class SignalType(val combined: Boolean) {
    CLOCK_SIZE(true),
    KEYGUARD_PROGRESS(false)
}
