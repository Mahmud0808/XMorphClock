package com.drdisagree.xmorphclock.data.repository

import com.drdisagree.xmorphclock.data.source.HookReportSource
import com.drdisagree.xmorphclock.data.source.ReportContract
import com.drdisagree.xmorphclock.data.source.ReportFileSource
import com.drdisagree.xmorphclock.data.source.RootShell
import com.drdisagree.xmorphclock.data.source.XposedServiceSource
import com.drdisagree.xmorphclock.domain.model.FrameworkInfo
import com.drdisagree.xmorphclock.domain.model.HookReport
import com.drdisagree.xmorphclock.domain.model.ReportLog
import com.drdisagree.xmorphclock.domain.repository.StatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StatusRepositoryImpl(
    private val service: XposedServiceSource,
    private val reports: HookReportSource,
    private val files: ReportFileSource
) : StatusRepository {

    override val framework: Flow<FrameworkInfo?> = service.service.map { service ->
        service ?: return@map null
        try {
            FrameworkInfo(
                name = service.frameworkName,
                version = service.frameworkVersion,
                apiVersion = service.apiVersion,
                scope = service.scope
            )
        } catch (_: Throwable) {
            null
        }
    }

    override val hookReport: Flow<HookReport?> = reports.report

    override fun requestHookReport() {
        reports.request()
    }

    override fun installedAt(): Long = reports.installedAt()

    override suspend fun restartSystemUi(): Boolean =
        RootShell.run("killall ${ReportContract.SYSTEMUI_PACKAGE}")

    override suspend fun checkRoot(): Boolean = RootShell.run("id")

    override suspend fun systemLogs(): Map<ReportLog, String> = files.systemLogs()

    override suspend fun saveReport(target: String, files: Map<String, String>): Boolean = this.files.write(target, files)
}
