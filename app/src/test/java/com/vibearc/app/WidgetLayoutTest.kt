package com.vibearc.app

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class WidgetLayoutTest {
    private val names = listOf("player", "album", "vinyl", "console", "lyrics", "mini", "aurora", "compact")
    private val namespace = "http://schemas.android.com/apk/res/android"
    @Test fun `layouts use only RemoteViews supported classes`() {
        val supported = setOf("FrameLayout", "LinearLayout", "TextView", "ImageView", "ImageButton", "ProgressBar")
        for (name in names) for (node in elements(name)) {
            assertTrue("$name ${node.tagName}", node.tagName in supported)
        }
    }
    private fun elements(name: String): List<Element> {
        val path = "src/main/res/layout/widget_$name.xml"
        val file = listOf(File(path), File("app/$path")).first { it.isFile }
        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(file)
        return document.getElementsByTagName("*").let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }
        }
    }
    @Test fun `oversized launcher cells do not stretch the visible glass panel`() {
        for (name in names) {
            val nodes = elements(name)
            val surface = nodes.first { it.getAttributeNS(namespace, "id") == "@android:id/background" }
            assertEquals(name, "wrap_content", surface.getAttributeNS(namespace, "layout_height"))
            assertEquals(name, "FrameLayout", nodes.first().tagName)
            assertFalse(name, nodes.first().hasAttributeNS(namespace, "background"))
        }
    }
    @Test fun `vertical space cannot push metadata and controls apart`() {
        for (name in names) for (node in elements(name)) {
            val parent = node.parentNode as? Element ?: continue
            if (parent.getAttributeNS(namespace, "orientation") == "vertical") {
                assertFalse("$name ${node.tagName}", node.hasAttributeNS(namespace, "layout_weight"))
            }
        }
    }
    @Test fun `transport icons are flat vectors rather than legacy system artwork`() {
        for (name in names) for (button in elements(name).filter { it.tagName == "ImageButton" }) {
            assertTrue(name, button.getAttributeNS(namespace, "src").startsWith("@drawable/widget_"))
        }
    }
    @Test fun `all transports retain labelled 48 dp touch targets`() {
        for (name in names) {
            val buttons = elements(name).filter { it.tagName == "ImageButton" }
            assertEquals(name, 3, buttons.size)
            for (button in buttons) {
                assertEquals(name, "48dp", button.getAttributeNS(namespace, "layout_width"))
                assertEquals(name, "48dp", button.getAttributeNS(namespace, "layout_height"))
                assertTrue(name, button.hasAttributeNS(namespace, "contentDescription"))
            }
        }
    }
}
