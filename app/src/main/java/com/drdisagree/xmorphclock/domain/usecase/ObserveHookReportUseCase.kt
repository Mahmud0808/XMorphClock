package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.HookReport
import com.drdisagree.xmorphclock.domain.repository.StatusRepository
import kotlinx.coroutines.flow.Flow

class ObserveHookReportUseCase(private val repository: StatusRepository) {
    operator fun invoke(): Flow<HookReport?> = repository.hookReport
}
