package com.resumestudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The clamps iOS applies on decode, so a hand-edited file cannot break a page. */
class ResumeSignatureTest {

    private fun stroke(vararg xy: Pair<Float, Float>) =
        SignatureStroke(xy.map { SignaturePoint(it.first, it.second) })

    @Test
    fun `width is held inside the range iOS allows`() {
        assertEquals(88.0, ResumeSignature(widthPoints = 10.0).normalized().widthPoints, 0.001)
        assertEquals(176.0, ResumeSignature(widthPoints = 900.0).normalized().widthPoints, 0.001)
        assertEquals(128.0, ResumeSignature().widthPoints, 0.001)
    }

    @Test
    fun `a negative page index cannot address a page that is not there`() {
        assertEquals(0, ResumeSignature(pageIndex = -4).normalized().pageIndex)
    }

    @Test
    fun `a single dot is not a signature`() {
        // One point is a tap, not a mark, and drawing it would put a speck on
        // the page that nobody meant to put there.
        assertTrue(ResumeSignature(strokes = listOf(stroke(0.5f to 0.5f))).isEmpty)
        assertFalse(ResumeSignature(strokes = listOf(stroke(0f to 0f, 1f to 1f))).isEmpty)
    }

    @Test
    fun `the default placement is the one iOS opens with`() {
        assertEquals(ResumeSignaturePlacement.LOWER_TRAILING, ResumeSignature().placement)
    }

    @Test
    fun `a signed document survives the wire`() {
        val signed = ResumeDocument.example.copy(
            signature = ResumeSignature(
                strokes = listOf(stroke(0f to 0.2f, 0.5f to 0.8f, 1f to 0.1f)),
                placement = ResumeSignaturePlacement.LOWER_CENTER,
                widthPoints = 152.0,
            ),
        )
        val reopened = ResumeJson.decode(ResumeJson.encode(signed))

        assertEquals(signed, reopened)
        assertEquals(3, reopened.signature?.strokes?.first()?.points?.size)
        assertEquals(ResumeSignaturePlacement.LOWER_CENTER, reopened.signature?.placement)
    }
}
