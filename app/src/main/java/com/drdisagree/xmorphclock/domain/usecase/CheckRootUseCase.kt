package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.StatusRepository

class CheckRootUseCase(private val repository: StatusRepository) {
    suspend operator fun invoke(): Boolean = repository.checkRoot()
}
