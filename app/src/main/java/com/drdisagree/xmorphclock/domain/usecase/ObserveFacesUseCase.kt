package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.FacePreset
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import kotlinx.coroutines.flow.Flow

class ObserveFacesUseCase(private val repository: FaceRepository) {
    operator fun invoke(kind: FaceKind = FaceKind.CLOCK): Flow<List<FacePreset>> = repository.faces(kind)
}
