package com.drdisagree.xmorphclock.ui.settings

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drdisagree.xmorphclock.di.AppContainer

fun settingsViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        SettingsViewModel(
            observeSettings = container.observeSettings,
            setClockEnabled = container.setClockEnabled,
            setBurnInProtection = container.setBurnInProtection,
            setThemeMode = container.setThemeMode,
            observeWeather = container.observeWeather,
            saveWeatherSettings = container.saveWeatherSettings,
            refreshWeather = container.refreshWeather,
            setWeatherLocation = container.setWeatherLocation,
            observeDepthSettings = container.observeDepthSettings,
            observeDepthStatus = container.observeDepthStatus,
            retryDepth = container.retryDepth,
            setDepthEnabled = container.setDepthEnabled,
            setDepthOpacity = container.setDepthOpacity,
            setDepthWidgetsFront = container.setDepthWidgetsFront,
            observeOwnPresets = container.observeOwnPresets,
            exportPresetFile = container.exportPresets,
            readPresetFile = container.readPresetFile,
            importPresetFile = container.importPresets,
            closePresetFile = container.closePresetFile
        )
    }
}
