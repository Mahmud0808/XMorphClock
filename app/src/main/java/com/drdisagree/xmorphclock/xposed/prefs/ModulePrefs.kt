package com.drdisagree.xmorphclock.xposed.prefs

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.drdisagree.xmorphclock.data.mapper.CalendarJsonMapper
import com.drdisagree.xmorphclock.data.mapper.PaletteJsonMapper
import com.drdisagree.xmorphclock.data.mapper.WeatherJsonMapper
import com.drdisagree.xmorphclock.data.source.ClockFaces
import com.drdisagree.xmorphclock.data.source.PrefKeys
import com.drdisagree.xmorphclock.domain.model.CalendarEvent
import com.drdisagree.xmorphclock.domain.model.WeatherSnapshot
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import io.github.libxposed.api.XposedInterface
import java.util.concurrent.CopyOnWriteArrayList

object ModulePrefs {

    private var prefs: SharedPreferences? = null
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        mainHandler.post { listeners.forEach { it() } }
    }

    val isClockEnabled: Boolean
        get() = prefs?.getBoolean(PrefKeys.CLOCK_ENABLED, PrefKeys.DEFAULT_CLOCK_ENABLED)
            ?: PrefKeys.DEFAULT_CLOCK_ENABLED

    val faceStyle: Int
        get() = prefs?.getInt(PrefKeys.FACE_STYLE, ClockFaces.STYLE_BASE) ?: ClockFaces.STYLE_BASE

    val weather: WeatherSnapshot?
        get() = WeatherJsonMapper.fromJson(prefs?.getString(PrefKeys.WEATHER_JSON, null))

    val events: List<CalendarEvent>
        get() = CalendarJsonMapper.fromJson(prefs?.getString(PrefKeys.CALENDAR_JSON, null))

    val steps: Int?
        get() = prefs?.getInt(PrefKeys.STEPS_TODAY, -1)?.takeIf { it >= 0 }

    val palette: Map<String, Int>
        get() = PaletteJsonMapper.fromJson(prefs?.getString(PrefKeys.WALLPAPER_PALETTE, null))

    val liveBackdrop: Long
        get() = prefs?.getLong(PrefKeys.LIVE_BACKDROP, 0L) ?: 0L

    val depthEnabled: Boolean
        get() = prefs?.getBoolean(PrefKeys.DEPTH_ENABLED, false) ?: false

    val depthStamp: Long
        get() = prefs?.getLong(PrefKeys.DEPTH_STAMP, 0L) ?: 0L

    val depthOpacity: Float
        get() = prefs?.getFloat(PrefKeys.DEPTH_OPACITY, 1f) ?: 1f

    val depthWidgetsFront: Boolean
        get() = prefs?.getBoolean(PrefKeys.DEPTH_WIDGETS_FRONT, true) ?: true

    val faceJson: String?
        get() = prefs?.getString(PrefKeys.FACE_JSON, null)

    val isBurnInProtectionEnabled: Boolean
        get() = prefs?.getBoolean(PrefKeys.BURN_IN, PrefKeys.DEFAULT_BURN_IN) ?: PrefKeys.DEFAULT_BURN_IN

    val wallpaperView: FloatArray?
        get() = prefs?.getString(PrefKeys.WALLPAPER_VIEW, null)
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
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }
}

private const val VIEW_PARTS = 4
