package com.jxguo92.mykarooextension.core.weather.qweather

import com.jxguo92.mykarooextension.BuildConfig

class QWeatherConfig private constructor(
    val apiHost: String,
    val apiKey: String,
) {
    companion object {
        fun create(apiHost: String, apiKey: String): QWeatherConfig {
            val host = normalizeHost(apiHost)
            val key = apiKey.trim()
            require(host.isNotBlank()) {
                "QWeather API host is blank. Set qweather.apiHost in local.properties."
            }
            require(!host.contains('/') && !host.contains('@') && ' ' !in host) {
                "QWeather API host must be a hostname."
            }
            require(key.isNotBlank()) {
                "QWeather API key is blank. Set qweather.apiKey in local.properties."
            }
            return QWeatherConfig(apiHost = host, apiKey = key)
        }

        /** Returns null when the build has no QWeather host or key. */
        fun fromBuildConfig(): QWeatherConfig? {
            if (BuildConfig.QWEATHER_API_HOST.isBlank() || BuildConfig.QWEATHER_API_KEY.isBlank()) {
                return null
            }
            return create(
                apiHost = BuildConfig.QWEATHER_API_HOST,
                apiKey = BuildConfig.QWEATHER_API_KEY,
            )
        }
    }
}

internal fun normalizeHost(raw: String): String {
    val trimmed = raw.trim().removeSuffix("/")
    require(!trimmed.startsWith("http://", ignoreCase = true)) { "QWeather API host must use HTTPS." }
    val scheme = "https://"
    return if (trimmed.startsWith(scheme, ignoreCase = true)) trimmed.substring(scheme.length) else trimmed
}
