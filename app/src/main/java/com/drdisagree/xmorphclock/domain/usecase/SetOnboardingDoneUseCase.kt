package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.SettingsRepository

class SetOnboardingDoneUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(done: Boolean) = repository.setOnboardingDone(done)
}
