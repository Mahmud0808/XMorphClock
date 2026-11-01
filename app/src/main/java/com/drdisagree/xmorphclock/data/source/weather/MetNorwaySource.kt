package com.drdisagree.xmorphclock.data.source.weather

import android.content.Context
import com.drdisagree.xmorphclock.domain.model.WeatherSnapshot
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

internal object MetNorwaySource {

    private const val BASE = "https://api.met.no/weatherapi/locationforecast/2.0/compact"
    private const val COORDINATE_FORMAT = "%.4f"
    private const val DAY_SUFFIX = "_day"
    private const val NIGHT_SUFFIX = "_night"
    private const val TWILIGHT_SUFFIX = "_polartwilight"
    private const val MPS_TO_KMH = 3.6
    private const val MPS_TO_MPH = 2.23694
    private const val FALLBACK_HOURS = 24
    private const val DAY_START = 6
    private const val DAY_END = 18

    suspend fun fetch(context: Context, latitude: Double, longitude: Double, metric: Boolean, location: String): WeatherSnapshot? {
        val url = "$BASE?lat=${COORDINATE_FORMAT.format(Locale.ROOT, latitude)}&lon=${COORDINATE_FORMAT.format(Locale.ROOT, longitude)}"
        val series = HttpJson.get(url)?.optJSONObject("properties")?.optJSONArray("timeseries") ?: return null
        val entries = List(series.length()) { series.optJSONObject(it) }.filterNotNull()
        val first = entries.firstOrNull()?.optJSONObject("data") ?: return null
        val details = first.optJSONObject("instant")?.optJSONObject("details") ?: return null
        val symbol = first.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code")
            ?: first.optJSONObject("next_6_hours")?.optJSONObject("summary")?.optString("symbol_code")
            ?: ""
        val isDay = when {
            symbol.endsWith(DAY_SUFFIX) -> true
            symbol.endsWith(NIGHT_SUFFIX) || symbol.endsWith(TWILIGHT_SUFFIX) -> false
            else -> LocalTime.now().hour in DAY_START until DAY_END
        }
        val code = WeatherCodes.fromMetNorway(symbol.substringBefore('_'), isDay)
        val temperatures = todayTemperatures(entries)
        val wind = details.optDouble("wind_speed", 0.0)
        return WeatherSnapshot(
            temperature = temperature(details.optDouble("air_temperature"), metric),
            high = temperatures.maxOrNull()?.let { temperature(it, metric) } ?: 0,
            low = temperatures.minOrNull()?.let { temperature(it, metric) } ?: 0,
            conditionCode = code,
            condition = WeatherCodes.condition(context, code),
            isDay = isDay,
            humidity = details.optDouble("relative_humidity", 0.0).roundToInt(),
            wind = (wind * if (metric) MPS_TO_KMH else MPS_TO_MPH).roundToInt(),
            windUnit = if (metric) "km/h" else "mph",
            location = location,
            metric = metric,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun todayTemperatures(entries: List<JSONObject>): List<Double> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val values = entries.mapNotNull { entry ->
            val time = runCatching { Instant.parse(entry.optString("time")) }.getOrNull() ?: return@mapNotNull null
            val value = entry.optJSONObject("data")?.optJSONObject("instant")?.optJSONObject("details")?.optDouble("air_temperature")
            if (value == null || value.isNaN() || time.atZone(zone).toLocalDate() != today) null else value
        }
        if (values.isNotEmpty()) return values
        return entries.take(FALLBACK_HOURS).mapNotNull { entry ->
            entry.optJSONObject("data")?.optJSONObject("instant")?.optJSONObject("details")?.optDouble("air_temperature")?.takeIf { !it.isNaN() }
        }
    }

    private fun temperature(celsius: Double, metric: Boolean): Int = (if (metric) celsius else celsius * 9.0 / 5.0 + 32.0).roundToInt()
}
