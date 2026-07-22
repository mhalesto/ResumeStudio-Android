package com.resumestudio.render

import android.graphics.Bitmap
import android.graphics.Color
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
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Dumps rendered pages as PNGs so a human can look at them.
 *
 * Not an assertion of anything — it exists because "the PDF parses" and "the
 * page looks right" are different claims, and until the golden-image suite
 * lands the second one is only answerable by eye. Run it, then:
 *
 *     adb pull /sdcard/Android/data/com.resumestudio.render.test/files/renders
 *
 * Rasterising through [PdfRenderer] rather than exporting the canvas directly
 * is deliberate: it round-trips the real PDF, so anything the writer mangles
 * shows up here instead of being masked by re-drawing from the source.
 */
@RunWith(AndroidJUnit4::class)
class RenderSampleDump {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val sample = ResumeDocument(
        personal = PersonalDetails(
            fullName = "Halalisani Mbanjwa",
            headline = "Senior Android Engineer",
            phone = "+27 82 000 0000",
            email = "mbanjwa.hg@gmail.com",
        ),
        professionalProfile = "Engineer with a decade of shipping consumer apps on mobile, " +
            "most recently a résumé builder whose rendering engine drives 140 templates " +
            "from a single shared layout vocabulary.",
        competencies = listOf(
            "Kotlin", "Jetpack Compose", "Swift", "SwiftUI",
            "PDF rendering", "Typography", "Gradle", "CI/CD",
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
                role = "Android Engineer", company = "Earlier Co", period = "2019 — 2023",
                highlights = listOf(
                    "Led the migration to Compose across a 40-screen app.",
                    "Cut cold start by 38% with a lazy dependency graph.",
                ),
            ),
        ),
        education = listOf(
            EducationEntry(
                qualification = "BSc Computer Science", institution = "University of Cape Town",
                period = "2015 — 2018", details = "Distinction",
            ),
        ),
        references = listOf(
            ReferenceEntry(name = "A Referee", company = "Somewhere Ltd", phone = "+27 11 000 0000", email = "referee@example.com"),
        ),
        additionalSections = listOf(
            ResumeAdditionalSection(title = "Languages", items = listOf("English", "isiZulu", "Afrikaans")),
        ),
        accent = ResumeAccent.COBALT,
    )

    @Test
    fun dumpRepresentativeTemplates() {
        val outDir = File(context.getExternalFilesDir(null), "renders").apply {
            deleteRecursively()
            mkdirs()
        }

        // One of each shape the plan vocabulary can take, so a regression in any
        // of them is visible in a single glance across four files.
        val subjects = listOf(
            ResumeTemplate.MODERN to "single column, the archetype",
            ResumeTemplate.ATLAS to "186pt dark leading column",
            ResumeTemplate.NOIR to "dark paper",
            ResumeTemplate.GAUGE to "trailing dark column with meters",
        )

        subjects.forEach { (template, _) ->
            val bytes = ResumePdfRenderer().render(sample.copy(template = template))
            val pdfFile = File(outDir, "${template.wireName}.pdf").apply { writeBytes(bytes) }

            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { pdf ->
                    pdf.openPage(0).use { page ->
                        // 2x for legibility when scaled down for review.
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888,
                        ).apply { eraseColor(Color.WHITE) }
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        File(outDir, "${template.wireName}.png").outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                    }
                }
            }
        }

        println("renders written to $outDir")
    }
}
