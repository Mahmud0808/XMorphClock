package com.drdisagree.xmorphclock.ui.editor

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.EditScope
import com.drdisagree.xmorphclock.ui.components.AppTab
import com.drdisagree.xmorphclock.ui.components.AppTabs
import com.drdisagree.xmorphclock.ui.components.FitText
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.theme.OnInk

@Composable
fun EditorScopeTabs(
    scope: EditScope,
    onScopeChange: (EditScope) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp
) {
    AppTabs(
        selectedIndex = scope.ordinal,
        onSelected = { onScopeChange(EditScope.entries[it]) },
        backdrop = LocalBackdrop.current,
        tabsCount = EditScope.entries.size,
        height = height,
        modifier = modifier
    ) {
        EditScope.entries.forEach { entry ->
            AppTab(onClick = { onScopeChange(entry) }) {
                FitText(
                    stringResource(scopeLabel(entry)),
                    style = MaterialTheme.typography.labelLarge,
                    color = OnInk,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

private fun scopeLabel(scope: EditScope): Int = when (scope) {
    EditScope.LARGE -> R.string.scope_large
    EditScope.SMALL -> R.string.scope_small
    EditScope.AOD -> R.string.scope_aod
}
