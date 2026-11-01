package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.FaceComposer
import com.drdisagree.xmorphclock.domain.model.ComposedFaces
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.repository.FaceDataRepository
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class ApplyCompositionUseCase(
    private val settings: SettingsRepository,
    private val faces: FaceRepository,
    private val data: FaceDataRepository
) {
    suspend operator fun invoke(clockStyle: Int? = null, composition: Composition? = null) {
        val current = settings.settings.first()
        val style = clockStyle ?: current.faceStyle
        val target = composition ?: current.composition
        val clock = faces.face(style)?.face ?: return
        val widgets = target.widgetStyles.mapNotNull { widget -> faces.face(widget)?.let { widget to it.face } }.toMap()
        val resolved = target.retain(widgets.keys)
        if (resolved != current.composition) settings.setComposition(resolved)
        val composed = FaceComposer.compose(ComposedFaces(clock, widgets, resolved))
        settings.applyFace(style, composed)
        data.prepare(composed)
    }
}
