package com.drdisagree.xmorphclock.ui.status

import com.drdisagree.xmorphclock.domain.model.DepthModelState
import com.drdisagree.xmorphclock.domain.model.FrameworkInfo
import com.drdisagree.xmorphclock.domain.model.HookReport

data class StatusUiState(
    val framework: FrameworkInfo? = null,
    val report: HookReport? = null,
    val restarting: Boolean = false,
    val refresh: RefreshState = RefreshState.IDLE,
    val save: SaveState = SaveState.IDLE,
    val depthModel: DepthModelState = DepthModelState.CHECKING,
    val installedAt: Long = 0L
) {
    val isScoped: Boolean
        get() = framework?.scope?.contains(SYSTEMUI) == true

    val isHealthy: Boolean
        get() = framework != null && isScoped && report?.isHealthy == true
}

private const val SYSTEMUI = "com.android.systemui"
