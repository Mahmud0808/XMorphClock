package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.ClockAttachState
import com.drdisagree.xmorphclock.domain.model.FrameworkInfo
import com.drdisagree.xmorphclock.domain.model.HookReport
import com.drdisagree.xmorphclock.domain.model.ModuleStatus
import com.drdisagree.xmorphclock.domain.repository.StatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveModuleStatusUseCase(private val repository: StatusRepository) {
    operator fun invoke(): Flow<ModuleStatus> = combine(repository.framework, repository.hookReport) { framework, report ->
        statusOf(framework, report)
    }

    private fun statusOf(framework: FrameworkInfo?, report: HookReport?): ModuleStatus {
        if (framework == null) return ModuleStatus.INACTIVE
        if (framework.scope.none { it == SYSTEMUI }) return ModuleStatus.ATTENTION
        report ?: return ModuleStatus.ATTENTION
        return when (report.attachState(repository.installedAt())) {
            ClockAttachState.OFF -> ModuleStatus.INACTIVE
            ClockAttachState.ATTACHED -> if (report.isHealthy) ModuleStatus.ACTIVE else ModuleStatus.ATTENTION
            else -> ModuleStatus.ATTENTION
        }
    }

    private companion object {
        const val SYSTEMUI = "com.android.systemui"
    }
}
