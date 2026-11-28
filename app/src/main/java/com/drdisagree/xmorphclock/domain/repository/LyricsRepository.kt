package com.drdisagree.xmorphclock.domain.repository

import com.drdisagree.xmorphclock.domain.model.LyricsTuning
import com.drdisagree.xmorphclock.domain.model.LyricsVersion
import com.drdisagree.xmorphclock.domain.model.NowPlayingLyrics
import kotlinx.coroutines.flow.Flow

interface LyricsRepository {
    val nowPlaying: Flow<NowPlayingLyrics?>
    suspend fun setTuning(key: String, tuning: LyricsTuning)
    suspend fun versions(playing: NowPlayingLyrics): List<LyricsVersion>?
    suspend fun pin(playing: NowPlayingLyrics, version: LyricsVersion?)
}
