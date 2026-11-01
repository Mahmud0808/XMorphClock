package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.FacePreset
import com.drdisagree.xmorphclock.domain.repository.FaceRepository

class GetFaceUseCase(private val faces: FaceRepository) {
    suspend operator fun invoke(style: Int): FacePreset? = faces.face(style)
}
