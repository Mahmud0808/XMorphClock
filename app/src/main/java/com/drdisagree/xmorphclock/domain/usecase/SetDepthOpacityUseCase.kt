package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.DepthRepository

class SetDepthOpacityUseCase(private val repository: DepthRepository) {
    suspend operator fun invoke(opacity: Float) = repository.setOpacity(opacity)
}
