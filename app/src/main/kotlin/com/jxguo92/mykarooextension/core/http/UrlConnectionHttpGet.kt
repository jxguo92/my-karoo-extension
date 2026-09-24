package com.jxguo92.mykarooextension.core.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

class UrlConnectionHttpGet(
    private val connectTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 10_000,
) : HttpGet {
    override suspend fun get(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URI(request.url).toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMillis
                readTimeout = readTimeoutMillis
                instanceFollowRedirects = false
                request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
            }
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { decodeResponseBody(it.readBytes()) }.orEmpty()
            HttpResponse(statusCode = statusCode, body = body)
        } catch (error: Exception) {
            throw HttpTransportException("HTTP request failed.", cause = error)
        } finally {
            connection?.disconnect()
        }
    }
}
