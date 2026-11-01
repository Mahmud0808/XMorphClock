package com.drdisagree.xmorphclock.xposed.clock

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.drdisagree.xmorphclock.data.source.ImageContract
import com.drdisagree.xmorphclock.xposed.hook.HookEnv

internal object RemoteImages {
    fun load(id: String): Bitmap? =
        HookEnv.module.openRemoteFile(ImageContract.fileName(id)).use { BitmapFactory.decodeFileDescriptor(it.fileDescriptor) }
}
