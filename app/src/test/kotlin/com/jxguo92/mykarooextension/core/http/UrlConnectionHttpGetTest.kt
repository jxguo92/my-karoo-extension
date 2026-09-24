package com.jxguo92.mykarooextension.core.http

import kotlinx.coroutines.test.runTest
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
}
