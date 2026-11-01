package com.drdisagree.xmorphclock.xposed.signal

import com.drdisagree.xmorphclock.xposed.hook.findField
import com.drdisagree.xmorphclock.xposed.systemui.ClockSwitchTracker
import java.lang.reflect.Field

class ClockSwitchSizeSource : SignalSource {

    override val name = "KeyguardClockSwitch"
    override val type = SignalType.CLOCK_SIZE

    private val sizeField: Field? by lazy { ClockSwitchTracker.type?.findField(DISPLAYED_SIZE) }
    private val notificationsField: Field? by lazy { ClockSwitchTracker.type?.findField(HAS_NOTIFICATIONS) }

    override fun isSupported(): Boolean = sizeField != null || notificationsField != null

    override fun install() {
        ClockSwitchTracker.install()
        ClockSignals.addLargeClockProvider(::isLarge)
    }

    private fun isLarge(): Boolean? {
        val view = ClockSwitchTracker.current() ?: return null
        return try {
            (sizeField?.get(view) as? Int)?.let { it == LARGE }
                ?: (notificationsField?.get(view) as? Boolean)?.not()
        } catch (_: Throwable) {
            null
        }
    }
}

private const val DISPLAYED_SIZE = "mDisplayedClockSize"
private const val HAS_NOTIFICATIONS = "mHasVisibleNotifications"
private const val LARGE = 0
