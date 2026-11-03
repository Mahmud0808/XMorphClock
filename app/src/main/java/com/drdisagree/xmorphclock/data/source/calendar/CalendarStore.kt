package com.drdisagree.xmorphclock.data.source.calendar

import android.content.Context
import android.content.SharedPreferences
import com.drdisagree.xmorphclock.data.mapper.CalendarJsonMapper
import com.drdisagree.xmorphclock.data.source.PrefKeys
import com.drdisagree.xmorphclock.domain.model.CalendarEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class CalendarStore(context: Context) {

    private val prefs = context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE)

    val events: Flow<List<CalendarEvent>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PrefKeys.CALENDAR_JSON) trySend(current())
        }
        trySend(current())
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    private fun current(): List<CalendarEvent> = CalendarJsonMapper.fromJson(prefs.getString(PrefKeys.CALENDAR_JSON, null))
}
