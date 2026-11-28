package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.domain.model.LyricLine
import com.drdisagree.xmorphclock.domain.model.LyricWord
import com.drdisagree.xmorphclock.domain.model.Lyrics

object LrcMapper {

    private val TIME = Regex("""\[(\d+):(\d+(?:[.:]\d+)?)]""")
    private val WORD = Regex("""<(\d+):(\d+(?:[.:]\d+)?)>([^<]*)""")

    fun fromLrc(key: String, lrc: String): Lyrics? {
        val entries = lrc.lineSequence().flatMap { raw ->
            val stamps = TIME.findAll(raw).toList()
            if (stamps.isEmpty()) return@flatMap emptySequence()
            val body = raw.substring(stamps.last().range.last + 1)
            stamps.asSequence().map { millis(it.groupValues[1], it.groupValues[2]) to body }
        }.sortedBy { it.first }.toList()
        val lines = entries.mapIndexedNotNull { index, (start, body) ->
            val end = entries.getOrNull(index + 1)?.first ?: (start + LAST_LINE_MS)
            val stamped = WORD.findAll(body).map { millis(it.groupValues[1], it.groupValues[2]) to it.groupValues[3] }.toList()
            val words = stamped.mapIndexed { wordIndex, (wordStart, text) ->
                LyricWord(text, wordStart, (stamped.getOrNull(wordIndex + 1)?.first ?: end).coerceAtLeast(wordStart))
            }.filter { it.text.isNotEmpty() }
            val text = (if (words.isEmpty()) body else words.joinToString("") { it.text }).trim()
            if (text.isEmpty()) null else LyricLine(text, start, end.coerceAtLeast(start), words)
        }
        return lines.takeIf { it.isNotEmpty() }?.let { Lyrics(key, it) }
    }

    private fun millis(minutes: String, seconds: String): Long {
        val normalized = seconds.replace(':', '.')
        return minutes.toLong() * MINUTE_MS + (normalized.toDoubleOrNull()?.times(SECOND_MS)?.toLong() ?: 0L)
    }

    private const val MINUTE_MS = 60_000L
    private const val SECOND_MS = 1000.0
    private const val LAST_LINE_MS = 5000L
}
