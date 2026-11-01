package com.drdisagree.xmorphclock.data.source.weather

import android.content.Context
import com.drdisagree.xmorphclock.domain.model.WeatherSnapshot
import kotlin.math.roundToInt

internal object OpenWeatherMapSource {

    private const val BASE = "https://api.openweathermap.org/data/2.5/weather"
    private const val MS_TO_KMH = 3.6

    suspend fun fetch(
        context: Context,
        latitude: Double,
        longitude: Double,
        metric: Boolean,
        key: String,
        location: String
    ): WeatherSnapshot? {
        if (key.isBlank()) return null
        val url = "$BASE?lat=$latitude&lon=$longitude&units=${if (metric) "metric" else "imperial"}&appid=$key"
        val root = HttpJson.get(url) ?: return null
        val main = root.optJSONObject("main") ?: return null
        val condition = root.optJSONArray("weather")?.optJSONObject(0)
        val icon = condition?.optString("icon").orEmpty()
        val isDay = !icon.endsWith("n")
        val code = WeatherCodes.fromOpenWeatherMap(condition?.optInt("id") ?: -1, isDay)
        val speed = root.optJSONObject("wind")?.optDouble("speed") ?: 0.0
        return WeatherSnapshot(
            temperature = main.optDouble("temp").roundToInt(),
            high = main.optDouble("temp_max").roundToInt(),
            low = main.optDouble("temp_min").roundToInt(),
            conditionCode = code,
            condition = WeatherCodes.condition(context, code),
            isDay = isDay,
            humidity = main.optInt("humidity"),
            wind = (if (metric) speed * MS_TO_KMH else speed).roundToInt(),
            windUnit = if (metric) "km/h" else "mph",
            location = location.ifEmpty { root.optString("name") },
            metric = metric,
            timestamp = System.currentTimeMillis()
        )
    }
}
