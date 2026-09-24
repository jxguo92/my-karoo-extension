package com.jxguo92.mykarooextension.core.weather.qweather

import com.jxguo92.mykarooextension.core.weather.GeoLocation
import com.jxguo92.mykarooextension.core.weather.HttpGet
import com.jxguo92.mykarooextension.core.weather.HttpRequest
import com.jxguo92.mykarooextension.core.weather.HttpResponse
import com.jxguo92.mykarooextension.core.weather.WeatherException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QWeatherProviderTest {
    @Test
    fun `requests latitude then longitude with the api key header`() = runTest {
        val http = RecordingHttpGet(
            HttpResponse(
                statusCode = 200,
                body = """{"wind":{"direction":{"degree":90},"speed":{"value":1,"unit":"m/s"}}}""",
            ),
        )
        val provider = QWeatherProvider(
            config = QWeatherConfig.create("https://abc.qweatherapi.com/", "secret-key"),
            http = http,
        )

        val weather = provider.currentWeather(GeoLocation(39.924, 116.414))

        assertEquals(90.0, weather.windDirectionDegrees)
        assertEquals(1, http.requests.size)
        assertEquals(
            "https://abc.qweatherapi.com/weather/v1/current/39.92/116.41",
            http.requests.single().url,
        )
        assertEquals("secret-key", http.requests.single().headers["X-QW-Api-Key"])
        assertFalse(http.requests.single().url.contains("secret-key"))
    }

    @Test
    fun `surfaces the provider error without retrying`() = runTest {
        val http = RecordingHttpGet(
            HttpResponse(
                statusCode = 401,
                body = """{"error":{"title":"Unauthorized","detail":"Authentication failed."}}""",
            ),
        )
        val provider = QWeatherProvider(
            config = QWeatherConfig.create("abc.qweatherapi.com", "secret-key"),
            http = http,
        )

        val error = runCatching {
            provider.currentWeather(GeoLocation(31.23, 121.47))
        }.exceptionOrNull() as WeatherException

        assertEquals(401, error.statusCode)
        assertEquals("Unauthorized: Authentication failed.", error.message)
        assertEquals(1, http.requests.size)
    }

    @Test
    fun `propagates cancellation instead of wrapping it`() = runTest {
        val http = object : HttpGet {
            override suspend fun get(request: HttpRequest): HttpResponse {
                throw CancellationException("field cancelled")
            }
        }
        val provider = QWeatherProvider(
            config = QWeatherConfig.create("abc.qweatherapi.com", "secret-key"),
            http = http,
        )

        val error = runCatching {
            provider.currentWeather(GeoLocation(31.23, 121.47))
        }.exceptionOrNull()

        assertTrue(error is CancellationException)
    }

    @Test
    fun `rejects coordinates outside the valid range before a request`() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoLocation(90.1, 0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GeoLocation(0.0, Double.NaN)
        }
    }

    @Test
    fun `rounds coordinates half up to two decimal places`() {
        assertEquals("116.42", formatCoordinate(116.415))
        assertEquals("-33.87", formatCoordinate(-33.868))
        assertEquals("90.00", formatCoordinate(90.0))
    }

    @Test
    fun `rejects a blank or insecure host`() {
        assertThrows(IllegalArgumentException::class.java) {
            QWeatherConfig.create("  ", "key")
        }
        assertThrows(IllegalArgumentException::class.java) {
            QWeatherConfig.create("http://abc.qweatherapi.com", "key")
        }
        assertThrows(IllegalArgumentException::class.java) {
            QWeatherConfig.create("HTTP://abc.qweatherapi.com", "key")
        }
        assertThrows(IllegalArgumentException::class.java) {
            QWeatherConfig.create("", "")
        }
    }

    @Test
    fun `strips an https scheme in any case`() {
        assertEquals("abc.qweatherapi.com", QWeatherConfig.create("HTTPS://abc.qweatherapi.com/", "key").apiHost)
        assertEquals("abc.qweatherapi.com", QWeatherConfig.create(" abc.qweatherapi.com ", " key ").apiHost)
    }

    private class RecordingHttpGet(private val response: HttpResponse) : HttpGet {
        val requests = mutableListOf<HttpRequest>()

        override suspend fun get(request: HttpRequest): HttpResponse {
            requests += request
            return response
        }
    }
}
