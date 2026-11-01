package com.drdisagree.xmorphclock.ui.components

import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.ui.theme.DarkPalette
import com.drdisagree.xmorphclock.ui.theme.Hairline
import com.drdisagree.xmorphclock.ui.theme.Ink
import com.drdisagree.xmorphclock.ui.theme.LocalPalette
import com.drdisagree.xmorphclock.ui.theme.SurfaceHigh
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun LockscreenPreview(
    face: ClockFace?,
    modifier: Modifier = Modifier,
    largeProgress: () -> Float = { 1f },
    sleepProgress: () -> Float = { 0f },
    cornerRadius: Dp = 32.dp,
    showWallpaper: Boolean = true,
    onExtents: ((Map<String, RectF>) -> Unit)? = null,
    animateChanges: Boolean = true,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val wallpaper = if (showWallpaper) LocalWallpaper.current else null
    val depth = if (showWallpaper) LocalDepth.current else null
    val geometry = LocalClockGeometry.current
    val layer = rememberLayerBackdrop()
    val density = LocalDensity.current
    val reporter = remember { ExtentsReporter() }
    reporter.onChange = onExtents
    DisposableEffect(reporter) { onDispose { reporter.detach() } }
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Ink)
            .border(OUTLINE_WIDTH, Hairline, RoundedCornerShape(cornerRadius))
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        reporter.origin = {
            val frame = geometry.frame(largeProgress())
            Offset(widthPx * frame.left, heightPx * frame.top)
        }
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(layer)
                .drawWithContent {
                    drawContent()
                    drawRect(Color.Black, alpha = sleepProgress())
                }
        ) {
            when {
                wallpaper != null -> Image(wallpaper, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                showWallpaper -> Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1C2333), Ink)))
                )

                else -> Box(
                    Modifier
                        .fillMaxSize()
                        .background(SurfaceHigh)
                )
            }
        }
        if (face != null) {
            val bitmap = wallpaper?.asAndroidBitmap()
            ClockFacePreview(
                face = face,
                modifier = Modifier.clockFrame(geometry, largeProgress),
                largeProgress = largeProgress,
                sleepProgress = sleepProgress,
                aodProgress = sleepProgress,
                fit = false,
                configure = { view ->
                    view.morphOnChange = animateChanges
                    if (onExtents != null) reporter.attach(view)
                    if (view.tag !== bitmap) {
                        view.tag = bitmap
                        view.setBackdrop(bitmap)
                    }
                    view.setDepth(depth?.bitmap, depth?.opacity ?: 0f)
                    if (bitmap != null) {
                        view.setBackdropFrame(backdropFrame(bitmap, widthPx, heightPx, geometry, largeProgress()))
                    }
                }
            )
        }
        CompositionLocalProvider(LocalBackdrop provides layer, LocalPalette provides DarkPalette) {
            overlay()
        }
    }
}

private val OUTLINE_WIDTH = 1.dp
