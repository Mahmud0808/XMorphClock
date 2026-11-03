package com.drdisagree.xmorphclock.xposed.signal

import com.drdisagree.xmorphclock.xposed.systemui.ClockViewModelTracker
import java.lang.reflect.Field

class ClockViewModelSizeSource : SignalSource {

    override val name = "KeyguardClockViewModel"
    override val type = SignalType.CLOCK_SIZE

    private val visibleField: Field? by lazy { ClockViewModelTracker.field(LARGE_CLOCK_VISIBLE) }

    override fun isSupported(): Boolean = visibleField != null

    override fun install() {
        ClockViewModelTracker.install()
        ClockSignals.addLargeClockProvider(::isLargeClockVisible)
    }

    private fun isLargeClockVisible(): Boolean? = ClockViewModelTracker.flowValue(visibleField) as? Boolean
}

private const val LARGE_CLOCK_VISIBLE = "isLargeClockVisible"
