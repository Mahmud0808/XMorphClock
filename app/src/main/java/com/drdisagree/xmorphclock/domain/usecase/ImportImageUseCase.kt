package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.FaceImage
import com.drdisagree.xmorphclock.domain.repository.ImageRepository

class ImportImageUseCase(private val repository: ImageRepository) {
    suspend operator fun invoke(uri: String): FaceImage? = repository.import(uri)
}
