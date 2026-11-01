package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.StatusRepository

class RequestHookReportUseCase(private val repository: StatusRepository) {
    operator fun invoke() = repository.requestHookReport()
}
