package com.drdisagree.xmorphclock.xposed.adapter

import android.view.View
import com.drdisagree.xmorphclock.xposed.clock.DepthLayer
import java.util.Collections
import java.util.WeakHashMap

internal class ClockSwitchState {
    var slot: View? = null
    var depth: DepthLayer? = null
    var frameWidth: Int? = null
    var frameHeight: Int? = null
    var framePaddingStart = 0
    var framePaddingEnd = 0
    var lastCancel = 0L
    val hidden: MutableSet<View> = Collections.newSetFromMap(WeakHashMap())
}
