package com.drdisagree.xmorphclock.xposed.adapter

import com.drdisagree.xmorphclock.xposed.clock.ClockHost

interface ClockAdapter {
    val name: String
    fun isSupported(): Boolean
    fun install(host: ClockHost)
    fun probes(): Map<String, Boolean> = emptyMap()
}
