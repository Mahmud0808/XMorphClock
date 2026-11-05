package com.drdisagree.xmorphclock.xposed.systemui

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

    private val KEYS = listOf("lock_screen_custom_clock_style")
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
        KEYS.forEach { key ->
            application.contentResolver.registerContentObserver(Settings.Secure.getUriFor(key), false, observer)
        }
        ModulePrefs.addListener(::apply)
        apply()
    }

    private fun apply() {
        val host = HostApplication.get() ?: return
        val resolver = host.contentResolver
        val store = host.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val states = KEYS.mapNotNull { key ->
            val current = Settings.Secure.getString(resolver, key)?.toIntOrNull() ?: return@mapNotNull null
            try {
                if (ModulePrefs.isClockEnabled) {
                    if (current != OFF) {
                        store.edit { putInt(key, current) }
                        Settings.Secure.putInt(resolver, key, OFF)
                        HookEnv.log("ROM clock $key $current -> $OFF")
                    }
                    "$key=$OFF (saved ${store.getInt(key, OFF)})"
                } else {
                    val saved = store.getInt(key, OFF)
                    if (saved != OFF && current == OFF) {
                        Settings.Secure.putInt(resolver, key, saved)
                        HookEnv.log("ROM clock $key restored to $saved")
                    }
                    if (saved != OFF) store.edit { remove(key) }
                    "$key=${Settings.Secure.getString(resolver, key)}"
                }
            } catch (throwable: Throwable) {
                HookEnv.log("ROM clock $key unavailable", throwable)
                "$key=error"
            }
        }
        lastState = states.joinToString().ifEmpty { "none" }
    }
}
