package com.resumestudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The matching rules, which decide what the Today card accuses the user of.
 *
 * Getting these wrong is not a crash — it is the app telling someone their
 * résumé does not demonstrate a skill it plainly does, which is worse. The
 * stemmer is deliberately blunt, so what is pinned here is the specific
 * vocabulary it was tuned to converge.
 */
class ClaimAuditTest {

    @Test
    fun `the stemmer converges the forms a resume actually uses`() {
        // manage / managed / management / manager
        val manage = ClaimAudit.stem("manage")
        listOf("managed", "management", "manager", "managing").forEach {
            assertEquals("$it should stem alongside manage", manage, ClaimAudit.stem(it))
        }
    }

    @Test
    fun `irregular verbs survive the length test`() {
        // "led" is three characters and would be discarded as noise before it
        // could become "lead" — the most common leadership evidence there is.
        assertEquals(ClaimAudit.stem("lead"), ClaimAudit.stem("led"))
        assertEquals(ClaimAudit.stem("build"), ClaimAudit.stem("built"))
        assertEquals(ClaimAudit.stem("grow"), ClaimAudit.stem("grew"))
    }

    @Test
    fun `a claim is demonstrated when its words appear in the evidence`() {
        assertTrue(
            ClaimAudit.demonstrates(
                "Manager Coaching",
                "Introduced a manager toolkit and coaching programme.",
            ),
        )
    }

    @Test
    fun `a claim is not demonstrated when only part of it appears`() {
        // Every meaningful word has to land, or "People Analytics" would be
        // matched by any bullet mentioning people.
        assertFalse(
            ClaimAudit.demonstrates(
                "People Analytics",
                "Partnered with leaders on organisation design and role clarity.",
            ),
        )
    }

    @Test
    fun `a skills list cannot be its own evidence`() {
        val document = ResumeDocument(
            competencies = listOf("Underwater Basket Weaving"),
            experience = listOf(ExperienceEntry(role = "Engineer", highlights = listOf("Shipped things."))),
        )
        // The competency appears in the document, but only in the competency
        // list — which is exactly the failure this is here to surface.
        assertEquals(listOf("Underwater Basket Weaving"), ClaimAudit.unsupportedCompetencies(document))
    }

    @Test
    fun `the example resume has the unsupported skills iOS reports`() {
        // The Today card says "4 skills are listed that no bullet demonstrates".
        assertEquals(4, ClaimAudit.unsupportedCompetencies(ResumeDocument.example).size)
    }

    @Test
    fun `a blank resume claims nothing and so is never accused`() {
        assertTrue(ClaimAudit.unsupportedCompetencies(ResumeDocument.blank).isEmpty())
    }
}

/** The letter's own rules: the pairing, and when it is worth rendering. */
class CoverLetterTest {

    @Test
    fun `a letter seeded from a resume wears the paired letterhead`() {
        val resume = ResumeDocument.example.copy(template = ResumeTemplate.NOIR)
        val letter = CoverLetterDocument.fromResume(resume)

        assertEquals("Avery Sample", letter.senderName)
        assertEquals(resume.accent, letter.accent)
        // Noir has a letter drawn to match it; the pairing is the point.
        assertEquals(ResumeTemplate.NOIR, letter.template.pairsWith)
    }

    @Test
    fun `a resume with no paired letter still gets a usable one`() {
        val unpaired = ResumeTemplate.entries.first { CoverLetterTemplate.pairedWith(it) == null }
        assertEquals(
            CoverLetterTemplate.MODERN,
            CoverLetterDocument.fromResume(ResumeDocument.example.copy(template = unpaired)).template,
        )
    }

    @Test
    fun `an empty letter is not worth rendering`() {
        assertFalse(CoverLetterDocument().isReadyToPreview)
        assertFalse(CoverLetterDocument(senderName = "A").isReadyToPreview)
        assertTrue(
            CoverLetterDocument(senderName = "A", bodyParagraphs = listOf("Something.")).isReadyToPreview,
        )
    }

    @Test
    fun `the filename is the one the recipient will see`() {
        assertEquals(
            "Avery Sample Cover Letter",
            CoverLetterDocument(senderName = "Avery Sample").suggestedFilename,
        )
        // Characters Windows and the web object to, not just Android's.
        assertEquals(
            "A-B Cover Letter",
            CoverLetterDocument(senderName = "A/B").suggestedFilename,
        )
    }
}
