package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.UserFont
import com.drdisagree.xmorphclock.domain.repository.FontRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserFontsUseCase(private val repository: FontRepository) {
    operator fun invoke(): Flow<List<UserFont>> = repository.fonts
}
