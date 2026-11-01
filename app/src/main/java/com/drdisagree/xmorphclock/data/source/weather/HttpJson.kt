package com.drdisagree.xmorphclock.data.source.weather

import com.drdisagree.xmorphclock.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object HttpJson {

    private const val TIMEOUT_MS = 15_000
    private val USER_AGENT = "XMorphClock/${BuildConfig.VERSION_NAME} github.com/Mahmud0808/XMorphClock"

    suspend fun get(url: String): JSONObject? = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode !in 200..299) return@withContext null
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
