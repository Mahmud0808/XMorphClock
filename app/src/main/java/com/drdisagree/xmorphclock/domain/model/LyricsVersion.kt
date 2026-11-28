package com.drdisagree.xmorphclock.domain.model

data class LyricsVersion(
    val id: String,
    val source: String,
    val artist: String?,
    val durationSec: Double?,
    val lyrics: Lyrics
) {
    val preview: String
        get() = lyrics.lines.firstOrNull { line -> line.text.any { it.isLetterOrDigit() } }?.text.orEmpty()
}
