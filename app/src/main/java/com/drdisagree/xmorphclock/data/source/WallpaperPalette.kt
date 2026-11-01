package com.drdisagree.xmorphclock.data.source

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.Score
import com.google.android.material.color.utilities.TonalPalette
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

@SuppressLint("RestrictedApi")
object WallpaperPalette {

    fun extract(bitmap: Bitmap): Map<String, Int> {
        val pixels = pixelsOf(bitmap)
        val quantized = QuantizerCelebi.quantize(pixels, MAX_COLORS)
        val source = Score.score(quantized).firstOrNull() ?: return emptyMap()
        val hct = Hct.fromInt(source)
        val palettes = mapOf(
            ACCENT + 1 to TonalPalette.fromHueAndChroma(hct.hue, PRIMARY_CHROMA),
            ACCENT + 2 to TonalPalette.fromHueAndChroma(hct.hue, SECONDARY_CHROMA),
            ACCENT + 3 to TonalPalette.fromHueAndChroma((hct.hue + TERTIARY_SHIFT) % FULL_TURN, TERTIARY_CHROMA),
            NEUTRAL + 1 to TonalPalette.fromHueAndChroma(hct.hue, NEUTRAL_CHROMA),
            NEUTRAL + 2 to TonalPalette.fromHueAndChroma(hct.hue, NEUTRAL_VARIANT_CHROMA)
        )
        return buildMap {
            palettes.forEach { (name, palette) ->
                SHADES.forEach { (shade, tone) -> put("${name}_$shade", palette.tone(tone)) }
            }
        }
    }

    private fun pixelsOf(bitmap: Bitmap): IntArray {
        val source = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        val area = source.width * source.height
        val scaled = if (area > SAMPLE_AREA) {
            val ratio = sqrt(SAMPLE_AREA.toDouble() / area)
            source.scale(max(1, (source.width * ratio).roundToInt()), max(1, (source.height * ratio).roundToInt()))
        } else {
            source
        }
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        return pixels
    }

    private const val ACCENT = "accent"
    private const val NEUTRAL = "neutral"
    private const val MAX_COLORS = 128
    private const val SAMPLE_AREA = 112 * 112
    private const val PRIMARY_CHROMA = 36.0
    private const val SECONDARY_CHROMA = 16.0
    private const val TERTIARY_CHROMA = 24.0
    private const val NEUTRAL_CHROMA = 6.0
    private const val NEUTRAL_VARIANT_CHROMA = 8.0
    private const val TERTIARY_SHIFT = 60.0
    private const val FULL_TURN = 360.0
    private val SHADES = listOf(
        0 to 100, 10 to 99, 50 to 95, 100 to 90, 200 to 80, 300 to 70, 400 to 60,
        500 to 50, 600 to 40, 700 to 30, 800 to 20, 900 to 10, 1000 to 0
    )
}
