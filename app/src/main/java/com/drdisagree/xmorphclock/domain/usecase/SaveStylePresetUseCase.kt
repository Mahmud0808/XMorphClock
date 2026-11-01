package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class SaveStylePresetUseCase(
    private val settings: SettingsRepository,
    private val faces: FaceRepository,
    private val update: UpdateCompositionUseCase
) {
    suspend operator fun invoke(name: String): Int? {
        val current = settings.settings.first()
        val clock = faces.face(current.faceStyle)?.face ?: return null
        val style = faces.createFace(FaceKind.CLOCK, clock.copy(name = name.trim().ifEmpty { clock.name }))
        faces.saveLayout(style, current.composition)
        update(clockStyle = style) { it }
        return style
    }
}
