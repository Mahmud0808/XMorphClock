package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.LyricsVersion
import com.drdisagree.xmorphclock.domain.model.NowPlayingLyrics
import com.drdisagree.xmorphclock.domain.repository.LyricsRepository

class PinLyricsVersionUseCase(private val repository: LyricsRepository) {
    suspend operator fun invoke(playing: NowPlayingLyrics, version: LyricsVersion?) = repository.pin(playing, version)
}
