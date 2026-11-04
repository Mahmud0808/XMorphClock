package com.drdisagree.xmorphclock.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drdisagree.xmorphclock.R

@Composable
fun WeatherSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsPage(stringResource(R.string.section_weather), state.loaded, onBack) {
        WeatherCard(state.weather, state.locationNotFound, viewModel::onWeatherSettingsChange, viewModel::onWeatherLocation, viewModel::onRefreshWeather)
    }
}
