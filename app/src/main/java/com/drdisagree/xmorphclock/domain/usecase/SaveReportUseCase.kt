package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.StatusRepository

class SaveReportUseCase(private val repository: StatusRepository) {
    suspend operator fun invoke(target: String, report: String, logTitle: String): Boolean {
        val log = repository.systemLog()
        val text = if (log == null) report else "$report\n\n## $logTitle\n$log"
        return repository.saveReport(target, text)
    }
}
