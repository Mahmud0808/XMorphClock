package com.drdisagree.xmorphclock.ui.onboarding

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drdisagree.xmorphclock.di.AppContainer

fun onboardingViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        OnboardingViewModel(
            observeFaces = container.observeFaces,
            observeFramework = container.observeFramework,
            checkRoot = container.checkRoot,
            restartSystemUi = container.restartSystemUi,
            setOnboardingDone = container.setOnboardingDone
        )
    }
}
