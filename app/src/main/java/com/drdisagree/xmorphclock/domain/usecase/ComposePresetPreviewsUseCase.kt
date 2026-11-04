package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.FaceComposer
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.ComposedFaces
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.FacePreset

class ComposePresetPreviewsUseCase {
    operator fun invoke(faces: Map<FaceKind, List<FacePreset>>): Map<Int, ClockFace> {
        val catalog = (faces[FaceKind.WEATHER].orEmpty() + faces[FaceKind.WIDGET].orEmpty()).associate { it.style to it.face }
        return faces[FaceKind.CLOCK].orEmpty()
            .filter { FaceKind.CLOCK.isMine(it.style) }
            .mapNotNull { preset ->
                val layout = preset.layout ?: return@mapNotNull null
                val used = layout.widgetStyles.mapNotNull { style -> catalog[style]?.let { style to it } }.toMap()
                preset.style to FaceComposer.compose(ComposedFaces(preset.face, used, layout.retain(used.keys)))
            }
            .toMap()
    }
}
