package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.FaceComposer
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.CompositionDraft
import com.drdisagree.xmorphclock.domain.repository.FaceRepository

class SaveCompositionUseCase(
    private val faces: FaceRepository,
    private val apply: ApplyCompositionUseCase
) {
    suspend operator fun invoke(draft: CompositionDraft, face: ClockFace) {
        val split = FaceComposer.decompose(face, draft.parts)
        if (split.composition.clockItem != null && split.clock != draft.parts.clock) save(draft.clockStyle, split.clock)
        split.widgets.forEach { (style, widget) -> if (widget != draft.parts.widgets[style]) save(style, widget) }
        apply(clockStyle = draft.clockStyle, composition = split.composition)
    }

    private suspend fun save(style: Int, face: ClockFace) {
        if (face == faces.defaultFace(style)) faces.resetFace(style) else faces.saveFace(style, face)
    }
}
