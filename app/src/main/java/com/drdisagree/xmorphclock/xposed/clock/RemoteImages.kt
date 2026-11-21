package com.drdisagree.xmorphclock.xposed.clock

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.drdisagree.xmorphclock.data.source.ImageContract
import com.drdisagree.xmorphclock.xposed.prefs.ModuleFiles

internal object RemoteImages {
    fun load(id: String): Bitmap? =
        ModuleFiles.open(ImageContract.fileName(id), preferApp = false)?.use { BitmapFactory.decodeFileDescriptor(it.fileDescriptor) }
}
