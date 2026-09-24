package com.jxguo92.mykarooextension.core.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.GZIPInputStream

class UrlConnectionHttpGet(
    private val connectTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 10_000,
) : HttpGet {
    override suspend fun get(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        val connection = (URI(request.url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            instanceFollowRedirects = false
            request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        try {
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { decodeResponseBody(it.readBytes()) }.orEmpty()
            HttpResponse(statusCode = statusCode, body = body)
        } catch (error: Exception) {
            throw WeatherException("Weather request failed.", cause = error)
        } finally {
            connection.disconnect()
        }
    }
}

internal fun decodeResponseBody(bytes: ByteArray): String {
    val payload = if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
        GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
    } else {
        bytes
    }
    return payload.toString(Charsets.UTF_8)
}
