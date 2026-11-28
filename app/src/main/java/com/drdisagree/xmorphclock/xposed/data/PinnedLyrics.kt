package com.drdisagree.xmorphclock.xposed.data

import com.drdisagree.xmorphclock.data.mapper.LrcMuxJsonMapper
import com.drdisagree.xmorphclock.data.source.LyricsContract
import com.drdisagree.xmorphclock.domain.model.Lyrics
import com.drdisagree.xmorphclock.xposed.prefs.ModuleFiles
import java.io.FileInputStream

internal object PinnedLyrics {

    private var loadedFor: String? = null
    private var loaded: Lyrics? = null

    fun load(key: String, pinned: String): Lyrics? {
        val stamp = key + SEPARATOR + pinned
        if (stamp == loadedFor) return loaded
        loaded = runCatching {
            ModuleFiles.open(LyricsContract.fileName(key), preferApp = false)?.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).bufferedReader().readText()
            }?.let { LrcMuxJsonMapper.fromJson(key, it) }
        }.getOrNull()
        loadedFor = stamp.takeIf { loaded != null }
        return loaded
    }

    private const val SEPARATOR = "#"
}
