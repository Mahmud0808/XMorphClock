package com.drdisagree.xmorphclock.data.repository

import android.app.AlarmManager
import android.content.Context
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.core.content.edit
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.data.source.BatterySource
import com.drdisagree.xmorphclock.data.source.PermissionGranter
import com.drdisagree.xmorphclock.data.source.SampleFaceData
import com.drdisagree.xmorphclock.data.source.steps.StepsReader
import com.drdisagree.xmorphclock.data.source.steps.StepsStore
import com.drdisagree.xmorphclock.data.source.weather.LocationSource
import com.drdisagree.xmorphclock.data.source.weather.WeatherScheduler
import com.drdisagree.xmorphclock.data.source.weather.WeatherStore
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.FaceLayerType
import com.drdisagree.xmorphclock.domain.model.WeatherSettings
import com.drdisagree.xmorphclock.domain.model.WeatherState
import com.drdisagree.xmorphclock.domain.repository.FaceDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FaceDataRepositoryImpl(
    private val context: Context,
    private val weatherStore: WeatherStore,
    private val steps: StepsStore,
    private val battery: BatterySource,
    private val permissions: PermissionGranter
) : FaceDataRepository {

    override val data: Flow<FaceData> = combine(
        weatherStore.changes.map { weatherStore.snapshot to weatherStore.settings.metric },
        steps.steps,
        battery.state
    ) { (snapshot, metric), count, (level, charging) ->
        val battery = context.getSystemService(BatteryManager::class.java)
        val full = charging && level >= FULL_LEVEL
        FaceData(
            weather = snapshot ?: SampleFaceData.weather(metric, context.getString(R.string.weather_partly_cloudy)),
            batteryLevel = level,
            charging = charging,
            batteryFull = full,
            chargeRemainingMs = if (charging && !full) battery?.computeChargeTimeRemaining()?.takeIf { it > 0 } else null,
            nextAlarm = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime,
            use24Hour = DateFormat.is24HourFormat(context),
            steps = count ?: SampleFaceData.STEPS,
            dataUsageBytes = SampleFaceData.DATA_USAGE,
            tiles = SampleFaceData.TILES
        )
    }.flowOn(Dispatchers.Default)

    override val samples: Flow<FaceData> = data.map { real ->
        real.copy(
            nextAlarm = real.nextAlarm ?: SampleFaceData.nextAlarm(),
            chargeRemainingMs = if (real.charging) real.chargeRemainingMs else SampleFaceData.CHARGE_REMAINING_MS
        )
    }

    override val weather: Flow<WeatherState> = weatherStore.changes.map {
        WeatherState(weatherStore.settings, weatherStore.snapshot, weatherStore.status)
    }

    override suspend fun saveWeatherSettings(settings: WeatherSettings) = withContext(Dispatchers.IO) {
        val previous = weatherStore.settings
        weatherStore.save(settings)
        if (weatherActive) {
            WeatherScheduler.schedule(context, settings.intervalHours)
            if (previous.copy(intervalHours = settings.intervalHours) != settings) WeatherScheduler.refreshNow(context)
        }
    }

    override suspend fun refreshWeather() = withContext(Dispatchers.IO) {
        permissions.grant(PermissionGranter.LOCATION)
        WeatherScheduler.refreshNow(context)
    }

    override suspend fun setWeatherLocation(query: String?): Boolean = withContext(Dispatchers.IO) {
        val current = weatherStore.settings
        if (query.isNullOrBlank()) {
            weatherStore.save(current.copy(latitude = null, longitude = null, locationName = ""))
            permissions.grant(PermissionGranter.LOCATION)
            WeatherScheduler.refreshNow(context)
            return@withContext true
        }
        val address = LocationSource(context).search(query.trim()) ?: return@withContext false
        val name = address.locality ?: address.subAdminArea ?: address.adminArea ?: query.trim()
        weatherStore.save(current.copy(latitude = address.latitude, longitude = address.longitude, locationName = name))
        WeatherScheduler.refreshNow(context)
        true
    }

    override suspend fun prepare(face: ClockFace) = withContext(Dispatchers.IO) {
        val types = face.layers.mapTo(HashSet()) { it.type }
        val needsWeather = types.any { it in WEATHER_TYPES }
        if (needsWeather && !weatherStore.settings.hasCustomLocation) permissions.grant(PermissionGranter.LOCATION)
        if (needsWeather) {
            val settings = weatherStore.settings
            WeatherScheduler.schedule(context, settings.intervalHours)
            val age = weatherStore.snapshot?.let { System.currentTimeMillis() - it.timestamp }
            if (!weatherActive || age == null || age > settings.intervalHours * HOUR_MS) WeatherScheduler.refreshNow(context)
        } else if (weatherActive) {
            WeatherScheduler.cancel(context)
        }
        weatherActive = needsWeather
        if (FaceLayerType.STEPS in types) {
            permissions.grant(PermissionGranter.ACTIVITY)
            StepsReader(context).publish()
        }
    }

    private var weatherActive: Boolean
        get() = context.getSharedPreferences(ACTIVE_GROUP, Context.MODE_PRIVATE).getBoolean(ACTIVE, false)
        set(value) {
            context.getSharedPreferences(ACTIVE_GROUP, Context.MODE_PRIVATE).edit { putBoolean(ACTIVE, value) }
        }

    private companion object {
        const val ACTIVE_GROUP = "weather_state"
        const val ACTIVE = "active"
        const val HOUR_MS = 3_600_000L
        val WEATHER_TYPES = setOf(
            FaceLayerType.WEATHER_TEMP, FaceLayerType.WEATHER_ICON, FaceLayerType.WEATHER_CONDITION,
            FaceLayerType.WEATHER_HIGH_LOW, FaceLayerType.WEATHER_HUMIDITY, FaceLayerType.WEATHER_WIND,
            FaceLayerType.WEATHER_LOCATION
        )
    }
}

private const val FULL_LEVEL = 100
