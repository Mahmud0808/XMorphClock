package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.StatusRepository

class SaveReportUseCase(private val repository: StatusRepository) {
    suspend operator fun invoke(target: String, report: String): Boolean {
        val logs = repository.systemLogs()
        val files = linkedMapOf(REPORT_FILE to report)
        logs.forEach { (kind, text) -> files[kind.fileName] = text }
        return repository.saveReport(target, files)
    }

    private companion object {
        const val REPORT_FILE = "report.txt"
    }
}
