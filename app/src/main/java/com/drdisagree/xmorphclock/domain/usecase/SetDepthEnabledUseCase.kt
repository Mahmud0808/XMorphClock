package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.DepthRepository

class SetDepthEnabledUseCase(private val repository: DepthRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setEnabled(enabled)
}
