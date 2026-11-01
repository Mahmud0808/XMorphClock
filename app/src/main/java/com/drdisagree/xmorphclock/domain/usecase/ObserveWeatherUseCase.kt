package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.WeatherState
import com.drdisagree.xmorphclock.domain.repository.FaceDataRepository
import kotlinx.coroutines.flow.Flow

class ObserveWeatherUseCase(private val repository: FaceDataRepository) {
    operator fun invoke(): Flow<WeatherState> = repository.weather
}
