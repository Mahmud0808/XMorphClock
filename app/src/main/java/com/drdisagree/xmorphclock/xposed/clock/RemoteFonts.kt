package com.drdisagree.xmorphclock.xposed.clock

import android.os.ParcelFileDescriptor
import com.drdisagree.xmorphclock.data.source.FontContract
import com.drdisagree.xmorphclock.xposed.hook.HookEnv

internal object RemoteFonts {
    fun open(id: String): ParcelFileDescriptor? = runCatching { HookEnv.module.openRemoteFile(FontContract.fileName(id)) }.getOrNull()
}
