package com.resumestudio.render

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.resumestudio.model.CoverLetterDocument
import java.io.File

/**
 * The letter's first page as a bitmap.
 *
 * Rasterises the real PDF rather than drawing to a canvas twice, for the same
 * reason [ResumePageRasterizer] does: the preview is the artefact that gets
 * sent, so anything the writer mangles is visible here rather than in someone
 * else's inbox.
 */
class CoverLetterPageRasterizer(
    private val cacheDir: File,
    private val renderer: CoverLetterPdfRenderer = CoverLetterPdfRenderer(),
) {
    fun rasterize(document: CoverLetterDocument, widthPx: Int): Bitmap? {
        val file = File.createTempFile("letter", ".pdf", cacheDir)
        return try {
            file.writeBytes(renderer.render(document))
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { pdf ->
                    if (pdf.pageCount == 0) return null
                    pdf.openPage(0).use { page ->
                        val scale = widthPx.toFloat() / page.width
                        val bitmap = Bitmap.createBitmap(
                            widthPx,
                            (page.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888,
                        ).apply { eraseColor(Color.WHITE) }
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (error: Exception) {
            null
        } finally {
            file.delete()
        }
    }
}
