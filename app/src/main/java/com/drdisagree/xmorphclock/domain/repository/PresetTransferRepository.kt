package com.drdisagree.xmorphclock.domain.repository

import com.drdisagree.xmorphclock.domain.model.PresetFilePreview
import com.drdisagree.xmorphclock.domain.model.PresetImportResult

interface PresetTransferRepository {
    suspend fun exportPresets(target: String, styles: List<Int>): Boolean
    suspend fun readPresets(source: String): PresetFilePreview
    suspend fun importPresets(source: String, styles: Set<Int>): PresetImportResult
    fun closePreview()
}
