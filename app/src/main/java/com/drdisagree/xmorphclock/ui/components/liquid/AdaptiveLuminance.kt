package com.drdisagree.xmorphclock.ui.components.liquid

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.Animatable as ColorAnimatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.drdisagree.xmorphclock.ui.theme.LocalPalette
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import kotlin.math.sign
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AdaptiveLuminance(private val layer: GraphicsLayer, dark: Boolean) {

    private val luminance = Animatable(if (dark) 0f else 1f)
    private val content = ColorAnimatable(if (dark) Color.White else Color.Black)

    val contentColor: Color get() = content.value

    companion object {
        val supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    fun record(scope: DrawScope, drawBackdrop: DrawScope.() -> Unit) {
        with(scope) { layer.record { drawBackdrop() } }
    }

    fun apply(scope: BackdropEffectScope) = with(scope) {
        val l = (luminance.value * 2f - 1f).let { sign(it) * it * it }
        colorControls(
            brightness = if (l > 0f) lerp(0.1f, 0.5f, l) else lerp(0.1f, -0.2f, -l),
            contrast = if (l > 0f) lerp(1f, 0f, l) else 1f,
            saturation = NEUTRAL_SATURATION
        )
        blur(if (l > 0f) lerp(8.dp.toPx(), 16.dp.toPx(), l) else lerp(8.dp.toPx(), 2.dp.toPx(), -l))
    }

    suspend fun run() = coroutineScope {
        if (!supported) return@coroutineScope
        val buffer = IntArray(SAMPLE * SAMPLE)
        while (isActive) {
            val average = try {
                val thumbnail = Bitmap.createScaledBitmap(layer.toImageBitmap().asAndroidBitmap(), SAMPLE, SAMPLE, true)
                thumbnail.getPixels(buffer, 0, SAMPLE, 0, 0, SAMPLE, SAMPLE)
                buffer.sumOf { argb ->
                    val r = (argb shr 16 and 0xFF) / 255.0
                    val g = (argb shr 8 and 0xFF) / 255.0
                    val b = (argb and 0xFF) / 255.0
                    0.2126 * r + 0.7152 * g + 0.0722 * b
                }.toFloat() / buffer.size
            } catch (_: Throwable) {
                null
            }
            if (average == null) {
                delay(RETRY_MS)
                continue
            }
            val light = when {
                average > LIGHT_THRESHOLD -> true
                average < DARK_THRESHOLD -> false
                else -> content.targetValue == Color.Black
            }
            launch { content.animateTo(if (light) Color.Black else Color.White, tween(DURATION)) }
            luminance.animateTo(average, tween(DURATION))
            delay(INTERVAL_MS)
        }
    }
}

@Composable
fun rememberAdaptiveLuminance(): AdaptiveLuminance {
    val layer = rememberGraphicsLayer()
    val dark = LocalPalette.current.isDark
    val adaptive = remember(layer, dark) { AdaptiveLuminance(layer, dark) }
    LaunchedEffect(adaptive) { adaptive.run() }
    return adaptive
}

private const val SAMPLE = 5
private const val NEUTRAL_SATURATION = 1f
private const val RETRY_MS = 250L
private const val DURATION = 400
private const val INTERVAL_MS = 300L
private const val LIGHT_THRESHOLD = 0.55f
private const val DARK_THRESHOLD = 0.45f
