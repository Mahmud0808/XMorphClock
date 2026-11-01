package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.FaceDataRepository

class RefreshWeatherUseCase(private val repository: FaceDataRepository) {
    suspend operator fun invoke() = repository.refreshWeather()
}
