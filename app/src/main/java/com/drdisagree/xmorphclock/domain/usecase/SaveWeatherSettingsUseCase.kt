package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.WeatherSettings
import com.drdisagree.xmorphclock.domain.repository.FaceDataRepository

class SaveWeatherSettingsUseCase(private val repository: FaceDataRepository) {
    suspend operator fun invoke(settings: WeatherSettings) = repository.saveWeatherSettings(settings)
}
