package com.drdisagree.xmorphclock.xposed.data

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.drdisagree.xmorphclock.data.source.OnlineLyricsMatch
import com.drdisagree.xmorphclock.data.source.OnlineLyricsSource
import com.drdisagree.xmorphclock.domain.model.Lyrics
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import java.util.concurrent.Executors

internal object OnlineLyrics {

    @Volatile
    var lyrics: Lyrics? = null
        private set

    @Volatile
    var exact = true
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val cache = LinkedHashMap<String, OnlineLyricsMatch?>(CACHE_SIZE, LOAD_FACTOR, true)
    private var onChange: () -> Unit = {}
    private var requested: String? = null
    private var failedKey: String? = null
    private var failedAt = 0L

    fun start(onChange: () -> Unit) {
        this.onChange = onChange
    }

    fun update(enabled: Boolean) {
        val title = MediaWatcher.title
        if (!enabled || title == null) {
            requested = null
            if (lyrics != null) {
                apply(null)
                onChange()
            }
            return
        }
        val artist = MediaWatcher.artist
        val key = OnlineLyricsSource.keyOf(title, artist)
        if (key == requested) return
        if (cache.containsKey(key)) {
            requested = key
            apply(cache[key])
            onChange()
            return
        }
        if (key == failedKey && SystemClock.elapsedRealtime() - failedAt < RETRY_MS) return
        requested = key
        if (lyrics != null) {
            apply(null)
            onChange()
        }
        val album = MediaWatcher.album
        val duration = MediaWatcher.durationMs
        worker.execute {
            val result = try {
                Result.success(OnlineLyricsSource.fetch(title, artist, album, duration))
            } catch (error: Throwable) {
                Result.failure(error)
            }
            mainHandler.post {
                result.onSuccess { found ->
                    HookEnv.log("Online lyrics: ${found?.lyrics?.lines?.size ?: 0} lines, ${if (found?.exact == false) "length differs" else "length matches"}")
                    cache[key] = found
                    while (cache.size > CACHE_SIZE) cache.remove(cache.keys.first())
                }.onFailure {
                    HookEnv.log("Online lyrics failed", it)
                    failedKey = key
                    failedAt = SystemClock.elapsedRealtime()
                }
                if (requested != key) return@post
                if (result.isFailure) requested = null
                apply(result.getOrNull())
                onChange()
            }
        }
    }

    private fun apply(match: OnlineLyricsMatch?) {
        lyrics = match?.lyrics
        exact = match?.exact ?: true
    }

    private const val CACHE_SIZE = 24
    private const val LOAD_FACTOR = 0.75f
    private const val RETRY_MS = 60_000L
}
