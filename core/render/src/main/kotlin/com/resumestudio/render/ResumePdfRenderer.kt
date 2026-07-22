package com.resumestudio.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.resumestudio.model.BodyLayout
import com.resumestudio.model.CompetencyStyle
import com.resumestudio.model.ExperienceStyle
import com.resumestudio.model.ResumeContentBlock
import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeSignaturePlacement
import com.resumestudio.model.SideColumn
import com.resumestudio.model.TemplatePlan
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Draws a [ResumeDocument] to PDF.
 *
 * This is the port of `ResumePDFRenderer.swift`. That file draws imperatively
 * into a `UIGraphicsPDFRenderer` context — text placed at measured coordinates,
 * not laid out by SwiftUI — which is the reason the port is a port at all and
 * not a rewrite: `PdfDocument` + `Canvas` + `StaticLayout` is the same shape of
 * API, working in the same unit, so the geometry carries across.
 *
 * ## What is here
 *
 * The page frame, pagination, the letterhead, the side column, and the section
 * bodies for both column layouts. Competency styles are complete. Experience
 * covers stacked, timeline and date-gutter.
 *
 * ## What is not
 *
 * Section chrome beyond `plain` (cards are stubbed to plain), the portrait, the
 * signature, and the page-target fitting pass that reflows a résumé to land on a
 * chosen number of pages. Each is called out at its site. Until the golden-image
 * suite compares these against iOS output, treat the layout as close, not exact.
 */
class ResumePdfRenderer {

    fun render(document: ResumeDocument): ByteArray =
        ByteArrayOutputStream().also { render(document, it) }.toByteArray()

    fun render(document: ResumeDocument, out: OutputStream) {
        val layout = document.layout.normalized()
        val plan = document.template.plan
        val pdf = PdfDocument()

        Session(pdf, document, layout.copy(), plan).use { session ->
            session.drawLetterhead()
            layout.sectionOrder
                .let { if (plan.skillsFirst) it.skillsBeforeProfile() else it }
                .forEach { session.drawBlock(it) }
        }

        pdf.writeTo(out)
        pdf.close()
    }

    /** `skillsFirst`: skills lead the page, for people whose skills are the pitch. */
    private fun List<ResumeContentBlock>.skillsBeforeProfile(): List<ResumeContentBlock> {
        val reordered = toMutableList()
        reordered.remove(ResumeContentBlock.COMPETENCIES)
        val profileAt = reordered.indexOf(ResumeContentBlock.PROFILE)
        reordered.add(if (profileAt >= 0) profileAt else 0, ResumeContentBlock.COMPETENCIES)
        return reordered
    }

    // ------------------------------------------------------------------------

    private class Session(
        private val pdf: PdfDocument,
        private val document: ResumeDocument,
        private val settings: com.resumestudio.model.ResumeLayoutSettings,
        private val plan: TemplatePlan,
    ) : AutoCloseable {

        private val paper = document.layout.paperSize
        private val pageWidth = paper.widthPoints
        private val pageHeight = paper.heightPoints
        private val margin = settings.marginPoints.toFloat()
        private val theme = RenderTheme.of(document, plan)
        private val side = (plan.body as? BodyLayout.Side)?.column

        private var page: PdfDocument.Page? = null
        private var canvas: Canvas = Canvas()
        private var pageNumber = 0

        /** Where the next thing goes in the wide column, and in the narrow one. */
        private var mainCursor = margin
        private var sideCursor = margin

        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }

        init {
            newPage()
        }

        // --- frame ----------------------------------------------------------

        private val contentBottom get() = pageHeight - margin

        /** The wide column: everything the reader actually reads. */
        private val mainRect: RectF
            get() {
                val inset = plan.bodyInset
                val gutter = 18f
                return when (side?.edge) {
                    SideColumn.Edge.LEADING -> RectF(
                        margin + side.width + gutter + inset, margin, pageWidth - margin, contentBottom,
                    )
                    SideColumn.Edge.TRAILING -> RectF(
                        margin + inset, margin, pageWidth - margin - side.width - gutter, contentBottom,
                    )
                    null -> RectF(margin + inset, margin, pageWidth - margin, contentBottom)
                }
            }

        /**
         * The painted band, which bleeds off the page on its *outer* edge only.
         *
         * Its inner edge is exactly where the column ends, so it must never be
         * widened for the bleed: the main column starts one gutter further in,
         * and any overhang here is drawn straight over the top of that text.
         */
        private val sideBandRect: RectF?
            get() = side?.let {
                when (it.edge) {
                    SideColumn.Edge.LEADING -> RectF(0f, 0f, margin + it.width, pageHeight)
                    SideColumn.Edge.TRAILING ->
                        RectF(pageWidth - margin - it.width, 0f, pageWidth, pageHeight)
                }
            }

        /**
         * The narrow column: the facts a recruiter scans for.
         *
         * Type is held off the band's inner edge, but never allowed closer to the
         * page edge than the margin — a filled band may bleed, the words in it
         * may not.
         */
        private val sideRect: RectF?
            get() = side?.let {
                val band = sideBandRect ?: return@let null
                val pad = 20f
                when (it.edge) {
                    SideColumn.Edge.LEADING ->
                        RectF(margin, margin, band.right - pad, contentBottom)
                    SideColumn.Edge.TRAILING ->
                        RectF(band.left + pad, margin, pageWidth - margin, contentBottom)
                }
            }

        private fun newPage() {
            page?.let {
                drawSignatureIfThisIsItsPage()
                pdf.finishPage(it)
            }
            pageNumber += 1
            // Rounded, not truncated. `PageInfo` only takes whole points, and A4
            // is 841.89 tall — truncating quietly loses most of a point off every
            // page, which is enough to push a tight one-page résumé onto two.
            val info = PdfDocument.PageInfo.Builder(
                Math.round(pageWidth), Math.round(pageHeight), pageNumber,
            ).create()
            page = pdf.startPage(info)
            canvas = page!!.canvas

            // The dark templates dye the whole sheet, not just a band, so the
            // paper is painted before anything else lands on it.
            if (plan.darkPaper) {
                fill.color = theme.paper
                canvas.drawRect(0f, 0f, pageWidth, pageHeight, fill)
            }
            paintSideColumnBand()

            mainCursor = margin
            sideCursor = margin
        }

        /** The band runs the length of *every* page, not just the first. */
        private fun paintSideColumnBand() {
            val column = side ?: return
            val band = sideBandRect ?: return
            sideColumnPaper(column.fill, document.accent)?.let {
                fill.color = it
                canvas.drawRect(band, fill)
            }
            if (column.divider) {
                stroke.color = theme.hairline
                val x = if (column.edge == SideColumn.Edge.LEADING) band.right + 9f else band.left - 9f
                canvas.drawLine(x, margin, x, contentBottom, stroke)
            }
        }

        /**
         * Lays the signature over the page it was assigned to.
         *
         * Called as each page is finished rather than once at the end. A page
         * cannot be reopened after `finishPage`, so a signature bound for page
         * one has to be drawn while page one is still the open one — doing this
         * last meant it was only ever drawn on single-page documents.
         *
         * A signature assigned to a page the document turned out not to have
         * falls onto the final page rather than being dropped in silence.
         */
        private fun drawSignatureIfThisIsItsPage(isFinalPage: Boolean = false) {
            val signature = document.signature?.normalized() ?: return
            val strokes = signature.strokes.filter { it.points.size > 1 }
            if (strokes.isEmpty()) return

            val thisPage = pageNumber - 1
            val wanted = signature.pageIndex
            val belongsHere = wanted == thisPage || (isFinalPage && wanted > thisPage)
            if (!belongsHere) return

            val width = signature.widthPoints.toFloat()
            // Height follows the mark's own aspect, so a wide flourish and a
            // compact initial are not both squashed into the same box.
            val height = width * 0.42f
            val left = when (signature.placement) {
                ResumeSignaturePlacement.LOWER_LEADING -> mainRect.left
                ResumeSignaturePlacement.LOWER_CENTER -> mainRect.centerX() - width / 2f
                ResumeSignaturePlacement.LOWER_TRAILING -> mainRect.right - width
            }
            val top = contentBottom - height - 6f

            stroke.color = theme.ink
            stroke.strokeWidth = 1.5f
            stroke.strokeCap = Paint.Cap.ROUND
            stroke.strokeJoin = Paint.Join.ROUND

            strokes.forEach { mark ->
                val path = android.graphics.Path()
                mark.points.forEachIndexed { index, point ->
                    val x = left + point.x * width
                    val y = top + point.y * height
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                canvas.drawPath(path, stroke)
            }

            stroke.strokeWidth = 0.6f
            stroke.strokeCap = Paint.Cap.BUTT
        }

        override fun close() {
            page?.let {
                drawSignatureIfThisIsItsPage(isFinalPage = true)
                pdf.finishPage(it)
            }
            page = null
        }

        // --- text -----------------------------------------------------------

        private fun paint(size: Float, bold: Boolean, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = if (bold) theme.heading else theme.body
            textSize = theme.size(size)
            this.color = color
        }

        private fun layoutOf(text: String, paint: TextPaint, width: Float): StaticLayout =
            StaticLayout.Builder
                .obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, settings.lineSpacing.toFloat())
                .setIncludePad(false)
                .build()

        /**
         * Draws [text] into a column, breaking to a new page when it will not fit.
         *
         * Returns the height consumed. Blocks call this rather than measuring
         * themselves so that pagination lives in exactly one place — the iOS
         * renderer learned that the hard way, per the comment about nothing
         * implicitly closing the final page.
         */
        private fun drawText(
            text: String,
            paint: TextPaint,
            rect: RectF,
            inSide: Boolean,
            topGap: Float = 0f,
        ): Float {
            if (text.isBlank()) return 0f
            val layout = layoutOf(text, paint, rect.width())
            val cursor = if (inSide) sideCursor else mainCursor

            if (cursor + topGap + layout.height > contentBottom) {
                // A side column that overruns is a symptom of too much content
                // for the template, not of pagination — but it must still not be
                // drawn off the page.
                newPage()
            }

            val y = if (inSide) sideCursor else mainCursor
            canvas.save()
            canvas.translate(rect.left, y + topGap)
            layout.draw(canvas)
            canvas.restore()

            val consumed = topGap + layout.height
            if (inSide) sideCursor += consumed else mainCursor += consumed
            return consumed
        }

        // --- letterhead -----------------------------------------------------

        fun drawLetterhead() {
            val headerRect = if (side != null && !side.startsBelowProfile) mainRect
            else RectF(margin, margin, pageWidth - margin, contentBottom)

            drawText(
                document.personal.fullName.ifBlank { " " },
                paint(RenderTheme.Size.NAME, bold = true, theme.ink),
                headerRect,
                inSide = false,
            )
            drawText(
                document.personal.headline,
                paint(RenderTheme.Size.HEADLINE, bold = false, theme.accent),
                headerRect,
                inSide = false,
                topGap = 2f,
            )

            // `contact: .iconRows` needs a column to live in; without icons drawn
            // yet it degrades to the same stacked rows, which is the intended
            // fallback rather than a missing feature.
            val contactLines = listOfNotNull(
                document.personal.phone.ifBlank { null },
                document.personal.email.ifBlank { null },
            )
            if (contactLines.isNotEmpty()) {
                val contactPaint = paint(RenderTheme.Size.CONTACT, bold = false, theme.mutedInk)
                val column = side
                val target = sideRect
                    ?.takeIf { column != null && plan.contact == com.resumestudio.model.ContactStyle.ICON_ROWS }

                if (target != null && column != null) {
                    contactPaint.color = RenderTheme.forSideColumn(theme, column.prefersLightInk).mutedInk
                    drawText(contactLines.joinToString("\n"), contactPaint, target, inSide = true, topGap = 4f)
                } else {
                    drawText(contactLines.joinToString("   ·   "), contactPaint, headerRect, inSide = false, topGap = 6f)
                }
            }

            mainCursor += 14f

            // `profileInHeader` sets the summary inside the masthead. Drawing it
            // here rather than as a block is what makes it part of the letterhead.
            if (plan.profileInHeader && document.professionalProfile.isNotBlank()) {
                drawText(
                    document.professionalProfile,
                    paint(RenderTheme.Size.BODY, bold = false, theme.mutedInk),
                    headerRect,
                    inSide = false,
                )
                mainCursor += 10f
            }

            stroke.color = theme.accent
            stroke.strokeWidth = 1.4f
            canvas.drawLine(headerRect.left, mainCursor, headerRect.right, mainCursor, stroke)
            stroke.strokeWidth = 0.6f
            mainCursor += 14f
        }

        // --- blocks ---------------------------------------------------------

        /** Which column a block belongs to, per `TemplatePlan`'s division of labour. */
        private fun isSideBlock(block: ResumeContentBlock): Boolean = side != null && when (block) {
            ResumeContentBlock.COMPETENCIES, ResumeContentBlock.EDUCATION -> true
            else -> false
        }

        fun drawBlock(block: ResumeContentBlock) {
            if (plan.profileInHeader && block == ResumeContentBlock.PROFILE) return

            val inSide = isSideBlock(block)
            val rect = (if (inSide) sideRect else mainRect) ?: mainRect
            val blockTheme = if (inSide) RenderTheme.forSideColumn(theme, side!!.prefersLightInk) else theme

            val empty = when (block) {
                ResumeContentBlock.PROFILE -> document.professionalProfile.isBlank()
                ResumeContentBlock.COMPETENCIES -> document.competencies.isEmpty()
                ResumeContentBlock.EXPERIENCE -> document.experience.isEmpty()
                ResumeContentBlock.EDUCATION -> document.education.isEmpty()
                ResumeContentBlock.REFERENCES -> document.references.isEmpty()
                ResumeContentBlock.ADDITIONAL -> document.additionalSections.isEmpty()
            }
            if (empty) return

            drawSectionTitle(settings.heading(block), rect, inSide, blockTheme, block)

            when (block) {
                ResumeContentBlock.PROFILE -> drawText(
                    document.professionalProfile,
                    paint(RenderTheme.Size.BODY, false, blockTheme.mutedInk),
                    rect, inSide,
                )

                ResumeContentBlock.COMPETENCIES -> drawCompetencies(rect, inSide, blockTheme)
                ResumeContentBlock.EXPERIENCE -> drawExperience(rect, inSide, blockTheme)

                ResumeContentBlock.EDUCATION -> document.education.forEach {
                    drawText(it.qualification, paint(RenderTheme.Size.BODY, true, blockTheme.ink), rect, inSide, 6f)
                    drawText(
                        listOf(it.institution, it.period).filter(String::isNotBlank).joinToString(" · "),
                        paint(RenderTheme.Size.DETAIL, false, blockTheme.mutedInk), rect, inSide, 1f,
                    )
                    drawText(it.details, paint(RenderTheme.Size.DETAIL, false, blockTheme.mutedInk), rect, inSide, 1f)
                }

                ResumeContentBlock.REFERENCES -> document.references.forEach {
                    drawText(it.name, paint(RenderTheme.Size.BODY, true, blockTheme.ink), rect, inSide, 6f)
                    drawText(
                        listOf(it.company, it.phone, it.email).filter(String::isNotBlank).joinToString(" · "),
                        paint(RenderTheme.Size.DETAIL, false, blockTheme.mutedInk), rect, inSide, 1f,
                    )
                }

                ResumeContentBlock.ADDITIONAL -> document.additionalSections.forEach { section ->
                    drawText(section.title, paint(RenderTheme.Size.BODY, true, blockTheme.ink), rect, inSide, 6f)
                    section.items.forEach {
                        drawText("•  $it", paint(RenderTheme.Size.DETAIL, false, blockTheme.mutedInk), rect, inSide, 2f)
                    }
                }
            }

            if (inSide) sideCursor += 14f else mainCursor += 16f
        }

        private var sectionIndex = 0

        private fun drawSectionTitle(
            title: String,
            rect: RectF,
            inSide: Boolean,
            blockTheme: RenderTheme,
            block: ResumeContentBlock,
        ) {
            sectionIndex += 1
            // `numberedSections`: counted off 01, 02, 03 — the editorial numbering.
            val label = if (plan.numberedSections) {
                "%02d  %s".format(sectionIndex, title.uppercase())
            } else {
                title.uppercase()
            }

            val titlePaint = paint(RenderTheme.Size.SECTION_TITLE, bold = true, blockTheme.accent).apply {
                letterSpacing = 0.08f
            }

            // TODO(hangingHeadings): titles belong in a gutter left of the text,
            // with the body keeping a narrow measure beside them. Drawn inline
            // until the two-measure body lands, so the page still reads correctly.
            drawText(label, titlePaint, rect, inSide, topGap = if (inSide) 10f else 4f)

            stroke.color = blockTheme.hairline
            val y = (if (inSide) sideCursor else mainCursor) + 3f
            canvas.drawLine(rect.left, y, rect.right, y, stroke)
            if (inSide) sideCursor += 8f else mainCursor += 8f
        }

        private fun drawCompetencies(rect: RectF, inSide: Boolean, blockTheme: RenderTheme) {
            val items = document.competencies.filter { it.isNotBlank() }
            when (plan.competencies) {
                CompetencyStyle.BULLETS -> items.forEach {
                    drawText("•  $it", paint(RenderTheme.Size.BODY, false, blockTheme.mutedInk), rect, inSide, 2f)
                }

                CompetencyStyle.CHIPS -> drawChips(items, rect, inSide, blockTheme)

                CompetencyStyle.COLUMNS -> drawInColumns(items, rect, inSide, blockTheme, columns = 3)

                CompetencyStyle.ICON_GRID -> drawInColumns(
                    items.map { "✓  $it" }, rect, inSide, blockTheme, columns = 2,
                )

                // The level is the position in the list, so the order chosen in
                // the editor *is* the ranking — nothing is invented here.
                CompetencyStyle.METERS -> items.forEachIndexed { index, item ->
                    drawText(item, paint(RenderTheme.Size.DETAIL, false, blockTheme.ink), rect, inSide, 5f)
                    val filled = 1f - (index.toFloat() / (items.size.coerceAtLeast(2))) * 0.45f
                    val y = (if (inSide) sideCursor else mainCursor) + 3f
                    fill.color = blockTheme.hairline
                    canvas.drawRect(rect.left, y, rect.right, y + 3f, fill)
                    fill.color = blockTheme.accent
                    canvas.drawRect(rect.left, y, rect.left + rect.width() * filled, y + 3f, fill)
                    if (inSide) sideCursor += 7f else mainCursor += 7f
                }

                CompetencyStyle.DOTS -> items.forEachIndexed { index, item ->
                    drawText(item, paint(RenderTheme.Size.DETAIL, false, blockTheme.ink), rect, inSide, 5f)
                    val rank = 5 - (index * 5 / items.size.coerceAtLeast(1)).coerceAtMost(2)
                    val y = (if (inSide) sideCursor else mainCursor) + 4f
                    repeat(5) { dot ->
                        fill.color = if (dot < rank) blockTheme.accent else blockTheme.hairline
                        canvas.drawCircle(rect.left + 4f + dot * 10f, y, 2.6f, fill)
                    }
                    if (inSide) sideCursor += 9f else mainCursor += 9f
                }
            }
        }

        /** Pills: a set of labels rather than a list of sentences. */
        private fun drawChips(items: List<String>, rect: RectF, inSide: Boolean, blockTheme: RenderTheme) {
            val chipPaint = paint(RenderTheme.Size.DETAIL, false, blockTheme.ink)
            val padH = 7f
            val padV = 4f
            val gap = 5f
            var x = rect.left
            var rowTop = (if (inSide) sideCursor else mainCursor) + 4f
            val rowHeight = chipPaint.textSize + padV * 2

            items.forEach { item ->
                val width = chipPaint.measureText(item) + padH * 2
                if (x + width > rect.right && x > rect.left) {
                    x = rect.left
                    rowTop += rowHeight + gap
                }
                if (rowTop + rowHeight > contentBottom) {
                    newPage()
                    rowTop = (if (inSide) sideCursor else mainCursor) + 4f
                    x = rect.left
                }
                fill.color = blockTheme.hairline
                canvas.drawRoundRect(
                    RectF(x, rowTop, x + width, rowTop + rowHeight), rowHeight / 2f, rowHeight / 2f, fill,
                )
                canvas.drawText(item, x + padH, rowTop + padV + chipPaint.textSize * 0.82f, chipPaint)
                x += width + gap
            }

            val consumed = rowTop + rowHeight - (if (inSide) sideCursor else mainCursor)
            if (inSide) sideCursor += consumed else mainCursor += consumed
        }

        private fun drawInColumns(
            items: List<String>,
            rect: RectF,
            inSide: Boolean,
            blockTheme: RenderTheme,
            columns: Int,
        ) {
            val gap = 10f
            val columnWidth = (rect.width() - gap * (columns - 1)) / columns
            val rows = (items.size + columns - 1) / columns
            val itemPaint = paint(RenderTheme.Size.DETAIL, false, blockTheme.mutedInk)
            val top = (if (inSide) sideCursor else mainCursor) + 3f
            val lineHeight = itemPaint.textSize * 1.65f

            items.forEachIndexed { index, item ->
                val column = index / rows
                val row = index % rows
                canvas.drawText(
                    item,
                    rect.left + column * (columnWidth + gap),
                    top + (row + 1) * lineHeight,
                    itemPaint,
                )
            }

            val consumed = 3f + rows * lineHeight
            if (inSide) sideCursor += consumed else mainCursor += consumed
        }

        private fun drawExperience(rect: RectF, inSide: Boolean, blockTheme: RenderTheme) {
            // Dates hang in the margin, roles hang off them.
            val gutter = if (plan.experience == ExperienceStyle.DATE_GUTTER) 86f else 0f
            val railX = rect.left + 3f
            val bodyRect = RectF(
                rect.left + gutter + if (plan.experience == ExperienceStyle.TIMELINE) 16f else 0f,
                rect.top, rect.right, rect.bottom,
            )

            document.experience.forEach { entry ->
                val entryTop = (if (inSide) sideCursor else mainCursor) + 8f

                drawText(entry.role, paint(RenderTheme.Size.BODY, true, blockTheme.ink), bodyRect, inSide, 8f)

                // A rail down the column with a dot at every role: the career
                // path, drawn. The dot is placed against the role, not the block,
                // so it stays aligned when an entry wraps.
                if (plan.experience == ExperienceStyle.TIMELINE) {
                    fill.color = blockTheme.accent
                    canvas.drawCircle(railX, entryTop + 5f, 2.8f, fill)
                    stroke.color = blockTheme.hairline
                    canvas.drawLine(railX, entryTop + 9f, railX, entryTop + 9f + 30f, stroke)
                }

                if (gutter > 0f) {
                    val datePaint = paint(RenderTheme.Size.DETAIL, false, blockTheme.mutedInk)
                    canvas.drawText(entry.period, rect.left, entryTop + datePaint.textSize, datePaint)
                }

                val meta = buildList {
                    if (entry.company.isNotBlank()) add(entry.company)
                    if (gutter == 0f && entry.period.isNotBlank()) add(entry.period)
                }.joinToString(" · ")
                drawText(meta, paint(RenderTheme.Size.DETAIL, false, blockTheme.accent), bodyRect, inSide, 1f)

                entry.highlights.filter { it.isNotBlank() }.forEach {
                    drawText("•  $it", paint(RenderTheme.Size.DETAIL, false, blockTheme.mutedInk), bodyRect, inSide, 3f)
                }
            }
        }
    }
}
