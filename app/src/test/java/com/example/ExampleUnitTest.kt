package com.example

import com.example.net.JsonParsers
import com.example.util.Abbrev
import com.example.util.DateUtils
import com.example.util.Slug
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

    @Test
    fun testSlugAlgorithmSingleCharFold() {
        // Verified in Plan Section 1 & Section 10
        assertEquals("shqiperia", Slug.of("Shqipëria"))
        assertEquals("cka", Slug.of("Çka"))
        assertEquals("fjale-kryq", Slug.of("fjale (kryq)"))
        assertEquals("shqiperi", Slug.of("shqiperi"))
        assertEquals("shkolle", Slug.of("SHKOLLË"))
    }

    @Test
    fun testMatchSlug() {
        val slugs = setOf("shkolle", "shkolla", "mesim", "liber", "shqiperia")
        assertEquals("shkolle", Slug.matchSlug("shkolle", slugs))
        // delete one char retry for length >= 6
        assertEquals("shkolle", Slug.matchSlug("shkollte", slugs))
        assertNull(Slug.matchSlug("xyz", slugs))
    }

    @Test
    fun testAbbrevExpansion() {
        assertEquals("femërore", Abbrev.expand("f."))
        assertEquals("mashkullore", Abbrev.expand("m."))
        assertEquals("shumës", Abbrev.expand("sh."))
        assertEquals("mbiemër", Abbrev.expand("mb."))
    }

    @Test
    fun testJsHash() {
        // Hash should be non-negative integer
        val hash = DateUtils.jsHash("2026-10-02")
        assert(hash >= 0)
        assertEquals(DateUtils.jsHash("2026-10-02"), DateUtils.jsHash("2026-10-02"))
    }

    @Test
    fun testJsonParsingOmittedDefinitions() {
        // As verified in plan, definitions can be omitted completely
        val jsonWithoutDefs = JSONObject("""{"slug":"shkolle","term":"SHKOLLË","attributes":["F.","SH."]}""")
        val entry1 = JsonParsers.parseEntry(jsonWithoutDefs)
        assertEquals("shkolle", entry1.slug)
        assertEquals("SHKOLLË", entry1.term)
        assertEquals(listOf("F.", "SH."), entry1.attributes)
        assertNull(entry1.definitions)
        assertEquals(emptyList<String>(), entry1.displayDefinitions)

        val jsonWithDefs = JSONObject("""{"slug":"shkolle","term":"SHKOLLË","attributes":["F."],"definitions":["Institucion arsimor"]}""")
        val entry2 = JsonParsers.parseEntry(jsonWithDefs)
        assertNotNull(entry2.definitions)
        assertEquals(1, entry2.displayDefinitions.size)
        assertEquals("Institucion arsimor", entry2.firstDefinition)
    }
}
