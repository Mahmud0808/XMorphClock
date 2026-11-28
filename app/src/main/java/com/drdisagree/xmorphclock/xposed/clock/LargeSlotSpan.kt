package com.drdisagree.xmorphclock.xposed.clock

import android.content.res.Resources

internal object LargeSlotSpan {

    @Volatile
    private var left = 0

    @Volatile
    private var width = 0

    fun record(left: Int, width: Int) {
        if (width <= 0) return
        this.left = left
        this.width = width
    }

    fun current(): Pair<Int, Int> = if (width > 0) left to width else 0 to Resources.getSystem().displayMetrics.widthPixels
}
