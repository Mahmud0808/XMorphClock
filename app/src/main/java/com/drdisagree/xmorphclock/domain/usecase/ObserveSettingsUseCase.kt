package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.ClockSettings
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<ClockSettings> = repository.settings
}
