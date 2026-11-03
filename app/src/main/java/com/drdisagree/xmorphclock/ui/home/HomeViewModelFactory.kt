package com.drdisagree.xmorphclock.ui.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drdisagree.xmorphclock.di.AppContainer

fun homeViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        HomeViewModel(
            observeFaces = container.observeFaces,
            observeSettings = container.observeSettings,
            observeModuleStatus = container.observeModuleStatus,
            observeLiveWallpaper = container.observeLiveWallpaper,
            observeAppliedFace = container.observeAppliedFace,
            updateComposition = container.updateComposition,
            createFace = container.createFace,
            deletePreset = container.deletePreset,
            duplicatePreset = container.duplicatePreset,
            importImage = container.importImage,
            applyClockPreset = container.applyClockPreset,
            saveStylePreset = container.saveStylePreset,
            updateStylePreset = container.updateStylePreset,
            observeStyleStatus = container.observeStyleStatus
        )
    }
}
