package com.drdisagree.xmorphclock.xposed.clock

import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.view.WindowManager

internal class DepthLayer(context: Context) : View(context) {

    private val painter = DepthPainter()

    var clipToSelf = false

    init {
        context.getSystemService(WindowManager::class.java)?.maximumWindowMetrics?.bounds?.let { bounds ->
            DepthOverlay.windowSize = bounds.width() to bounds.height()
        }
        DepthOverlay.register(this)
    }

    override fun onDraw(canvas: Canvas) {
        if (DepthOverlay.inClock) return
        painter.draw(this, canvas, clipToSelf)
    }
}
