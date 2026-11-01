package com.drdisagree.xmorphclock.data.source.weather

import android.content.Context
import com.drdisagree.xmorphclock.domain.model.WeatherSnapshot
import kotlin.math.roundToInt

internal object OpenMeteoSource {

    private const val BASE = "https://api.open-meteo.com/v1/forecast"

    suspend fun fetch(context: Context, latitude: Double, longitude: Double, metric: Boolean, location: String): WeatherSnapshot? {
        val units = if (metric) "" else "&temperature_unit=fahrenheit&wind_speed_unit=mph"
        val url = "$BASE?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day" +
                "&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto$units"
        val root = HttpJson.get(url) ?: return null
        val current = root.optJSONObject("current") ?: return null
        val daily = root.optJSONObject("daily")
        val isDay = current.optInt("is_day", 1) == 1
        val code = WeatherCodes.fromWmo(current.optInt("weather_code", -1), isDay)
        return WeatherSnapshot(
            temperature = current.optDouble("temperature_2m").roundToInt(),
            high = daily?.optJSONArray("temperature_2m_max")?.optDouble(0)?.roundToInt() ?: 0,
            low = daily?.optJSONArray("temperature_2m_min")?.optDouble(0)?.roundToInt() ?: 0,
            conditionCode = code,
            condition = WeatherCodes.condition(context, code),
            isDay = isDay,
            humidity = current.optInt("relative_humidity_2m"),
            wind = current.optDouble("wind_speed_10m").roundToInt(),
            windUnit = if (metric) "km/h" else "mph",
            location = location,
            metric = metric,
            timestamp = System.currentTimeMillis()
        )
    }
}
