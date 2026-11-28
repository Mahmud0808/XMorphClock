package com.drdisagree.xmorphclock.domain.model

data class LyricLine(
    val text: String,
    val start: Long,
    val end: Long,
    val words: List<LyricWord> = emptyList()
)
