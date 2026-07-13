package com.dreamjournal.app.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

data class WeatherSnapshot(
    val weatherText: String,
    val locationText: String?
)

class WeatherRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun currentWeather(): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val deviceLocation = getDeviceLocation()
            val networkLocation = if (deviceLocation == null) getNetworkLocation() else null
            val latitude = deviceLocation?.latitude ?: networkLocation?.latitude
                ?: error("没有获取到纬度")
            val longitude = deviceLocation?.longitude ?: networkLocation?.longitude
                ?: error("没有获取到经度")
            val locationText = deviceLocation?.let(::reverseGeocode)
                ?: networkLocation?.label

            val weather = getJson(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,weather_code&timezone=auto"
            )["current"]?.jsonObject ?: error("天气服务没有返回当前天气")
            val temperature = weather["temperature_2m"]?.jsonPrimitive?.doubleOrNull
                ?: error("天气服务没有返回温度")
            val code = weather["weather_code"]?.jsonPrimitive?.intOrNull ?: -1
            WeatherSnapshot(
                weatherText = "${weatherLabel(code)} ${temperature.toInt()}°C",
                locationText = locationText
            )
        }
    }

    private fun getNetworkLocation(): NetworkLocation {
        val location = getJson("https://ipwho.is/")
        require(location["success"]?.jsonPrimitive?.content != "false") { "无法定位所在城市" }
        val latitude = location["latitude"]?.jsonPrimitive?.doubleOrNull
            ?: error("没有获取到纬度")
        val longitude = location["longitude"]?.jsonPrimitive?.doubleOrNull
            ?: error("没有获取到经度")
        val city = location["city"]?.jsonPrimitive?.content.orEmpty().trim()
        val region = location["region"]?.jsonPrimitive?.content.orEmpty().trim()
        return NetworkLocation(latitude, longitude, city.ifBlank { region }.ifBlank { null })
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private suspend fun getDeviceLocation(): Location? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = manager.getProviders(true)
        val recent = providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull(Location::getTime)
        if (recent != null && System.currentTimeMillis() - recent.time < 30 * 60 * 1000L) return recent

        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return recent
        }
        return withTimeoutOrNull(6_000L) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }

                    override fun onProviderDisabled(provider: String) = Unit
                    override fun onProviderEnabled(provider: String) = Unit
                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                runCatching {
                    manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }.onFailure {
                    manager.removeUpdates(listener)
                    if (continuation.isActive) continuation.resume(recent)
                }
            }
        } ?: recent
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(location: Location): String? {
        if (!Geocoder.isPresent()) return null
        val address = runCatching {
            Geocoder(context, Locale.SIMPLIFIED_CHINESE)
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
        }.getOrNull() ?: return null
        val city = address.locality ?: address.subAdminArea ?: address.adminArea
        val district = address.subLocality
            ?: address.featureName?.takeIf { it.endsWith("区") || it.endsWith("县") }
        return listOfNotNull(city, district)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString("")
            .ifBlank { null }
    }

    private fun getJson(url: String) = client.newCall(
        Request.Builder().url(url).header("User-Agent", "QingJi-Android/1.0").build()
    ).execute().use { response ->
        require(response.isSuccessful) { "天气服务暂时不可用 (${response.code})" }
        json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
    }

    private fun weatherLabel(code: Int): String = when (code) {
        0 -> "晴"
        1, 2 -> "多云"
        3 -> "阴"
        45, 48 -> "雾"
        51, 53, 55, 56, 57 -> "毛毛雨"
        61, 63, 66, 80, 81 -> "雨"
        65, 67, 82 -> "大雨"
        71, 73, 77, 85 -> "雪"
        75, 86 -> "大雪"
        95, 96, 99 -> "雷雨"
        else -> "天气未知"
    }

    private data class NetworkLocation(
        val latitude: Double,
        val longitude: Double,
        val label: String?
    )
}
