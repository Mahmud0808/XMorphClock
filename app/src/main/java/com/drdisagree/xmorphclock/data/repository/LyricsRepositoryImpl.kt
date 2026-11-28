package com.drdisagree.xmorphclock.data.repository

import com.drdisagree.xmorphclock.data.mapper.LrcMuxJsonMapper
import com.drdisagree.xmorphclock.data.source.LyricsContract
import com.drdisagree.xmorphclock.data.source.LyricsStateSource
import com.drdisagree.xmorphclock.data.source.LyricsTuningStore
import com.drdisagree.xmorphclock.data.source.OnlineLyricsSource
import com.drdisagree.xmorphclock.data.source.PrefKeys
import com.drdisagree.xmorphclock.data.source.XposedServiceSource
import com.drdisagree.xmorphclock.domain.model.LyricsTuning
import com.drdisagree.xmorphclock.domain.model.LyricsVersion
import com.drdisagree.xmorphclock.domain.model.NowPlayingLyrics
import com.drdisagree.xmorphclock.domain.repository.LyricsRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class LyricsRepositoryImpl(
    private val state: LyricsStateSource,
    private val store: LyricsTuningStore,
    private val remote: XposedServiceSource
) : LyricsRepository {

    init {
        remote.addBindListener { remote.edit { it.putString(PrefKeys.LYRICS_TUNING, store.json) } }
    }

    override val nowPlaying: Flow<NowPlayingLyrics?> = combine(state.state, store.tunings) { playing, tunings ->
        playing?.copy(tuning = tunings[playing.key] ?: LyricsTuning())
    }

    override suspend fun setTuning(key: String, tuning: LyricsTuning) = withContext(Dispatchers.IO) {
        val json = store.set(key, tuning)
        remote.edit { it.putString(PrefKeys.LYRICS_TUNING, json) }
    }

    override suspend fun versions(playing: NowPlayingLyrics): List<LyricsVersion>? = withContext(Dispatchers.IO) {
        try {
            OnlineLyricsSource.search(playing.title, playing.artist, playing.album, playing.durationMs)
        } catch (_: IOException) {
            null
        }
    }

    override suspend fun pin(playing: NowPlayingLyrics, version: LyricsVersion?) = withContext(Dispatchers.IO) {
        val name = LyricsContract.fileName(playing.key)
        val tuning = store.current[playing.key] ?: LyricsTuning()
        if (version == null) {
            remote.deleteFile(name)
            setTuning(playing.key, tuning.copy(pinned = null))
            return@withContext
        }
        val json = LrcMuxJsonMapper.toJson(version.lyrics)
        if (!remote.writeFile(name) { it.write(json.toByteArray(Charsets.UTF_8)) }) return@withContext
        setTuning(playing.key, tuning.copy(pinned = version.id))
    }
}
