package com.drdisagree.xmorphclock.data.source.steps

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.edit
import com.drdisagree.xmorphclock.data.source.PrefKeys
import com.drdisagree.xmorphclock.data.source.XposedServiceSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

class StepsReader(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE)

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    suspend fun publish() {
        HealthSteps(context).today()?.let { steps ->
            store(steps)
            return
        }
        val counter = counter() ?: return
        val today = DAY_FORMAT.format(Date())
        val last = prefs.getFloat(LAST, -1f)
        var base = prefs.getFloat(BASE, counter)
        when {
            prefs.getString(DAY, null) != today -> base = if (last in 0f..counter) last else 0f
            last > counter -> base -= last
        }
        val steps = (counter - base).toInt().coerceAtLeast(0)
        prefs.edit {
            putString(DAY, today)
            putFloat(BASE, base)
            putFloat(LAST, counter)
        }
        store(steps)
    }

    private fun store(steps: Int) {
        prefs.edit { putInt(PrefKeys.STEPS_TODAY, steps) }
        XposedServiceSource.edit { it.putInt(PrefKeys.STEPS_TODAY, steps) }
    }

    private suspend fun counter(): Float? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(SensorManager::class.java) ?: return null
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
        return withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        manager.unregisterListener(this)
                        if (continuation.isActive) continuation.resume(event.values.firstOrNull())
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                continuation.invokeOnCancellation { manager.unregisterListener(listener) }
                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    private companion object {
        const val DAY = "steps_day"
        const val BASE = "steps_base"
        const val LAST = "steps_last"
        const val TIMEOUT_MS = 5_000L
        val DAY_FORMAT = SimpleDateFormat("yyyyMMdd", Locale.US)
    }
}
