package com.drdisagree.xmorphclock.data.source

import com.drdisagree.xmorphclock.domain.model.CalendarEvent
import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.FaceTile
import com.drdisagree.xmorphclock.domain.model.HourlyPoint
import com.drdisagree.xmorphclock.domain.model.WeatherSnapshot
import java.util.Calendar

object SampleFaceData {

    fun weather(metric: Boolean, condition: String): WeatherSnapshot = WeatherSnapshot(
        temperature = if (metric) 24 else 75,
        high = if (metric) 27 else 81,
        low = if (metric) 17 else 63,
        conditionCode = PARTLY_CLOUDY,
        condition = condition,
        isDay = true,
        humidity = 58,
        wind = if (metric) 12 else 7,
        windUnit = if (metric) "km/h" else "mph",
        location = "",
        metric = metric,
        timestamp = 0L,
        sunrises = listOf(at(0, SUNRISE_HOUR, SUNRISE_MINUTE), at(1, SUNRISE_HOUR, SUNRISE_MINUTE)),
        sunsets = listOf(at(0, SUNSET_HOUR, SUNSET_MINUTE), at(1, SUNSET_HOUR, SUNSET_MINUTE)),
        uvIndex = SAMPLE_UV,
        hourly = (0..HOURLY_POINTS).map { offset ->
            val time = System.currentTimeMillis() / HOUR_MS * HOUR_MS + offset * HOUR_MS
            HourlyPoint(time, (if (metric) 24 else 75) - offset % HOURLY_SWING, if (offset % HOURLY_SWING == 2) CLOUDY else PARTLY_CLOUDY, true)
        }
    )

    private fun at(days: Int, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, days)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    const val STEPS = 6_482
    const val CHARGE_REMAINING_MS = 45 * 60_000L
    const val NOTIFICATIONS = 3

    fun events(title: String): List<CalendarEvent> {
        val begin = (System.currentTimeMillis() / HOUR_MS + 2) * HOUR_MS
        return listOf(CalendarEvent(title, begin, begin + HOUR_MS, false, ""))
    }

    fun nextAlarm(): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, ALARM_HOUR)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    const val DATA_USAGE = 2_400_000_000L
    val TILES = mapOf(
        FaceTile.WIFI to FaceData.TILE_ON,
        FaceTile.BLUETOOTH to FaceData.TILE_ON,
        FaceTile.MOBILE_DATA to FaceData.TILE_ON,
        FaceTile.MEDIA to FaceData.TILE_ALT,
        FaceTile.MEDIA_PREVIOUS to FaceData.TILE_ON,
        FaceTile.MEDIA_NEXT to FaceData.TILE_ON
    )
    private const val PARTLY_CLOUDY = 30
    private const val CLOUDY = 26
    private const val SUNRISE_HOUR = 6
    private const val SUNRISE_MINUTE = 12
    private const val SUNSET_HOUR = 17
    private const val SUNSET_MINUTE = 48
    private const val SAMPLE_UV = 5f
    private const val HOURLY_POINTS = 24
    private const val HOURLY_SWING = 4
    private const val HOUR_MS = 3_600_000L
    private const val ALARM_HOUR = 7
}
