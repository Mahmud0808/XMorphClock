package com.drdisagree.xmorphclock.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.drdisagree.xmorphclock.ui.theme.Ink

@Composable
fun LockscreenBackground(wallpaper: ImageBitmap?, modifier: Modifier = Modifier) {
    Box(modifier.drawWithContent {
        drawContent()
        drawRect(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.2f),
                0.5f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.55f)
            )
        )
    }) {
        Crossfade(wallpaper, animationSpec = tween(600), label = "wallpaper") { bitmap ->
            if (bitmap != null) {
                Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1B2030), Ink)))
                )
            }
        }
    }
}
