package com.drdisagree.xmorphclock.data.source

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.drdisagree.xmorphclock.data.mapper.CompositionJsonMapper
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.ClockSettings
import com.drdisagree.xmorphclock.domain.model.ThemeMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class LocalPrefsSource(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE)

    val current: ClockSettings
        get() = ClockSettings(
            clockEnabled = prefs.getBoolean(PrefKeys.CLOCK_ENABLED, PrefKeys.DEFAULT_CLOCK_ENABLED),
            faceStyle = prefs.getInt(PrefKeys.FACE_STYLE, ClockFaces.STYLE_BASE),
            burnInProtection = prefs.getBoolean(PrefKeys.BURN_IN, PrefKeys.DEFAULT_BURN_IN),
            themeMode = prefs.getString(PrefKeys.THEME_MODE, null)
                ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: ThemeMode.SYSTEM,
            composition = CompositionJsonMapper.fromJson(prefs.getString(PrefKeys.COMPOSITION, null))
        )

    val isOnboardingDone: Boolean
        get() = prefs.getBoolean(PrefKeys.ONBOARDING_DONE, false)

    val settings: Flow<ClockSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in SETTINGS_KEYS) trySend(current)
        }
        trySend(current)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    fun setClockEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(PrefKeys.CLOCK_ENABLED, enabled) }
    }

    val faceJson: String?
        get() = prefs.getString(PrefKeys.FACE_JSON, null)

    fun setFace(style: Int, json: String) {
        prefs.edit {
            putInt(PrefKeys.FACE_STYLE, style)
            putString(PrefKeys.FACE_JSON, json)
        }
    }

    fun setComposition(composition: Composition) {
        prefs.edit { putString(PrefKeys.COMPOSITION, CompositionJsonMapper.toJson(composition)) }
    }

    fun setBurnInProtection(enabled: Boolean) {
        prefs.edit { putBoolean(PrefKeys.BURN_IN, enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(PrefKeys.THEME_MODE, mode.name) }
    }

    fun setOnboardingDone(done: Boolean) {
        prefs.edit { putBoolean(PrefKeys.ONBOARDING_DONE, done) }
    }
}

private val SETTINGS_KEYS = setOf(PrefKeys.CLOCK_ENABLED, PrefKeys.FACE_STYLE, PrefKeys.BURN_IN, PrefKeys.THEME_MODE, PrefKeys.COMPOSITION)
