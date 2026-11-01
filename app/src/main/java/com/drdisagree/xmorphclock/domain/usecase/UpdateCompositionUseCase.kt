package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class UpdateCompositionUseCase(
    private val settings: SettingsRepository,
    private val apply: ApplyCompositionUseCase
) {
    suspend operator fun invoke(clockStyle: Int? = null, transform: (Composition) -> Composition) {
        val current = settings.settings.first().composition
        apply(clockStyle = clockStyle, composition = transform(current))
    }
}
