package com.drdisagree.xmorphclock.domain.repository

import com.drdisagree.xmorphclock.domain.model.FrameworkInfo
import com.drdisagree.xmorphclock.domain.model.HookReport
import kotlinx.coroutines.flow.Flow

interface StatusRepository {
    val framework: Flow<FrameworkInfo?>
    val hookReport: Flow<HookReport?>
    fun requestHookReport()
    fun installedAt(): Long
    suspend fun restartSystemUi(): Boolean
    suspend fun checkRoot(): Boolean
    suspend fun systemLog(): String?
    suspend fun saveReport(target: String, text: String): Boolean
}
