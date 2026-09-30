package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import com.jxguo92.mykarooextension.core.weather.CurrentWeather
import com.jxguo92.mykarooextension.core.weather.GeoLocation
import com.jxguo92.mykarooextension.core.weather.WeatherProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlin.math.*

/** One cache per view. Times are monotonic milliseconds, never wall time. */
class ConcentricWeather(private val provider: WeatherProvider?, private val now: () -> Long) {
    private data class Success(val location: GeoLocation, val time: Long, val weather: CurrentWeather)
    @Volatile private var success: Success? = null
    private var failedAt: Long? = null
    private val request = Mutex()

    fun current(): CurrentWeather? = success?.takeIf { now() - it.time <= 60 * 60_000L }?.weather

    suspend fun refresh(location: GeoLocation?) {
        if (provider == null || location == null || !request.tryLock()) return
        try {
            val time = now()
            if (failedAt?.let { time - it < 2 * 60_000L } == true) return
            val previous = success
            if (failedAt == null && previous != null && time - previous.time < 30 * 60_000L &&
                distanceKm(previous.location, location) <= 5) return
            try {
                val weather = provider.currentWeather(location)
                success = Success(location, now(), weather)
                failedAt = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Never log provider exceptions: these may contain URLs or credentials.
                failedAt = now()
            }
        } finally {
            request.unlock()
        }
    }

    private fun distanceKm(a: GeoLocation, b: GeoLocation): Double {
        val lat = Math.toRadians(b.latitude - a.latitude)
        val lon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(lon / 2).pow(2)
        return 6371.0 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}
