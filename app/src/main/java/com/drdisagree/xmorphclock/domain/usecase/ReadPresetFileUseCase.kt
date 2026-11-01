package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.PresetFilePreview
import com.drdisagree.xmorphclock.domain.repository.PresetTransferRepository

class ReadPresetFileUseCase(private val transfer: PresetTransferRepository) {
    suspend operator fun invoke(source: String): PresetFilePreview = transfer.readPresets(source)
}
