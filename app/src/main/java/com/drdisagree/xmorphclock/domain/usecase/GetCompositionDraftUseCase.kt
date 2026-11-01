package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.FaceComposer
import com.drdisagree.xmorphclock.domain.model.ComposedFaces
import com.drdisagree.xmorphclock.domain.model.CompositionDraft
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class GetCompositionDraftUseCase(
    private val settings: SettingsRepository,
    private val faces: FaceRepository
) {
    suspend operator fun invoke(): CompositionDraft? {
        val current = settings.settings.first()
        val clock = faces.face(current.faceStyle) ?: faces.faces(FaceKind.CLOCK).first().firstOrNull() ?: return null
        val widgets = current.composition.widgetStyles.mapNotNull { style -> faces.face(style)?.let { style to it.face } }.toMap()
        val composition = current.composition.retain(widgets.keys)
        val parts = ComposedFaces(clock.face, widgets, composition)
        val defaults = ComposedFaces(
            clock = faces.defaultFace(clock.style) ?: clock.face,
            widgets = widgets.mapValues { (style, face) -> faces.defaultFace(style) ?: face },
            composition = composition
        )
        return CompositionDraft(clock.style, parts, FaceComposer.compose(parts), FaceComposer.compose(defaults))
    }
}
