package com.drdisagree.xmorphclock.xposed.clock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.view.WindowManager
import com.drdisagree.xmorphclock.xposed.signal.ClockSignals
import kotlin.math.max
import kotlin.math.roundToInt

internal class DepthLayer(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val frame = RectF()
    private val location = IntArray(2)
    private val area = Rect()
    private val matrix = ColorMatrix()
    private var filterKey = Long.MIN_VALUE

    var clipToSelf = false

    init {
        context.getSystemService(WindowManager::class.java)?.maximumWindowMetrics?.bounds?.let { bounds ->
            DepthOverlay.windowSize = bounds.width() to bounds.height()
        }
        DepthOverlay.register(this)
    }

    override fun onDraw(canvas: Canvas) {
        val bitmap = DepthOverlay.subject ?: return
        val asleep = max(ClockSignals.aodProgress, ClockSignals.sleepProgress)
        val alpha = DepthOverlay.opacity * (1f - asleep / FADE).coerceIn(0f, 1f) * ScrimTracker.fade()
        if (alpha <= 0f) return
        val (screenWidth, screenHeight) = DepthOverlay.windowSize
        if (screenWidth <= 0 || screenHeight <= 0) return
        getLocationOnScreen(location)
        area.set(location[0], location[1], location[0] + width, location[1] + height)
        val scale = WallpaperCapture.scale
        val scaledWidth = screenWidth * scale
        val scaledHeight = screenHeight * scale
        val left = (screenWidth - scaledWidth) / 2f - location[0] + WallpaperCapture.shiftX * scale
        val top = (screenHeight - scaledHeight) / 2f - location[1] + WallpaperCapture.shiftY * scale
        frame.set(left, top, left + scaledWidth, top + scaledHeight)
        paint.alpha = (alpha * OPAQUE).roundToInt()
        updateFilter()
        if (clipToSelf) canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(bitmap, null, frame, paint)
    }

    private fun updateFilter() {
        val dim = ScrimTracker.wallpaperDim.coerceIn(0f, 1f)
        val scrim = ScrimTracker.dimOver(area)
        val tint = ScrimTracker.dimTint()
        val key = ((dim * STEPS).roundToInt().toLong() shl 40) or ((scrim * STEPS).roundToInt().toLong() shl 32) or (tint.toLong() and LOW_BITS)
        if (key == filterKey) return
        filterKey = key
        if (dim == 0f && scrim == 0f) {
            paint.colorFilter = null
            return
        }
        val keep = (1f - dim) * (1f - scrim)
        matrix.set(
            floatArrayOf(
                keep, 0f, 0f, 0f, Color.red(tint) * scrim,
                0f, keep, 0f, 0f, Color.green(tint) * scrim,
                0f, 0f, keep, 0f, Color.blue(tint) * scrim,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(matrix)
    }

    private companion object {
        const val FADE = 0.25f
        const val OPAQUE = 255f
        const val STEPS = 255f
        const val LOW_BITS = 0xFFFFFFFFL
    }
}
