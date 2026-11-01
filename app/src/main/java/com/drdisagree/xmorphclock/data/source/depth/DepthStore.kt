package com.drdisagree.xmorphclock.data.source.depth

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.edit
import com.drdisagree.xmorphclock.data.source.PrefKeys
import com.drdisagree.xmorphclock.data.source.WallpaperContract
import com.drdisagree.xmorphclock.data.source.XposedServiceSource
import com.drdisagree.xmorphclock.domain.model.DepthSettings
import com.drdisagree.xmorphclock.domain.model.DepthStatus
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DepthStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE)
    private val preview = File(context.filesDir, PREVIEW_FILE)
    private val settingsState = MutableStateFlow(
        DepthSettings(prefs.getBoolean(PrefKeys.DEPTH_ENABLED, false), prefs.getFloat(PrefKeys.DEPTH_OPACITY, 1f))
    )
    private val subjectState = MutableStateFlow<Bitmap?>(null)
    private val statusState = MutableStateFlow(if (settingsState.value.enabled) DepthStatus.WAITING else DepthStatus.OFF)

    val status: StateFlow<DepthStatus> = statusState.asStateFlow()

    val settings: StateFlow<DepthSettings> = settingsState.asStateFlow()
    val subject: StateFlow<Bitmap?> = subjectState.asStateFlow()

    val key: String?
        get() = prefs.getString(PrefKeys.DEPTH_KEY, null)

    init {
        XposedServiceSource.addBindListener { pushSettings() }
    }

    fun loadPreview() {
        if (subjectState.value == null && preview.isFile) subjectState.value = BitmapFactory.decodeFile(preview.path)
        if (subjectState.value != null && settingsState.value.enabled) statusState.value = DepthStatus.READY
    }

    fun clear() {
        preview.delete()
        subjectState.value = null
        prefs.edit { remove(PrefKeys.DEPTH_KEY) }
        XposedServiceSource.deleteFile(WallpaperContract.DEPTH_FILE)
        XposedServiceSource.edit { it.remove(PrefKeys.DEPTH_STAMP) }
    }

    fun setStatus(status: DepthStatus) {
        statusState.value = if (settingsState.value.enabled) status else DepthStatus.OFF
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(PrefKeys.DEPTH_ENABLED, enabled) }
        settingsState.value = settingsState.value.copy(enabled = enabled)
        statusState.value = when {
            !enabled -> DepthStatus.OFF
            subjectState.value != null -> DepthStatus.READY
            else -> DepthStatus.WAITING
        }
        pushSettings()
    }

    fun setOpacity(opacity: Float) {
        val value = opacity.coerceIn(0f, 1f)
        prefs.edit { putFloat(PrefKeys.DEPTH_OPACITY, value) }
        settingsState.value = settingsState.value.copy(opacity = value)
        pushSettings()
    }

    fun save(key: String, previewSubject: Bitmap, lockSubject: Bitmap?) {
        preview.outputStream().use { previewSubject.compress(Bitmap.CompressFormat.PNG, FULL_QUALITY, it) }
        subjectState.value = previewSubject
        statusState.value = DepthStatus.READY
        val written = lockSubject != null && XposedServiceSource.writeFile(WallpaperContract.DEPTH_FILE) {
            lockSubject.compress(Bitmap.CompressFormat.PNG, FULL_QUALITY, it)
        }
        prefs.edit { putString(PrefKeys.DEPTH_KEY, key) }
        XposedServiceSource.edit {
            if (written) it.putLong(PrefKeys.DEPTH_STAMP, System.currentTimeMillis()) else it.remove(PrefKeys.DEPTH_STAMP)
        }
    }

    private fun pushSettings() {
        val settings = settingsState.value
        XposedServiceSource.edit {
            it.putBoolean(PrefKeys.DEPTH_ENABLED, settings.enabled)
            it.putFloat(PrefKeys.DEPTH_OPACITY, settings.opacity)
        }
    }

    private companion object {
        const val PREVIEW_FILE = "depth_preview.png"
        const val FULL_QUALITY = 100
    }
}
