package com.drdisagree.xmorphclock.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.ThemeMode
import com.drdisagree.xmorphclock.ui.components.AppSwitch
import com.drdisagree.xmorphclock.ui.components.AppTab
import com.drdisagree.xmorphclock.ui.components.AppTabs
import com.drdisagree.xmorphclock.ui.components.FitText
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted

@Composable
internal fun ThemeCard(mode: ThemeMode, onModeChange: (ThemeMode) -> Unit, glass: Boolean, onGlassChange: (Boolean) -> Unit) {
    GlassCard {
        Text(
            stringResource(R.string.section_appearance),
            style = MaterialTheme.typography.titleSmall,
            color = OnInk,
            modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 12.dp)
        )
        AppTabs(
            selectedIndex = mode.ordinal,
            onSelected = { onModeChange(ThemeMode.entries[it]) },
            backdrop = LocalBackdrop.current,
            tabsCount = ThemeMode.entries.size,
            height = 44.dp,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                .fillMaxWidth()
        ) {
            ThemeMode.entries.forEach { entry ->
                AppTab(onClick = { onModeChange(entry) }) {
                    FitText(
                        stringResource(themeLabel(entry)),
                        style = MaterialTheme.typography.labelLarge,
                        color = OnInk,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onGlassChange(!glass) }
                .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ui_glass_title), style = MaterialTheme.typography.bodyLarge, color = OnInk)
                Text(stringResource(R.string.ui_glass_summary), style = MaterialTheme.typography.bodySmall, color = OnInkMuted)
            }
            AppSwitch(selected = glass, onSelect = onGlassChange)
        }
    }
}
