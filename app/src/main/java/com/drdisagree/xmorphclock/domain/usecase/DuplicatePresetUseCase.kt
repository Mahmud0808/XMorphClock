package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.FacePreset
import com.drdisagree.xmorphclock.domain.repository.FaceRepository

class DuplicatePresetUseCase(
    private val faces: FaceRepository,
    private val snapshot: SnapshotWidgetsUseCase
) {
    suspend operator fun invoke(preset: FacePreset, name: String) {
        if (FaceKind.of(preset.style) != FaceKind.CLOCK) return
        val style = faces.createFace(FaceKind.CLOCK, preset.face.copy(name = name))
        preset.layout?.let { faces.saveLayout(style, snapshot(it)) }
    }
}
