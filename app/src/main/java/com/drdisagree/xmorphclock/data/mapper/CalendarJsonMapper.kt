package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.domain.model.CalendarEvent
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object CalendarJsonMapper {

    fun toJson(events: List<CalendarEvent>): String = JSONArray().apply {
        events.forEach { event ->
            put(
                JSONObject()
                    .put("title", event.title)
                    .put("begin", event.begin)
                    .put("end", event.end)
                    .put("allDay", event.allDay)
                    .put("location", event.location)
            )
        }
    }.toString()

    fun fromJson(json: String?): List<CalendarEvent> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                CalendarEvent(
                    title = item.optString("title"),
                    begin = item.optLong("begin"),
                    end = item.optLong("end"),
                    allDay = item.optBoolean("allDay"),
                    location = item.optString("location")
                )
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }
}
