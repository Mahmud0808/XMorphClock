package com.drdisagree.xmorphclock.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

fun sharedAxisX(forward: Boolean, slideDistance: Int): ContentTransform {
    val direction = if (forward) 1 else -1
    val enter = slideInHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { direction * slideDistance } +
            fadeIn(tween(FADE_IN, delayMillis = FADE_OUT, easing = LinearOutSlowInEasing))
    val exit = slideOutHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { -direction * slideDistance } +
            fadeOut(tween(FADE_OUT, easing = FastOutLinearInEasing))
    return enter togetherWith exit
}

private const val DURATION = 300
private const val FADE_OUT = 90
private const val FADE_IN = DURATION - FADE_OUT
