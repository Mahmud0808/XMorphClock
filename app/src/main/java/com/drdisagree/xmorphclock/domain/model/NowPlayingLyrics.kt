package com.drdisagree.xmorphclock.domain.model

data class NowPlayingLyrics(
    val key: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val origin: LyricsOrigin,
    val tuning: LyricsTuning = LyricsTuning()
)
