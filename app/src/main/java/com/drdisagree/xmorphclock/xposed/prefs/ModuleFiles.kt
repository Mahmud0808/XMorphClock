package com.drdisagree.xmorphclock.xposed.prefs

import android.os.ParcelFileDescriptor
import com.drdisagree.xmorphclock.data.source.RemotePrefsContract
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.HostApplication

internal object ModuleFiles {

    fun open(name: String, preferApp: Boolean): ParcelFileDescriptor? =
        if (preferApp) fromApp(name) ?: fromFramework(name) else fromFramework(name) ?: fromApp(name)

    private fun fromApp(name: String): ParcelFileDescriptor? {
        val resolver = HostApplication.get()?.contentResolver ?: return null
        return runCatching { resolver.openFileDescriptor(RemotePrefsContract.fileUri(name), READ) }.getOrNull()?.takeIf(::hasContent)
    }

    private fun fromFramework(name: String): ParcelFileDescriptor? =
        runCatching { HookEnv.module.openRemoteFile(name) }.getOrNull()?.takeIf(::hasContent)

    private fun hasContent(descriptor: ParcelFileDescriptor): Boolean {
        if (descriptor.statSize > 0L) return true
        runCatching { descriptor.close() }
        return false
    }

    private const val READ = "r"
}
