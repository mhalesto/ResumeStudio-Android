package com.resumestudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scan's arithmetic.
 *
 * The score is shown to someone about their own career, so the failure that
 * matters is not a crash — it is marking a résumé down for something it does
 * correctly, or reassuring one that would not survive the pass.
 */
class RecruiterScanTest {

    @Test
    fun `a quantified bullet is one carrying a figure`() {
        assertTrue(RecruiterScan.isQuantified("Cut cold start by 38%."))
        assertTrue(RecruiterScan.isQuantified("Led 4 teams."))
        assertTrue(RecruiterScan.isQuantified("Grew revenue 2x."))
        // Prose making a claim with nothing behind it.
        assertTrue(!RecruiterScan.isQuantified("Improved the onboarding experience."))
    }

    @Test
    fun `a blank resume is invisible in seven seconds`() {
        val report = RecruiterScan.analyze(ResumeDocument.blank)

        // Not zero: the fact-rail check grades the *template*, which a blank
        // document still has. Everything that grades content scores nothing,
        // which is what the verdict is reading.
        assertTrue("a blank page should score close to nothing, got ${report.score}", report.score <= 5)
        assertEquals("Invisible in seven seconds", report.verdict)
        assertTrue(report.capturedFacts.isEmpty())
        assertTrue(report.missedFacts.isNotEmpty())
        assertTrue(
            "no content check should pass on a blank page",
            report.findings.filter { it.id != "facts-rail" }.none { it.severity == RecruiterScan.Severity.PASS },
        )
    }

    @Test
    fun `the example scores the same at medium and high`() {
        // Not a dead control: the example's summary fits both skim limits and
        // its opening bullet is unquantified under either, so nothing moves.
        val medium = RecruiterScan.analyze(ResumeDocument.example, RecruiterScan.Strictness.MEDIUM)
        val high = RecruiterScan.analyze(ResumeDocument.example, RecruiterScan.Strictness.HIGH)
        assertEquals(87, medium.score)
        assertEquals(medium.score, high.score)
    }

    @Test
    fun `a generous read forgives the prose bullet`() {
        val low = RecruiterScan.analyze(ResumeDocument.example, RecruiterScan.Strictness.LOW)
        assertEquals(97, low.score)
        assertEquals("Survives the first pass", low.verdict)
    }

    @Test
    fun `a number in the first bullet is what the strict read is looking for`() {
        val quantified = ResumeDocument.example.copy(
            experience = ResumeDocument.example.experience.mapIndexed { index, entry ->
                if (index == 0) entry.copy(highlights = listOf("Cut time-to-hire by 38%.")) else entry
            },
        )
        val before = RecruiterScan.analyze(ResumeDocument.example, RecruiterScan.Strictness.HIGH).score
        val after = RecruiterScan.analyze(quantified, RecruiterScan.Strictness.HIGH).score
        assertTrue("adding a figure should raise the score", after > before)
    }

    @Test
    fun `a long summary is marked down only once it stops skimming`() {
        fun scoreFor(words: Int) = RecruiterScan.analyze(
            ResumeDocument.example.copy(professionalProfile = List(words) { "word" }.joinToString(" ")),
            RecruiterScan.Strictness.HIGH,
        ).findings.first { it.id == "profile-skim" }

        assertEquals(RecruiterScan.Severity.PASS, scoreFor(40).severity)
        assertEquals(RecruiterScan.Severity.WARNING, scoreFor(120).severity)
    }

    @Test
    fun `a two-column template earns the fact rail`() {
        val single = RecruiterScan.analyze(ResumeDocument.example.copy(template = ResumeTemplate.MODERN))
        val twoColumn = RecruiterScan.analyze(ResumeDocument.example.copy(template = ResumeTemplate.ATLAS))
        assertTrue(twoColumn.score > single.score)
    }

    @Test
    fun `the gaze budget never exceeds the study's seven seconds`() {
        val report = RecruiterScan.analyze(ResumeDocument.example)
        val total = report.findings.sumOf { it.gazeSeconds }
        assertEquals(RecruiterScan.SCAN_SECONDS, total, 0.01)
    }
}
