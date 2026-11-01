package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.repository.FaceDataRepository
import kotlinx.coroutines.flow.Flow

class ObserveFaceDataUseCase(private val repository: FaceDataRepository) {
    operator fun invoke(): Flow<FaceData> = repository.data
}
