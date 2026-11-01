package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.FaceRepository

class ApplyClockPresetUseCase(
    private val faces: FaceRepository,
    private val update: UpdateCompositionUseCase
) {
    suspend operator fun invoke(style: Int) {
        val layout = faces.face(style)?.layout
        update(clockStyle = style) { current -> layout ?: current.withClock() }
    }
}
