package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.FacePreset

class DeletePresetUseCase(
    private val delete: DeleteFaceUseCase,
    private val prune: PruneOwnedFacesUseCase
) {
    suspend operator fun invoke(preset: FacePreset) {
        delete(preset.style, preset.face)
        prune()
    }
}
