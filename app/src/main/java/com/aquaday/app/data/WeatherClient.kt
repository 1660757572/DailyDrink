package com.aquaday.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** 问候语轮播需要的三项天气，全部来自 Open-Meteo 的 current 字段。 */
data class WeatherInfo(val temperatureC: Int, val humidity: Int, val weatherCode: Int)

/**
 * 只读系统缓存的位置（lastKnown），不主动发起定位、不需要 API key。
 * 任何一步失败都返回 null，界面退回原来的记录天数文案。
 */
class WeatherClient(private val context: Context) {

    suspend fun current(): WeatherInfo? = withContext(Dispatchers.IO) {
        val point = coarsePoint() ?: return@withContext null
        runCatching { fetch(point.first, point.second) }.getOrElse {
            Log.w(TAG, "current: 天气获取失败", it)
            null
        }
    }

    private fun coarsePoint(): Pair<Double, Double>? {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val latest = WEATHER_PROVIDERS
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?: return null
        return latest.latitude to latest.longitude
    }

    private fun fetch(latitude: Double, longitude: Double): WeatherInfo? {
        val url = String.format(
            Locale.US,
            "%s?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,weather_code",
            ENDPOINT,
            latitude,
            longitude,
        )
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            val body = connection.inputStream.use { it.readBytes().decodeToString() }
            val current = JSONObject(body).optJSONObject("current") ?: return null
            val temperature = current.optDouble("temperature_2m")
            if (temperature.isNaN()) return null
            WeatherInfo(
                temperatureC = temperature.roundToInt(),
                humidity = current.optInt("relative_humidity_2m"),
                weatherCode = current.optInt("weather_code"),
            ).also { Log.i(TAG, "fetch: $it") }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TAG = "WeatherClient"
        const val ENDPOINT = "https://api.open-meteo.com/v1/forecast"
        const val TIMEOUT_MS = 4_000
        val WEATHER_PROVIDERS = listOf(
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
        )
    }
}
