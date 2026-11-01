package com.drdisagree.xmorphclock.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.ui.theme.OnInk

@Composable
fun StatusPill(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(32.dp)
            .glass(LocalBackdrop.current, CircleShape)
            .padding(start = 10.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusGlyph(tone, size = 14.dp)
        Text(text, style = MaterialTheme.typography.labelMedium, color = OnInk)
    }
}
