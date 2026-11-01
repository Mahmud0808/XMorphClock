package com.drdisagree.xmorphclock.xposed.signal

interface SignalSource {
    val name: String
    val type: SignalType
    fun isSupported(): Boolean
    fun install()
}
