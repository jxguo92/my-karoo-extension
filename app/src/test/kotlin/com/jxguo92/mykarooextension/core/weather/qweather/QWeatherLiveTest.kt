package com.jxguo92.mykarooextension.core.weather.qweather

import com.jxguo92.mykarooextension.core.weather.GeoLocation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class QWeatherLiveTest {
    @Test
    fun `fetches current weather for Beijing from QWeather`() = runTest {
        val config = QWeatherConfig.fromBuildConfig()
        assumeTrue("QWeather host and key are not set in local.properties", config != null)

        val weather = QWeatherProvider(config!!)
            .currentWeather(GeoLocation(latitude = 39.92, longitude = 116.41))

        println(
            "QWeather Beijing: ${weather.conditionText} " +
                "${weather.temperatureCelsius}°C, " +
                "wind ${weather.windSpeedMetersPerSecond} m/s " +
                "from ${weather.windDirectionDegrees}°, " +
                "gust ${weather.windGustMetersPerSecond} m/s",
        )

        val temperature = weather.temperatureCelsius
        val windSpeed = weather.windSpeedMetersPerSecond
        assertTrue(temperature != null || windSpeed != null)
        if (temperature != null) assertTrue(temperature in -80.0..60.0)
        if (windSpeed != null) assertTrue(windSpeed >= 0.0)
        weather.windDirectionDegrees?.let { direction ->
            assertTrue(direction in 0.0..360.0)
        }
        assertTrue(weather.attributions.isNotEmpty())
    }
}
