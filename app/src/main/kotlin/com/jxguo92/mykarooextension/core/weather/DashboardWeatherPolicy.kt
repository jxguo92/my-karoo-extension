package com.jxguo92.mykarooextension.core.weather

import kotlin.math.*

/** Refresh and expiry rules shared by the two full-page dashboards. Times are monotonic ms. */
object DashboardWeatherPolicy {
    fun shouldRequest(
        now: Long,
        location: GeoLocation?,
        succeededAt: Long?,
        successLocation: GeoLocation?,
        failedAt: Long?,
    ): Boolean {
        if (location == null) return false
        if (failedAt != null) return now - failedAt >= 2 * 60_000L
        return succeededAt == null || successLocation == null || now - succeededAt >= 30 * 60_000L ||
            distanceKm(successLocation, location) > 5
    }

    fun expired(now: Long, succeededAt: Long?): Boolean = succeededAt == null || now - succeededAt > 60 * 60_000L

    private fun distanceKm(a: GeoLocation, b: GeoLocation): Double {
        val lat = Math.toRadians(b.latitude - a.latitude)
        val lon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(lon / 2).pow(2)
        return 6371.0 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}
