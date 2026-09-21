package com.jxguo92.mykarooextension.extension

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DataFieldRegistryTest {
    @Test
    fun `XML and Kotlin registry declare the same unique type ids`() {
        val typeIds = DataFieldRegistry.typeIds
        val xmlTypeIds = extensionInfo().getElementsByTagName("DataType")
            .let { nodes -> (0 until nodes.length).map { nodes.item(it).attributes.getNamedItem("typeId").nodeValue } }

        assertEquals(typeIds.size, typeIds.toSet().size)
        assertEquals(typeIds.toSet(), xmlTypeIds.toSet())
    }

    @Test
    fun `extension id matches XML and contains no dots`() {
        val xmlExtensionId = extensionInfo().documentElement.getAttribute("id")

        assertEquals(MyKarooExtension.EXTENSION_ID, xmlExtensionId)
        assertFalse(MyKarooExtension.EXTENSION_ID.contains('.'))
    }

    private fun extensionInfo() = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(File("src/main/res/xml/extension_info.xml"))
}
