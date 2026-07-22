package com.resumestudio.android

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.resumestudio.model.CoverLetterDocument
import com.resumestudio.model.ResumeDocument
import com.resumestudio.render.CoverLetterPdfRenderer
import com.resumestudio.render.ResumeDocxRenderer
import com.resumestudio.render.ResumePdfRenderer
import java.io.File

/**
 * Renders the résumé and hands it to whatever the user wants to send it with.
 *
 * The file is written into `cache/exports` and shared through a [FileProvider],
 * because a `file://` URI has not been allowed to cross an app boundary since
 * Android 7 — sharing one raises `FileUriExposedException` at the receiving end,
 * where it looks like the other app's fault.
 *
 * It keeps the name iOS gives it. The export usually becomes an email
 * attachment, and "Halalisani Mbanjwa Resume.pdf" is what the person on the
 * other end sees.
 */
object ResumeExporter {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    fun share(context: Context, document: ResumeDocument) = share(context, render(context, document))

    fun shareCoverLetter(context: Context, letter: CoverLetterDocument) {
        val directory = exportDirectory(context)
        val file = File(directory, "${letter.suggestedFilename}.pdf")
        file.writeBytes(CoverLetterPdfRenderer().render(letter))
        share(context, file)
    }

    private fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            file,
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "Share résumé").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }

    /**
     * Writes the PDF and returns it.
     *
     * Previous exports are cleared first. They are only ever handed out through
     * a content URI, so nothing is holding a path to them, and leaving a
     * directory of stale résumés in the cache is how an old draft ends up
     * attached to an application.
     */
    fun render(context: Context, document: ResumeDocument): File {
        val file = File(exportDirectory(context), "${document.suggestedFilename}.pdf")
        file.writeBytes(ResumePdfRenderer().render(document))
        return file
    }

    /**
     * Hands the PDF to Android's print stack.
     *
     * `PrintManager` takes a document adapter rather than a file, so the export
     * is written first and streamed from there — the same bytes that would be
     * shared, rather than a second rendering that could differ from it.
     */
    fun print(context: Context, document: ResumeDocument) {
        val file = render(context, document)
        val manager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        manager.print(
            document.suggestedFilename,
            object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    old: android.print.PrintAttributes?,
                    new: android.print.PrintAttributes?,
                    signal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback,
                    extras: android.os.Bundle?,
                ) {
                    if (signal?.isCanceled == true) {
                        callback.onLayoutCancelled()
                        return
                    }
                    callback.onLayoutFinished(
                        android.print.PrintDocumentInfo.Builder("${document.suggestedFilename}.pdf")
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .build(),
                        true,
                    )
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    signal: android.os.CancellationSignal?,
                    callback: WriteResultCallback,
                ) {
                    runCatching {
                        file.inputStream().use { input ->
                            java.io.FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }.fold(
                        onSuccess = { callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES)) },
                        onFailure = { callback.onWriteFailed(it.message) },
                    )
                }
            },
            null,
        )
    }

    /**
     * The résumé as plain text, for pasting into a form that wants it typed out.
     *
     * Built from the document rather than scraped back out of the PDF: the PDF
     * has columns and a reading order, and what a form wants is the content.
     */
    fun plainText(document: ResumeDocument): String = buildString {
        appendLine(document.personal.fullName)
        if (document.personal.headline.isNotBlank()) appendLine(document.personal.headline)
        listOf(document.personal.email, document.personal.phone)
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let { appendLine(it.joinToString(" · ")) }

        if (document.professionalProfile.isNotBlank()) {
            appendLine()
            appendLine("PROFESSIONAL PROFILE")
            appendLine(document.professionalProfile)
        }
        if (document.competencies.any { it.isNotBlank() }) {
            appendLine()
            appendLine("CORE COMPETENCIES")
            document.competencies.filter { it.isNotBlank() }.forEach { appendLine("• $it") }
        }
        if (document.experience.isNotEmpty()) {
            appendLine()
            appendLine("PROFESSIONAL EXPERIENCE")
            document.experience.forEach { entry ->
                appendLine()
                appendLine(listOf(entry.role, entry.company, entry.period).filter { it.isNotBlank() }.joinToString(" · "))
                entry.highlights.filter { it.isNotBlank() }.forEach { appendLine("• $it") }
            }
        }
        if (document.education.isNotEmpty()) {
            appendLine()
            appendLine("EDUCATION")
            document.education.forEach {
                appendLine(listOf(it.qualification, it.institution, it.period).filter { s -> s.isNotBlank() }.joinToString(" · "))
                if (it.details.isNotBlank()) appendLine(it.details)
            }
        }
        if (document.references.isNotEmpty()) {
            appendLine()
            appendLine("REFERENCES")
            document.references.forEach {
                appendLine(listOf(it.name, it.company, it.email, it.phone).filter { s -> s.isNotBlank() }.joinToString(" · "))
            }
        }
    }.trim()

    /** The résumé as an editable Word document, saved beside the PDF. */
    fun renderDocx(context: Context, document: ResumeDocument): File {
        val file = File(exportDirectory(context), "${document.suggestedFilename}.docx")
        file.writeBytes(ResumeDocxRenderer.render(document))
        return file
    }

    fun shareDocx(context: Context, document: ResumeDocument) =
        share(context, renderDocx(context, document))

    private fun exportDirectory(context: Context): File =
        File(context.cacheDir, "exports").apply {
            deleteRecursively()
            mkdirs()
        }
}
