package com.jxguo92.mykarooextension.core.http

import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

data class HttpRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

data class HttpResponse(
    val statusCode: Int,
    val body: String,
)

interface HttpGet {
    /**
     * Returns any HTTP status as a response. Transport failures throw [HttpTransportException];
     * cancellation propagates unchanged.
     */
    suspend fun get(request: HttpRequest): HttpResponse
}

class HttpTransportException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

internal fun decodeResponseBody(bytes: ByteArray): String {
    val payload = if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
        GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
    } else {
        bytes
    }
    return payload.toString(Charsets.UTF_8)
}
