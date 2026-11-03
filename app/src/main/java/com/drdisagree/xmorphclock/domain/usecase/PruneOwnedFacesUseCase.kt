package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class PruneOwnedFacesUseCase(
    private val settings: SettingsRepository,
    private val faces: FaceRepository,
    private val delete: DeleteFaceUseCase
) {
    suspend operator fun invoke() {
        val used = settings.settings.first().composition.widgetStyles.toHashSet()
        faces.faces(FaceKind.CLOCK).first()
            .filter { FaceKind.CLOCK.isMine(it.style) }
            .forEach { preset -> preset.layout?.let { used += it.widgetStyles } }
        listOf(FaceKind.WEATHER, FaceKind.WIDGET).forEach { kind ->
            faces.faces(kind).first()
                .filter { kind.isMine(it.style) && CustomFaces.isPrivate(it.face) && it.style !in used }
                .forEach { delete(it.style, it.face) }
        }
    }
}
