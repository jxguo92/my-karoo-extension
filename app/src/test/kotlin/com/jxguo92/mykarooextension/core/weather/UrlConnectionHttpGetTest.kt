package com.jxguo92.mykarooextension.core.weather

import org.junit.Assert.assertEquals
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
}
