package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.DepthModelState
import com.drdisagree.xmorphclock.domain.repository.DepthRepository
import kotlinx.coroutines.flow.Flow

class ObserveDepthModelUseCase(private val repository: DepthRepository) {
    operator fun invoke(): Flow<DepthModelState> = repository.modelState
}
