package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.StatusRepository

class GetInstallTimeUseCase(private val repository: StatusRepository) {
    operator fun invoke(): Long = repository.installedAt()
}
