package com.drdisagree.xmorphclock.data.source.weather

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.drdisagree.xmorphclock.data.mapper.WeatherJsonMapper
import com.drdisagree.xmorphclock.data.source.PrefKeys
import com.drdisagree.xmorphclock.data.source.XposedServiceSource
import com.drdisagree.xmorphclock.domain.model.WeatherProvider
import com.drdisagree.xmorphclock.domain.model.WeatherSettings
import com.drdisagree.xmorphclock.domain.model.WeatherSnapshot
import com.drdisagree.xmorphclock.domain.model.WeatherStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class WeatherStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE)

    val settings: WeatherSettings
        get() = WeatherSettings(
            provider = prefs.getString(PrefKeys.WEATHER_PROVIDER, null)
                ?.let { name -> WeatherProvider.entries.firstOrNull { it.name == name } }
                ?: WeatherProvider.OPEN_METEO,
            apiKey = prefs.getString(PrefKeys.WEATHER_KEY, null).orEmpty(),
            metric = prefs.getBoolean(PrefKeys.WEATHER_METRIC, true),
            latitude = prefs.getString(PrefKeys.WEATHER_LATITUDE, null)?.toDoubleOrNull(),
            longitude = prefs.getString(PrefKeys.WEATHER_LONGITUDE, null)?.toDoubleOrNull(),
            intervalHours = prefs.getInt(PrefKeys.WEATHER_INTERVAL, 1),
            locationName = prefs.getString(PrefKeys.WEATHER_PLACE, null).orEmpty()
        )

    val snapshot: WeatherSnapshot?
        get() = WeatherJsonMapper.fromJson(prefs.getString(PrefKeys.WEATHER_JSON, null) ?: XposedServiceSource.readString(PrefKeys.WEATHER_JSON))

    val changes: Flow<Unit> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PrefKeys.WEATHER_JSON || key == PrefKeys.WEATHER_STATUS || key in SETTINGS) trySend(Unit)
        }
        trySend(Unit)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    fun save(settings: WeatherSettings) {
        prefs.edit {
            putString(PrefKeys.WEATHER_PROVIDER, settings.provider.name)
            putString(PrefKeys.WEATHER_KEY, settings.apiKey)
            putBoolean(PrefKeys.WEATHER_METRIC, settings.metric)
            putString(PrefKeys.WEATHER_LATITUDE, settings.latitude?.toString())
            putString(PrefKeys.WEATHER_LONGITUDE, settings.longitude?.toString())
            putInt(PrefKeys.WEATHER_INTERVAL, settings.intervalHours)
            putString(PrefKeys.WEATHER_PLACE, settings.locationName)
        }
    }

    val status: WeatherStatus
        get() = prefs.getString(PrefKeys.WEATHER_STATUS, null)
            ?.let { name -> WeatherStatus.entries.firstOrNull { it.name == name } }
            ?: if (snapshot != null) WeatherStatus.READY else WeatherStatus.IDLE

    fun setStatus(status: WeatherStatus) {
        prefs.edit { putString(PrefKeys.WEATHER_STATUS, status.name) }
    }

    fun publish(weather: WeatherSnapshot) {
        val json = WeatherJsonMapper.toJson(weather)
        prefs.edit {
            putString(PrefKeys.WEATHER_JSON, json)
            putString(PrefKeys.WEATHER_STATUS, WeatherStatus.READY.name)
        }
        XposedServiceSource.edit { it.putString(PrefKeys.WEATHER_JSON, json) }
    }

    private companion object {
        val SETTINGS = setOf(
            PrefKeys.WEATHER_PROVIDER, PrefKeys.WEATHER_KEY, PrefKeys.WEATHER_METRIC,
            PrefKeys.WEATHER_LATITUDE, PrefKeys.WEATHER_LONGITUDE, PrefKeys.WEATHER_INTERVAL, PrefKeys.WEATHER_PLACE
        )
    }
}
