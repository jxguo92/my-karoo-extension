package com.jxguo92.mykarooextension.core.weather

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
     * Returns any HTTP status as a response. Transport failures throw [WeatherException];
     * cancellation propagates unchanged.
     */
    suspend fun get(request: HttpRequest): HttpResponse
}
