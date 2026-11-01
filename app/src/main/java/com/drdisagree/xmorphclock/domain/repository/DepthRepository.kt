package com.drdisagree.xmorphclock.domain.repository

import android.graphics.Bitmap
import com.drdisagree.xmorphclock.domain.model.DepthModelState
import com.drdisagree.xmorphclock.domain.model.DepthSettings
import com.drdisagree.xmorphclock.domain.model.DepthStatus
import kotlinx.coroutines.flow.Flow

interface DepthRepository {
    val settings: Flow<DepthSettings>
    val subject: Flow<Bitmap?>
    val modelState: Flow<DepthModelState>
    val status: Flow<DepthStatus>
    suspend fun retry()
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setOpacity(opacity: Float)
}
