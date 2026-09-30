package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import com.jxguo92.mykarooextension.core.weather.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConcentricWeatherTest {
    private val origin = GeoLocation(0.0, 0.0)
    private val weather = CurrentWeather(null, null, null, null, 270.0, 4.0, null)

    @Test fun `refresh after thirty minutes and retain failed cache until over sixty minutes`() = runTest {
        var now = 0L
        var calls = 0
        val cache = ConcentricWeather(object : WeatherProvider {
            override suspend fun currentWeather(location: GeoLocation): CurrentWeather {
                calls++
                if (calls > 1) error("offline")
                return weather
            }
        }, { now })
        cache.refresh(origin)
        now = 1_799_999; cache.refresh(origin); assertEquals(1, calls)
        now = 1_800_000; cache.refresh(origin); assertEquals(2, calls)
        assertEquals(weather, cache.current())
        now += 119_999; cache.refresh(origin); assertEquals(2, calls)
        now++; cache.refresh(origin); assertEquals(3, calls)
        now = 3_600_000; assertEquals(weather, cache.current())
        now++; assertNull(cache.current())
    }

    @Test fun `movement refreshes only beyond five km and requests cannot overlap`() = runTest {
        var calls = 0
        val cache = ConcentricWeather(object : WeatherProvider {
            override suspend fun currentWeather(location: GeoLocation): CurrentWeather {
                calls++; delay(100); return weather
            }
        }, { testScheduler.currentTime })
        val first = launch { cache.refresh(origin) }
        runCurrent()
        cache.refresh(origin)
        assertEquals(1, calls)
        first.join()
        cache.refresh(GeoLocation(.04, 0.0)); assertEquals(1, calls)
        cache.refresh(GeoLocation(.05, 0.0)); assertEquals(2, calls)
    }

    @Test fun `cancellation reaches provider and absent configuration or location never requests`() = runTest {
        var cancelled = false
        var calls = 0
        val cache = ConcentricWeather(object : WeatherProvider {
            override suspend fun currentWeather(location: GeoLocation): CurrentWeather {
                calls++
                try { awaitCancellation() } finally { cancelled = true }
            }
        }, { 0 })
        cache.refresh(null); assertEquals(0, calls)
        ConcentricWeather(null, { 0 }).refresh(origin)
        val job = launch { cache.refresh(origin) }
        runCurrent(); job.cancelAndJoin()
        assertTrue(cancelled)
        assertNull(cache.current())
    }
}
