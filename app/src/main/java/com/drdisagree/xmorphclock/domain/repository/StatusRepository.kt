package com.drdisagree.xmorphclock.domain.repository

import com.drdisagree.xmorphclock.domain.model.FrameworkInfo
import com.drdisagree.xmorphclock.domain.model.HookReport
import com.drdisagree.xmorphclock.domain.model.ReportLog
import kotlinx.coroutines.flow.Flow

interface StatusRepository {
    val framework: Flow<FrameworkInfo?>
    val hookReport: Flow<HookReport?>
    fun requestHookReport()
    fun installedAt(): Long
    suspend fun restartSystemUi(): Boolean
    suspend fun checkRoot(): Boolean
    suspend fun systemLogs(): Map<ReportLog, String>
    suspend fun depthFiles(): Map<String, ByteArray>
    suspend fun saveReport(target: String, files: Map<String, ByteArray>): Boolean
}
