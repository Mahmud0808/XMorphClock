package com.drdisagree.xmorphclock.xposed.data

import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import com.drdisagree.xmorphclock.BuildConfig
import com.drdisagree.xmorphclock.domain.model.LyricLine
import com.drdisagree.xmorphclock.domain.model.LyricWord
import com.drdisagree.xmorphclock.domain.model.Lyrics
import com.drdisagree.xmorphclock.domain.model.PlaybackClock
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import io.github.proify.lyricon.lyric.model.Song
import io.github.proify.lyricon.subscriber.ActivePlayerListener
import io.github.proify.lyricon.subscriber.LyriconFactory
import io.github.proify.lyricon.subscriber.LyriconSubscriber
import io.github.proify.lyricon.subscriber.ProviderInfo

internal object LyriconLyrics {

    @Volatile
    var lyrics: Lyrics? = null
        private set

    @Volatile
    var clock: PlaybackClock? = null
        private set

    private var subscriber: LyriconSubscriber? = null
    private var onChange: () -> Unit = {}
    private var playing = false

    private val listener = object : ActivePlayerListener {
        override fun onActiveProviderChanged(providerInfo: ProviderInfo?) {
            if (providerInfo != null) return
            lyrics = null
            clock = null
            onChange()
        }

        override fun onSongChanged(song: Song?) {
            lyrics = song?.let(::lyricsOf)
            onChange()
        }

        override fun onReceiveText(text: String?) = Unit

        override fun onPlaybackStateChanged(isPlaying: Boolean) {
            playing = isPlaying
            val now = SystemClock.elapsedRealtime()
            clock = PlaybackClock(clock?.positionAt(now) ?: 0L, now, playing = isPlaying)
            onChange()
        }

        override fun onPositionChanged(position: Long) = moveTo(position)

        override fun onSeekTo(position: Long) = moveTo(position)

        override fun onDisplayTranslationChanged(isDisplayTranslation: Boolean) = Unit

        override fun onDisplayRomaChanged(isDisplayRoma: Boolean) = Unit
    }

    fun start(context: Context, onChange: () -> Unit) {
        if (subscriber != null) return
        this.onChange = onChange
        runCatching {
            val identity = object : ContextWrapper(context.applicationContext ?: context) {
                override fun getPackageName(): String = BuildConfig.APPLICATION_ID
            }
            LyriconFactory.createSubscriber(identity).also {
                it.subscribeActivePlayer(listener)
                it.register()
                subscriber = it
            }
        }.onFailure { HookEnv.log("Lyricon subscriber unavailable", it) }
    }

    private fun moveTo(position: Long) {
        clock = PlaybackClock(position, SystemClock.elapsedRealtime(), playing = playing)
        onChange()
    }

    private fun lyricsOf(song: Song): Lyrics? {
        val lines = song.lyrics.orEmpty().mapNotNull { line ->
            val words = line.words.orEmpty().map { LyricWord(it.text.orEmpty(), it.begin, it.end.coerceAtLeast(it.begin)) }
            val text = line.text?.takeIf { it.isNotBlank() } ?: words.joinToString("") { it.text }
            if (text.isBlank()) null else LyricLine(text.trim(), line.begin, line.end.coerceAtLeast(line.begin), words)
        }.sortedBy { it.start }
        if (lines.isEmpty()) return null
        return Lyrics(song.id ?: "${song.artist.orEmpty()}|${song.name.orEmpty()}", lines)
    }
}
