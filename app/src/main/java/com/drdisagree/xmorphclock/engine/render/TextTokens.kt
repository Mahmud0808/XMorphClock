package com.drdisagree.xmorphclock.engine.render

import android.text.format.DateFormat
import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.FaceLayer
import com.drdisagree.xmorphclock.domain.model.FaceLayerType
import java.util.Calendar

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

    fun expand(format: String, calendar: Calendar, data: FaceData): String {
        if ('{' !in format) return format
        return TOKEN.replace(format) { match ->
            val key = match.groupValues[1].trim()
            DATA[key.lowercase()]?.let { type -> FaceValues.text(FaceLayer(id = "", type = type), data).orEmpty() }
                ?: runCatching { DateFormat.format(key, calendar).toString() }.getOrDefault(match.value)
        }
    }
}
