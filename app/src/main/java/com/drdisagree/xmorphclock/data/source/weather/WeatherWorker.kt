package com.drdisagree.xmorphclock.data.source.weather

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.drdisagree.xmorphclock.domain.model.WeatherProvider
import com.drdisagree.xmorphclock.domain.model.WeatherStatus

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = WeatherStore(applicationContext)
        val settings = store.settings
        store.setStatus(WeatherStatus.UPDATING)
        val location = LocationSource(applicationContext)
        val (latitude, longitude) = if (settings.hasCustomLocation) {
            settings.latitude!! to settings.longitude!!
        } else {
            val current = location.current() ?: return Result.retry().also { store.setStatus(WeatherStatus.NO_LOCATION) }
            current.latitude to current.longitude
        }
        val city = settings.locationName.takeIf { settings.hasCustomLocation && it.isNotEmpty() } ?: location.city(latitude, longitude)
        val weather = when (settings.provider) {
            WeatherProvider.OPEN_METEO -> null
            WeatherProvider.OPEN_WEATHER_MAP ->
                OpenWeatherMapSource.fetch(applicationContext, latitude, longitude, settings.metric, settings.apiKey, city)
        } ?: OpenMeteoSource.fetch(applicationContext, latitude, longitude, settings.metric, city)
            ?: MetNorwaySource.fetch(applicationContext, latitude, longitude, settings.metric, city)
            ?: return Result.retry().also { store.setStatus(if (isOnline()) WeatherStatus.SERVICE_BUSY else WeatherStatus.NO_NETWORK) }
        store.publish(weather)
        return Result.success()
    }

    private fun isOnline(): Boolean {
        val connectivity = applicationContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
