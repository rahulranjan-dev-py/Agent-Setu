package app.agentsetu

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element

/** Every translatable English string must also exist in Hindi, with the same placeholders. */
class StringResourcesTest {

    private fun load(dir: String): Map<String, Element> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$dir/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as Element }.associateBy { it.getAttribute("name") }
    }

    private fun placeholders(text: String) = Regex("%\\d+\\$[sd]").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun hindiMatchesEnglish() {
        val en = load("values").filterValues { it.getAttribute("translatable") != "false" }
        val hi = load("values-hi")
        assertEquals("missing in values-hi", emptySet<String>(), en.keys - hi.keys)
        assertEquals("only in values-hi", emptySet<String>(), hi.keys - en.keys)
        for ((key, value) in en) {
            assertEquals("placeholders differ for $key", placeholders(value.textContent), placeholders(hi.getValue(key).textContent))
        }
    }
}
