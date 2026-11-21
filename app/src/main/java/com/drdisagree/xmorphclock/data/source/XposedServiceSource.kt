package com.drdisagree.xmorphclock.data.source

import android.content.Context
import android.content.SharedPreferences
import android.util.AtomicFile
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.io.File
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
    private var mirror: SharedPreferences? = null
    private var sharedDir: File? = null
    private var mirrorListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    val service: StateFlow<XposedService?> = current.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (registered) return
        registered = true
        val app = context.applicationContext
        sharedDir = RemotePrefsContract.sharedDir(app)
        val prefs = app.getSharedPreferences(RemotePrefsContract.MIRROR, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == RemotePrefsContract.COMPLETE_MARKER) return@OnSharedPreferenceChangeListener
            runCatching { app.contentResolver.notifyChange(RemotePrefsContract.changedUri(key), null) }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        mirror = prefs
        mirrorListener = listener
        XposedServiceHelper.registerListener(this)
    }

    fun addBindListener(listener: () -> Unit) {
        bindListeners += listener
        if (current.value != null) listener()
    }

    fun edit(block: (SharedPreferences.Editor) -> Unit) {
        mirror?.edit()?.also(block)?.apply()
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
        val local = sharedFile(name) ?: return false
        val stream = try {
            local.startWrite()
        } catch (_: Throwable) {
            return false
        }
        try {
            block(stream)
            local.finishWrite(stream)
        } catch (_: Throwable) {
            local.failWrite(stream)
            return false
        }
        val service = current.value ?: return true
        try {
            service.openRemoteFile(name).use { descriptor ->
                FileOutputStream(descriptor.fileDescriptor).use { output ->
                    output.channel.truncate(0)
                    local.baseFile.inputStream().use { it.copyTo(output) }
                }
            }
        } catch (_: Throwable) {
        }
        return true
    }

    fun deleteFile(name: String) {
        runCatching { sharedFile(name)?.delete() }
        try {
            current.value?.deleteRemoteFile(name)
        } catch (_: Throwable) {
        }
    }

    private fun sharedFile(name: String): AtomicFile? {
        val dir = sharedDir ?: return null
        if (!dir.isDirectory && !dir.mkdirs()) return null
        return AtomicFile(File(dir, name))
    }

    override fun onServiceBind(service: XposedService) {
        current.value = service
        adoptRemote(service)
        bindListeners.forEach { it() }
    }

    private fun adoptRemote(service: XposedService) {
        val local = mirror ?: return
        if (local.getBoolean(RemotePrefsContract.COMPLETE_MARKER, false)) return
        val remote = try {
            service.getRemotePreferences(PrefKeys.GROUP).all
        } catch (_: Throwable) {
            return
        }
        val editor = local.edit()
        remote.forEach { (key, value) ->
            if (local.contains(key)) return@forEach
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.putBoolean(RemotePrefsContract.COMPLETE_MARKER, true).apply()
    }

    override fun onServiceDied(service: XposedService) {
        if (current.value === service) current.value = null
    }
}
