package com.resumestudio.render

import android.graphics.Color
import android.graphics.Typeface
import com.resumestudio.model.ResumeAccent
import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeFontChoice
import com.resumestudio.model.TemplatePlan

/**
 * The ink, paper and type a page is drawn with.
 *
 * Everything here is in points at 72dpi, the same space `UIGraphicsPDFRenderer`
 * works in, so the numbers lifted from the iOS renderer carry over unchanged.
 * Android's `PdfDocument` also measures its pages in points, so there is no
 * density conversion anywhere in this module — and there must not be one, or
 * every measurement stops matching the iOS source it was ported from.
 */
data class RenderTheme(
    val accent: Int,
    val ink: Int,
    val mutedInk: Int,
    val paper: Int,
    val hairline: Int,
    val heading: Typeface,
    val body: Typeface,
    val scale: Float,
) {
    /** Type sizes, before [scale] and before the plan's density. */
    object Size {
        const val NAME = 24f
        const val HEADLINE = 11.5f
        const val SECTION_TITLE = 10.5f
        const val BODY = 9.5f
        const val DETAIL = 8.5f
        const val CONTACT = 8.5f
    }

    fun size(base: Float): Float = base * scale

    companion object {

        /**
         * White type on a dark page, ink on a pale one.
         *
         * `darkPaper` is the whole sheet rather than a band, so the swap happens
         * once here instead of at every draw call — which is what stops a section
         * being ported with the wrong ink and nobody noticing until export.
         */
        fun of(document: ResumeDocument, plan: TemplatePlan = document.template.plan): RenderTheme {
            val dark = plan.darkPaper
            val accent = document.accent.argb
            val fonts = typefacesFor(document.layout.fontChoice)

            return RenderTheme(
                accent = if (dark) lighten(accent, 0.25f) else accent,
                ink = if (dark) Color.parseColor("#F2F2F0") else Color.parseColor("#1A1A1A"),
                mutedInk = if (dark) Color.parseColor("#A8A8A4") else Color.parseColor("#5A5A5A"),
                paper = if (dark) Color.parseColor("#14161A") else Color.WHITE,
                hairline = if (dark) Color.parseColor("#3A3D42") else Color.parseColor("#D8D8D4"),
                heading = fonts.first,
                body = fonts.second,
                scale = (document.layout.fontScale * plan.density).toFloat(),
            )
        }

        /**
         * The ink used inside a filled side column, which has its own paper.
         *
         * See `SideColumn.prefersLightInk`: the dark and accent bands take white
         * type, the quiet ones keep the page's own.
         */
        fun forSideColumn(base: RenderTheme, prefersLightInk: Boolean): RenderTheme =
            if (!prefersLightInk) base
            else base.copy(
                ink = Color.WHITE,
                mutedInk = Color.argb(200, 255, 255, 255),
                hairline = Color.argb(70, 255, 255, 255),
                accent = Color.WHITE,
            )

        /**
         * Android has no metric equivalent of the iOS font choices, so these map
         * onto the closest families the platform guarantees. A bundled face per
         * choice is what finally closes the gap with iOS; until then this keeps
         * the *shape* of the choice — serif stays serif, mono stays mono — so
         * layout code can be ported and reviewed before the fonts land.
         */
        private fun typefacesFor(choice: ResumeFontChoice): Pair<Typeface, Typeface> = when (choice) {
            ResumeFontChoice.TEMPLATE,
            ResumeFontChoice.CLEAN_SANS,
            ResumeFontChoice.MODERN_ROUNDED,
                -> Typeface.create("sans-serif-medium", Typeface.BOLD) to
                    Typeface.create("sans-serif", Typeface.NORMAL)

            ResumeFontChoice.EDITORIAL_SERIF ->
                Typeface.create("serif", Typeface.BOLD) to Typeface.create("serif", Typeface.NORMAL)

            ResumeFontChoice.TECHNICAL_MONO ->
                Typeface.create("monospace", Typeface.BOLD) to
                    Typeface.create("monospace", Typeface.NORMAL)
        }

        private fun lighten(color: Int, amount: Float): Int {
            fun mix(channel: Int) = (channel + (255 - channel) * amount).toInt().coerceIn(0, 255)
            return Color.argb(
                Color.alpha(color),
                mix(Color.red(color)),
                mix(Color.green(color)),
                mix(Color.blue(color)),
            )
        }
    }
}

/** The accent as the side column paints it, per `SideColumn.Fill`. */
fun sideColumnPaper(fill: com.resumestudio.model.SideColumn.Fill, accent: ResumeAccent): Int? = when (fill) {
    com.resumestudio.model.SideColumn.Fill.DARK -> Color.parseColor("#1F2329")
    com.resumestudio.model.SideColumn.Fill.ACCENT -> accent.argb
    com.resumestudio.model.SideColumn.Fill.TINT -> Color.argb(
        26,
        Color.red(accent.argb),
        Color.green(accent.argb),
        Color.blue(accent.argb),
    )
    com.resumestudio.model.SideColumn.Fill.NONE -> null
}
