package com.drdisagree.xmorphclock.data.source

import android.content.SharedPreferences
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object XposedServiceSource : XposedServiceHelper.OnServiceListener {

    private val current = MutableStateFlow<XposedService?>(null)
    private val bindListeners = CopyOnWriteArrayList<() -> Unit>()
    private var registered = false

    val service: StateFlow<XposedService?> = current.asStateFlow()

    @Synchronized
    fun init() {
        if (registered) return
        registered = true
        XposedServiceHelper.registerListener(this)
    }

    fun addBindListener(listener: () -> Unit) {
        bindListeners += listener
        if (current.value != null) listener()
    }

    fun edit(block: (SharedPreferences.Editor) -> Unit) {
        val service = current.value ?: return
        try {
            service.getRemotePreferences(PrefKeys.GROUP).edit().also(block).apply()
        } catch (_: Throwable) {
        }
    }

    fun readString(key: String): String? {
        val service = current.value ?: return null
        return try {
            service.getRemotePreferences(PrefKeys.GROUP).getString(key, null)
        } catch (_: Throwable) {
            null
        }
    }

    fun writeFile(name: String, block: (OutputStream) -> Unit): Boolean {
        val service = current.value ?: return false
        return try {
            service.openRemoteFile(name).use { descriptor ->
                FileOutputStream(descriptor.fileDescriptor).use { stream ->
                    stream.channel.truncate(0)
                    block(stream)
                }
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun deleteFile(name: String) {
        try {
            current.value?.deleteRemoteFile(name)
        } catch (_: Throwable) {
        }
    }

    override fun onServiceBind(service: XposedService) {
        current.value = service
        bindListeners.forEach { it() }
    }

    override fun onServiceDied(service: XposedService) {
        if (current.value === service) current.value = null
    }
}
