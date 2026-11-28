package com.drdisagree.xmorphclock.data.source

import com.drdisagree.xmorphclock.domain.model.LyricsVersion

internal data class OnlineLyricsCandidate(
    val version: LyricsVersion,
    val gapSeconds: Double?
)
