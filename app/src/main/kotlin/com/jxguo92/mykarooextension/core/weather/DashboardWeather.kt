package com.jxguo92.mykarooextension.core.weather

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

/** One cache per view. Times are monotonic milliseconds, never wall time. */
class DashboardWeather(private val provider: WeatherProvider?, private val now: () -> Long) {
    private data class Success(val location: GeoLocation, val time: Long, val weather: CurrentWeather)
    @Volatile private var success: Success? = null
    private var failedAt: Long? = null
    private val request = Mutex()

    fun current(): CurrentWeather? = success?.takeIf { !DashboardWeatherPolicy.expired(now(), it.time) }?.weather

    suspend fun refresh(location: GeoLocation?) {
        if (provider == null || location == null || !request.tryLock()) return
        try {
            val time = now()
            val previous = success
            if (!DashboardWeatherPolicy.shouldRequest(time, location, previous?.time, previous?.location, failedAt)) return
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

}
