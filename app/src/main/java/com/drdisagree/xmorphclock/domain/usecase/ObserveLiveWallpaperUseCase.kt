package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow

class ObserveLiveWallpaperUseCase(private val repository: WallpaperRepository) {
    operator fun invoke(): Flow<Boolean> = repository.isLive
}
