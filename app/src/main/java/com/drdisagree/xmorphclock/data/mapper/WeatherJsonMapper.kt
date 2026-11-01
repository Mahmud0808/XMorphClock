package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.domain.model.WeatherSnapshot
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
                timestamp = root.optLong("timestamp")
            )
        } catch (_: Exception) {
            null
        }
    }
}
