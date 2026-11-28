package com.drdisagree.xmorphclock.ui.settings

import com.drdisagree.xmorphclock.domain.model.LyricsVersion
import com.drdisagree.xmorphclock.domain.model.NowPlayingLyrics

data class LyricsVersionsUiState(
    val playing: NowPlayingLyrics,
    val loading: Boolean = true,
    val versions: List<LyricsVersion> = emptyList(),
    val failed: Boolean = false
)
