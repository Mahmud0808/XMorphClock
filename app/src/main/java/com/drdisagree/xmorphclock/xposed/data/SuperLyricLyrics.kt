package com.drdisagree.xmorphclock.xposed.data

import android.os.SystemClock
import com.drdisagree.xmorphclock.domain.model.LyricLine
import com.drdisagree.xmorphclock.domain.model.LyricWord
import com.drdisagree.xmorphclock.domain.model.Lyrics
import com.drdisagree.xmorphclock.domain.model.PlaybackClock
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.hchen.superlyricapi.LyricListener
import com.hchen.superlyricapi.LyricView
import com.hchen.superlyricapi.SuperLyric
import com.hchen.superlyricapi.model.Lyric
import com.hchen.superlyricapi.model.PlaybackState
import com.hchen.superlyricapi.model.Stop
import java.util.concurrent.Executors

internal object SuperLyricLyrics {

    @Volatile
    var lyrics: Lyrics? = null
        private set

    @Volatile
    var clock: PlaybackClock? = null
        private set

    private val worker = Executors.newSingleThreadExecutor()
    private var started = false
    private var onChange: () -> Unit = {}
    private var source: Lyric? = null

    private val listener = object : LyricListener {
        override fun onLyric(view: LyricView) {
            if (view.lyric !== source) {
                source = view.lyric
                lyrics = lyricsOf(view.lyric)
            }
            clock = PlaybackClock(view.position, SystemClock.elapsedRealtime(), view.speed.takeIf { it > 0f } ?: 1f, view.state == PlaybackState.PLAYING)
            onChange()
        }

        override fun onStop(stop: Stop) {
            source = null
            lyrics = null
            clock = null
            onChange()
        }
    }

    fun start(onChange: () -> Unit) {
        if (started) return
        started = true
        this.onChange = onChange
        worker.execute {
            runCatching {
                if (SuperLyric.isAvailable()) SuperLyric.receiver().register(listener)
            }.onFailure { HookEnv.log("SuperLyric receiver unavailable", it) }
        }
    }

    private fun lyricsOf(lyric: Lyric): Lyrics? {
        val lines = lyric.lines.mapNotNull { line ->
            val words = line.words.orEmpty().map { LyricWord(it.text, it.start, it.end) }
            val text = line.text.ifBlank { words.joinToString("") { it.text } }.trim()
            if (text.isEmpty()) null else LyricLine(text, line.start, line.end, words)
        }
        return lines.takeIf { it.isNotEmpty() }?.let { Lyrics(lyric.id, it) }
    }
}
