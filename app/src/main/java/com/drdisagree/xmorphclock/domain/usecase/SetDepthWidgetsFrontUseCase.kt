package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.DepthRepository

class SetDepthWidgetsFrontUseCase(private val repository: DepthRepository) {
    suspend operator fun invoke(front: Boolean) = repository.setWidgetsFront(front)
}
