package com.drdisagree.xmorphclock.domain.usecase

import android.graphics.Bitmap
import com.drdisagree.xmorphclock.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow

class ObserveWallpaperUseCase(private val repository: WallpaperRepository) {
    operator fun invoke(): Flow<Bitmap?> = repository.wallpaper
}
