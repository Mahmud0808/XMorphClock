package com.drdisagree.xmorphclock.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.ThemeMode
import com.drdisagree.xmorphclock.ui.components.FitText
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTab
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTabs
import com.drdisagree.xmorphclock.ui.theme.OnInk

@Composable
internal fun ThemeCard(mode: ThemeMode, onModeChange: (ThemeMode) -> Unit) {
    GlassCard {
        Text(
            stringResource(R.string.section_appearance),
            style = MaterialTheme.typography.titleSmall,
            color = OnInk,
            modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 12.dp)
        )
        LiquidTabs(
            selectedIndex = mode.ordinal,
            onSelected = { onModeChange(ThemeMode.entries[it]) },
            backdrop = LocalBackdrop.current,
            tabsCount = ThemeMode.entries.size,
            height = 44.dp,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
        ) {
            ThemeMode.entries.forEach { entry ->
                LiquidTab(onClick = { onModeChange(entry) }) {
                    FitText(
                        stringResource(themeLabel(entry)),
                        style = MaterialTheme.typography.labelLarge,
                        color = OnInk,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
