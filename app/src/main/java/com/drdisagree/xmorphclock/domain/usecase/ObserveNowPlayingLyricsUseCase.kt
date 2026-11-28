package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.NowPlayingLyrics
import com.drdisagree.xmorphclock.domain.repository.LyricsRepository
import kotlinx.coroutines.flow.Flow

class ObserveNowPlayingLyricsUseCase(private val repository: LyricsRepository) {
    operator fun invoke(): Flow<NowPlayingLyrics?> = repository.nowPlaying
}
