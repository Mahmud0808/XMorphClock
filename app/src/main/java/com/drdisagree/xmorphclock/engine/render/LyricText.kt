package com.drdisagree.xmorphclock.engine.render

import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.FaceLayerType
import com.drdisagree.xmorphclock.domain.model.LyricLine

internal object LyricText {

    fun position(data: FaceData, now: Long): Long = data.lyricClock?.positionAt(now) ?: 0L

    fun line(type: FaceLayerType, data: FaceData, now: Long): LyricLine? {
        val lyrics = data.lyrics ?: return null
        val index = lyrics.indexAt(position(data, now))
        return if (type == FaceLayerType.LYRIC_NEXT) lyrics.lines.getOrNull(index + 1) else lyrics.lines.getOrNull(index)
    }

    fun sung(line: LyricLine, position: Long): Float {
        val text = line.text
        if (position <= line.start) return 0f
        if (line.words.isEmpty()) {
            val span = line.end - line.start
            if (span <= 0L) return text.length.toFloat()
            return ((position - line.start).toFloat() / span).coerceIn(0f, 1f) * text.length
        }
        var cursor = 0
        line.words.forEach { word ->
            val token = word.text.trim()
            val at = if (token.isEmpty()) cursor else text.indexOf(token, cursor, ignoreCase = true).takeIf { it >= 0 } ?: cursor
            val end = (at + token.length).coerceAtMost(text.length)
            if (position < word.start) return cursor.toFloat()
            if (position < word.end && word.end > word.start) {
                return at + (end - at) * ((position - word.start).toFloat() / (word.end - word.start))
            }
            cursor = end
        }
        return text.length.toFloat()
    }
}
