package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.UiStyle
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository

class SetUiStyleUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(style: UiStyle) = repository.setUiStyle(style)
}
