package com.jxguo92.mykarooextension.core.weather.qweather

import com.jxguo92.mykarooextension.core.weather.CurrentWeather
import com.jxguo92.mykarooextension.core.weather.WeatherException
import org.json.JSONArray
import org.json.JSONObject

internal object QWeatherCurrentWeatherParser {
    fun parse(body: String): CurrentWeather {
        val root = try {
            JSONObject(body)
        } catch (error: Exception) {
            throw WeatherException("QWeather response was not JSON.", cause = error)
        }

        val wind = root.optObject("wind")
        val direction = wind?.optObject("direction")
        return CurrentWeather(
            conditionText = root.optObject("condition")?.optText("text"),
            temperatureCelsius = root.optObject("temperature")?.celsius(),
            feelsLikeCelsius = root.optObject("feelsLike")?.celsius(),
            relativeHumidity = root.optFiniteDouble("humidity"),
            windDirectionDegrees = direction?.optFiniteDouble("degree"),
            windSpeedMetersPerSecond = wind?.optObject("speed")?.metersPerSecond(),
            windGustMetersPerSecond = root.optObject("windGust")?.metersPerSecond(),
            attributions = root.optObject("metadata")?.optStringList("attributions").orEmpty(),
        )
    }

    fun errorMessage(statusCode: Int, body: String): String {
        val error = runCatching { JSONObject(body).optObject("error") }.getOrNull()
        val title = error?.optText("title")
        val detail = error?.optText("detail")
        val invalid = error?.optJSONArray("invalidParams")?.toStringList()?.takeIf { it.isNotEmpty() }
        val summary = listOfNotNull(title, detail).joinToString(": ").ifBlank { null }
        val withParams = when {
            summary == null -> null
            invalid == null -> summary
            else -> "$summary (${invalid.joinToString()})"
        }
        return withParams ?: "QWeather request failed with HTTP $statusCode."
    }
}

private fun JSONObject.optObject(name: String): JSONObject? {
    if (!has(name) || isNull(name)) return null
    return optJSONObject(name)
}

private fun JSONObject.optText(name: String): String? {
    if (!has(name) || isNull(name)) return null
    return optString(name).trim().takeIf { it.isNotEmpty() && it != "null" }
}

private fun JSONObject.optFiniteDouble(name: String): Double? {
    if (!has(name) || isNull(name)) return null
    return optDouble(name).takeIf { it.isFinite() }
}

private fun JSONObject.optStringList(name: String): List<String> {
    return optJSONArray(name)?.toStringList().orEmpty()
}

private fun JSONArray.toStringList(): List<String> {
    return buildList {
        for (index in 0 until length()) {
            if (!isNull(index)) {
                optString(index).trim().takeIf { it.isNotEmpty() }?.let(::add)
            }
        }
    }
}

private fun JSONObject.celsius(): Double? = quantity { value, unit ->
    when (unit) {
        "°c", "c" -> value
        else -> null
    }
}

private fun JSONObject.metersPerSecond(): Double? = quantity { value, unit ->
    when (unit) {
        "m/s" -> value
        "km/h" -> value / 3.6
        else -> null
    }
}

/** [convert] receives the lowercase unit and returns null when the unit is unsupported. */
private inline fun JSONObject.quantity(convert: (value: Double, unit: String) -> Double?): Double? {
    val value = optFiniteDouble("value") ?: return null
    val unit = optText("unit") ?: throw WeatherException("QWeather quantity is missing a unit.")
    return convert(value, unit.lowercase()) ?: throw WeatherException("Unsupported QWeather unit: $unit.")
}
