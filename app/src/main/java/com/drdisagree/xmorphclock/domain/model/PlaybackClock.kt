package com.drdisagree.xmorphclock.domain.model

import kotlin.math.abs

data class PlaybackClock(
    val position: Long,
    val capturedAt: Long,
    val speed: Float = 1f,
    val playing: Boolean
) {
    fun positionAt(now: Long): Long =
        if (!playing || speed <= 0f || now <= capturedAt) position else position + ((now - capturedAt) * speed).toLong()

    fun matches(other: PlaybackClock?, now: Long, tolerance: Long): Boolean {
        if (other == null) return false
        if (playing != other.playing || speed != other.speed) return false
        return abs(positionAt(now) - other.positionAt(now)) <= tolerance
    }
}
