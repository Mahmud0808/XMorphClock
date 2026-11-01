package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.ModuleStatus
import com.drdisagree.xmorphclock.domain.repository.StatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveModuleStatusUseCase(private val repository: StatusRepository) {
    operator fun invoke(): Flow<ModuleStatus> = repository.framework.map {
        if (it != null) ModuleStatus.ACTIVE else ModuleStatus.INACTIVE
    }
}
