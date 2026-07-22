package com.resumestudio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The résumé itself, mirroring `ResumeDocument.swift`.
 *
 * The wire format is the sync contract between the two platforms, so the
 * property names here are the iOS `CodingKeys`, not Kotlin-idiomatic renames.
 * Anything that reads or writes this must go through [ResumeJson].
 */
@Serializable
data class ResumeDocument(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val personal: PersonalDetails = PersonalDetails(),
    val professionalProfile: String = "",
    val competencies: List<String> = emptyList(),
    val experience: List<ExperienceEntry> = emptyList(),
    val education: List<EducationEntry> = emptyList(),
    val references: List<ReferenceEntry> = emptyList(),
    val additionalSections: List<ResumeAdditionalSection> = emptyList(),
    val accent: ResumeAccent = ResumeAccent.ORANGE,
    val template: ResumeTemplate = ResumeTemplate.MODERN,
    val layout: ResumeLayoutSettings = ResumeLayoutSettings(),

    /** JPEG/PNG bytes. Base64 on the wire, matching Swift's `Data` encoding. */
    @Serializable(with = Base64ByteArraySerializer::class)
    val photo: ByteArray? = null,
    val photoCrop: PhotoCrop? = null,
    val isPhotoVisible: Boolean = true,
    val signature: ResumeSignature? = null,
) {
    val showsPortrait: Boolean get() = isPhotoVisible && photo != null

    val initials: String
        get() = personal.fullName
            .split(' ', '\t', '\n')
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

    /**
     * The name an exported file takes, mirroring `suggestedFilename`.
     *
     * The character set stripped here is the one Windows and the web object to,
     * not just the one Android does — an exported résumé is mostly going to end
     * up as an email attachment on somebody else's machine.
     */
    val suggestedFilename: String
        get() {
            val source = personal.fullName.trim()
            val base = if (source.isEmpty()) "Resume" else "$source Resume"
            return base.split(*"""/\:?%*|"<>""".toCharArray()).joinToString("-")
        }

    /** How much of the résumé has content, 0–1. The nine checks iOS makes. */
    val completion: Double
        get() {
            val checks = listOf(
                personal.fullName.isNotBlank(),
                personal.headline.isNotBlank(),
                personal.email.isNotBlank(),
                personal.phone.isNotBlank(),
                professionalProfile.isNotBlank(),
                competencies.any { it.isNotBlank() },
                experience.any { it.role.isNotBlank() || it.company.isNotBlank() },
                education.any { it.qualification.isNotBlank() || it.institution.isNotBlank() },
                references.any { it.name.isNotBlank() },
            )
            return checks.count { it }.toDouble() / checks.size
        }

    /** [completion] as a whole percentage, for display. */
    val completionPercentage: Int get() = Math.round(completion * 100).toInt()

    // Data classes with an array member need these written out by hand; the
    // generated versions compare the reference, not the bytes.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResumeDocument) return false
        return schemaVersion == other.schemaVersion &&
            personal == other.personal &&
            professionalProfile == other.professionalProfile &&
            competencies == other.competencies &&
            experience == other.experience &&
            education == other.education &&
            references == other.references &&
            additionalSections == other.additionalSections &&
            accent == other.accent &&
            template == other.template &&
            layout == other.layout &&
            (photo?.contentEquals(other.photo) ?: (other.photo == null)) &&
            photoCrop == other.photoCrop &&
            isPhotoVisible == other.isPhotoVisible &&
            signature == other.signature
    }

    override fun hashCode(): Int {
        var result = schemaVersion
        result = 31 * result + personal.hashCode()
        result = 31 * result + professionalProfile.hashCode()
        result = 31 * result + competencies.hashCode()
        result = 31 * result + experience.hashCode()
        result = 31 * result + education.hashCode()
        result = 31 * result + references.hashCode()
        result = 31 * result + additionalSections.hashCode()
        result = 31 * result + accent.hashCode()
        result = 31 * result + template.hashCode()
        result = 31 * result + layout.hashCode()
        result = 31 * result + (photo?.contentHashCode() ?: 0)
        result = 31 * result + (photoCrop?.hashCode() ?: 0)
        result = 31 * result + isPhotoVisible.hashCode()
        result = 31 * result + (signature?.hashCode() ?: 0)
        return result
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

@Serializable
data class PersonalDetails(
    val fullName: String = "",
    val headline: String = "",
    val phone: String = "",
    val email: String = "",
)

@Serializable
data class ExperienceEntry(
    val id: String = newId(),
    val role: String = "",
    val company: String = "",
    val period: String = "",
    val highlights: List<String> = emptyList(),
)

@Serializable
data class EducationEntry(
    val id: String = newId(),
    val qualification: String = "",
    val institution: String = "",
    val period: String = "",
    val details: String = "",
)

@Serializable
data class ReferenceEntry(
    val id: String = newId(),
    val name: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
)

@Serializable
data class ResumeAdditionalSection(
    val id: String = newId(),
    val title: String = "",
    val items: List<String> = emptyList(),
)

@Serializable
data class PhotoCrop(
    val originX: Double = 0.0,
    val originY: Double = 0.0,
    val size: Double = 1.0,
)

/**
 * Per-document layout, mirroring `ResumeLayoutSettings`.
 *
 * The iOS side hand-writes its decoder so that *every* field falls back to its
 * default rather than throwing on a missing key — a résumé must never fail to
 * open because a setting was added after it was saved. kotlinx.serialization
 * already applies defaults for absent keys, so the mirror of that rule on this
 * side is [ResumeJson]'s `ignoreUnknownKeys`: a field iOS adds tomorrow must not
 * break a document opening here today.
 */
@Serializable
data class ResumeLayoutSettings(
    val fontChoice: ResumeFontChoice = ResumeFontChoice.TEMPLATE,
    val fontScale: Double = 1.0,
    val lineSpacing: Double = 1.0,
    val marginPoints: Double = 34.0,
    val pageTarget: ResumePageTarget = ResumePageTarget.AUTOMATIC,
    val paperSize: ResumePaperSize = ResumePaperSize.A4,
    val sectionOrder: List<ResumeContentBlock> = ResumeContentBlock.entries.toList(),
    val customHeadings: Map<String, String> = emptyMap(),
) {
    /** Mirrors `normalize()`: clamp to the same bounds, dedupe, then backfill. */
    fun normalized(): ResumeLayoutSettings {
        val seen = LinkedHashSet<ResumeContentBlock>()
        sectionOrder.forEach { seen.add(it) }
        ResumeContentBlock.entries.forEach { seen.add(it) }
        return copy(
            fontScale = fontScale.coerceIn(0.82, 1.12),
            lineSpacing = lineSpacing.coerceIn(0.88, 1.15),
            marginPoints = marginPoints.coerceIn(24.0, 50.0),
            sectionOrder = seen.toList(),
        )
    }

    fun heading(block: ResumeContentBlock): String =
        customHeadings[block.wireName]?.trim()?.ifBlank { null } ?: block.title
}

@Serializable
enum class ResumeContentBlock(val wireName: String, val title: String) {
    @SerialName("profile") PROFILE("profile", "Professional Profile"),
    @SerialName("competencies") COMPETENCIES("competencies", "Core Competencies"),
    @SerialName("experience") EXPERIENCE("experience", "Professional Experience"),
    @SerialName("education") EDUCATION("education", "Education"),
    @SerialName("additional") ADDITIONAL("additional", "Additional Sections"),
    @SerialName("references") REFERENCES("references", "References"),
}

@Serializable
enum class ResumeFontChoice(val title: String) {
    @SerialName("template") TEMPLATE("Template default"),
    @SerialName("cleanSans") CLEAN_SANS("Clean Sans"),
    @SerialName("editorialSerif") EDITORIAL_SERIF("Editorial Serif"),
    @SerialName("modernRounded") MODERN_ROUNDED("Modern Rounded"),
    @SerialName("technicalMono") TECHNICAL_MONO("Technical Mono"),
}

@Serializable
enum class ResumePageTarget(val title: String, val pageLimit: Int?) {
    @SerialName("automatic") AUTOMATIC("Automatic", null),
    @SerialName("one") ONE("One page", 1),
    @SerialName("two") TWO("Two pages", 2),
}

/** Points at 72dpi — the same coordinate space `UIGraphicsPDFRenderer` uses. */
@Serializable
enum class ResumePaperSize(val widthPoints: Float, val heightPoints: Float) {
    @SerialName("a4") A4(595.276f, 841.89f),
    @SerialName("letter") LETTER(612f, 792f),
}

/** The one JSON configuration allowed to touch documents. */
object ResumeJson {
    val format: Json = Json {
        // A field iOS ships tomorrow must not stop a document opening here today.
        ignoreUnknownKeys = true
        // Defaults are meaningful on both sides, so write them: the file stays
        // readable by an older build that has no fallback for the key.
        encodeDefaults = true
        explicitNulls = false
        isLenient = false
    }

    fun decode(text: String): ResumeDocument = format.decodeFromString(text)

    fun encode(document: ResumeDocument): String = format.encodeToString(document)
}

private fun newId(): String = java.util.UUID.randomUUID().toString().uppercase()
