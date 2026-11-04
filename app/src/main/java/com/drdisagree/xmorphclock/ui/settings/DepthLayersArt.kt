package com.drdisagree.xmorphclock.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted

@Composable
internal fun DepthLayersArt(enabled: Boolean, widgetsFront: Boolean, opacity: Float, modifier: Modifier = Modifier) {
    val spec = tween<Float>(LAYER_MS, easing = FastOutSlowInEasing)
    val front = widgetsFront && enabled
    val widgetsSlot by animateFloatAsState(if (front) 3f else 2f, spec, label = "widgetsSlot")
    val subjectSlot by animateFloatAsState(if (front) 2f else 3f, spec, label = "subjectSlot")
    val subjectAlpha by animateFloatAsState(if (enabled) opacity.coerceIn(MIN_SUBJECT, 1f) else OFF_SUBJECT, spec, label = "subjectAlpha")
    val glow by animateFloatAsState(if (enabled) 1f else 0f, spec, label = "subjectGlow")
    val accent = Accent
    val subjectLabel by animateColorAsState(if (enabled) accent else OnInkMuted, tween(LAYER_MS), label = "subjectLabel")
    Box(
        modifier
            .fillMaxWidth()
            .height(SLOT_STEP * (LAYERS - 1) + PLATE_HEIGHT)
    ) {
        Plate(0f, stringResource(R.string.depth_layer_wallpaper), OnInkMuted) { WallpaperScene() }
        Plate(1f, stringResource(R.string.depth_layer_clock), OnInkMuted) {
            Text(
                CLOCK_SAMPLE,
                style = ClockStyle,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 10.dp)
            )
        }
        Plate(subjectSlot, stringResource(R.string.depth_layer_subject), subjectLabel) {
            SubjectFigure(subjectAlpha, glow)
        }
        Plate(widgetsSlot, stringResource(R.string.depth_layer_widgets), OnInkMuted) { WidgetPills() }
    }
}

@Composable
private fun BoxScope.Plate(slot: Float, label: String, labelColor: Color, content: @Composable BoxScope.() -> Unit) {
    val density = LocalDensity.current
    val step = with(density) { SLOT_STEP.toPx() }
    val shadow = with(density) { PLATE_SHADOW.toPx() }
    val lift = (LAYERS - 1 - slot) * step
    val shape = RoundedCornerShape(PLATE_RADIUS)
    Box(
        Modifier
            .zIndex(slot)
            .align(Alignment.TopCenter)
            .offset(x = -LABEL_SPACE / 2)
            .size(PLATE_WIDTH, PLATE_HEIGHT)
            .graphicsLayer {
                translationY = lift
                rotationX = TILT
                rotationZ = TWIST
                cameraDistance = CAMERA * this.density
                shadowElevation = shadow
                this.shape = shape
                clip = true
                spotShadowColor = Color.Black
                ambientShadowColor = Color.Black
            }
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = GLASS_TOP), Color.White.copy(alpha = GLASS_BOTTOM))))
            .background(PlateBase)
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = RIM_BRIGHT), Color.White.copy(alpha = RIM_DIM))), shape),
        content = content
    )
    Row(
        Modifier
            .zIndex(LAYERS + slot)
            .align(Alignment.TopCenter)
            .offset(x = PLATE_WIDTH / 2 + LABEL_GAP + LEADER / 2, y = PLATE_HEIGHT / 2 - LABEL_HALF)
            .width(LABEL_SPACE + LEADER)
            .graphicsLayer { translationY = lift },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(Modifier.size(LEADER, 6.dp)) {
            val y = size.height / 2f
            drawLine(labelColor.copy(alpha = LEADER_ALPHA), Offset(0f, y), Offset(size.width - size.height, y), strokeWidth = 1.dp.toPx())
            drawCircle(labelColor, size.height / 2f, Offset(size.width - size.height / 2f, y))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = labelColor, maxLines = 1)
    }
}

@Composable
private fun WallpaperScene() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(SKY_TOP, SKY_BOTTOM)))
        drawCircle(Brush.radialGradient(listOf(SUN.copy(alpha = 0.9f), SUN.copy(alpha = 0f)), Offset(size.width * 0.72f, size.height * 0.3f), size.height * 0.45f), size.height * 0.45f, Offset(size.width * 0.72f, size.height * 0.3f))
        val far = Path().apply {
            moveTo(0f, size.height * 0.72f)
            cubicTo(size.width * 0.2f, size.height * 0.48f, size.width * 0.36f, size.height * 0.62f, size.width * 0.52f, size.height * 0.5f)
            cubicTo(size.width * 0.7f, size.height * 0.38f, size.width * 0.86f, size.height * 0.6f, size.width, size.height * 0.52f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(far, Brush.verticalGradient(listOf(HILL_FAR, HILL_FAR.copy(alpha = 0.6f)), startY = size.height * 0.4f, endY = size.height))
        val near = Path().apply {
            moveTo(0f, size.height * 0.86f)
            cubicTo(size.width * 0.3f, size.height * 0.7f, size.width * 0.6f, size.height * 0.92f, size.width, size.height * 0.76f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(near, HILL_NEAR)
    }
}

@Composable
private fun SubjectFigure(alpha: Float, glow: Float) {
    val accent = Accent
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
    ) {
        val head = size.height * 0.15f
        val center = Offset(size.width * 0.5f, size.height * 0.4f)
        if (glow > 0f) {
            drawCircle(
                Brush.radialGradient(listOf(accent.copy(alpha = GLOW_ALPHA * glow), accent.copy(alpha = 0f)), center, size.height * 0.62f),
                size.height * 0.62f,
                center
            )
        }
        val body = Brush.verticalGradient(listOf(Color.White, FIGURE_SHADE), startY = center.y - head, endY = size.height)
        drawCircle(body, head, center)
        drawRoundRect(
            body,
            topLeft = Offset(center.x - head * 2f, center.y + head * 1.3f),
            size = Size(head * 4f, size.height),
            cornerRadius = CornerRadius(head * 1.8f)
        )
    }
}

@Composable
private fun BoxScope.WidgetPills() {
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .offset(y = (-18).dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Pill(58.dp, true)
        Pill(44.dp, false)
    }
}

@Composable
private fun Pill(width: Dp, dot: Boolean) {
    val accent = Accent
    Box(
        Modifier
            .size(width, PILL_HEIGHT)
            .background(Color.White.copy(alpha = PILL_FILL), RoundedCornerShape(PILL_HEIGHT / 2))
            .border(1.dp, Color.White.copy(alpha = PILL_RIM), RoundedCornerShape(PILL_HEIGHT / 2))
            .drawBehind {
                val r = size.height * 0.22f
                if (dot) drawCircle(accent, r, Offset(size.height / 2f, size.height / 2f))
                val start = if (dot) size.height else size.height * 0.45f
                drawRoundRect(
                    Color.White.copy(alpha = 0.75f),
                    Offset(start, size.height * 0.4f),
                    Size(size.width - start - size.height * 0.45f, size.height * 0.2f),
                    CornerRadius(size.height * 0.1f)
                )
            }
    )
}

private val ClockStyle = TextStyle(
    fontFamily = FontFamily(
        Font(
            R.font.morph_roboto_flex,
            FontWeight(CLOCK_WEIGHT),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(CLOCK_WEIGHT),
                FontVariation.width(CLOCK_WIDTH),
                FontVariation.Setting("YTFI", CLOCK_FIGURE),
                FontVariation.Setting("opsz", CLOCK_OPSZ)
            )
        )
    ),
    fontSize = 44.sp,
    letterSpacing = (-1).sp
)

private val PlateBase = Color(0x14000000)
private val SKY_TOP = Color(0xFF1E5E8C)
private val SKY_BOTTOM = Color(0xFF2BB3A3)
private val SUN = Color(0xFFFFD9A0)
private val HILL_FAR = Color(0xFF0E3B4F)
private val HILL_NEAR = Color(0xFF0A2533)
private val FIGURE_SHADE = Color(0xFFB8C7D6)
private const val CLOCK_SAMPLE = "12:30"
private const val CLOCK_WEIGHT = 250
private const val CLOCK_WIDTH = 60f
private const val CLOCK_FIGURE = 788f
private const val CLOCK_OPSZ = 144f
private const val LAYER_MS = 520
private const val LAYERS = 4
private const val TILT = 56f
private const val TWIST = -8f
private const val CAMERA = 14f
private const val GLASS_TOP = 0.16f
private const val GLASS_BOTTOM = 0.04f
private const val RIM_BRIGHT = 0.55f
private const val RIM_DIM = 0.06f
private const val MIN_SUBJECT = 0.1f
private const val OFF_SUBJECT = 0.18f
private const val GLOW_ALPHA = 0.45f
private const val LEADER_ALPHA = 0.4f
private const val PILL_FILL = 0.14f
private const val PILL_RIM = 0.3f
private val PILL_HEIGHT = 18.dp
private val PLATE_WIDTH = 176.dp
private val PLATE_HEIGHT = 112.dp
private val PLATE_RADIUS = 20.dp
private val PLATE_SHADOW = 18.dp
private val SLOT_STEP = 54.dp
private val LABEL_SPACE = 84.dp
private val LABEL_GAP = 6.dp
private val LABEL_HALF = 10.dp
private val LEADER = 26.dp
