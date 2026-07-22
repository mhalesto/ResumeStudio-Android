package com.resumestudio.android

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.resumestudio.model.CoverLetterDocument
import com.resumestudio.model.ResumeDocument
import com.resumestudio.render.CoverLetterPdfRenderer
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

    private fun exportDirectory(context: Context): File =
        File(context.cacheDir, "exports").apply {
            deleteRecursively()
            mkdirs()
        }
}
