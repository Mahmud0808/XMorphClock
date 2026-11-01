package com.drdisagree.xmorphclock.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.ui.theme.Ink
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect

@Composable
fun ProgressiveBlurBar(
    height: Dp,
    exported: LayerBackdrop,
    modifier: Modifier = Modifier,
    progress: () -> Float = { 1f }
) {
    val tint = Ink
    val base = modifier
        .fillMaxWidth()
        .height(height)
        .graphicsLayer { alpha = progress().coerceIn(0f, 1f) }
    val masked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        base
    } else {
        base
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val strength = progress().coerceIn(0f, 1f)
                drawRect(tint.copy(alpha = tint.alpha * TINT_INTENSITY * strength))
                drawRect(
                    Brush.verticalGradient(*FADE_STOPS),
                    blendMode = BlendMode.DstIn
                )
            }
    }
    Box(
        masked.drawPlainBackdrop(
            backdrop = LocalBackdrop.current,
            shape = { RectangleShape },
            exportedBackdrop = exported,
            effects = {
                if (GlassSupport.full) {
                    blur(BLUR_RADIUS.toPx() * progress().coerceIn(0f, 1f))
                    runtimeShaderEffect(SHADER_KEY, MASK_SHADER, "content") {
                        setFloatUniform("size", size.width, size.height)
                        setFloatUniform("tint", tint.red, tint.green, tint.blue, tint.alpha)
                        setFloatUniform("tintIntensity", TINT_INTENSITY * progress().coerceIn(0f, 1f))
                    }
                }
            }
        )
    )
}

private val BLUR_RADIUS = 28.dp
private val FADE_STOPS = arrayOf(
    0f to Color.Black,
    0.4f to Color.Black,
    0.51f to Color.Black.copy(alpha = 0.84f),
    0.625f to Color.Black.copy(alpha = 0.5f),
    0.74f to Color.Black.copy(alpha = 0.16f),
    0.85f to Color.Transparent
)
private const val TINT_INTENSITY = 0.4f
private const val SHADER_KEY = "XMorphProgressiveBlur"
private const val MASK_SHADER = """
uniform shader content;
uniform float2 size;
uniform float4 tint;
uniform float tintIntensity;

half4 main(float2 coord) {
    float alpha = smoothstep(size.y, size.y * 0.55, coord.y);
    half4 blurred = content.eval(coord) * alpha;
    half4 tinted = half4(tint.rgb * tint.a, tint.a) * alpha;
    return mix(blurred, tinted, tintIntensity);
}"""
