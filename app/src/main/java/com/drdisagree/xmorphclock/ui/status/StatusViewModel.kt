package com.drdisagree.xmorphclock.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drdisagree.xmorphclock.domain.usecase.ObserveDepthModelUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveFrameworkUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveHookReportUseCase
import com.drdisagree.xmorphclock.domain.usecase.RequestHookReportUseCase
import com.drdisagree.xmorphclock.domain.usecase.RestartSystemUiUseCase
import com.drdisagree.xmorphclock.domain.usecase.SaveReportUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class StatusViewModel(
    observeFramework: ObserveFrameworkUseCase,
    private val observeHookReport: ObserveHookReportUseCase,
    private val requestHookReport: RequestHookReportUseCase,
    private val restartSystemUi: RestartSystemUiUseCase,
    private val saveReport: SaveReportUseCase,
    observeDepthModel: ObserveDepthModelUseCase
) : ViewModel() {

    private val restarting = MutableStateFlow(false)
    private val refreshState = MutableStateFlow(RefreshState.IDLE)
    private val saveState = MutableStateFlow(SaveState.IDLE)
    private var refreshJob: Job? = null

    val uiState: StateFlow<StatusUiState> = combine(
        observeFramework(),
        observeHookReport(),
        restarting,
        refreshState,
        saveState
    ) { framework, report, isRestarting, refresh, save ->
        StatusUiState(framework, report, isRestarting, refresh, save)
    }.combine(observeDepthModel()) { state, model ->
        state.copy(depthModel = model)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), StatusUiState())

    init {
        requestHookReport()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshState.value = RefreshState.CHECKING
        refreshJob = viewModelScope.launch {
            val requestedAt = System.currentTimeMillis()
            val reply = withTimeoutOrNull(RESPONSE_TIMEOUT) {
                observeHookReport()
                    .onStart { requestHookReport() }
                    .first { it != null && it.timestamp >= requestedAt - CLOCK_SLACK }
            }
            delay(MIN_CHECKING)
            refreshState.value = if (reply != null) RefreshState.UPDATED else RefreshState.NO_RESPONSE
            delay(if (reply != null) UPDATED_HOLD else NO_RESPONSE_HOLD)
            refreshState.value = RefreshState.IDLE
        }
    }

    fun onSaveReport(target: String, report: String, logTitle: String) {
        if (saveState.value == SaveState.SAVING) return
        saveState.value = SaveState.SAVING
        viewModelScope.launch {
            val saved = saveReport(target, report, logTitle)
            saveState.value = if (saved) SaveState.SAVED else SaveState.FAILED
            delay(SAVE_HOLD)
            saveState.value = SaveState.IDLE
        }
    }

    fun onRestartSystemUi() {
        if (restarting.value) return
        restarting.value = true
        viewModelScope.launch {
            if (restartSystemUi()) {
                delay(RESTART_SETTLE)
                requestHookReport()
            }
            restarting.value = false
        }
    }
}

private const val STOP_TIMEOUT = 5_000L
private const val RESTART_SETTLE = 6_000L
private const val RESPONSE_TIMEOUT = 4_000L
private const val CLOCK_SLACK = 1_000L
private const val MIN_CHECKING = 450L
private const val UPDATED_HOLD = 1_500L
private const val NO_RESPONSE_HOLD = 2_500L
private const val SAVE_HOLD = 2_000L
