package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.LyricsTuning
import com.drdisagree.xmorphclock.domain.repository.LyricsRepository

class SetLyricsTuningUseCase(private val repository: LyricsRepository) {
    suspend operator fun invoke(key: String, tuning: LyricsTuning) = repository.setTuning(key, tuning)
}
