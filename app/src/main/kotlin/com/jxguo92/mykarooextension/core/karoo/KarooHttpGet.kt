package com.jxguo92.mykarooextension.core.karoo

import com.jxguo92.mykarooextension.core.http.HttpGet
import com.jxguo92.mykarooextension.core.http.HttpRequest
import com.jxguo92.mykarooextension.core.http.HttpResponse
import com.jxguo92.mykarooextension.core.http.HttpTransportException
import com.jxguo92.mykarooextension.core.http.decodeResponseBody
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.HttpResponseState
import io.hammerhead.karooext.models.OnHttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

fun interface KarooHttpChannel {
    fun send(request: OnHttpResponse.MakeHttpRequest): Flow<HttpResponseState>
}

class KarooSystemHttpChannel(private val system: KarooSystemService) : KarooHttpChannel {
    override fun send(request: OnHttpResponse.MakeHttpRequest): Flow<HttpResponseState> = callbackFlow {
        val id = system.addConsumer<OnHttpResponse>(
            params = request,
            onError = { close(HttpTransportException("Karoo HTTP request failed: $it")) },
            onComplete = { close() },
            onEvent = { trySend(it.state) },
        )
        awaitClose { system.removeConsumer(id) }
    }
}

/**
 * Sends requests through the Karoo System, which picks Wi-Fi or, where supported,
 * the companion app over Bluetooth. Hammerhead limits this channel to small
 * (< 100 KB) ride-relevant requests.
 */
class KarooHttpGet(
    private val channel: KarooHttpChannel,
    private val timeoutMillis: Long = 10_000,
) : HttpGet {
    override suspend fun get(request: HttpRequest): HttpResponse {
        val params = OnHttpResponse.MakeHttpRequest(
            method = "GET",
            url = request.url,
            headers = request.headers,
            waitForConnection = true,
        )
        val completed = try {
            withTimeoutOrNull(timeoutMillis) {
                channel.send(params).filterIsInstance<HttpResponseState.Complete>().firstOrNull()
                    ?: throw HttpTransportException("Karoo HTTP request ended without a response.")
            } ?: throw HttpTransportException("Karoo HTTP request timed out after $timeoutMillis ms.")
        } catch (error: CancellationException) {
            throw error
        } catch (error: HttpTransportException) {
            throw error
        } catch (error: Exception) {
            throw HttpTransportException("Karoo HTTP request failed.", error)
        }
        if (completed.statusCode !in 100..599) {
            throw HttpTransportException(
                "Karoo HTTP request failed: ${completed.error ?: "status ${completed.statusCode}"}",
            )
        }
        return HttpResponse(
            statusCode = completed.statusCode,
            body = completed.body?.let(::decodeResponseBody).orEmpty(),
        )
    }
}
