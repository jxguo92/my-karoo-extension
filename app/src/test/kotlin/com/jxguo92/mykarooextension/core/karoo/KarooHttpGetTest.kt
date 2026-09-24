package com.jxguo92.mykarooextension.core.karoo

import com.jxguo92.mykarooextension.core.http.HttpRequest
import com.jxguo92.mykarooextension.core.http.HttpResponse
import com.jxguo92.mykarooextension.core.http.HttpTransportException
import io.hammerhead.karooext.models.HttpResponseState
import io.hammerhead.karooext.models.OnHttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class KarooHttpGetTest {
    private val request = HttpRequest(url = "https://example.com/data", headers = mapOf("X-Key" to "k"))

    @Test
    fun `sends a queued GET with the request url and headers`() = runTest {
        val channel = FakeChannel(flowOf(complete(200, "ok")))

        KarooHttpGet(channel).get(request)

        val sent = channel.requests.single()
        assertEquals("GET", sent.method)
        assertEquals("https://example.com/data", sent.url)
        assertEquals(mapOf("X-Key" to "k"), sent.headers)
        assertNull(sent.body)
        assertTrue(sent.waitForConnection)
    }

    @Test
    fun `waits past queued and in-progress states for the completed response`() = runTest {
        val channel = FakeChannel(
            flowOf(HttpResponseState.Queued, HttpResponseState.InProgress, complete(200, """{"a":1}""")),
        )

        val response = KarooHttpGet(channel).get(request)

        assertEquals(HttpResponse(200, """{"a":1}"""), response)
    }

    @Test
    fun `returns http error statuses as responses`() = runTest {
        val channel = FakeChannel(flowOf(complete(401, "denied", error = "Unauthorized")))

        val response = KarooHttpGet(channel).get(request)

        assertEquals(HttpResponse(401, "denied"), response)
    }

    @Test
    fun `treats a missing body as empty`() = runTest {
        val channel = FakeChannel(flowOf(HttpResponseState.Complete(204, emptyMap(), null, null)))

        assertEquals(HttpResponse(204, ""), KarooHttpGet(channel).get(request))
    }

    @Test
    fun `reports a completion without an http status as a transport failure`() = runTest {
        val channel = FakeChannel(flowOf(HttpResponseState.Complete(0, emptyMap(), null, "No connection")))

        val error = runCatching { KarooHttpGet(channel).get(request) }.exceptionOrNull()

        assertTrue(error is HttpTransportException)
        assertTrue(error!!.message!!.contains("No connection"))
    }

    @Test
    fun `reports a channel that ends without a response as a transport failure`() = runTest {
        val channel = FakeChannel(emptyFlow())

        val error = runCatching { KarooHttpGet(channel).get(request) }.exceptionOrNull()

        assertTrue(error is HttpTransportException)
    }

    @Test
    fun `wraps channel errors as transport failures`() = runTest {
        val channel = FakeChannel(flow { throw IllegalStateException("service disconnected") })

        val error = runCatching { KarooHttpGet(channel).get(request) }.exceptionOrNull()

        assertTrue(error is HttpTransportException)
        assertTrue(error!!.cause is IllegalStateException)
        assertEquals("service disconnected", error.cause!!.message)
    }

    @Test
    fun `gives up after the timeout and releases the channel`() = runTest {
        var released = false
        val channel = FakeChannel(
            flow<HttpResponseState> {
                emit(HttpResponseState.Queued)
                awaitCancellation()
            }.onCompletion { released = true },
        )

        val error = runCatching {
            KarooHttpGet(channel, timeoutMillis = 5_000).get(request)
        }.exceptionOrNull()

        assertTrue(error is HttpTransportException)
        assertTrue(released)
        assertEquals(5_000L, testScheduler.currentTime)
    }

    @Test
    fun `cancelling the caller releases the channel without wrapping`() = runTest {
        var released = false
        var failure: Throwable? = null
        val channel = FakeChannel(
            flow<HttpResponseState> { awaitCancellation() }.onCompletion { released = true },
        )

        val job = launch {
            try {
                KarooHttpGet(channel).get(request)
            } catch (error: Throwable) {
                failure = error
                throw error
            }
        }
        runCurrent()
        job.cancel()
        advanceTimeBy(1)

        assertTrue(released)
        assertTrue(failure is CancellationException)
    }

    @Test
    fun `decodes gzip bodies from the karoo channel`() = runTest {
        val json = """{"temperature":1}"""
        val gzip = ByteArrayOutputStream().also { output ->
            GZIPOutputStream(output).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        }.toByteArray()
        val channel = FakeChannel(flowOf(HttpResponseState.Complete(200, emptyMap(), gzip, null)))

        assertEquals(json, KarooHttpGet(channel).get(request).body)
    }

    private fun complete(status: Int, body: String, error: String? = null) =
        HttpResponseState.Complete(status, emptyMap(), body.toByteArray(Charsets.UTF_8), error)

    private class FakeChannel(private val states: Flow<HttpResponseState>) : KarooHttpChannel {
        val requests = mutableListOf<OnHttpResponse.MakeHttpRequest>()

        override fun send(request: OnHttpResponse.MakeHttpRequest): Flow<HttpResponseState> {
            requests += request
            return states
        }
    }
}
