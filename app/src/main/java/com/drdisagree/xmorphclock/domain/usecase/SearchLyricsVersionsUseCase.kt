package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.LyricsVersion
import com.drdisagree.xmorphclock.domain.model.NowPlayingLyrics
import com.drdisagree.xmorphclock.domain.repository.LyricsRepository

class SearchLyricsVersionsUseCase(private val repository: LyricsRepository) {
    suspend operator fun invoke(playing: NowPlayingLyrics): List<LyricsVersion>? = repository.versions(playing)
}
