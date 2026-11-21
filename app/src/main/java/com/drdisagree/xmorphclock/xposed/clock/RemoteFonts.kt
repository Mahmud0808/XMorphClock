package com.drdisagree.xmorphclock.xposed.clock

import android.os.ParcelFileDescriptor
import com.drdisagree.xmorphclock.data.source.FontContract
import com.drdisagree.xmorphclock.xposed.prefs.ModuleFiles

internal object RemoteFonts {
    fun open(id: String): ParcelFileDescriptor? = ModuleFiles.open(FontContract.fileName(id), preferApp = false)
}
