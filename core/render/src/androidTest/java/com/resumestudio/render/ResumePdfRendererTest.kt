package com.resumestudio.render

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.resumestudio.model.EducationEntry
import com.resumestudio.model.ExperienceEntry
import com.resumestudio.model.PersonalDetails
import com.resumestudio.model.ReferenceEntry
import com.resumestudio.model.ResumeAccent
import com.resumestudio.model.ResumeAdditionalSection
import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeTemplate
import com.resumestudio.model.TemplateCatalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Proves the renderer produces a PDF a reader will actually open.
 *
 * These run on a device because `PdfDocument`, `Canvas` and `StaticLayout` are
 * framework classes with no meaningful JVM stand-in — a unit test here would be
 * testing a mock, which is precisely the thing that would let a broken export
 * ship. Every page is reopened with [PdfRenderer] afterwards, so a file that
 * writes without error but cannot be parsed still fails.
 *
 * This is not yet the golden-image suite. It shows that pages are produced and
 * are well-formed; it does not compare them to iOS output. That comparison is
 * what finally pins the layout, and it is the next thing to build.
 */
@RunWith(AndroidJUnit4::class)
class ResumePdfRendererTest {

    private val renderer = ResumePdfRenderer()

    private val sample = ResumeDocument(
        personal = PersonalDetails(
            fullName = "Halalisani Mbanjwa",
            headline = "Senior Android Engineer",
            phone = "+27 82 000 0000",
            email = "mbanjwa.hg@gmail.com",
        ),
        professionalProfile = "Engineer with a decade of shipping consumer apps on mobile, " +
            "most recently a résumé builder with a 140-template rendering engine.",
        competencies = listOf(
            "Kotlin", "Jetpack Compose", "Swift", "SwiftUI", "PDF rendering",
            "Typography", "Gradle", "CI/CD",
        ),
        experience = listOf(
            ExperienceEntry(
                role = "Senior Engineer", company = "ResumeStudio", period = "2023 — present",
                highlights = listOf(
                    "Built the template engine behind 140 résumé layouts.",
                    "Ported the PDF renderer to Android without a second source of truth.",
                ),
            ),
            ExperienceEntry(
                role = "Engineer", company = "Earlier Co", period = "2019 — 2023",
                highlights = listOf("Shipped a thing.", "Shipped another thing."),
            ),
        ),
        education = listOf(
            EducationEntry(
                qualification = "BSc Computer Science", institution = "University",
                period = "2015 — 2018", details = "Distinction",
            ),
        ),
        references = listOf(
            ReferenceEntry(name = "A Referee", company = "Somewhere", phone = "555", email = "r@e.com"),
        ),
        additionalSections = listOf(
            ResumeAdditionalSection(title = "Languages", items = listOf("English", "isiZulu")),
        ),
        accent = ResumeAccent.COBALT,
    )

    private fun assertOpensAsPdf(bytes: ByteArray, label: String): Int {
        assertTrue("$label: not a PDF header", bytes.decodeToString(0, 5) == "%PDF-")

        val file = File.createTempFile("render-$label", ".pdf", context.cacheDir)
        file.writeBytes(bytes)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { pdf ->
                assertTrue("$label: no pages", pdf.pageCount >= 1)
                // Opening each page is what actually parses its content stream.
                for (index in 0 until pdf.pageCount) {
                    pdf.openPage(index).use { page ->
                        assertTrue("$label: zero-size page", page.width > 0 && page.height > 0)
                    }
                }
                return pdf.pageCount
            }
        }
    }

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theDefaultSingleColumnTemplateRenders() {
        val pages = assertOpensAsPdf(renderer.render(sample.copy(template = ResumeTemplate.MODERN)), "modern")
        assertTrue("expected at least one page", pages >= 1)
    }

    @Test
    fun aTwoColumnTemplateRenders() {
        assertOpensAsPdf(renderer.render(sample.copy(template = ResumeTemplate.ATLAS)), "atlas")
    }

    @Test
    fun aDarkPaperTemplateRenders() {
        assertOpensAsPdf(renderer.render(sample.copy(template = ResumeTemplate.NOIR)), "noir")
    }

    @Test
    fun anEmptyDocumentStillProducesAPage() {
        // A résumé opened before anything is typed into it must still preview.
        assertEquals(1, assertOpensAsPdf(renderer.render(ResumeDocument()), "empty"))
    }

    @Test
    fun aPageIsTheSizeTheDocumentAsksFor() {
        val bytes = renderer.render(sample.copy(template = ResumeTemplate.MODERN))
        val file = File.createTempFile("size", ".pdf", context.cacheDir)
        file.writeBytes(bytes)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { pdf ->
                pdf.openPage(0).use { page ->
                    // A4 at 72dpi, the space the iOS renderer works in.
                    assertEquals(595, page.width)
                    assertEquals(842, page.height)
                }
            }
        }
    }

    @Test
    fun longContentPaginatesRatherThanRunningOffThePage() {
        val padded = sample.copy(
            experience = List(14) {
                ExperienceEntry(
                    role = "Role $it", company = "Company $it", period = "20$it",
                    highlights = List(4) { line -> "A reasonably long highlight line number $line." },
                )
            },
        )
        assertTrue("expected more than one page", assertOpensAsPdf(renderer.render(padded), "long") > 1)
    }

    @Test
    fun everyTemplateInTheCatalogueRenders() {
        // The catalogue is mirrored from iOS, so a template added there and
        // regenerated here must not be able to reach users unrendered.
        val failures = ResumeTemplate.entries.mapNotNull { template ->
            runCatching { assertOpensAsPdf(renderer.render(sample.copy(template = template)), template.wireName) }
                .exceptionOrNull()?.let { "${template.wireName}: ${it.message}" }
        }
        assertTrue("templates that failed to render: $failures", failures.isEmpty())
        assertEquals(140, TemplateCatalogue.templateCount)
    }
}
