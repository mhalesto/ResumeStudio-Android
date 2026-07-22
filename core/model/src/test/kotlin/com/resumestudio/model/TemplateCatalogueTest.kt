package com.resumestudio.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parity suite.
 *
 * This is the whole of what keeps the Android catalogue honest, so it is worth
 * being explicit about what it can and cannot catch. It proves that the template
 * *vocabulary* here matches the spec lifted from iOS — every case present, every
 * plan resolving to the same values. It says nothing about whether the two
 * renderers then draw those plans the same way; that is what the golden-image
 * tests in `:core:render` are for.
 *
 * When this fails after an iOS change, the fix is to regenerate, not to edit:
 *
 *     python3 tools/generate-template-spec.py \
 *       ../ios/ResumeStudio/ResumeStudio/Models/ResumeDocument.swift \
 *       spec/template-catalogue.json
 */
class TemplateCatalogueTest {

    private val specText: String =
        requireNotNull(javaClass.getResourceAsStream("/template-catalogue.json")) {
            "spec resource missing"
        }.bufferedReader().use { it.readText() }

    private val spec = Json.parseToJsonElement(specText).jsonObject

    @Test
    fun `every template in the enum has a plan in the spec`() {
        val specNames = spec["templates"]!!.jsonObject.keys
        val missing = ResumeTemplate.entries.map { it.wireName }.filterNot { it in specNames }
        assertTrue("templates missing from the spec: $missing", missing.isEmpty())
    }

    @Test
    fun `every template in the spec has a case in the enum`() {
        val unmirrored = spec["templates"]!!.jsonObject.keys
            .filter { ResumeTemplate.from(it) == null }
        assertTrue(
            "iOS has templates Android has not mirrored: $unmirrored — regenerate ResumeTemplate.kt",
            unmirrored.isEmpty(),
        )
    }

    @Test
    fun `the spec's own count agrees with the enum`() {
        val declared = spec["templateCount"]!!.jsonPrimitive.content.toInt()
        assertEquals(declared, ResumeTemplate.entries.size)
        assertEquals(declared, TemplateCatalogue.templateCount)
    }

    // Hand-checked against `ResumeDocument.swift`. These are the shapes most
    // likely to be broken by a careless regeneration — a two-column plan, a
    // density below 1, a body inset, and a dark page.

    @Test
    fun `atlas is a dark leading column with chips and icon rows`() {
        val plan = ResumeTemplate.ATLAS.plan
        val body = plan.body
        assertTrue("expected a side column", body is BodyLayout.Side)
        val column = (body as BodyLayout.Side).column
        assertEquals(SideColumn.Edge.LEADING, column.edge)
        assertEquals(186f, column.width, 0.001f)
        assertEquals(SideColumn.Fill.DARK, column.fill)
        assertTrue(column.prefersLightInk)
        assertEquals(CompetencyStyle.CHIPS, plan.competencies)
        assertEquals(ContactStyle.ICON_ROWS, plan.contact)
        assertTrue(plan.hasSideColumn)
    }

    @Test
    fun `concise keeps the summary full width and buys space with density`() {
        val plan = ResumeTemplate.CONCISE.plan
        val column = (plan.body as BodyLayout.Side).column
        assertEquals(SideColumn.Edge.TRAILING, column.edge)
        assertEquals(SideColumn.Fill.NONE, column.fill)
        assertTrue("the summary must run full width before the split", column.startsBelowProfile)
        assertFalse(column.prefersLightInk)
        assertEquals(0.86f, plan.density, 0.001f)
    }

    @Test
    fun `geneva draws its divide rather than painting it`() {
        val column = (ResumeTemplate.GENEVA.plan.body as BodyLayout.Side).column
        assertTrue(column.divider)
        assertEquals(SideColumn.Fill.NONE, column.fill)
        assertEquals(CompetencyStyle.METERS, ResumeTemplate.GENEVA.plan.competencies)
    }

    @Test
    fun `axis insets the body past its rail`() {
        assertEquals(54f, ResumeTemplate.AXIS.plan.bodyInset, 0.001f)
    }

    @Test
    fun `noir prints light type on a dark page`() {
        assertTrue(ResumeTemplate.NOIR.plan.darkPaper)
    }

    @Test
    fun `the plain catalogue really is plain`() {
        // `modern` is the archetype of the original single-column catalogue; if
        // regeneration ever starts inventing plans, this is where it shows.
        assertEquals(TemplatePlan(), ResumeTemplate.MODERN.plan)
    }

    @Test
    fun `the two-column templates are the expected minority`() {
        val twoColumn = TemplateCatalogue.twoColumnTemplates()
        assertEquals(27, twoColumn.size)
        assertTrue(ResumeTemplate.ATLAS in twoColumn)
        assertFalse(ResumeTemplate.MODERN in twoColumn)
    }

    @Test
    fun `every accent survives a round trip through its wire name`() {
        ResumeAccent.entries.forEach {
            assertEquals(it, ResumeAccent.from(it.wireName))
        }
        assertEquals(4, ResumeAccent.entries.count { !it.isPremium })
    }

    @Test
    fun `burnt orange keeps the exact channel values iOS renders`() {
        val orange = ResumeAccent.ORANGE
        assertEquals(0.82f, orange.red, 0.0001f)
        assertEquals(0.28f, orange.green, 0.0001f)
        assertEquals(0.04f, orange.blue, 0.0001f)
        assertEquals(0xFFD1470A.toInt(), orange.argb)
    }
}

/** The document wire format is the sync contract, so it gets its own guard. */
class ResumeDocumentJsonTest {

    @Test
    fun `a document survives a round trip`() {
        val original = ResumeDocument(
            personal = PersonalDetails("Halalisani Mbanjwa", "Engineer", "555", "a@b.com"),
            professionalProfile = "Builds things.",
            competencies = listOf("Kotlin", "Swift"),
            experience = listOf(ExperienceEntry(role = "Dev", company = "Acme", period = "2020–", highlights = listOf("Shipped"))),
            accent = ResumeAccent.COBALT,
            template = ResumeTemplate.ATLAS,
        )
        assertEquals(original, ResumeJson.decode(ResumeJson.encode(original)))
    }

    @Test
    fun `a field iOS adds tomorrow does not stop a document opening today`() {
        val json = """
            {"schemaVersion":2,"personal":{"fullName":"A"},"template":"atlas",
             "accent":"cobalt","somethingIOSAddedLater":{"nested":true}}
        """.trimIndent()
        val document = ResumeJson.decode(json)
        assertEquals("A", document.personal.fullName)
        assertEquals(ResumeTemplate.ATLAS, document.template)
    }

    @Test
    fun `a missing setting falls back rather than throwing`() {
        // The iOS decoder is hand-written so that no absent key can take a résumé
        // down with it. Defaults give us the same guarantee; this pins it.
        val document = ResumeJson.decode("""{"schemaVersion":2}""")
        assertEquals(ResumeLayoutSettings(), document.layout)
        assertEquals(34.0, document.layout.marginPoints, 0.001)
        assertEquals(ResumeContentBlock.entries.size, document.layout.sectionOrder.size)
        assertNotNull(document.template)
    }

    @Test
    fun `normalize clamps and backfills the section order the way iOS does`() {
        val settings = ResumeLayoutSettings(
            fontScale = 9.0,
            marginPoints = 2.0,
            sectionOrder = listOf(ResumeContentBlock.REFERENCES, ResumeContentBlock.REFERENCES),
        ).normalized()

        assertEquals(1.12, settings.fontScale, 0.001)
        assertEquals(24.0, settings.marginPoints, 0.001)
        assertEquals(ResumeContentBlock.REFERENCES, settings.sectionOrder.first())
        assertEquals(ResumeContentBlock.entries.size, settings.sectionOrder.size)
        assertEquals(settings.sectionOrder.size, settings.sectionOrder.distinct().size)
    }
}
