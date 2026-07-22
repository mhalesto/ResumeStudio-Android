package com.resumestudio.render

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.resumestudio.model.ResumeDocument
import java.io.File

/**
 * Turns a rendered résumé into page images, mirroring `ResumePageRasterizer.swift`.
 *
 * Deliberately goes through the real PDF rather than drawing to a bitmap
 * directly. Rasterising the exported file means the preview is the artefact the
 * user will actually send, so anything the PDF writer mangles is visible on
 * screen instead of only being discovered by whoever opens the attachment.
 *
 * Rendering is not cheap and not main-thread work; call it from a background
 * dispatcher.
 */
class ResumePageRasterizer(
    private val cacheDir: File,
    private val renderer: ResumePdfRenderer = ResumePdfRenderer(),
) {

    /** A page as a bitmap [widthPx] wide, keeping the page's aspect ratio. */
    fun rasterize(document: ResumeDocument, pageIndex: Int = 0, widthPx: Int): Page? =
        withPdf(document) { pdf ->
            if (pageIndex !in 0 until pdf.pageCount) return@withPdf null
            pdf.openPage(pageIndex).use { page ->
                val scale = widthPx.toFloat() / page.width
                val bitmap = Bitmap.createBitmap(
                    widthPx,
                    (page.height * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                ).apply {
                    // The renderer only paints the paper for dark templates, so a
                    // pale page would otherwise composite onto transparency.
                    eraseColor(Color.WHITE)
                }
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                Page(bitmap, pageIndex, pdf.pageCount)
            }
        }

    fun pageCount(document: ResumeDocument): Int = withPdf(document) { it.pageCount } ?: 0

    data class Page(val bitmap: Bitmap, val index: Int, val pageCount: Int)

    private fun <T> withPdf(document: ResumeDocument, body: (PdfRenderer) -> T): T? {
        val file = File.createTempFile("preview", ".pdf", cacheDir)
        return try {
            file.writeBytes(renderer.render(document))
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use(body)
            }
        } catch (error: Exception) {
            // A template that will not render costs the user a preview; throwing
            // here would cost them the gallery.
            null
        } finally {
            file.delete()
        }
    }
}
