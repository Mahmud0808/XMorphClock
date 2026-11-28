package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.domain.model.LyricLine
import com.drdisagree.xmorphclock.domain.model.LyricWord
import com.drdisagree.xmorphclock.domain.model.Lyrics
import org.json.JSONArray
import org.json.JSONObject

object LrcMuxJsonMapper {

    fun fromJson(key: String, json: String): Lyrics? = runCatching {
        val lines = JSONObject(json).optJSONArray(LINES) ?: return null
        val parsed = (0 until lines.length()).mapNotNull { index ->
            val line = lines.optJSONObject(index) ?: return@mapNotNull null
            if (!line.has(START)) return@mapNotNull null
            val words = words(line.optJSONArray(WORDS))
            val text = line.optString(TEXT).ifBlank { words.joinToString("") { it.text } }.trim()
            if (text.isEmpty()) return@mapNotNull null
            val start = line.optLong(START)
            LyricLine(text, start, line.optLong(END, start).coerceAtLeast(start), words)
        }.sortedBy { it.start }
        parsed.takeIf { it.isNotEmpty() }?.let { Lyrics(key, it) }
    }.getOrNull()

    fun toJson(lyrics: Lyrics): String {
        val lines = JSONArray()
        lyrics.lines.forEach { line ->
            val words = JSONArray()
            line.words.forEach { word ->
                words.put(JSONObject().put(TEXT, word.text).put(START, word.start).put(END, word.end))
            }
            lines.put(JSONObject().put(TEXT, line.text).put(START, line.start).put(END, line.end).put(WORDS, words))
        }
        return JSONObject().put(LINES, lines).toString()
    }

    fun trackArtist(json: String): String? = runCatching {
        JSONObject(json).optJSONObject(TRACK)?.optString(ARTIST)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun trackId(json: String): String? = runCatching {
        JSONObject(json).optJSONObject(TRACK)?.optString(ISRC)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun sourceName(json: String): String? = runCatching {
        JSONObject(json).optJSONObject(META)?.optJSONObject(SOURCE)?.optString(NAME)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun trackTitle(json: String): String? = runCatching {
        JSONObject(json).optJSONObject(TRACK)?.optString(TITLE)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun trackSeconds(json: String): Double? = runCatching {
        JSONObject(json).optJSONObject(TRACK)?.optDouble(DURATION)?.takeIf { !it.isNaN() && it > 0.0 }
    }.getOrNull()

    private fun words(array: JSONArray?): List<LyricWord> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val word = array.optJSONObject(index) ?: return@mapNotNull null
            val start = word.optLong(START)
            LyricWord(word.optString(TEXT), start, word.optLong(END, start).coerceAtLeast(start))
        }.sortedBy { it.start }
    }

    private const val LINES = "lines"
    private const val WORDS = "words"
    private const val TEXT = "text"
    private const val START = "start"
    private const val END = "end"
    private const val TRACK = "track"
    private const val ARTIST = "artist"
    private const val ISRC = "isrc"
    private const val META = "meta"
    private const val SOURCE = "source"
    private const val NAME = "name"
    private const val TITLE = "title"
    private const val DURATION = "duration"
}
