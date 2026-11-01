package com.drdisagree.xmorphclock.xposed.clock

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import com.drdisagree.xmorphclock.xposed.hook.HookEnv

internal object StaticWallpaperFrames {

    private var started = false

    fun start(context: Context, onBitmap: (Context, Bitmap) -> Unit) {
        val app = context.applicationContext
        if (!started) {
            started = true
            WallpaperManager.getInstance(app).addOnColorsChangedListener(
                { _, _ -> capture(app, onBitmap) },
                Handler(Looper.getMainLooper())
            )
        }
        capture(app, onBitmap)
    }

    fun capture(context: Context, onBitmap: (Context, Bitmap) -> Unit) {
        WallpaperCapture.execute { load(context)?.let { onBitmap(context, it) } }
    }

    @SuppressLint("MissingPermission")
    private fun load(context: Context): Bitmap? = try {
        val manager = WallpaperManager.getInstance(context)
        val file = manager.getWallpaperFile(WallpaperManager.FLAG_LOCK) ?: manager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)
        file?.use { BitmapFactory.decodeFileDescriptor(it.fileDescriptor) }
    } catch (throwable: Throwable) {
        HookEnv.log("Static wallpaper unavailable", throwable)
        null
    }
}
