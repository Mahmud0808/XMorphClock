package com.drdisagree.xmorphclock.data.source

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.drdisagree.xmorphclock.data.mapper.LyricsTuningJsonMapper
import com.drdisagree.xmorphclock.domain.model.LyricsTuning
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class LyricsTuningStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE)

    val json: String
        get() = prefs.getString(PrefKeys.LYRICS_TUNING, null) ?: EMPTY

    val current: Map<String, LyricsTuning>
        get() = LyricsTuningJsonMapper.fromJson(json)

    val tunings: Flow<Map<String, LyricsTuning>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PrefKeys.LYRICS_TUNING) trySend(current)
        }
        trySend(current)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    fun set(key: String, tuning: LyricsTuning): String {
        val next = LinkedHashMap(current)
        next.remove(key)
        if (!tuning.isDefault) next[key] = tuning
        while (next.size > MAX_SONGS) next.remove(next.keys.first())
        val encoded = LyricsTuningJsonMapper.toJson(next)
        prefs.edit { putString(PrefKeys.LYRICS_TUNING, encoded) }
        return encoded
    }

    private companion object {
        const val MAX_SONGS = 200
        const val EMPTY = "{}"
    }
}
