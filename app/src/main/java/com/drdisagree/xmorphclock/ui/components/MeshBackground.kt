package com.drdisagree.xmorphclock.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import com.drdisagree.xmorphclock.ui.theme.LocalPalette

@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ShaderMesh(modifier) else GradientMesh(modifier)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderMesh(modifier: Modifier) {
    val palette = LocalPalette.current
    val colors = palette.mesh
    val strength = if (palette.isDark) DARK_STRENGTH else LIGHT_STRENGTH
    val shader = remember { RuntimeShader(GLOW_SHADER) }
    Canvas(modifier) {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("strength", strength)
        colors.forEachIndexed { index, color ->
            shader.setFloatUniform("c$index", color.red, color.green, color.blue, 1f)
        }
        drawRect(ShaderBrush(shader))
    }
}

@Composable
private fun GradientMesh(modifier: Modifier) {
    val palette = LocalPalette.current
    val colors = palette.mesh
    val strength = if (palette.isDark) DARK_STRENGTH else LIGHT_STRENGTH
    Canvas(modifier) {
        drawRect(colors[0])
        GLOWS.forEachIndexed { index, glow ->
            val color = colors.getOrNull(index + 1) ?: return@forEachIndexed
            drawRect(
                Brush.radialGradient(
                    listOf(color.copy(alpha = strength * glow[3]), color.copy(alpha = 0f)),
                    center = Offset(glow[0] * size.width, glow[1] * size.height),
                    radius = glow[2] * size.width * GRADIENT_REACH
                )
            )
        }
    }
}

private const val DARK_STRENGTH = 0.85f
private const val GRADIENT_REACH = 1.6f
private val GLOWS = listOf(
    floatArrayOf(0.08f, 0.05f, 0.8f, 1f),
    floatArrayOf(1f, 0.58f, 0.85f, 0.8f),
    floatArrayOf(0.25f, 1.05f, 0.75f, 0.7f)
)
private const val LIGHT_STRENGTH = 0.9f

private const val GLOW_SHADER = """
uniform float2 size;
uniform float strength;
uniform float4 c0;
uniform float4 c1;
uniform float4 c2;
uniform float4 c3;

float glow(float2 p, float2 center, float radius) {
    float d = distance(p, center) / radius;
    return exp(-d * d);
}

half4 main(float2 coord) {
    float2 p = coord / size.x;
    float h = size.y / size.x;
    float3 color = c0.rgb;
    color = mix(color, c1.rgb, glow(p, float2(0.08, 0.1), 0.8) * strength);
    color = mix(color, c2.rgb, glow(p, float2(1.0, h * 0.58), 0.85) * strength * 0.8);
    color = mix(color, c3.rgb, glow(p, float2(0.25, h * 1.05), 0.75) * strength * 0.7);
    float noise = fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453);
    color += (noise - 0.5) / 128.0;
    return half4(color, 1.0);
}"""
