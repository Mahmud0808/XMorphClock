package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.FrameworkInfo
import com.drdisagree.xmorphclock.domain.repository.StatusRepository
import kotlinx.coroutines.flow.Flow

class ObserveFrameworkUseCase(private val repository: StatusRepository) {
    operator fun invoke(): Flow<FrameworkInfo?> = repository.framework
}
