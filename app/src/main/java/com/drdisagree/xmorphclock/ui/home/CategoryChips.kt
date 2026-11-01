package com.drdisagree.xmorphclock.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.ui.components.fadingEdges
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import com.drdisagree.xmorphclock.ui.theme.onTint

@Composable
fun CategoryChips(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val list = rememberLazyListState()
    LaunchedEffect(selected) {
        if (selected in labels.indices) list.animateScrollToItem((selected - 1).coerceAtLeast(0))
    }
    LazyRow(
        state = list,
        modifier = modifier.fadingEdges(list, Orientation.Horizontal),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(labels) { index, label ->
            Chip(label, index == selected) { onSelect(index) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val fill by animateColorAsState(if (selected) Accent.copy(alpha = SELECTED_FILL) else Color.Transparent, tween(CHIP_MS), label = "chipFill")
    val text by animateColorAsState(if (selected) onTint(Accent) else OnInkMuted, tween(CHIP_MS), label = "chipText")
    Box(
        Modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(fill)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = text, maxLines = 1)
    }
}

private const val SELECTED_FILL = 0.32f
private const val CHIP_MS = 240
