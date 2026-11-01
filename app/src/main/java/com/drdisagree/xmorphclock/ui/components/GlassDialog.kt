package com.drdisagree.xmorphclock.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.ui.theme.FlatSurface
import com.drdisagree.xmorphclock.ui.theme.LocalPalette
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val DialogShape = RoundedCornerShape(48.dp)

@Composable
fun GlassDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit
) {
    GlassDialogFrame(visible, onDismiss) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = OnInk,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 8.dp)
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = OnInkMuted,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
        )
        DialogButton(
            confirmLabel,
            primary = true,
            onClick = onDismiss,
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
fun GlassDialogFrame(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val dark = LocalPalette.current.isDark
    val container = if (dark) Color(0xFF121212).copy(alpha = 0.6f) else Color(0xFFFAFAFA).copy(alpha = 0.78f)
    val dim = if (dark) Color(0xFF121212).copy(alpha = 0.56f) else Color(0xFF29293A).copy(alpha = 0.23f)
    val flat = FlatSurface
    val scrim by animateFloatAsState(if (visible) 1f else 0f, tween(260), label = "scrim")
    BackHandler(enabled = visible, onBack = onDismiss)
    if (scrim > 0f) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(dim.copy(alpha = dim.alpha * scrim)) }
                .then(if (visible) Modifier.clickable(interactionSource = null, indication = null, onClick = onDismiss) else Modifier)
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + scaleIn(tween(360, easing = Emphasized), initialScale = 0.9f),
        exit = fadeOut(tween(160)) + scaleOut(tween(220), targetScale = 0.94f),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .padding(32.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = LocalBackdrop.current,
                        shape = { DialogShape },
                        effects = {
                            if (GlassSupport.full) {
                                colorControls(brightness = if (dark) 0f else 0.2f, saturation = 1.5f)
                                blur(if (dark) 8.dp.toPx() else 16.dp.toPx())
                                lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true)
                            }
                        },
                        highlight = { Highlight.Default.copy(alpha = RIM_ALPHA) },
                        onDrawSurface = {
                            if (!GlassSupport.full) drawRect(flat)
                            drawRect(container)
                        }
                    )
                    .clickable(interactionSource = null, indication = null) {},
                content = content
            )
        }
    }
}

private const val RIM_ALPHA = 0.5f
