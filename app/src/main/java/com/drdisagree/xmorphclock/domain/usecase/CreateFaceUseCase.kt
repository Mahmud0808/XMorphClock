package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.repository.FaceRepository

class CreateFaceUseCase(private val faces: FaceRepository) {
    suspend operator fun invoke(kind: FaceKind, face: ClockFace, layout: Composition? = null): Int {
        val style = faces.createFace(kind, face)
        if (layout != null) faces.saveLayout(style, layout)
        return style
    }
}
