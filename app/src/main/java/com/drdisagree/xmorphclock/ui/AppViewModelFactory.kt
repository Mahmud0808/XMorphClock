package com.drdisagree.xmorphclock.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drdisagree.xmorphclock.di.AppContainer

fun appViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer { AppViewModel(container.observeWallpaper, container.observeHookReport, container.observeSettings, container.syncAppliedFace, container.observeFaceData, container.observeSampleFaceData, container.observeDepthSettings, container.observeDepthSubject) }
}
