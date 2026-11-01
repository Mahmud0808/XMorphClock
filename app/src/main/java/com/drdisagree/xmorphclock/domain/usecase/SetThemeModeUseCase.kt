package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.ThemeMode
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository

class SetThemeModeUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
