package com.drdisagree.xmorphclock.data.source

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import com.drdisagree.xmorphclock.data.mapper.HookReportJsonMapper
import com.drdisagree.xmorphclock.domain.model.HookReport
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class HookReportSource(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE)

    private val current: HookReport?
        get() = prefs.getString(PrefKeys.HOOK_REPORT, null)?.let { HookReportJsonMapper.fromJson(it) }

    val report: Flow<HookReport?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PrefKeys.HOOK_REPORT) trySend(current)
        }
        trySend(current)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    fun save(json: String) {
        val incoming = HookReportJsonMapper.fromJson(json) ?: return
        val previous = current
        val merged = if (previous != null && (incoming.largeFrame == null || incoming.screenAspect == null)) {
            incoming.copy(
                largeFrame = incoming.largeFrame ?: previous.largeFrame,
                smallFrame = incoming.smallFrame ?: previous.smallFrame,
                screenAspect = incoming.screenAspect ?: previous.screenAspect
            )
        } else {
            incoming
        }
        prefs.edit { putString(PrefKeys.HOOK_REPORT, if (merged === incoming) json else HookReportJsonMapper.toJson(merged)) }
    }

    fun request() {
        context.sendBroadcast(
            Intent(ReportContract.ACTION_REQUEST).setPackage(ReportContract.SYSTEMUI_PACKAGE)
        )
    }
}
