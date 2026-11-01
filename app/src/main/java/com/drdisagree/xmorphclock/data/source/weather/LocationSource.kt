package com.drdisagree.xmorphclock.data.source.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class LocationSource(private val context: Context) {

    private val manager = context.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun current(): Location? {
        if (!hasPermission() || manager == null) return null
        val recent = manager.getProviders(true)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.takeIf { System.currentTimeMillis() - it.time < FRESH_MS }
        if (recent != null) return recent
        val provider = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) } ?: return null
        return withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(provider, signal, context.mainExecutor) { continuation.resume(it) }
            }
        }
    }

    suspend fun city(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext ""
        val geocoder = Geocoder(context, Locale.getDefault())
        val address: Address? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            legacyReverse(geocoder, latitude, longitude)
        } else {
            withTimeoutOrNull(TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    })
                }
            }
        }
        address?.locality ?: address?.subAdminArea ?: address?.adminArea.orEmpty()
    }

    suspend fun search(query: String): Address? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            legacySearch(geocoder, query)
        } else {
            withTimeoutOrNull(TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    })
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyReverse(geocoder: Geocoder, latitude: Double, longitude: Double): Address? =
        runCatching { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }.getOrNull()

    @Suppress("DEPRECATION")
    private fun legacySearch(geocoder: Geocoder, query: String): Address? =
        runCatching { geocoder.getFromLocationName(query, 1)?.firstOrNull() }.getOrNull()

    private companion object {
        const val FRESH_MS = 30 * 60_000L
        const val TIMEOUT_MS = 15_000L
    }
}
