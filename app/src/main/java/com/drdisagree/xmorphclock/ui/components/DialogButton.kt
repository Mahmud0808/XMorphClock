package com.drdisagree.xmorphclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInk

private val ButtonShape = RoundedCornerShape(50)

@Composable
fun DialogButton(label: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(48.dp)
            .clip(ButtonShape)
            .background(if (primary) Accent else OnInk.copy(alpha = SECONDARY_ALPHA))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        FitText(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (primary) Color.White else OnInk
        )
    }
}

private const val SECONDARY_ALPHA = 0.1f
