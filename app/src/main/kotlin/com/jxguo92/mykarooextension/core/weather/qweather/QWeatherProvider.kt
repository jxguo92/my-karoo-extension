package com.jxguo92.mykarooextension.core.weather.qweather

import com.jxguo92.mykarooextension.core.http.HttpGet
import com.jxguo92.mykarooextension.core.http.HttpRequest
import com.jxguo92.mykarooextension.core.http.HttpTransportException
import com.jxguo92.mykarooextension.core.http.UrlConnectionHttpGet
import com.jxguo92.mykarooextension.core.weather.CurrentWeather
import com.jxguo92.mykarooextension.core.weather.GeoLocation
import com.jxguo92.mykarooextension.core.weather.WeatherException
import com.jxguo92.mykarooextension.core.weather.WeatherProvider
import java.math.BigDecimal
import java.math.RoundingMode

class QWeatherProvider(
    private val config: QWeatherConfig,
    private val http: HttpGet = UrlConnectionHttpGet(),
) : WeatherProvider {
    override suspend fun currentWeather(location: GeoLocation): CurrentWeather {
        val request = HttpRequest(
            url = currentWeatherUrl(config.apiHost, location),
            headers = mapOf("X-QW-Api-Key" to config.apiKey),
        )
        val response = try {
            http.get(request)
        } catch (error: HttpTransportException) {
            throw WeatherException("Weather request failed.", cause = error)
        }
        if (response.statusCode !in 200..299) {
            throw WeatherException(
                message = QWeatherCurrentWeatherParser.errorMessage(response.statusCode, response.body),
                statusCode = response.statusCode,
            )
        }
        return QWeatherCurrentWeatherParser.parse(response.body)
    }
}

internal fun currentWeatherUrl(apiHost: String, location: GeoLocation): String {
    val latitude = formatCoordinate(location.latitude)
    val longitude = formatCoordinate(location.longitude)
    return "https://$apiHost/weather/v1/current/$latitude/$longitude"
}

internal fun formatCoordinate(value: Double): String {
    return BigDecimal.valueOf(value)
        .setScale(2, RoundingMode.HALF_UP)
        .toPlainString()
}
