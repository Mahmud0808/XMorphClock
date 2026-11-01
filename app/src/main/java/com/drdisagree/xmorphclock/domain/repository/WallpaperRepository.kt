package com.drdisagree.xmorphclock.domain.repository

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {
    val wallpaper: Flow<Bitmap?>
    val isLive: Flow<Boolean>
    suspend fun onLockscreenFrame(scale: Float): Bitmap?
    suspend fun lastFrame(): Bitmap?
}
