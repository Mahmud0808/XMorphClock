package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.repository.FaceRepository

class GetDefaultFaceUseCase(private val repository: FaceRepository) {
    suspend operator fun invoke(style: Int): ClockFace? = repository.defaultFace(style)
}
