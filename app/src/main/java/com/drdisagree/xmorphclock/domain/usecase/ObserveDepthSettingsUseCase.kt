package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.DepthSettings
import com.drdisagree.xmorphclock.domain.repository.DepthRepository
import kotlinx.coroutines.flow.Flow

class ObserveDepthSettingsUseCase(private val repository: DepthRepository) {
    operator fun invoke(): Flow<DepthSettings> = repository.settings
}
