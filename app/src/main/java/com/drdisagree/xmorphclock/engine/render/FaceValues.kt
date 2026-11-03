package com.drdisagree.xmorphclock.engine.render

import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.FaceLayer
import com.drdisagree.xmorphclock.domain.model.FaceLayerType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
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

    fun text(layer: FaceLayer, data: FaceData, phrases: DataPhrases): String? {
        when (layer.type) {
            FaceLayerType.ALARM_TIME -> return data.nextAlarm?.let { alarm(it, data.use24Hour) } ?: phrases.noAlarm
            FaceLayerType.ALARM_ICON -> return FaceGlyphs.ALARM
            FaceLayerType.CHARGE_STATUS -> return charge(data, phrases)
            FaceLayerType.CHARGE_ICON -> return FaceGlyphs.BOLT
            else -> Unit
        }
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

    fun isIdle(layer: FaceLayer, data: FaceData): Boolean = when (layer.type) {
        FaceLayerType.ALARM_TIME, FaceLayerType.ALARM_ICON -> data.nextAlarm == null
        FaceLayerType.CHARGE_STATUS, FaceLayerType.CHARGE_ICON -> !isCharging(data)
        else -> false
    }

    private fun isCharging(data: FaceData): Boolean = data.charging || data.chargeRemainingMs != null

    private fun charge(data: FaceData, phrases: DataPhrases): String {
        if (!isCharging(data)) return phrases.notCharging
        if (data.batteryFull) return phrases.charged
        val remaining = data.chargeRemainingMs?.takeIf { it > 0 } ?: return phrases.charging
        val minutes = ((remaining + MINUTE_MS - 1) / MINUTE_MS).toInt()
        val duration = if (minutes >= MINUTES_PER_HOUR) {
            phrases.hoursMinutes.format(minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
        } else {
            phrases.minutes.format(minutes)
        }
        return phrases.chargingLeft.format(duration)
    }

    private fun alarm(time: Long, use24Hour: Boolean): String {
        val locale = Locale.getDefault()
        val clock = SimpleDateFormat(if (use24Hour) ALARM_24 else ALARM_12, locale).format(Date(time))
        if (time - System.currentTimeMillis() < DAY_MS) return clock
        return SimpleDateFormat(ALARM_DAY, locale).format(Date(time)) + " " + clock
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
private const val MINUTE_MS = 60_000L
private const val MINUTES_PER_HOUR = 60
private const val DAY_MS = 86_400_000L
private const val ALARM_24 = "HH:mm"
private const val ALARM_12 = "h:mm a"
private const val ALARM_DAY = "EEE"
