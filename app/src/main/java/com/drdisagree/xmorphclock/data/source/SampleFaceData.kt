package com.drdisagree.xmorphclock.data.source

import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.FaceTile
import com.drdisagree.xmorphclock.domain.model.WeatherSnapshot

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
        timestamp = 0L
    )

    const val STEPS = 6_482
    const val DATA_USAGE = 2_400_000_000L
    val TILES = mapOf(
        FaceTile.WIFI to FaceData.TILE_ON,
        FaceTile.BLUETOOTH to FaceData.TILE_ON,
        FaceTile.MOBILE_DATA to FaceData.TILE_ON
    )
    private const val PARTLY_CLOUDY = 30
}
