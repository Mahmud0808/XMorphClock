package com.drdisagree.xmorphclock.ui.editor

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.theme.OnInk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun StepButton(@DrawableRes icon: Int, description: String, onStep: () -> Unit) {
    val step by rememberUpdatedState(onStep)
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) PRESSED_SCALE else 1f, tween(PRESS_MS), label = "stepScale")
    Box(
        Modifier
            .size(STEP_SIZE)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(OnInk.copy(alpha = STEP_FILL))
            .semantics {
                contentDescription = description
                role = Role.Button
                onClick {
                    step()
                    true
                }
            }
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown()
                        pressed = true
                        step()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val repeat = launch {
                            delay(REPEAT_DELAY)
                            while (isActive) {
                                step()
                                delay(REPEAT_INTERVAL)
                            }
                        }
                        waitForUpOrCancellation()
                        repeat.cancel()
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AppIcon(icon, tint = OnInk, size = ICON_SIZE)
    }
}

private val STEP_SIZE = 34.dp
private val ICON_SIZE = 16.dp
private const val STEP_FILL = 0.1f
private const val PRESSED_SCALE = 0.88f
private const val PRESS_MS = 120
private const val REPEAT_DELAY = 400L
private const val REPEAT_INTERVAL = 50L
