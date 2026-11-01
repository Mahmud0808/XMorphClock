package com.drdisagree.xmorphclock.data.source.steps

import android.content.Context
import android.content.SharedPreferences
import com.drdisagree.xmorphclock.data.source.PrefKeys
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class StepsStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE)

    val steps: Flow<Int?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PrefKeys.STEPS_TODAY) trySend(current())
        }
        trySend(current())
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    private fun current(): Int? = if (prefs.contains(PrefKeys.STEPS_TODAY)) prefs.getInt(PrefKeys.STEPS_TODAY, 0) else null
}
