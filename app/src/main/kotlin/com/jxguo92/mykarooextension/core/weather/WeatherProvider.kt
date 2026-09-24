package com.jxguo92.mykarooextension.core.weather

/**
 * Current conditions at one point. Quantities use a single unit system so
 * callers do not depend on a provider's native units.
 *
 * [relativeHumidity] is a fraction from 0 to 1. Wind direction is the
 * meteorological direction the wind comes from, in degrees clockwise from north.
 * [attributions] are provider-required credit URLs and must stay with any display.
 */
data class CurrentWeather(
    val conditionText: String?,
    val temperatureCelsius: Double?,
    val feelsLikeCelsius: Double?,
    val relativeHumidity: Double?,
    val windDirectionDegrees: Double?,
    val windSpeedMetersPerSecond: Double?,
    val windGustMetersPerSecond: Double?,
    val attributions: List<String> = emptyList(),
)

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Latitude must be between -90 and 90."
        }
        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Longitude must be between -180 and 180."
        }
    }
}

interface WeatherProvider {
    suspend fun currentWeather(location: GeoLocation): CurrentWeather
}

class WeatherException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
