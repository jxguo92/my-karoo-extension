package com.jxguo92.mykarooextension.core.weather.qweather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QWeatherCurrentWeatherParserTest {
    @Test
    fun `parses the documented current weather response`() {
        val weather = QWeatherCurrentWeatherParser.parse(
            """
            {
              "metadata": {
                "attributions": ["https://developer.qweather.com/attribution.html"]
              },
              "condition": { "text": "少云", "code": "102" },
              "temperature": { "value": 31.71, "unit": "°C" },
              "feelsLike": { "value": 33.64, "unit": "°C" },
              "humidity": 0.69,
              "wind": {
                "direction": { "degree": 226, "compass": "sw" },
                "speed": { "value": 4.74, "unit": "m/s" }
              },
              "windGust": { "value": 7.07, "unit": "m/s" }
            }
            """.trimIndent(),
        )

        assertEquals("少云", weather.conditionText)
        assertEquals(31.71, weather.temperatureCelsius)
        assertEquals(33.64, weather.feelsLikeCelsius)
        assertEquals(0.69, weather.relativeHumidity)
        assertEquals(226.0, weather.windDirectionDegrees)
        assertEquals(4.74, weather.windSpeedMetersPerSecond)
        assertEquals(7.07, weather.windGustMetersPerSecond)
        assertEquals(listOf("https://developer.qweather.com/attribution.html"), weather.attributions)
    }

    @Test
    fun `converts kilometers per hour and leaves missing wind fields empty`() {
        val weather = QWeatherCurrentWeatherParser.parse(
            """
            {
              "temperature": { "value": 20, "unit": "°C" },
              "wind": { "speed": { "value": 36, "unit": "km/h" } }
            }
            """.trimIndent(),
        )

        assertEquals(10.0, weather.windSpeedMetersPerSecond!!, 0.0001)
        assertNull(weather.windDirectionDegrees)
        assertNull(weather.windGustMetersPerSecond)
        assertNull(weather.conditionText)
    }

    @Test
    fun `rejects an unsupported wind unit`() {
        val error = runCatching {
            QWeatherCurrentWeatherParser.parse(
                """{"wind":{"speed":{"value":1,"unit":"mph"}}}""",
            )
        }.exceptionOrNull()

        assertEquals("Unsupported QWeather unit: mph.", error?.message)
    }

    @Test
    fun `rejects an unsupported temperature unit`() {
        val error = runCatching {
            QWeatherCurrentWeatherParser.parse(
                """{"temperature":{"value":70,"unit":"°F"}}""",
            )
        }.exceptionOrNull()

        assertEquals("Unsupported QWeather unit: °F.", error?.message)
    }

    @Test
    fun `includes problem details from an error body`() {
        val message = QWeatherCurrentWeatherParser.errorMessage(
            400,
            """
            {
              "error": {
                "title": "Invalid Parameters",
                "detail": "Invalid parameters, please check your request.",
                "invalidParams": ["lang"]
              }
            }
            """.trimIndent(),
        )

        assertEquals(
            "Invalid Parameters: Invalid parameters, please check your request. (lang)",
            message,
        )
    }

    @Test
    fun `falls back when the error body is not JSON`() {
        assertEquals(
            "QWeather request failed with HTTP 500.",
            QWeatherCurrentWeatherParser.errorMessage(500, "upstream"),
        )
    }
}
