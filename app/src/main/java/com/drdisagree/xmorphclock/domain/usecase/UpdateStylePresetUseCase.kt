package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class UpdateStylePresetUseCase(
    private val settings: SettingsRepository,
    private val faces: FaceRepository,
    private val update: UpdateCompositionUseCase
) {
    suspend operator fun invoke(style: Int): Boolean {
        if (!FaceKind.CLOCK.isMine(style)) return false
        val target = faces.face(style)?.face ?: return false
        val current = settings.settings.first()
        val switching = current.faceStyle != style
        if (switching) {
            val clock = faces.face(current.faceStyle)?.face ?: return false
            faces.saveFace(style, clock.copy(name = target.name, category = target.category))
        }
        faces.saveLayout(style, current.composition)
        if (switching) update(clockStyle = style) { it }
        return true
    }
}
