package com.resumestudio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The career coach's conversation, mirroring the coach types in `AIModels.swift`.
 *
 * The wire names are the iOS ones because the same backend action serves both
 * apps — `/v1/ai` does not know or care which client is asking, so the payload
 * has to be the shape it already understands.
 */
@Serializable
enum class CareerCoachMessageRole {
    @SerialName("user") USER,
    @SerialName("assistant") ASSISTANT,
}

@Serializable
data class CareerCoachMessage(
    val id: String = uuid(),
    val role: CareerCoachMessageRole,
    val content: String,
    val createdAt: Double = nowSeconds(),
)

/**
 * What the coach is told about the person asking.
 *
 * Deliberately a summary rather than the document: the coach needs to know a
 * résumé is 60% complete and has three roles, not to be handed every bullet.
 */
@Serializable
data class CareerCoachContext(
    val resumeTitle: String = "",
    val resumeCompletionPercentage: Int = 0,
    val resume: AIResumeSnapshot = AIResumeSnapshot(),
    val applications: List<CoachApplicationSnapshot> = emptyList(),
)

@Serializable
data class AIResumeSnapshot(
    val headline: String = "",
    val profile: String = "",
    val competencies: List<String> = emptyList(),
    val roles: List<AIRoleSnapshot> = emptyList(),
)

@Serializable
data class AIRoleSnapshot(
    val role: String = "",
    val company: String = "",
    val period: String = "",
    val highlights: List<String> = emptyList(),
)

@Serializable
data class CoachApplicationSnapshot(
    val role: String = "",
    val company: String = "",
    val status: String = "",
)

@Serializable
data class AICareerCoachReply(
    val reply: String = "",
    val suggestedPrompts: List<String> = emptyList(),
)

/** The actions `/v1/ai` accepts. Only the ones Android sends are listed. */
@Serializable
enum class ResumeAIAction {
    @SerialName("careerCoach") CAREER_COACH,
    @SerialName("improveBullet") IMPROVE_BULLET,
    @SerialName("writeProfile") WRITE_PROFILE,
    @SerialName("suggestCompetencies") SUGGEST_COMPETENCIES,
    @SerialName("writeCoverLetter") WRITE_COVER_LETTER,
}

/** Builds the summary the coach is given, from what Android actually holds. */
fun coachContext(
    document: ResumeDocument,
    title: String,
    applications: List<JobApplication>,
): CareerCoachContext = CareerCoachContext(
    resumeTitle = title,
    resumeCompletionPercentage = document.completionPercentage,
    resume = AIResumeSnapshot(
        headline = document.personal.headline,
        profile = document.professionalProfile,
        competencies = document.competencies,
        roles = document.experience.map {
            AIRoleSnapshot(it.role, it.company, it.period, it.highlights)
        },
    ),
    applications = applications.map {
        CoachApplicationSnapshot(it.role, it.company, it.status.name.lowercase())
    },
)
