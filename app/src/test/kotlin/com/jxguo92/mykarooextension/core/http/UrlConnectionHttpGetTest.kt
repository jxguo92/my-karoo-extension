package com.jxguo92.mykarooextension.core.http

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.*
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class UrlConnectionHttpGetTest {
    @Test
    fun `decodes gzip bodies and leaves plain json unchanged`() {
        val json = """{"temperature":{"value":1,"unit":"°C"}}"""
        val gzip = ByteArrayOutputStream().also { output ->
            GZIPOutputStream(output).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        }.toByteArray()

        assertEquals(json, decodeResponseBody(gzip))
        assertEquals(json, decodeResponseBody(json.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `reports an unusable url as a transport failure`() = runTest {
        listOf("not a url", "relative/path").forEach { url ->
            val error = runCatching { UrlConnectionHttpGet().get(HttpRequest(url)) }.exceptionOrNull()

            assertTrue("$url -> $error", error is HttpTransportException)
        }
    }

    @Test
    fun `cancelling an in flight request does not wait for the read timeout`() = runBlocking {
        val arrived = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            arrived.countDown()
            release.await(10, TimeUnit.SECONDS)
            exchange.close()
        }
        server.start()
        try {
            val job = launch(Dispatchers.Default) {
                UrlConnectionHttpGet(readTimeoutMillis = 10_000).get(HttpRequest("http://127.0.0.1:${server.address.port}/"))
            }
            assertTrue(arrived.await(3, TimeUnit.SECONDS))
            withTimeout(1_500) { job.cancelAndJoin() }
            assertTrue(job.isCancelled)
        } finally {
            release.countDown()
            server.stop(0)
        }
    }
}
