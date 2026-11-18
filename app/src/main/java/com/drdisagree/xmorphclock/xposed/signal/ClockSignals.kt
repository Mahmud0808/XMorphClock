package com.drdisagree.xmorphclock.xposed.signal

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

object ClockSignals {

    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val largeClockProviders = CopyOnWriteArrayList<() -> Boolean?>()

    @Volatile
    var aodProgress = 0f
        private set

    @Volatile
    var sleepProgress = 0f
        private set

    @Volatile
    var isAsleep = false
        private set

    @Volatile
    var isAmbient = false
        private set

    fun addLargeClockProvider(provider: () -> Boolean?) {
        largeClockProviders += provider
    }

    fun isLargeClock(): Boolean? = largeClockProviders.firstNotNullOfOrNull { it() }

    fun updateProgress(aod: Float, sleep: Float, asleep: Boolean, ambient: Boolean = false) {
        isAsleep = asleep
        val ambientChanged = isAmbient != ambient
        isAmbient = ambient
        if (aodProgress == aod && sleepProgress == sleep && !ambientChanged) return
        aodProgress = aod
        sleepProgress = sleep
        if (Looper.myLooper() == Looper.getMainLooper()) dispatch() else mainHandler.post { dispatch() }
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    private fun dispatch() {
        listeners.forEach { it() }
    }
}
