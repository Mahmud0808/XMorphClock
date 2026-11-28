package com.drdisagree.xmorphclock.data.source

import com.drdisagree.xmorphclock.domain.model.Lyrics

data class OnlineLyricsMatch(
    val lyrics: Lyrics,
    val exact: Boolean
)
