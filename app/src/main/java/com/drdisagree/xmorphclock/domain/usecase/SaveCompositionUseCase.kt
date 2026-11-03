package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.editor.FaceComposer
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.CompositionDraft
import com.drdisagree.xmorphclock.domain.repository.FaceRepository

class SaveCompositionUseCase(
    private val faces: FaceRepository,
    private val apply: ApplyCompositionUseCase,
    private val delete: DeleteFaceUseCase
) {
    suspend operator fun invoke(draft: CompositionDraft, face: ClockFace) {
        val split = FaceComposer.decompose(face, draft.parts)
        if (split.composition.clockItem != null && split.clock != draft.parts.clock) save(draft.clockStyle, split.clock)
        split.widgets.forEach { (style, widget) -> if (widget != draft.parts.widgets[style]) save(style, widget) }
        apply(clockStyle = draft.clockStyle, composition = split.composition)
        val kept = split.composition.widgetStyles
        draft.parts.composition.widgetStyles.filterNot { it in kept }.forEach { style ->
            val widget = draft.parts.widgets[style] ?: return@forEach
            if (widget.category == CustomFaces.ELEMENT_CATEGORY) delete(style, widget)
        }
    }

    private suspend fun save(style: Int, face: ClockFace) {
        if (face == faces.defaultFace(style)) faces.resetFace(style) else faces.saveFace(style, face)
    }
}
