package com.drdisagree.xmorphclock.ui.editor

import android.graphics.Color as AndroidColor

internal data class Hsv(val hue: Float, val saturation: Float, val value: Float) {

    val argb: Int
        get() = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))

    companion object {
        fun of(color: Int): Hsv {
            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(color, hsv)
            return Hsv(hsv[0], hsv[1], hsv[2])
        }
    }
}
