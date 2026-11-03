package com.drdisagree.xmorphclock.ui.editor

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.StyleKey
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.FitText
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.fadingEdges
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidButton
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidSlider
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import com.drdisagree.xmorphclock.ui.theme.onTint
import java.util.Locale
import kotlin.math.roundToInt

private val TileShape = RoundedCornerShape(18.dp)

@Composable
fun EditorBlock(title: String, value: String? = null, trailing: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(top = 18.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = OnInkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
            if (value != null) {
                Text(
                    value,
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR),
                    color = OnInk,
                    maxLines = 1
                )
            }
        }
        content()
    }
}

fun sliderLabel(key: StyleKey, value: Float): String = when (key) {
    StyleKey.SIZE, StyleKey.OPACITY, StyleKey.EFFECT_TINT -> "${(value * 100).roundToInt()}%"
    StyleKey.WEIGHT, StyleKey.WIDTH, StyleKey.HEIGHT -> value.roundToInt().toString()
    StyleKey.ROTATION -> "${value.roundToInt()}°"
    StyleKey.TRACKING -> String.format(Locale.getDefault(), "%+.2f", value)
    StyleKey.LINE_HEIGHT, StyleKey.STRETCH, StyleKey.STRETCH_X -> String.format(Locale.getDefault(), "%.2f×", value)
    StyleKey.OUTLINE_WIDTH -> String.format(Locale.getDefault(), "%.1f", value)
}

private const val TABULAR = "tnum"

@Composable
fun TileRow(content: LazyListScope.() -> Unit) {
    val list = rememberLazyListState()
    LazyRow(
        state = list,
        modifier = Modifier.fadingEdges(list),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        content = content
    )
}

@Composable
fun RangeSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    start: (@Composable () -> Unit)? = null,
    end: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        start?.invoke()
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            LiquidSlider(value, onChange, range, LocalBackdrop.current)
        }
        end?.invoke()
    }
}

@Composable
fun OptionTile(
    selected: Boolean,
    onClick: () -> Unit,
    width: Dp = 76.dp,
    height: Dp = 76.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val fill by animateColorAsState(
        if (selected) Accent.copy(alpha = 0.2f) else OnInk.copy(alpha = 0.07f),
        tween(TILE_FADE),
        label = "tile"
    )
    val outline by animateColorAsState(if (selected) Accent else Color.Transparent, tween(TILE_FADE), label = "tileOutline")
    Box(
        Modifier
            .size(width, height)
            .clip(TileShape)
            .drawBehind { drawRect(fill) }
            .border(TILE_OUTLINE, outline, TileShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun TextTile(text: String, selected: Boolean, onClick: () -> Unit, width: Dp = 104.dp) {
    val color by animateColorAsState(if (selected) Accent else OnInk, tween(TILE_FADE), label = "tileText")
    OptionTile(selected, onClick, width = width, height = 48.dp) {
        FitText(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

@Composable
fun Swatch(color: Color, selected: Boolean, dynamic: Boolean = false, onClick: () -> Unit) {
    val ring by animateColorAsState(if (selected) Accent else Color.Transparent, tween(TILE_FADE), label = "ring")
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .drawBehind { drawRect(ring) }
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (dynamic) {
            Box(
                Modifier
                    .size(DYNAMIC_DOT)
                    .clip(CircleShape)
                    .background(if (color.luminance() > DOT_CONTRAST) Color.Black.copy(alpha = DOT_ALPHA) else Color.White.copy(alpha = DOT_ALPHA))
            )
        }
    }
}

@Composable
fun StateSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .padding(end = 8.dp)
            .clip(CircleShape)
            .background(OnInk.copy(alpha = SWITCH_TRACK))
            .padding(2.dp)
    ) {
        listOf(false to R.string.state_off, true to R.string.state_on).forEach { (value, label) ->
            val fill by animateColorAsState(if (on == value) OnInk.copy(alpha = SWITCH_SELECTED) else Color.Transparent, tween(TILE_FADE), label = "stateFill")
            val text by animateColorAsState(if (on == value) OnInk else OnInkMuted, tween(TILE_FADE), label = "stateText")
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(fill)
                    .clickable(role = Role.Tab) { onChange(value) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = text, maxLines = 1)
            }
        }
    }
}

private val DYNAMIC_DOT = 8.dp
private const val DOT_CONTRAST = 0.5f
private const val DOT_ALPHA = 0.55f
private const val SWITCH_TRACK = 0.08f
private const val SWITCH_SELECTED = 0.16f

@Composable
fun CustomSwatch(colors: List<Color>?, onClick: () -> Unit) {
    val ring by animateColorAsState(if (colors != null) Accent else Color.Transparent, tween(TILE_FADE), label = "customRing")
    val description = stringResource(R.string.color_custom)
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .drawBehind { drawRect(ring) }
            .padding(4.dp)
            .clip(CircleShape)
            .drawBehind {
                drawCircle(Brush.sweepGradient(CustomHues))
                if (colors != null) {
                    val radius = size.minDimension * CUSTOM_CENTER
                    if (colors.size > 1) drawCircle(Brush.linearGradient(colors), radius) else drawCircle(colors.first(), radius)
                    drawCircle(Color.White, radius, style = Stroke(CUSTOM_RIM.toPx()))
                } else {
                    drawCircle(Color.White, size.minDimension * 0.18f)
                }
            }
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
    )
}

@Composable
fun ActionRow(text: String, color: Color, @DrawableRes icon: Int = R.drawable.ic_reset, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidButton(onClick, LocalBackdrop.current, tint = color, height = 44.dp, horizontalPadding = 20.dp) {
            AppIcon(icon, tint = onTint(color), size = 18.dp)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = onTint(color))
        }
    }
}

private const val TILE_FADE = 220
private val TILE_OUTLINE = 1.5.dp
private val CustomHues = List(7) { Color.hsv(it * 60f % 360f, 0.85f, 1f) }
private const val CUSTOM_CENTER = 0.3f
private val CUSTOM_RIM = 1.5.dp
