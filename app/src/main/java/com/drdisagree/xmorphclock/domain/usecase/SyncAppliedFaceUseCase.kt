package com.drdisagree.xmorphclock.domain.usecase

class SyncAppliedFaceUseCase(private val apply: ApplyCompositionUseCase) {
    suspend operator fun invoke() = apply()
}
