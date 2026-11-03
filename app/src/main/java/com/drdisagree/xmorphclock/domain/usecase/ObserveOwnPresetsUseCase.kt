package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.FacePreset
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveOwnPresetsUseCase(private val repository: FaceRepository) {
    operator fun invoke(): Flow<List<FacePreset>> =
        repository.faces(FaceKind.CLOCK).map { list -> list.filter { FaceKind.CLOCK.isMine(it.style) } }
}
