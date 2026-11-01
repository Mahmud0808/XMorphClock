package com.drdisagree.xmorphclock.xposed.clock

import kotlin.math.ceil
import kotlin.math.max

object BurnInOffset {

    private const val MINUTE_MS = 60_000L
    private const val Y_SKEW = 3

    fun x(maxPx: Float, stepPx: Float): Float = offset(maxPx, stepPx, 0)

    fun y(maxPx: Float, stepPx: Float): Float = offset(maxPx, stepPx, Y_SKEW)

    private fun offset(maxPx: Float, stepPx: Float, skew: Int): Float {
        if (maxPx <= 0f || stepPx <= 0f) return 0f
        val steps = max(1, ceil(2f * maxPx / stepPx).toInt()) + skew
        val phase = (System.currentTimeMillis() / MINUTE_MS % (2L * steps)).toInt()
        val position = if (phase <= steps) phase else 2 * steps - phase
        return -maxPx + 2f * maxPx * position / steps
    }
}
