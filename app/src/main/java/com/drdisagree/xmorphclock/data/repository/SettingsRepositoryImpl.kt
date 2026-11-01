package com.drdisagree.xmorphclock.data.repository

import com.drdisagree.xmorphclock.data.mapper.ClockFaceJsonMapper
import com.drdisagree.xmorphclock.data.source.LocalPrefsSource
import com.drdisagree.xmorphclock.data.source.PrefKeys
import com.drdisagree.xmorphclock.data.source.XposedServiceSource
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.ClockSettings
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.ThemeMode
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
    private val local: LocalPrefsSource,
    private val remote: XposedServiceSource
) : SettingsRepository {

    init {
        remote.addBindListener { push(local.current) }
    }

    override val settings: Flow<ClockSettings> = local.settings

    override val isOnboardingDone: Boolean
        get() = local.isOnboardingDone

    override suspend fun setClockEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        local.setClockEnabled(enabled)
        remote.edit { it.putBoolean(PrefKeys.CLOCK_ENABLED, enabled) }
    }

    override suspend fun applyFace(style: Int, face: ClockFace) = withContext(Dispatchers.IO) {
        val json = ClockFaceJsonMapper.toJson(face)
        local.setFace(style, json)
        remote.edit {
            it.putInt(PrefKeys.FACE_STYLE, style)
            it.putString(PrefKeys.FACE_JSON, json)
        }
    }

    override suspend fun setComposition(composition: Composition) = withContext(Dispatchers.IO) {
        local.setComposition(composition)
    }

    override suspend fun setBurnInProtection(enabled: Boolean) = withContext(Dispatchers.IO) {
        local.setBurnInProtection(enabled)
        remote.edit { it.putBoolean(PrefKeys.BURN_IN, enabled) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) = withContext(Dispatchers.IO) {
        local.setThemeMode(mode)
    }

    override suspend fun setOnboardingDone(done: Boolean) = withContext(Dispatchers.IO) {
        local.setOnboardingDone(done)
    }

    private fun push(settings: ClockSettings) {
        remote.edit {
            it.putBoolean(PrefKeys.CLOCK_ENABLED, settings.clockEnabled)
            it.putInt(PrefKeys.FACE_STYLE, settings.faceStyle)
            local.faceJson?.let { json -> it.putString(PrefKeys.FACE_JSON, json) }
            it.putBoolean(PrefKeys.BURN_IN, settings.burnInProtection)
        }
    }
}
