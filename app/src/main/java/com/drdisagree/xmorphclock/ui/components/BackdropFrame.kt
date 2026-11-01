package com.drdisagree.xmorphclock.ui.components

import android.graphics.Bitmap
import android.graphics.RectF
import com.drdisagree.xmorphclock.domain.model.ClockGeometry
import kotlin.math.max

fun backdropFrame(bitmap: Bitmap, width: Float, height: Float, geometry: ClockGeometry, largeProgress: Float): RectF {
    val frame = geometry.frame(largeProgress)
    val scale = max(width / bitmap.width, height / bitmap.height)
    val w = bitmap.width * scale
    val h = bitmap.height * scale
    val left = (width - w) / 2f - frame.left * width
    val top = (height - h) / 2f - frame.top * height
    return RectF(left, top, left + w, top + h)
}
