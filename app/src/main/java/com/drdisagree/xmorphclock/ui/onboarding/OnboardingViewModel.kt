package com.drdisagree.xmorphclock.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drdisagree.xmorphclock.domain.usecase.CheckRootUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveFacesUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveFrameworkUseCase
import com.drdisagree.xmorphclock.domain.usecase.RestartSystemUiUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetOnboardingDoneUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OnboardingViewModel(
    observeFaces: ObserveFacesUseCase,
    observeFramework: ObserveFrameworkUseCase,
    private val checkRoot: CheckRootUseCase,
    private val restartSystemUi: RestartSystemUiUseCase,
    private val setOnboardingDone: SetOnboardingDoneUseCase
) : ViewModel() {

    private val root = MutableStateFlow<Boolean?>(null)
    private val restarting = MutableStateFlow(false)
    private val rootChecking = MutableStateFlow(false)

    val uiState: StateFlow<OnboardingUiState> = combine(
        observeFaces(),
        observeFramework(),
        root,
        rootChecking,
        restarting
    ) { faces, framework, rootGranted, checking, isRestarting ->
        OnboardingUiState(faces.firstOrNull(), rootGranted, checking, framework, isRestarting)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), OnboardingUiState())

    fun onCheckRoot() {
        if (rootChecking.value || root.value == true) return
        rootChecking.value = true
        viewModelScope.launch {
            root.value = checkRoot()
            rootChecking.value = false
        }
    }

    fun onRestartSystemUi() {
        if (restarting.value) return
        restarting.value = true
        viewModelScope.launch {
            restartSystemUi()
            restarting.value = false
        }
    }

    fun onFinish(then: () -> Unit) {
        viewModelScope.launch {
            setOnboardingDone(true)
            then()
        }
    }
}

private const val STOP_TIMEOUT = 5_000L
