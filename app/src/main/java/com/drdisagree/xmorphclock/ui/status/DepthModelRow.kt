package com.drdisagree.xmorphclock.ui.status

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.DepthModelState
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.StatusGlyph
import com.drdisagree.xmorphclock.ui.components.StatusTone
import com.drdisagree.xmorphclock.ui.components.glass
import com.drdisagree.xmorphclock.ui.components.toneTextColor
import com.drdisagree.xmorphclock.ui.theme.OnInk

private val RowShape = RoundedCornerShape(28.dp)

@Composable
internal fun DepthModelRow(state: DepthModelState, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .glass(LocalBackdrop.current, RowShape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.status_depth_model),
            style = MaterialTheme.typography.titleMedium,
            color = OnInk,
            modifier = Modifier.weight(1f)
        )
        AnimatedVisibility(
            visible = toneOf(state) == StatusTone.BUSY,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(160))
        ) {
            StatusGlyph(StatusTone.BUSY, size = 14.dp)
        }
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
            label = "depthState"
        ) { current ->
            Text(stringResource(stateLabel(current)), style = MaterialTheme.typography.bodyMedium, color = toneTextColor(toneOf(current)), maxLines = 1)
        }
    }
}

private fun toneOf(state: DepthModelState): StatusTone = when (state) {
    DepthModelState.READY -> StatusTone.OK
    DepthModelState.CHECKING, DepthModelState.DOWNLOADING -> StatusTone.BUSY
    DepthModelState.MISSING -> StatusTone.IDLE
    DepthModelState.UNAVAILABLE -> StatusTone.WARNING
}

private fun stateLabel(state: DepthModelState): Int = when (state) {
    DepthModelState.CHECKING -> R.string.depth_model_checking
    DepthModelState.READY -> R.string.depth_model_ready
    DepthModelState.DOWNLOADING -> R.string.depth_model_downloading
    DepthModelState.MISSING -> R.string.depth_model_missing
    DepthModelState.UNAVAILABLE -> R.string.depth_model_unavailable
}
