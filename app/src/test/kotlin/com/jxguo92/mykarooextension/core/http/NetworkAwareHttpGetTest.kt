package com.jxguo92.mykarooextension.core.http

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkAwareHttpGetTest {
    private val request = HttpRequest(url = "https://example.com/data", headers = mapOf("X-Key" to "k"))

    @Test
    fun `uses the direct client while wifi is connected`() = runTest {
        val direct = RecordingHttpGet(HttpResponse(200, "direct"))
        val karoo = RecordingHttpGet(HttpResponse(200, "karoo"))
        val http = NetworkAwareHttpGet(wifi = { true }, direct = direct, karoo = karoo)

        val response = http.get(request)

        assertEquals("direct", response.body)
        assertEquals(listOf(request), direct.requests)
        assertEquals(emptyList<HttpRequest>(), karoo.requests)
    }

    @Test
    fun `uses the karoo channel without wifi`() = runTest {
        val direct = RecordingHttpGet(HttpResponse(200, "direct"))
        val karoo = RecordingHttpGet(HttpResponse(200, "karoo"))
        val http = NetworkAwareHttpGet(wifi = { false }, direct = direct, karoo = karoo)

        val response = http.get(request)

        assertEquals("karoo", response.body)
        assertEquals(emptyList<HttpRequest>(), direct.requests)
        assertEquals(listOf(request), karoo.requests)
    }

    @Test
    fun `checks wifi again for every request`() = runTest {
        var wifiConnected = true
        val direct = RecordingHttpGet(HttpResponse(200, "direct"))
        val karoo = RecordingHttpGet(HttpResponse(200, "karoo"))
        val http = NetworkAwareHttpGet(wifi = { wifiConnected }, direct = direct, karoo = karoo)

        val first = http.get(request)
        wifiConnected = false
        val second = http.get(request)

        assertEquals(listOf("direct", "karoo"), listOf(first.body, second.body))
    }

    private class RecordingHttpGet(private val response: HttpResponse) : HttpGet {
        val requests = mutableListOf<HttpRequest>()

        override suspend fun get(request: HttpRequest): HttpResponse {
            requests += request
            return response
        }
    }
}
