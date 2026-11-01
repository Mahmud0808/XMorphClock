package com.drdisagree.xmorphclock.data.repository

import android.graphics.Bitmap
import com.drdisagree.xmorphclock.data.source.FrameZoom
import com.drdisagree.xmorphclock.data.source.PaletteStore
import com.drdisagree.xmorphclock.data.source.PrefKeys
import com.drdisagree.xmorphclock.data.source.WallpaperContract
import com.drdisagree.xmorphclock.data.source.WallpaperPalette
import com.drdisagree.xmorphclock.data.source.WallpaperSource
import com.drdisagree.xmorphclock.data.source.XposedServiceSource
import com.drdisagree.xmorphclock.domain.repository.WallpaperRepository
import com.drdisagree.xmorphclock.engine.FaceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WallpaperRepositoryImpl(
    private val source: WallpaperSource,
    private val remote: XposedServiceSource,
    private val palette: PaletteStore
) : WallpaperRepository {

    init {
        remote.addBindListener { pushView() }
    }

    private fun pushView() {
        val view = source.visibleView
        remote.edit { if (view != null) it.putString(PrefKeys.WALLPAPER_VIEW, view) else it.remove(PrefKeys.WALLPAPER_VIEW) }
    }

    private fun pushLiveBackdrop(bitmap: Bitmap) {
        if (!source.lastLive) {
            remote.deleteFile(WallpaperContract.LIVE_BACKDROP_FILE)
            remote.edit { it.remove(PrefKeys.LIVE_BACKDROP) }
            return
        }
        val written = remote.writeFile(WallpaperContract.LIVE_BACKDROP_FILE) {
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, LIVE_QUALITY, it)
        }
        if (written) remote.edit { it.putLong(PrefKeys.LIVE_BACKDROP, System.currentTimeMillis()) }
    }

    private suspend fun publish(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            pushView()
            pushLiveBackdrop(bitmap)
        }
        if (source.lastFrame() != null) return
        withContext(Dispatchers.Default) {
            val colors = WallpaperPalette.extract(bitmap)
            FaceColors.setPalette(colors)
            palette.save(colors)
        }
    }

    private val frames = MutableSharedFlow<Bitmap>(extraBufferCapacity = 1)

    override val wallpaper: Flow<Bitmap?> = channelFlow {
        var frame: Bitmap? = source.lastFrame()?.let { FrameZoom.zoomed(it, source.frameScale) }
        var root: Bitmap? = source.cached()
        send(frame ?: root)
        launch {
            frames.collect {
                frame = it
                send(it)
            }
        }
        source.refresh()?.let {
            publish(it)
            root = it
            send(frame ?: it)
        }
        source.changes().collect {
            frame = null
            source.clearFrame()
            source.refresh()?.let { bitmap ->
                publish(bitmap)
                root = bitmap
            }
            send(root)
        }
    }

    override suspend fun onLockscreenFrame(scale: Float): Bitmap? {
        val bitmap = source.readFrame() ?: return null
        source.frameScale = scale
        frames.tryEmit(withContext(Dispatchers.Default) { FrameZoom.zoomed(bitmap, scale) })
        withContext(Dispatchers.Default) {
            val colors = WallpaperPalette.extract(bitmap)
            FaceColors.setPalette(colors)
            palette.save(colors)
        }
        return bitmap
    }

    override suspend fun lastFrame(): Bitmap? = source.lastFrame()

    override val isLive: Flow<Boolean> = flow {
        emit(source.isLive())
        source.changes().collect { emit(source.isLive()) }
    }.distinctUntilChanged()
}

private const val LIVE_QUALITY = 90
