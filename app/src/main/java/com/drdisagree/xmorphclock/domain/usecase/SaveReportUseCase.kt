package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.DepthRepository
import com.drdisagree.xmorphclock.domain.repository.StatusRepository
import kotlinx.coroutines.flow.first

class SaveReportUseCase(
    private val repository: StatusRepository,
    private val depth: DepthRepository
) {
    suspend operator fun invoke(target: String, report: String): Boolean {
        val logs = repository.systemLogs()
        val files = linkedMapOf(REPORT_FILE to report.toByteArray())
        logs.forEach { (kind, text) -> files[kind.fileName] = text.toByteArray() }
        if (depth.settings.first().enabled) files.putAll(repository.depthFiles())
        return repository.saveReport(target, files)
    }

    private companion object {
        const val REPORT_FILE = "report.txt"
    }
}
