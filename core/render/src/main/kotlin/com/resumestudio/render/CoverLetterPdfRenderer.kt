package com.resumestudio.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.resumestudio.model.CoverLetterDocument
import com.resumestudio.model.CoverLetterTemplate
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Draws a [CoverLetterDocument] to PDF, porting `CoverLetterPDFRenderer.swift`.
 *
 * The iOS renderer varies almost entirely in the **letterhead** — there are
 * dozens of `drawXHeader` functions over one shared body — so this follows the
 * same shape: [HeaderFamily] is the axis, and every template maps onto one.
 * The body below the letterhead is identical for all 65, which is why a letter
 * still reads as a letter whichever template is chosen.
 *
 * Not every iOS letterhead is drawn yet. Templates without their own family map
 * to the closest one that is, which is stated in [family] rather than hidden —
 * a letter that renders in the wrong dress is recoverable, one that renders
 * blank is not.
 */
class CoverLetterPdfRenderer {

    fun render(document: CoverLetterDocument): ByteArray =
        ByteArrayOutputStream().also { render(document, it) }.toByteArray()

    fun render(document: CoverLetterDocument, out: OutputStream) {
        val pdf = PdfDocument()
        Session(pdf, document).use { it.draw() }
        pdf.writeTo(out)
        pdf.close()
    }

    /** The shapes a letterhead can take. Each iOS header maps onto one. */
    enum class HeaderFamily {
        /** A name, a headline, and an accent rule beneath. */
        RULE,

        /** A full-bleed band of accent behind light type. */
        BAND,

        /** A dark slab, for the letters that lead with weight. */
        DARK,

        /** Centred name over a hairline, the way a formal letter is set. */
        CENTRED,

        /** The initials in a disc beside the name. */
        MONOGRAM,

        /** A narrow accent column down the leading edge. */
        SIDEBAR,

        /** No furniture: the name, then the letter. */
        PLAIN,
    }

    private class Session(
        private val pdf: PdfDocument,
        private val document: CoverLetterDocument,
    ) : AutoCloseable {

        // A4 in points, the space the iOS renderer works in.
        private val pageWidth = 595.276f
        private val pageHeight = 841.89f
        private val margin = 56f

        private val accent = document.accent.argb
        private val ink = android.graphics.Color.parseColor("#1A1A1A")
        private val mutedInk = android.graphics.Color.parseColor("#5A5A5A")

        private var page: PdfDocument.Page? = null
        private var canvas = Canvas()
        private var pageNumber = 0
        private var cursor = margin

        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }

        private val contentBottom get() = pageHeight - margin
        private val bodyRect get() = RectF(margin + bodyInset, margin, pageWidth - margin, contentBottom)

        /** The sidebar family holds the body clear of its column. */
        private val bodyInset: Float
            get() = if (family() == HeaderFamily.SIDEBAR) 46f else 0f

        init {
            newPage()
        }

        fun draw() {
            drawHeader()
            drawDate()
            drawRecipient()
            drawSubject()
            drawGreeting()
            drawBody()
            drawClosing()
        }

        // --- frame ------------------------------------------------------

        private fun newPage() {
            page?.let { pdf.finishPage(it) }
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(
                Math.round(pageWidth), Math.round(pageHeight), pageNumber,
            ).create()
            page = pdf.startPage(info)
            canvas = page!!.canvas
            cursor = margin

            // The sidebar bleeds down every page, not only the first.
            if (family() == HeaderFamily.SIDEBAR) {
                fill.color = accent
                canvas.drawRect(0f, 0f, 26f, pageHeight, fill)
            }
        }

        override fun close() {
            page?.let { pdf.finishPage(it) }
            page = null
        }

        private fun paint(size: Float, bold: Boolean, color: Int, letterSpacing: Float = 0f) =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = if (bold) {
                    android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
                } else {
                    android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                }
                textSize = size
                this.color = color
                this.letterSpacing = letterSpacing
            }

        /**
         * Draws text at the cursor, breaking to a new page when it will not fit.
         *
         * A paragraph is never split across the break: a cover letter is short
         * enough that an orphaned line reads as a mistake rather than as
         * pagination.
         */
        private fun drawText(
            text: String,
            paint: TextPaint,
            rect: RectF = bodyRect,
            topGap: Float = 0f,
        ) {
            if (text.isBlank()) return
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, rect.width().toInt().coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1.06f)
                .setIncludePad(false)
                .setEllipsize(null)
                .build()

            if (cursor + topGap + layout.height > contentBottom) newPage()

            canvas.save()
            canvas.translate(rect.left, cursor + topGap)
            layout.draw(canvas)
            canvas.restore()
            cursor += topGap + layout.height
        }

        // --- letterhead -------------------------------------------------

        private fun family(): HeaderFamily = document.template.family()

        private fun drawHeader() {
            when (family()) {
                HeaderFamily.BAND -> {
                    fill.color = accent
                    canvas.drawRect(0f, 0f, pageWidth, 118f, fill)
                    cursor = 34f
                    drawText(document.senderName, paint(21f, true, android.graphics.Color.WHITE))
                    drawText(
                        document.senderHeadline,
                        paint(10.5f, false, android.graphics.Color.argb(215, 255, 255, 255)),
                        topGap = 2f,
                    )
                    cursor = 118f + 26f
                    drawContactLine(mutedInk)
                }

                HeaderFamily.DARK -> {
                    fill.color = android.graphics.Color.parseColor("#14161A")
                    canvas.drawRect(0f, 0f, pageWidth, 132f, fill)
                    cursor = 38f
                    drawText(document.senderName, paint(22f, true, android.graphics.Color.parseColor("#F2F2F0")))
                    drawText(document.senderHeadline, paint(10.5f, false, accent), topGap = 3f)
                    cursor = 132f + 26f
                    drawContactLine(mutedInk)
                }

                HeaderFamily.CENTRED -> {
                    val centre = paint(20f, true, ink).apply { textAlign = Paint.Align.CENTER }
                    canvas.drawText(document.senderName, pageWidth / 2f, cursor + 20f, centre)
                    cursor += 30f
                    val sub = paint(10f, false, mutedInk).apply { textAlign = Paint.Align.CENTER }
                    canvas.drawText(document.senderHeadline, pageWidth / 2f, cursor + 10f, sub)
                    cursor += 18f
                    val contact = contactLine()
                    if (contact.isNotBlank()) {
                        canvas.drawText(contact, pageWidth / 2f, cursor + 10f, sub)
                        cursor += 16f
                    }
                    stroke.color = accent
                    canvas.drawLine(margin, cursor + 8f, pageWidth - margin, cursor + 8f, stroke)
                    cursor += 26f
                }

                HeaderFamily.MONOGRAM -> {
                    val discRadius = 21f
                    fill.color = accent
                    canvas.drawCircle(margin + discRadius, cursor + discRadius, discRadius, fill)
                    val initials = paint(15f, true, android.graphics.Color.WHITE).apply {
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(
                        document.initials.ifBlank { "•" },
                        margin + discRadius,
                        cursor + discRadius + 5.5f,
                        initials,
                    )
                    val textLeft = margin + discRadius * 2 + 14f
                    val block = RectF(textLeft, cursor, pageWidth - margin, contentBottom)
                    val saved = cursor
                    drawText(document.senderName, paint(18f, true, ink), block)
                    drawText(document.senderHeadline, paint(10.5f, false, mutedInk), block, topGap = 1f)
                    cursor = maxOf(cursor, saved + discRadius * 2) + 18f
                    drawContactLine(mutedInk)
                }

                HeaderFamily.SIDEBAR -> {
                    drawText(document.senderName, paint(21f, true, ink))
                    drawText(document.senderHeadline, paint(10.5f, false, accent), topGap = 2f)
                    cursor += 8f
                    drawContactLine(mutedInk)
                }

                HeaderFamily.PLAIN -> {
                    drawText(document.senderName, paint(19f, true, ink))
                    drawText(document.senderHeadline, paint(10.5f, false, mutedInk), topGap = 2f)
                    cursor += 6f
                    drawContactLine(mutedInk)
                }

                HeaderFamily.RULE -> {
                    drawText(document.senderName, paint(21f, true, ink))
                    drawText(document.senderHeadline, paint(10.5f, false, accent), topGap = 2f)
                    cursor += 6f
                    drawContactLine(mutedInk)
                    stroke.color = accent
                    stroke.strokeWidth = 1.4f
                    canvas.drawLine(bodyRect.left, cursor + 10f, bodyRect.right, cursor + 10f, stroke)
                    stroke.strokeWidth = 0.7f
                    cursor += 26f
                }
            }
        }

        private fun contactLine(): String = listOf(
            document.senderPhone, document.senderEmail,
        ).filter { it.isNotBlank() }.joinToString("   ·   ")

        private fun drawContactLine(color: Int) {
            drawText(contactLine(), paint(9.5f, false, color), topGap = 4f)
        }

        // --- body -------------------------------------------------------

        private fun drawDate() {
            val formatted = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                .format(Date((document.date * 1000).toLong()))
            drawText(formatted, paint(9.5f, false, mutedInk), topGap = 20f)
        }

        private fun drawRecipient() {
            val block = listOf(
                document.recipientName,
                document.recipientTitle,
                document.companyName,
                document.companyAddress,
            ).filter { it.isNotBlank() }
            if (block.isEmpty()) return
            drawText(block.joinToString("\n"), paint(10f, false, ink), topGap = 18f)
        }

        private fun drawSubject() {
            val subject = document.subject.ifBlank {
                document.jobTitle.takeIf { it.isNotBlank() }?.let { "Application for $it" }.orEmpty()
            }
            if (subject.isBlank()) return
            drawText(subject, paint(10.5f, true, ink), topGap = 20f)
        }

        private fun drawGreeting() {
            drawText(document.greeting, paint(10.5f, false, ink), topGap = 18f)
        }

        private fun drawBody() {
            document.bodyParagraphs
                .filter { it.isNotBlank() }
                .forEach { drawText(it, paint(10.5f, false, ink), topGap = 12f) }
        }

        private fun drawClosing() {
            drawText(document.closing, paint(10.5f, false, ink), topGap = 20f)
            drawText(document.senderName, paint(10.5f, true, ink), topGap = 22f)
        }
    }
}

/**
 * Which letterhead a template wears.
 *
 * iOS draws each of these individually; the families below are the shapes those
 * drawings fall into. A template not yet given its own treatment takes the
 * closest family rather than nothing.
 */
fun CoverLetterTemplate.family(): CoverLetterPdfRenderer.HeaderFamily = when (this) {
    CoverLetterTemplate.GRADIENT, CoverLetterTemplate.MARQUEE, CoverLetterTemplate.NOVA,
    CoverLetterTemplate.AURELIA, CoverLetterTemplate.VANTAGE,
        -> CoverLetterPdfRenderer.HeaderFamily.BAND

    CoverLetterTemplate.NOIR, CoverLetterTemplate.NOCTURNE, CoverLetterTemplate.ECLIPSE,
        -> CoverLetterPdfRenderer.HeaderFamily.DARK

    CoverLetterTemplate.CLASSIC, CoverLetterTemplate.EXECUTIVE, CoverLetterTemplate.LETTERPRESS,
    CoverLetterTemplate.BROADSHEET, CoverLetterTemplate.LAUREATE, CoverLetterTemplate.IVY,
    CoverLetterTemplate.CREST,
        -> CoverLetterPdfRenderer.HeaderFamily.CENTRED

    CoverLetterTemplate.MONOGRAM, CoverLetterTemplate.SIGNATURE, CoverLetterTemplate.ICONIC,
        -> CoverLetterPdfRenderer.HeaderFamily.MONOGRAM

    CoverLetterTemplate.SIDEBAR, CoverLetterTemplate.RAIL, CoverLetterTemplate.PLINTH,
        -> CoverLetterPdfRenderer.HeaderFamily.SIDEBAR

    CoverLetterTemplate.MINIMAL, CoverLetterTemplate.MEMO, CoverLetterTemplate.STOCKHOLM,
        -> CoverLetterPdfRenderer.HeaderFamily.PLAIN

    else -> CoverLetterPdfRenderer.HeaderFamily.RULE
}
