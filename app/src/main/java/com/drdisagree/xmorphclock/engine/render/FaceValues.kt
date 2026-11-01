package com.drdisagree.xmorphclock.engine.render

import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.FaceLayer
import com.drdisagree.xmorphclock.domain.model.FaceLayerType
import java.text.NumberFormat
import java.util.Locale

internal object FaceValues {

    private const val EMPTY = "--"
    private const val KILO = 1024.0
    private val UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

    fun progress(layer: FaceLayer, data: FaceData): Float? = when (layer.format) {
        GAUGE_BATTERY -> data.batteryLevel?.let { it / 100f }
        GAUGE_STEPS -> data.steps?.let { it / STEP_GOAL }
        GAUGE_WEATHER -> data.weather?.let { weather ->
            val span = (weather.high - weather.low).toFloat()
            if (span <= 0f) 0.5f else ((weather.temperature - weather.low) / span).coerceIn(0f, 1f)
        }
        GAUGE_HUMIDITY -> data.weather?.let { it.humidity / 100f }
        else -> null
    }

    fun text(layer: FaceLayer, data: FaceData): String? {
        val weather = data.weather
        val value = when (layer.type) {
            FaceLayerType.WEATHER_TEMP -> weather?.let { "${it.temperature}°" }
            FaceLayerType.WEATHER_ICON -> weather?.let { FaceGlyphs.weather(it.conditionCode, it.isDay) } ?: FaceGlyphs.weather(-1, true)
            FaceLayerType.WEATHER_CONDITION -> weather?.condition
            FaceLayerType.WEATHER_HIGH_LOW -> weather?.let { template(layer, "${it.high}°", "${it.low}°") ?: "${it.high}° / ${it.low}°" }
            FaceLayerType.WEATHER_HUMIDITY -> weather?.let { "${it.humidity}%" }
            FaceLayerType.WEATHER_WIND -> weather?.let { "${it.wind} ${it.windUnit}" }
            FaceLayerType.WEATHER_LOCATION -> weather?.location
            FaceLayerType.BATTERY_LEVEL -> data.batteryLevel?.let { "$it%" }
            FaceLayerType.BATTERY_ICON -> FaceGlyphs.battery(data.batteryLevel ?: 100, data.charging)
            FaceLayerType.STEPS -> data.steps?.let { NumberFormat.getIntegerInstance(Locale.getDefault()).format(it) }
            FaceLayerType.DATA_USAGE -> data.dataUsageBytes?.let(::bytes)
            FaceLayerType.TILE -> layer.tile?.let { FaceGlyphs.tile(it, data.tileState(it)) }
            else -> return null
        } ?: EMPTY
        if (layer.isIcon || layer.type == FaceLayerType.WEATHER_HIGH_LOW) return value
        return template(layer, value) ?: value
    }

    private fun template(layer: FaceLayer, vararg values: String): String? =
        layer.format.takeIf { it.contains('%') }?.let { runCatching { it.format(*values) }.getOrNull() }

    private fun bytes(value: Long): String {
        var size = value.toDouble()
        var unit = 0
        while (size >= KILO && unit < UNITS.lastIndex) {
            size /= KILO
            unit++
        }
        return if (unit == 0 || size >= 100) "${size.toInt()} ${UNITS[unit]}" else "%.1f %s".format(size, UNITS[unit])
    }
}

private const val GAUGE_BATTERY = "battery"
private const val GAUGE_STEPS = "steps"
private const val GAUGE_WEATHER = "weather"
private const val GAUGE_HUMIDITY = "humidity"
private const val STEP_GOAL = 10_000f
