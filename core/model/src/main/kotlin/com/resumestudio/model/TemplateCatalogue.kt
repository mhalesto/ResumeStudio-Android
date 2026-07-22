package com.resumestudio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Resolves a [ResumeTemplate] to the [TemplatePlan] it draws with.
 *
 * The mapping is not written twice. iOS owns it — it lives in the `plan` switch
 * in `ResumeDocument.swift` — and `tools/generate-template-spec.py` lifts it into
 * `spec/template-catalogue.json`, which ships here as a resource and is read at
 * first use. So Android cannot drift from the catalogue by construction: there is
 * no second copy of the mapping to fall out of step, only a regeneration that is
 * either run or not. `TemplateCatalogueTest` fails loudly if it has not been.
 */
object TemplateCatalogue {

    private const val RESOURCE = "/template-catalogue.json"

    private val json = Json { ignoreUnknownKeys = true }

    private val catalogue: Map<ResumeTemplate, TemplatePlan> by lazy { load() }

    /** The plan for [template]; the single-column default if the spec omits it. */
    fun plan(template: ResumeTemplate): TemplatePlan =
        catalogue[template] ?: TemplatePlan()

    /** Every template that lays the page out in two columns. */
    fun twoColumnTemplates(): List<ResumeTemplate> =
        catalogue.filterValues { it.hasSideColumn }.keys.sortedBy { it.wireName }

    val templateCount: Int get() = catalogue.size

    private fun load(): Map<ResumeTemplate, TemplatePlan> {
        val text = requireNotNull(TemplateCatalogue::class.java.getResourceAsStream(RESOURCE)) {
            "$RESOURCE is missing. Run tools/generate-template-spec.py."
        }.bufferedReader().use { it.readText() }

        val file = json.decodeFromString<CatalogueFile>(text)
        return buildMap {
            file.templates.forEach { (wireName, plan) ->
                // A template iOS has added but Android has not yet mirrored is
                // skipped rather than fatal — the parity test is where that is
                // reported, so a stale spec never crashes someone's résumé.
                ResumeTemplate.from(wireName)?.let { put(it, plan.toDomain()) }
            }
        }
    }

    // --- wire shapes -------------------------------------------------------
    // Kept separate from the domain types so the generated file's layout is free
    // to change without disturbing what the renderer consumes.

    @Serializable
    private data class CatalogueFile(
        val schemaVersion: Int = 1,
        val templateCount: Int = 0,
        val templates: Map<String, PlanDto> = emptyMap(),
    )

    @Serializable
    private data class PlanDto(
        val body: BodyDto = BodyDto(),
        val experience: ExperienceStyle = ExperienceStyle.STACKED,
        val competencies: CompetencyStyle = CompetencyStyle.BULLETS,
        val sectionChrome: SectionChrome = SectionChrome.PLAIN,
        val contact: ContactStyle = ContactStyle.INLINE,
        val education: EducationStyle? = null,
        val references: ReferenceStyle? = null,
        val additional: AdditionalSectionStyle? = null,
        val skillsFirst: Boolean = false,
        val numberedSections: Boolean = false,
        val darkPaper: Boolean = false,
        val hangingHeadings: Boolean = false,
        val profileInHeader: Boolean = false,
        val bodyInset: Float = 0f,
        val density: Float = 1f,
    ) {
        fun toDomain() = TemplatePlan(
            body = body.toDomain(),
            experience = experience,
            competencies = competencies,
            sectionChrome = sectionChrome,
            contact = contact,
            education = education,
            references = references,
            additional = additional,
            skillsFirst = skillsFirst,
            numberedSections = numberedSections,
            darkPaper = darkPaper,
            hangingHeadings = hangingHeadings,
            profileInHeader = profileInHeader,
            bodyInset = bodyInset,
            density = density,
        )
    }

    @Serializable
    private data class BodyDto(
        val kind: String = "single",
        val column: ColumnDto? = null,
    ) {
        fun toDomain(): BodyLayout =
            if (kind == "side" && column != null) BodyLayout.Side(column.toDomain())
            else BodyLayout.Single
    }

    @Serializable
    private data class ColumnDto(
        val edge: SideColumn.Edge,
        val width: Float,
        val fill: SideColumn.Fill,
        @SerialName("startsBelowProfile") val startsBelowProfile: Boolean = false,
        val divider: Boolean = false,
    ) {
        fun toDomain() = SideColumn(
            edge = edge,
            width = width,
            fill = fill,
            startsBelowProfile = startsBelowProfile,
            divider = divider,
        )
    }
}
