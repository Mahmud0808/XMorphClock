package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.PresetTransferRepository

class ExportPresetsUseCase(private val transfer: PresetTransferRepository) {
    suspend operator fun invoke(styles: List<Int>, target: String): Boolean =
        styles.isNotEmpty() && transfer.exportPresets(target, styles)
}
