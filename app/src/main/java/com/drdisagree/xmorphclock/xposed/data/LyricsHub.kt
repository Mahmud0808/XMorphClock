package com.drdisagree.xmorphclock.xposed.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.drdisagree.xmorphclock.data.source.OnlineLyricsSource
import com.drdisagree.xmorphclock.domain.model.Lyrics
import com.drdisagree.xmorphclock.domain.model.LyricsOrigin
import com.drdisagree.xmorphclock.domain.model.LyricsTuning
import com.drdisagree.xmorphclock.domain.model.PlaybackClock
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.prefs.ModulePrefs
import java.util.concurrent.CopyOnWriteArrayList

internal object LyricsHub {

    @Volatile
    var lyrics: Lyrics? = null
        private set

    @Volatile
    var clock: PlaybackClock? = null
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private var started = false
    private val changed = Runnable { resolve() }
    private var offset = 0L
    private var lastOrigin: LyricsOrigin? = null
    private var lastKey: String? = null

    fun start(context: Context) {
        if (started) return
        started = true
        MediaWatcher.start(context)
        LyricsReporter.start(context)
        MediaWatcher.addListener(::post)
        ModulePrefs.addListener(::post)
        OnlineLyrics.start(::post)
        LyriconLyrics.start(context, ::post)
        SuperLyricLyrics.start(::post)
        resolve()
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    private fun post() {
        mainHandler.removeCallbacks(changed)
        mainHandler.post(changed)
    }

    private fun resolve() {
        val title = MediaWatcher.title
        val artist = MediaWatcher.artist
        val key = title?.let { OnlineLyricsSource.keyOf(it, artist) }
        val tuning = key?.let { ModulePrefs.lyricsTuning[it] } ?: LyricsTuning()
        val hub = LyriconLyrics.lyrics?.let { it to LyriconLyrics.clock }
            ?: SuperLyricLyrics.lyrics?.let { it to SuperLyricLyrics.clock }
        val pinned = if (hub == null && key != null) tuning.pinned?.let { PinnedLyrics.load(key, it) } else null
        OnlineLyrics.update(hub == null && pinned == null && ModulePrefs.isLyricsOnline)
        val found = hub?.first ?: pinned ?: OnlineLyrics.lyrics
        val origin = when {
            hub != null -> LyricsOrigin.PLAYER
            pinned != null -> LyricsOrigin.PINNED
            found != null && !OnlineLyrics.exact -> LyricsOrigin.ONLINE_OTHER_LENGTH
            found != null -> LyricsOrigin.ONLINE
            else -> LyricsOrigin.NONE
        }
        if (origin != lastOrigin || (key == null) != (lastKey == null)) {
            HookEnv.log("Lyrics: ${origin.name.lowercase()}, media ${if (key == null) "none" else "playing"}, online ${ModulePrefs.isLyricsOnline}")
            lastOrigin = origin
            lastKey = key
        }
        LyricsReporter.update(key, title, artist, MediaWatcher.album, MediaWatcher.durationMs, origin)
        val nextLyrics = found?.takeUnless { tuning.hidden }
        val baseClock = hub?.second ?: MediaWatcher.clock
        val nextClock = if (tuning.offsetMs == 0L) baseClock else baseClock?.let { it.copy(position = it.position + tuning.offsetMs) }
        val now = SystemClock.elapsedRealtime()
        val clockSame = tuning.offsetMs == offset && if (nextClock == null) clock == null else nextClock.matches(clock, now, CLOCK_TOLERANCE_MS)
        if (nextLyrics == lyrics && clockSame) return
        lyrics = nextLyrics
        clock = nextClock
        offset = tuning.offsetMs
        listeners.forEach { it() }
    }

    private const val CLOCK_TOLERANCE_MS = 250L
}
