package com.resumestudio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What a template does with the *page*, as opposed to what it does with the
 * letterhead.
 *
 * The original catalogue is all [BodyLayout.Single]: one column, section after
 * section, down the page. The difference between those templates is the header
 * art — which is why, past the first inch, they all read rather alike.
 *
 * This is the other axis. A plan moves the content itself: puts contact details
 * and skills in a column of their own, hangs dates in the margin, threads a rail
 * through the roles, or leads with skills instead of the summary. The layouts
 * recruiters actually meet — a narrow column for the scannable facts beside a
 * wide one for the story — are combinations of these, not new headers.
 *
 * This is a direct mirror of `TemplatePlan.swift` in the iOS repo. The two are
 * held together by `spec/template-catalogue.json` and the parity suite; if you
 * add a case here, add it there, or the conformance test will fail loudly.
 */
@Serializable
data class TemplatePlan(
    val body: BodyLayout = BodyLayout.Single,
    val experience: ExperienceStyle = ExperienceStyle.STACKED,
    val competencies: CompetencyStyle = CompetencyStyle.BULLETS,
    val sectionChrome: SectionChrome = SectionChrome.PLAIN,
    val contact: ContactStyle = ContactStyle.INLINE,

    /**
     * The three sections whose look was, until section styles, always inferred:
     * education followed the roles and the chrome, references wore the template's
     * own card art, extra sections were always bullets. `null` keeps exactly that,
     * so none of the catalogue changed when these were added — a value here is
     * only ever the user asking for something else.
     */
    val education: EducationStyle? = null,
    val references: ReferenceStyle? = null,
    val additional: AdditionalSectionStyle? = null,

    /** Skills before the summary, for people whose skills are the pitch. */
    val skillsFirst: Boolean = false,

    /** Sections counted off — 01, 02, 03 — the editorial numbering. */
    val numberedSections: Boolean = false,

    /**
     * Light type on a dark page: the whole sheet, not just a band. The renderer
     * swaps its ink for the paper's sake everywhere the body is drawn.
     */
    val darkPaper: Boolean = false,

    /**
     * Section titles hang in a gutter to the left of the text, the way a reference
     * book sets its side-headings. The body keeps a narrow measure beside them, so
     * the page reads as one column with the headings living outside it.
     */
    val hangingHeadings: Boolean = false,

    /**
     * The summary is set inside the letterhead itself rather than beneath it —
     * the opening paragraph as part of the masthead.
     */
    val profileInHeader: Boolean = false,

    /**
     * How far the body clears the left margin, for the templates whose page
     * furniture — a full-height rail — occupies it. Points, not pixels.
     */
    val bodyInset: Float = 0f,

    /** Multiplies every type size. Below 1 it buys lines, and therefore pages. */
    val density: Float = 1f,
) {
    /**
     * Whether the page carries a second column of text. Real text in two columns
     * stays selectable and searchable, but some applicant-tracking parsers read
     * the columns in the wrong order, which is worth saying out loud rather than
     * discovering after an application disappears.
     */
    val hasSideColumn: Boolean get() = body is BodyLayout.Side
}

@Serializable
sealed interface BodyLayout {
    @Serializable
    @SerialName("single")
    data object Single : BodyLayout

    @Serializable
    @SerialName("side")
    data class Side(val column: SideColumn) : BodyLayout
}

/**
 * The narrow column: contact, skills, education — the facts a recruiter scans
 * for. The wide column keeps the summary and the experience, which is what they
 * actually read.
 */
@Serializable
data class SideColumn(
    val edge: Edge,
    /** Points at 72dpi, matching the iOS renderer's coordinate space. */
    val width: Float,
    val fill: Fill,

    /**
     * The split templates run the summary the full width of the page and only
     * then divide, so the opening paragraph is never squeezed into a gutter.
     */
    val startsBelowProfile: Boolean = false,

    /**
     * A hairline between the columns, for the unfilled ones — the Swiss look,
     * where the divide is drawn rather than painted.
     */
    val divider: Boolean = false,
) {
    @Serializable
    enum class Edge {
        @SerialName("leading") LEADING,
        @SerialName("trailing") TRAILING,
    }

    @Serializable
    enum class Fill {
        /** A full-height band of colour, the length of every page. */
        @SerialName("dark") DARK,
        @SerialName("accent") ACCENT,
        @SerialName("tint") TINT,

        /** No band: the column is simply a second column of type. */
        @SerialName("none") NONE,
    }

    /** White type on the dark and accent bands, ink on the quiet ones. */
    val prefersLightInk: Boolean get() = fill == Fill.DARK || fill == Fill.ACCENT
}

@Serializable
enum class ExperienceStyle {
    @SerialName("stacked") STACKED,

    /** A rail down the column with a dot at every role: the career path, drawn. */
    @SerialName("timeline") TIMELINE,

    /** Dates hang in the margin, roles hang off them. */
    @SerialName("dateGutter") DATE_GUTTER,
}

@Serializable
enum class CompetencyStyle {
    @SerialName("bullets") BULLETS,

    /** Pills. They read as a set of labels rather than a list of sentences. */
    @SerialName("chips") CHIPS,

    /** Ticked, two across — the "skills matrix" look. */
    @SerialName("iconGrid") ICON_GRID,

    /**
     * Ranked bars, strongest first. The level is the position in the list, so
     * the order chosen in the editor is the ranking — nothing is invented.
     */
    @SerialName("meters") METERS,

    /**
     * A row of five dots, filled by rank — the "rating" look the portfolio
     * templates use. Same ranking logic as [METERS], drawn as beads.
     */
    @SerialName("dots") DOTS,

    /** Three across, plain type: the compact list the ATS guides recommend. */
    @SerialName("columns") COLUMNS,
}

@Serializable
enum class SectionChrome {
    @SerialName("plain") PLAIN,

    /** Every entry in its own bordered card. */
    @SerialName("card") CARD,
}

@Serializable
enum class ContactStyle {
    @SerialName("inline") INLINE,

    /** An icon beside each detail, stacked. Needs a column to live in, or a strip. */
    @SerialName("iconRows") ICON_ROWS,
}

@Serializable
enum class EducationStyle {
    /** Qualification, then institution and dates on one line beneath it. */
    @SerialName("stacked") STACKED,

    /** Dates hang in the margin, the qualification hangs off them. */
    @SerialName("dateGutter") DATE_GUTTER,

    /** Every qualification in its own bordered card. */
    @SerialName("card") CARD,
}

@Serializable
enum class ReferenceStyle {
    /** The template's own card art — the panel, the accent edge, the tint. */
    @SerialName("cards") CARDS,

    /**
     * The same two-up grid with the card removed: type on the page, separated by
     * a hairline. Reads quieter, and survives being pasted into a form.
     */
    @SerialName("plain") PLAIN,

    /**
     * One line per referee. The most space a résumé can buy back at the foot of
     * the page without dropping anyone.
     */
    @SerialName("compact") COMPACT,
}

@Serializable
enum class AdditionalSectionStyle {
    @SerialName("bullets") BULLETS,

    /** Pills, for sets of short items — languages, tools, certifications. */
    @SerialName("chips") CHIPS,

    /** Three across, plain type. */
    @SerialName("columns") COLUMNS,

    /** Bullet markers dropped; the items simply run down the page. */
    @SerialName("plain") PLAIN,
}
