package com.drdisagree.xmorphclock.engine.render

import android.text.format.DateFormat
import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.FaceLayer
import com.drdisagree.xmorphclock.domain.model.FaceLayerType
import java.util.Calendar
import java.util.Locale

internal object TextTokens {

    private val TOKEN = Regex("""\{([^{}]+)\}""")

    private val DATA = mapOf(
        "temp" to FaceLayerType.WEATHER_TEMP,
        "condition" to FaceLayerType.WEATHER_CONDITION,
        "highlow" to FaceLayerType.WEATHER_HIGH_LOW,
        "humidity" to FaceLayerType.WEATHER_HUMIDITY,
        "wind" to FaceLayerType.WEATHER_WIND,
        "city" to FaceLayerType.WEATHER_LOCATION,
        "battery" to FaceLayerType.BATTERY_LEVEL,
        "steps" to FaceLayerType.STEPS,
        "data" to FaceLayerType.DATA_USAGE
    )

    private fun words(key: String, calendar: Calendar): String? {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return when (key) {
            "hour_word" -> WordTime.hour(hour, Locale.getDefault())
            "minute_word" -> WordTime.minute(minute, Locale.getDefault())
            "time_words" -> WordTime.time(hour, minute, Locale.getDefault())
            "fuzzy_time" -> WordTime.fuzzy(hour, minute)
            "fuzzy_lead" -> WordTime.fuzzyLead(hour, minute)
            "fuzzy_hour" -> WordTime.fuzzyHour(hour, minute)
            else -> null
        }
    }

    fun expand(format: String, calendar: Calendar, data: FaceData): String {
        if ('{' !in format) return format
        return TOKEN.replace(format) { match ->
            val key = match.groupValues[1].trim()
            words(key.lowercase(), calendar)
                ?: DATA[key.lowercase()]?.let { type -> FaceValues.text(FaceLayer(id = "", type = type), data).orEmpty() }
                ?: runCatching { DateFormat.format(key, calendar).toString() }.getOrDefault(match.value)
        }
    }
}
