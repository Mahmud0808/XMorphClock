package com.drdisagree.xmorphclock.xposed.systemui

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.edit
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.HostApplication
import com.drdisagree.xmorphclock.xposed.prefs.ModulePrefs

object RomClockGuard {

    private const val STORE = "xmorph_rom_clock"
    private const val OFF = 0
    private const val RETRY_MS = 1_000L
    private const val MAX_TRIES = 30

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastState = "untouched"

    private val observer = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            apply()
        }
    }

    fun install() {
        attach(0)
    }

    fun describe(): String = lastState

    private fun attach(tries: Int) {
        val application = HostApplication.get()
        if (application == null) {
            if (tries < MAX_TRIES) mainHandler.postDelayed({ attach(tries + 1) }, RETRY_MS)
            return
        }
        RomClockSetting.entries.forEach { setting ->
            application.contentResolver.registerContentObserver(uriOf(setting), false, observer)
        }
        ModulePrefs.addListener(::apply)
        apply()
    }

    private fun apply() {
        val host = HostApplication.get() ?: return
        val resolver = host.contentResolver
        val store = host.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val states = RomClockSetting.entries.mapNotNull { setting ->
            val key = setting.key
            val current = read(resolver, setting) ?: return@mapNotNull null
            try {
                if (ModulePrefs.isClockEnabled) {
                    if (current != OFF) {
                        store.edit { putInt(key, current) }
                        write(resolver, setting, OFF)
                        HookEnv.log("ROM setting $key $current -> $OFF")
                    }
                    "$key=$OFF (saved ${store.getInt(key, OFF)})"
                } else {
                    val saved = store.getInt(key, OFF)
                    if (saved != OFF && current == OFF) {
                        write(resolver, setting, saved)
                        HookEnv.log("ROM setting $key restored to $saved")
                    }
                    if (saved != OFF) store.edit { remove(key) }
                    "$key=${read(resolver, setting)}"
                }
            } catch (throwable: Throwable) {
                HookEnv.log("ROM setting $key unavailable", throwable)
                "$key=error"
            }
        }
        lastState = states.joinToString().ifEmpty { "none" }
    }

    private fun uriOf(setting: RomClockSetting): Uri =
        if (setting.system) Settings.System.getUriFor(setting.key) else Settings.Secure.getUriFor(setting.key)

    private fun read(resolver: ContentResolver, setting: RomClockSetting): Int? =
        (if (setting.system) Settings.System.getString(resolver, setting.key) else Settings.Secure.getString(resolver, setting.key))?.toIntOrNull()

    private fun write(resolver: ContentResolver, setting: RomClockSetting, value: Int) {
        if (setting.system) Settings.System.putInt(resolver, setting.key, value) else Settings.Secure.putInt(resolver, setting.key, value)
    }
}
