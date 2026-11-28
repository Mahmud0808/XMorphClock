package com.drdisagree.xmorphclock.data.source

import com.drdisagree.xmorphclock.BuildConfig
import com.drdisagree.xmorphclock.data.mapper.LrcMapper
import com.drdisagree.xmorphclock.data.mapper.LrcMuxJsonMapper
import com.drdisagree.xmorphclock.domain.model.Lyrics
import com.drdisagree.xmorphclock.domain.model.LyricsVersion
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs
import org.json.JSONArray

object OnlineLyricsSource {

    fun keyOf(title: String, artist: String?): String = "${artist.orEmpty().lowercase()}|${title.lowercase()}"

    @Throws(IOException::class)
    fun fetch(title: String, artist: String?, album: String?, durationMs: Long): OnlineLyricsMatch? {
        val fits = ArrayList<OnlineLyricsCandidate>()
        val others = ArrayList<OnlineLyricsCandidate>()
        collect(title, artist, album, durationMs, quick = true, fits, others) { fits.isNotEmpty() }
        fits.firstOrNull()?.let { return OnlineLyricsMatch(it.version.lyrics, exact = true) }
        return others.minByOrNull { it.gapSeconds ?: 0.0 }?.let { OnlineLyricsMatch(it.version.lyrics, exact = false) }
    }

    @Throws(IOException::class)
    fun search(title: String, artist: String?, album: String?, durationMs: Long): List<LyricsVersion> {
        val fits = ArrayList<OnlineLyricsCandidate>()
        val others = ArrayList<OnlineLyricsCandidate>()
        collect(title, artist, album, durationMs, quick = false, fits, others) { false }
        return (fits + others.sortedBy { it.gapSeconds ?: 0.0 }).map { it.version }
    }

    private fun collect(
        title: String,
        artist: String?,
        album: String?,
        durationMs: Long,
        quick: Boolean,
        fits: MutableList<OnlineLyricsCandidate>,
        others: MutableList<OnlineLyricsCandidate>,
        done: () -> Boolean
    ) {
        val key = keyOf(title, artist)
        var failure: IOException? = null
        LyricsQueries.of(title, artist).forEach { query ->
            val found = try {
                candidates(key, query, album, durationMs, quick)
            } catch (error: IOException) {
                if (failure == null) failure = error
                return@forEach
            }
            found.forEach { candidate ->
                val known = (fits + others).any { same(it.version.lyrics, candidate.version.lyrics) }
                if (known) return@forEach
                val gap = candidate.gapSeconds
                if (gap == null || gap <= toleranceOf(query)) fits += candidate else others += candidate
            }
            if (done()) return
        }
        failure?.let { if (fits.isEmpty() && others.isEmpty()) throw it }
    }

    private fun candidates(key: String, query: LyricsQuery, album: String?, durationMs: Long, quick: Boolean): List<OnlineLyricsCandidate> {
        var failure: IOException? = null
        val muxed = try {
            lrcmux(key, query, album, durationMs)
        } catch (error: IOException) {
            failure = error
            null
        }
        if (quick && muxed != null && (muxed.gapSeconds ?: 0.0) <= toleranceOf(query)) return listOf(muxed)
        val library = try {
            lrclib(key, query, durationMs)
        } catch (error: IOException) {
            if (muxed == null) throw failure ?: error
            emptyList()
        }
        return listOfNotNull(muxed) + library
    }

    private fun lrcmux(key: String, query: LyricsQuery, album: String?, durationMs: Long): OnlineLyricsCandidate? {
        val params = buildList {
            add("title" to query.title)
            query.artist?.let { add("artist" to it) }
            if (query.strict) album?.let { add("album" to it) }
            if (durationMs > 0) add("duration" to (durationMs / SECOND_MS).toString())
            add("level" to "word")
            add("format" to "json")
        }
        val body = get(LRCMUX_URL + queryOf(params)) ?: return null
        if (!LyricsQueries.sameTitle(query.title, LrcMuxJsonMapper.trackTitle(body))) return null
        val lyrics = LrcMuxJsonMapper.fromJson(key, body) ?: return null
        val seconds = LrcMuxJsonMapper.trackSeconds(body)
        val source = LrcMuxJsonMapper.sourceName(body)?.let { "$it$VIA$LRCMUX" } ?: LRCMUX
        val id = LRCMUX_ID + (LrcMuxJsonMapper.trackId(body) ?: lyrics.lines.hashCode().toString())
        return OnlineLyricsCandidate(LyricsVersion(id, source, LrcMuxJsonMapper.trackArtist(body), seconds, lyrics), gapOf(seconds, durationMs))
    }

    private fun lrclib(key: String, query: LyricsQuery, durationMs: Long): List<OnlineLyricsCandidate> {
        val params = buildList {
            add("track_name" to query.title)
            query.artist?.let { add("artist_name" to it) }
        }
        val body = get(LRCLIB_URL + queryOf(params)) ?: return emptyList()
        val results = runCatching { JSONArray(body) }.getOrNull() ?: return emptyList()
        return (0 until results.length())
            .mapNotNull { results.optJSONObject(it) }
            .filter { it.optString(SYNCED).isNotBlank() }
            .filter { LyricsQueries.sameTitle(query.title, it.optString(TRACK_NAME)) }
            .mapNotNull { item ->
                val lyrics = LrcMapper.fromLrc(key, item.optString(SYNCED)) ?: return@mapNotNull null
                val seconds = item.optDouble(DURATION, 0.0).takeIf { it > 0.0 }
                val version = LyricsVersion(LRCLIB_ID + item.optLong(ID), LRCLIB, item.optString(ARTIST_NAME).takeIf { it.isNotBlank() }, seconds, lyrics)
                OnlineLyricsCandidate(version, gapOf(seconds, durationMs))
            }
            .sortedBy { it.gapSeconds ?: 0.0 }
    }

    private fun gapOf(seconds: Double?, durationMs: Long): Double? {
        if (durationMs <= 0 || seconds == null || seconds <= 0.0) return null
        return abs(seconds - durationMs / SECOND_MS.toDouble())
    }

    private fun toleranceOf(query: LyricsQuery): Double = if (query.strict) STRICT_TOLERANCE else LOOSE_TOLERANCE

    private fun same(first: Lyrics, second: Lyrics): Boolean {
        if (first.lines.size != second.lines.size) return false
        return first.lines.zip(second.lines).all { (a, b) ->
            abs(a.start - b.start) <= SAME_LINE_MS && a.text.trim().equals(b.text.trim(), ignoreCase = true)
        }
    }

    private fun queryOf(params: List<Pair<String, String>>): String =
        params.joinToString("&", prefix = "?") { (name, value) -> "$name=${URLEncoder.encode(value, Charsets.UTF_8.name())}" }

    private fun get(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> connection.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw IOException("HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private const val LRCMUX_URL = "https://api.lrcmux.dev/get"
    private const val LRCLIB_URL = "https://lrclib.net/api/search"
    private const val USER_AGENT = "XMorphClock/" + BuildConfig.VERSION_NAME
    private const val SYNCED = "syncedLyrics"
    private const val TRACK_NAME = "trackName"
    private const val ARTIST_NAME = "artistName"
    private const val ID = "id"
    private const val LRCMUX = "lrcmux"
    private const val LRCLIB = "LRCLIB"
    private const val VIA = " via "
    private const val LRCMUX_ID = "lrcmux:"
    private const val LRCLIB_ID = "lrclib:"
    private const val DURATION = "duration"
    private const val STRICT_TOLERANCE = 3.0
    private const val LOOSE_TOLERANCE = 15.0
    private const val SAME_LINE_MS = 300L
    private const val SECOND_MS = 1000
    private const val TIMEOUT_MS = 8000
}
