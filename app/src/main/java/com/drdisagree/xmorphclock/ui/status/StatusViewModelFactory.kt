package com.drdisagree.xmorphclock.ui.status

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drdisagree.xmorphclock.di.AppContainer

fun statusViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        StatusViewModel(
            observeFramework = container.observeFramework,
            observeHookReport = container.observeHookReport,
            getInstallTime = container.getInstallTime,
            requestHookReport = container.requestHookReport,
            restartSystemUi = container.restartSystemUi,
            saveReport = container.saveReport,
            observeDepthModel = container.observeDepthModel
        )
    }
}
