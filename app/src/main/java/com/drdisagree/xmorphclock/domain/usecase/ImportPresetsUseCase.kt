package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.PresetImportResult
import com.drdisagree.xmorphclock.domain.repository.PresetTransferRepository

class ImportPresetsUseCase(private val transfer: PresetTransferRepository) {
    suspend operator fun invoke(source: String, styles: Set<Int>): PresetImportResult =
        if (styles.isEmpty()) PresetImportResult() else transfer.importPresets(source, styles)
}
