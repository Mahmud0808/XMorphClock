package com.drdisagree.xmorphclock.data.source

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.graphics.createBitmap
import kotlin.math.roundToInt

object FrameZoom {

    fun zoomed(bitmap: Bitmap, scale: Float): Bitmap {
        if (scale <= 1f) return bitmap
        val width = (bitmap.width / scale).roundToInt()
        val height = (bitmap.height / scale).roundToInt()
        val left = (bitmap.width - width) / 2
        val top = (bitmap.height - height) / 2
        val config = bitmap.config ?: Bitmap.Config.ARGB_8888
        val output = createBitmap(bitmap.width, bitmap.height, if (config == Bitmap.Config.HARDWARE) Bitmap.Config.ARGB_8888 else config)
        Canvas(output).drawBitmap(bitmap, Rect(left, top, left + width, top + height), Rect(0, 0, bitmap.width, bitmap.height), Paint(Paint.FILTER_BITMAP_FLAG))
        return output
    }
}
