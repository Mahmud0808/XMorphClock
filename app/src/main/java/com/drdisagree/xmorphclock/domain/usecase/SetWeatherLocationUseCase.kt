package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.FaceDataRepository

class SetWeatherLocationUseCase(private val repository: FaceDataRepository) {
    suspend operator fun invoke(query: String?): Boolean = repository.setWeatherLocation(query)
}
