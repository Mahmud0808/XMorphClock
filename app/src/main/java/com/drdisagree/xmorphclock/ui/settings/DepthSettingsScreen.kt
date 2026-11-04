package com.drdisagree.xmorphclock.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drdisagree.xmorphclock.R

@Composable
fun DepthSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var opacity by remember(state.depth.opacity) { mutableFloatStateOf(state.depth.opacity) }
    SettingsPage(stringResource(R.string.section_depth), state.loaded, onBack) {
        DepthLayersArt(state.depth.enabled, state.depth.widgetsFront, opacity)
        DepthCard(
            settings = state.depth,
            status = state.depthStatus,
            opacity = opacity,
            onEnabledChange = viewModel::onDepthEnabledChange,
            onOpacityDrag = { opacity = it },
            onOpacityChange = viewModel::onDepthOpacityChange,
            onWidgetsFrontChange = viewModel::onDepthWidgetsFrontChange,
            onRetry = viewModel::onDepthRetry
        )
    }
}
