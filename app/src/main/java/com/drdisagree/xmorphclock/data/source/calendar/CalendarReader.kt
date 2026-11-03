package com.drdisagree.xmorphclock.data.source.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.edit
import com.drdisagree.xmorphclock.data.mapper.CalendarJsonMapper
import com.drdisagree.xmorphclock.data.source.PrefKeys
import com.drdisagree.xmorphclock.data.source.XposedServiceSource
import com.drdisagree.xmorphclock.domain.model.CalendarEvent
import java.util.TimeZone

class CalendarReader(private val context: Context) {

    private val prefs = context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE)

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun publish() {
        if (!hasPermission()) return
        val json = CalendarJsonMapper.toJson(read())
        if (prefs.getString(PrefKeys.CALENDAR_JSON, null) == json) return
        prefs.edit { putString(PrefKeys.CALENDAR_JSON, json) }
        XposedServiceSource.edit { it.putString(PrefKeys.CALENDAR_JSON, json) }
    }

    private fun read(): List<CalendarEvent> {
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also { builder ->
            ContentUris.appendId(builder, now - DAY_MS)
            ContentUris.appendId(builder, now + WINDOW_MS)
        }.build()
        val selection = "${CalendarContract.Instances.VISIBLE} = 1 AND ${CalendarContract.Instances.SELF_ATTENDEE_STATUS} != ${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}"
        val events = ArrayList<CalendarEvent>()
        runCatching {
            context.contentResolver.query(uri, PROJECTION, selection, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
                while (cursor.moveToNext()) {
                    val allDay = cursor.getInt(ALL_DAY) == 1
                    val begin = if (allDay) local(cursor.getLong(BEGIN)) else cursor.getLong(BEGIN)
                    val end = if (allDay) local(cursor.getLong(END)) else cursor.getLong(END)
                    if (end <= now) continue
                    events += CalendarEvent(cursor.getString(TITLE).orEmpty(), begin, end, allDay, cursor.getString(LOCATION).orEmpty())
                    if (events.size >= MAX_EVENTS) break
                }
            }
        }
        return events
    }

    private fun local(utc: Long): Long = utc - TimeZone.getDefault().getOffset(utc)

    private companion object {
        val PROJECTION = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION
        )
        const val TITLE = 0
        const val BEGIN = 1
        const val END = 2
        const val ALL_DAY = 3
        const val LOCATION = 4
        const val MAX_EVENTS = 8
        const val DAY_MS = 86_400_000L
        const val WINDOW_MS = 7 * DAY_MS
    }
}
