package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.PresetTransferRepository

class ClosePresetFileUseCase(private val transfer: PresetTransferRepository) {
    operator fun invoke() = transfer.closePreview()
}
