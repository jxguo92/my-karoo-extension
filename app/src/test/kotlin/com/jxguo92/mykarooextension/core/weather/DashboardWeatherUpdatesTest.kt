package com.jxguo92.mykarooextension.core.weather

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardWeatherUpdatesTest {
    private val conditions = CurrentWeather(null, null, null, null, 270.0, 4.0, null)

    @Test fun `weather loop waits for location retries on policy and stops after cancellation`() = runTest {
        var calls = 0
        val weather = DashboardWeather(object : WeatherProvider {
            override suspend fun currentWeather(location: GeoLocation): CurrentWeather {
                calls++
                if (calls == 1) error("offline")
                return conditions
            }
        }, { testScheduler.currentTime })
        val location = MutableStateFlow<GeoLocation?>(null)
        val job = startDashboardWeatherUpdates(backgroundScope, weather, location)
        runCurrent(); assertEquals(0, calls)
        location.value = GeoLocation(0.0, 0.0)
        advanceTimeBy(1000); runCurrent(); assertEquals(1, calls)
        advanceTimeBy(119_999); runCurrent(); assertEquals(1, calls)
        advanceTimeBy(1); runCurrent(); assertEquals(2, calls)
        assertEquals(conditions, weather.current())
        job.cancelAndJoin()
        advanceTimeBy(1_800_000); runCurrent(); assertEquals(2, calls)
    }

    @Test fun `cancelling polling cancels in flight provider request`() = runTest {
        var cancelled = false
        val weather = DashboardWeather(object : WeatherProvider {
            override suspend fun currentWeather(location: GeoLocation): CurrentWeather {
                try { awaitCancellation() } finally { cancelled = true }
            }
        }, { testScheduler.currentTime })
        val job = startDashboardWeatherUpdates(backgroundScope, weather, MutableStateFlow(GeoLocation(0.0, 0.0)))
        runCurrent()
        job.cancelAndJoin()
        assertTrue(cancelled)
        assertNull(weather.current())
    }
}
