package com.drdisagree.xmorphclock.domain.repository

import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.WeatherSettings
import com.drdisagree.xmorphclock.domain.model.WeatherState
import kotlinx.coroutines.flow.Flow

interface FaceDataRepository {
    val data: Flow<FaceData>
    val weather: Flow<WeatherState>
    suspend fun saveWeatherSettings(settings: WeatherSettings)
    suspend fun refreshWeather()
    suspend fun setWeatherLocation(query: String?): Boolean
    suspend fun prepare(face: ClockFace)
}
