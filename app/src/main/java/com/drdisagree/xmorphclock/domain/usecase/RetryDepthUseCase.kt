package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.DepthRepository

class RetryDepthUseCase(private val repository: DepthRepository) {
    suspend operator fun invoke() = repository.retry()
}
