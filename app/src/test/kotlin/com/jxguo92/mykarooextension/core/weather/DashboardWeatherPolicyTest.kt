package com.jxguo92.mykarooextension.core.weather

import org.junit.Assert.*
import org.junit.Test

class DashboardWeatherPolicyTest {
    private val origin = GeoLocation(0.0, 0.0)

    @Test fun `first location requests thirty minutes refreshes and over sixty minutes expires`() {
        assertFalse(DashboardWeatherPolicy.shouldRequest(0, null, null, null, null))
        assertTrue(DashboardWeatherPolicy.shouldRequest(0, origin, null, null, null))
        assertFalse(DashboardWeatherPolicy.shouldRequest(1_799_999, origin, 0, origin, null))
        assertTrue(DashboardWeatherPolicy.shouldRequest(1_800_000, origin, 0, origin, null))
        assertFalse(DashboardWeatherPolicy.expired(3_600_000, 0))
        assertTrue(DashboardWeatherPolicy.expired(3_600_001, 0))
        assertTrue(DashboardWeatherPolicy.expired(0, null))
    }

    @Test fun `movement must exceed five km and failure cooldown wins over movement`() {
        val fiveKm = GeoLocation(0.0, 0.04496608029593652)
        assertFalse(DashboardWeatherPolicy.shouldRequest(0, fiveKm, 0, origin, null))
        assertTrue(DashboardWeatherPolicy.shouldRequest(0, GeoLocation(0.0, .0449661), 0, origin, null))
        assertFalse(DashboardWeatherPolicy.shouldRequest(119_999, GeoLocation(0.0, 1.0), 0, origin, 0))
        assertTrue(DashboardWeatherPolicy.shouldRequest(120_000, origin, 0, origin, 0))
    }
}
