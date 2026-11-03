package com.drdisagree.xmorphclock.ui.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.ui.components.GlassContainer
import com.drdisagree.xmorphclock.ui.components.GlassIconButton
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidButton
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.onTint

@Composable
fun GroupingBar(count: Int, onCancel: () -> Unit, onGroup: () -> Unit, modifier: Modifier = Modifier) {
    val ready = count >= MIN_ITEMS
    val alpha by animateFloatAsState(if (ready) 1f else DISABLED_ALPHA, tween(200), label = "groupReady")
    Box(
        modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        GlassIconButton(
            R.drawable.ic_cross,
            onClick = onCancel,
            description = stringResource(R.string.action_cancel),
            modifier = Modifier.align(Alignment.CenterStart)
        )
        GlassContainer(shape = PillShape, modifier = Modifier.align(Alignment.Center)) {
            Text(
                stringResource(R.string.grouping_hint),
                style = MaterialTheme.typography.labelLarge,
                color = OnInk,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        LiquidButton(
            onClick = { if (ready) onGroup() },
            backdrop = LocalBackdrop.current,
            tint = Accent,
            height = 44.dp,
            horizontalPadding = 16.dp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .graphicsLayer { this.alpha = alpha }
        ) {
            Text(stringResource(R.string.action_group_count, count), style = MaterialTheme.typography.labelLarge, color = onTint(Accent))
        }
    }
}

private val PillShape = RoundedCornerShape(50)
private const val MIN_ITEMS = 2
private const val DISABLED_ALPHA = 0.45f
