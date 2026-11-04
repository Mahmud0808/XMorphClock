package com.drdisagree.xmorphclock.xposed.clock

import android.graphics.Bitmap
import android.view.View
import com.drdisagree.xmorphclock.xposed.signal.ClockSignals
import java.util.Collections
import java.util.WeakHashMap

internal object DepthOverlay {

    private val views: MutableSet<View> = Collections.newSetFromMap(WeakHashMap())
    private var listening = false

    var subject: Bitmap? = null
        private set
    var opacity = 1f
        private set
    var inClock = false
        private set
    var windowSize = 0 to 0

    fun update(bitmap: Bitmap?, alpha: Float, widgetsFront: Boolean) {
        subject = bitmap
        opacity = alpha
        inClock = widgetsFront
        invalidate()
    }

    fun register(view: View) {
        views += view
        if (listening) return
        listening = true
        ClockSignals.addListener(::invalidate)
        WallpaperCapture.addScaleListener(::invalidate)
        ScrimTracker.addListener(::invalidate)
    }

    fun describe(): String {
        val bitmap = subject ?: return "none"
        val attached = views.count { it.isAttachedToWindow }
        return "${bitmap.width}x${bitmap.height} opacity=$opacity layers=$attached/${views.size} ${ScrimTracker.describe()}"
    }

    private fun invalidate() {
        views.toList().forEach { it.invalidate() }
    }
}
