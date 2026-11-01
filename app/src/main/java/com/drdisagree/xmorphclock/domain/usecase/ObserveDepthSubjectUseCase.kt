package com.drdisagree.xmorphclock.domain.usecase

import android.graphics.Bitmap
import com.drdisagree.xmorphclock.domain.repository.DepthRepository
import kotlinx.coroutines.flow.Flow

class ObserveDepthSubjectUseCase(private val repository: DepthRepository) {
    operator fun invoke(): Flow<Bitmap?> = repository.subject
}
