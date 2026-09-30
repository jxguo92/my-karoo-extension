package com.jxguo92.mykarooextension.core.http

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UrlConnectionHttpGet(
    private val connectTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 10_000,
) : HttpGet {
    override suspend fun get(request: HttpRequest): HttpResponse = suspendCancellableCoroutine { continuation ->
        val connection = AtomicReference<HttpURLConnection?>()
        // HttpURLConnection blocks. Cancellation disconnects its socket without waiting for IO timeout.
        val worker = CoroutineScope(Dispatchers.IO).launch {
            var active: HttpURLConnection? = null
            try {
                active = (URI(request.url).toURL().openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = connectTimeoutMillis
                    readTimeout = readTimeoutMillis
                    instanceFollowRedirects = false
                    request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
                }
                connection.set(active)
                if (!continuation.isActive) return@launch
                val statusCode = active.responseCode
                val stream = if (statusCode in 200..299) active.inputStream else active.errorStream
                val body = stream?.use { decodeResponseBody(it.readBytes()) }.orEmpty()
                if (continuation.isActive) continuation.resume(HttpResponse(statusCode, body))
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(HttpTransportException("HTTP request failed.", error))
            } finally {
                connection.set(null)
                active?.disconnect()
            }
        }
        continuation.invokeOnCancellation {
            worker.cancel()
            // Some JVM implementations synchronize disconnect with an ongoing read; keep the caller cancellable.
            CoroutineScope(Dispatchers.IO).launch { connection.getAndSet(null)?.disconnect() }
        }
    }
}
