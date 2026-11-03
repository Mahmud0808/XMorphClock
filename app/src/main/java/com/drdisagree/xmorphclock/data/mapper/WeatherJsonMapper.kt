package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.domain.model.HourlyPoint
import com.drdisagree.xmorphclock.domain.model.WeatherSnapshot
import org.json.JSONArray
import org.json.JSONObject

object WeatherJsonMapper {

    fun toJson(weather: WeatherSnapshot): String = JSONObject()
        .put("temperature", weather.temperature)
        .put("high", weather.high)
        .put("low", weather.low)
        .put("code", weather.conditionCode)
        .put("condition", weather.condition)
        .put("day", weather.isDay)
        .put("humidity", weather.humidity)
        .put("wind", weather.wind)
        .put("windUnit", weather.windUnit)
        .put("location", weather.location)
        .put("metric", weather.metric)
        .put("timestamp", weather.timestamp)
        .put("sunrises", JSONArray(weather.sunrises))
        .put("sunsets", JSONArray(weather.sunsets))
        .apply { weather.uvIndex?.let { put("uv", it.toDouble()) } }
        .put("hourly", JSONArray().apply {
            weather.hourly.forEach { point ->
                put(JSONObject().put("t", point.time).put("temp", point.temperature).put("code", point.conditionCode).put("day", point.isDay))
            }
        })
        .toString()

    fun fromJson(json: String?): WeatherSnapshot? {
        if (json.isNullOrEmpty()) return null
        return try {
            val root = JSONObject(json)
            WeatherSnapshot(
                temperature = root.getInt("temperature"),
                high = root.optInt("high"),
                low = root.optInt("low"),
                conditionCode = root.optInt("code", -1),
                condition = root.optString("condition"),
                isDay = root.optBoolean("day", true),
                humidity = root.optInt("humidity"),
                wind = root.optInt("wind"),
                windUnit = root.optString("windUnit"),
                location = root.optString("location"),
                metric = root.optBoolean("metric", true),
                timestamp = root.optLong("timestamp"),
                sunrises = longs(root.optJSONArray("sunrises")),
                sunsets = longs(root.optJSONArray("sunsets")),
                uvIndex = if (root.has("uv")) root.optDouble("uv").toFloat() else null,
                hourly = root.optJSONArray("hourly")?.let { array ->
                    (0 until array.length()).mapNotNull { index ->
                        array.optJSONObject(index)?.let { HourlyPoint(it.optLong("t"), it.optInt("temp"), it.optInt("code", -1), it.optBoolean("day", true)) }
                    }
                }.orEmpty()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun longs(array: JSONArray?): List<Long> =
        array?.let { (0 until it.length()).map { index -> it.optLong(index) }.filter { value -> value > 0L } }.orEmpty()
}
