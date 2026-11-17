package com.drdisagree.xmorphclock.xposed.prefs

import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.drdisagree.xmorphclock.data.mapper.CalendarJsonMapper
import com.drdisagree.xmorphclock.data.mapper.PaletteJsonMapper
import com.drdisagree.xmorphclock.data.mapper.WeatherJsonMapper
import com.drdisagree.xmorphclock.data.source.ClockFaces
import com.drdisagree.xmorphclock.data.source.PrefKeys
import com.drdisagree.xmorphclock.data.source.RemotePrefsContract
import com.drdisagree.xmorphclock.domain.model.CalendarEvent
import com.drdisagree.xmorphclock.domain.model.WeatherSnapshot
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.HostApplication
import io.github.libxposed.api.XposedInterface
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

object ModulePrefs {

    private var prefs: SharedPreferences? = null
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val overrides = ConcurrentHashMap<String, Optional<Any>>()
    private val dispatched = HashMap<String, Any?>()

    private val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        worker.execute { runCatching { onRemoteChanged(key) } }
    }

    private val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            val key = uri?.pathSegments?.getOrNull(1)
            worker.execute { runCatching { pull(key) } }
        }
    }

    val isClockEnabled: Boolean
        get() = boolean(PrefKeys.CLOCK_ENABLED, PrefKeys.DEFAULT_CLOCK_ENABLED)

    val faceStyle: Int
        get() = int(PrefKeys.FACE_STYLE, ClockFaces.STYLE_BASE)

    val weather: WeatherSnapshot?
        get() = WeatherJsonMapper.fromJson(string(PrefKeys.WEATHER_JSON))

    val events: List<CalendarEvent>
        get() = CalendarJsonMapper.fromJson(string(PrefKeys.CALENDAR_JSON))

    val steps: Int?
        get() = int(PrefKeys.STEPS_TODAY, -1).takeIf { it >= 0 }

    val palette: Map<String, Int>
        get() = PaletteJsonMapper.fromJson(string(PrefKeys.WALLPAPER_PALETTE))

    val liveBackdrop: Long
        get() = long(PrefKeys.LIVE_BACKDROP, 0L)

    val depthEnabled: Boolean
        get() = boolean(PrefKeys.DEPTH_ENABLED, false)

    val depthStamp: Long
        get() = long(PrefKeys.DEPTH_STAMP, 0L)

    val depthOpacity: Float
        get() = float(PrefKeys.DEPTH_OPACITY, 1f)

    val depthWidgetsFront: Boolean
        get() = boolean(PrefKeys.DEPTH_WIDGETS_FRONT, true)

    val faceJson: String?
        get() = string(PrefKeys.FACE_JSON)

    val isBurnInProtectionEnabled: Boolean
        get() = boolean(PrefKeys.BURN_IN, PrefKeys.DEFAULT_BURN_IN)

    val wallpaperView: FloatArray?
        get() = string(PrefKeys.WALLPAPER_VIEW)
            ?.split(',')
            ?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == VIEW_PARTS && it[2] > 0f && it[3] > 0f }
            ?.toFloatArray()

    fun init(module: XposedInterface) {
        prefs = try {
            module.getRemotePreferences(PrefKeys.GROUP)
        } catch (throwable: Throwable) {
            HookEnv.log("Remote preferences unavailable", throwable)
            null
        }
        prefs?.registerOnSharedPreferenceChangeListener(changeListener)
        worker.execute { runCatching { remoteAll().forEach { (key, value) -> dispatched[key] = value } } }
        observe(0)
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    private fun observe(tries: Int) {
        val app = HostApplication.get()
        if (app == null) {
            if (tries < MAX_TRIES) mainHandler.postDelayed({ observe(tries + 1) }, RETRY_MS)
            return
        }
        try {
            app.contentResolver.registerContentObserver(RemotePrefsContract.CHANGED_URI, true, observer)
        } catch (throwable: Throwable) {
            HookEnv.log("Preference change channel unavailable", throwable)
        }
    }

    private fun pull(key: String?) {
        val app = HostApplication.get() ?: return
        val result = app.contentResolver.call(RemotePrefsContract.AUTHORITY_URI, RemotePrefsContract.METHOD_READ, key, null) ?: return
        val values = result.getBundle(RemotePrefsContract.VALUES) ?: Bundle.EMPTY
        val types = result.getBundle(RemotePrefsContract.TYPES) ?: Bundle.EMPTY
        val touched = ArrayList<String>()
        types.keySet().forEach { name ->
            valueOf(values, name, types.getString(name))?.let {
                overrides[name] = Optional.of(it)
                touched += name
            }
        }
        result.getStringArrayList(RemotePrefsContract.REMOVED)?.forEach { name ->
            overrides[name] = Optional.empty()
            touched += name
        }
        if (touched.count(::settle) > 0) dispatch()
    }

    private fun onRemoteChanged(key: String?) {
        if (key != null) {
            overrides.remove(key)
            if (settle(key)) dispatch()
            return
        }
        val keys = HashSet<String>(dispatched.keys).apply {
            addAll(overrides.keys)
            addAll(remoteAll().keys)
        }
        overrides.clear()
        if (keys.count(::settle) > 0) dispatch()
    }

    private fun settle(key: String): Boolean {
        val now = effective(key)
        if (dispatched[key] == now) return false
        dispatched[key] = now
        return true
    }

    private fun dispatch() {
        mainHandler.post { listeners.forEach { it() } }
    }

    private fun effective(key: String): Any? {
        overrides[key]?.let { return it.orElse(null) }
        return remoteAll()[key]
    }

    private fun remoteAll(): Map<String, *> = try {
        prefs?.all.orEmpty()
    } catch (_: Throwable) {
        emptyMap<String, Any?>()
    }

    private fun valueOf(values: Bundle, key: String, type: String?): Any? = when (type) {
        RemotePrefsContract.TYPE_BOOLEAN -> values.getBoolean(key)
        RemotePrefsContract.TYPE_INT -> values.getInt(key)
        RemotePrefsContract.TYPE_LONG -> values.getLong(key)
        RemotePrefsContract.TYPE_FLOAT -> values.getFloat(key)
        RemotePrefsContract.TYPE_STRING -> values.getString(key)
        RemotePrefsContract.TYPE_STRING_SET -> values.getStringArray(key)?.toSet()
        else -> null
    }

    private fun boolean(key: String, default: Boolean): Boolean {
        overrides[key]?.let { return it.orElse(null) as? Boolean ?: default }
        return prefs?.getBoolean(key, default) ?: default
    }

    private fun int(key: String, default: Int): Int {
        overrides[key]?.let { return it.orElse(null) as? Int ?: default }
        return prefs?.getInt(key, default) ?: default
    }

    private fun long(key: String, default: Long): Long {
        overrides[key]?.let { return it.orElse(null) as? Long ?: default }
        return prefs?.getLong(key, default) ?: default
    }

    private fun float(key: String, default: Float): Float {
        overrides[key]?.let { return it.orElse(null) as? Float ?: default }
        return prefs?.getFloat(key, default) ?: default
    }

    private fun string(key: String): String? {
        overrides[key]?.let { return it.orElse(null) as? String }
        return prefs?.getString(key, null)
    }
}

private const val VIEW_PARTS = 4
private const val MAX_TRIES = 30
private const val RETRY_MS = 1_000L
