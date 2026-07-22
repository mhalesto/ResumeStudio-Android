package com.resumestudio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The answers every application form asks for, mirroring `ApplicationAnswer.swift`.
 *
 * The point of the vault is that these questions never change but the answers
 * are easy to get wrong under time pressure — a salary figure typed in a hurry
 * is hard to walk back. Written once, calmly, and copied thereafter.
 */
@Serializable
enum class ApplicationAnswerCategory(
    val wireName: String,
    val title: String,
    val starterTitle: String,
    val matchTerms: List<String>,
) {
    @SerialName("workAuthorization") WORK_AUTHORIZATION(
        "workAuthorization", "Work authorization",
        "Are you legally authorized to work here?",
        listOf("authoriz", "authoris", "eligible to work", "right to work", "work permit"),
    ),

    @SerialName("sponsorship") SPONSORSHIP(
        "sponsorship", "Visa sponsorship",
        "Will you require visa sponsorship?",
        listOf("sponsor", "visa", "immigration"),
    ),

    @SerialName("availability") AVAILABILITY(
        "availability", "Availability",
        "When can you start?",
        listOf("start date", "notice period", "available", "availability"),
    ),

    @SerialName("compensation") COMPENSATION(
        "compensation", "Salary expectations",
        "What are your salary expectations?",
        listOf("salary", "compensation", "pay expectation", "remuneration", "package"),
    ),

    @SerialName("relocation") RELOCATION(
        "relocation", "Relocation",
        "Are you willing to relocate?",
        listOf("relocat", "willing to move"),
    ),

    @SerialName("workPreference") WORK_PREFERENCE(
        "workPreference", "Work arrangement",
        "What work arrangement do you prefer?",
        listOf("remote", "hybrid", "on-site", "onsite", "in office", "work arrangement"),
    ),

    @SerialName("motivation") MOTIVATION(
        "motivation", "Role motivation",
        "Why are you interested in this role?",
        listOf("why do you want", "why are you interested", "motivat", "why this role"),
    ),

    @SerialName("custom") CUSTOM(
        "custom", "Custom question",
        "Application question",
        emptyList(),
    ),
    ;

    companion object {
        private val byWireName = entries.associateBy(ApplicationAnswerCategory::wireName)

        fun from(wireName: String): ApplicationAnswerCategory? = byWireName[wireName]

        /**
         * The category a form question is asking about, if any.
         *
         * Matched on substrings rather than whole words because the same
         * question arrives phrased a dozen ways — "Do you now or will you in
         * the future require sponsorship" and "Visa status" are the same
         * question and should reach the same saved answer.
         */
        fun matching(question: String): ApplicationAnswerCategory? {
            val haystack = question.lowercase()
            return entries.firstOrNull { category ->
                category.matchTerms.any { haystack.contains(it) }
            }
        }
    }
}

@Serializable
data class ApplicationAnswer(
    val id: String = uuid(),
    val category: ApplicationAnswerCategory = ApplicationAnswerCategory.CUSTOM,
    val title: String = "",
    val answer: String = "",
    val keywords: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val updatedAt: Double = nowSeconds(),
) {
    val isUsable: Boolean get() = isEnabled && answer.isNotBlank()
}

@Serializable
data class AnswerVaultArchive(
    val answers: List<ApplicationAnswer> = emptyList(),
) {
    companion object {
        /**
         * One empty prompt per category, so the vault opens as a checklist
         * rather than an empty page with an add button.
         */
        fun starter(): AnswerVaultArchive = AnswerVaultArchive(
            ApplicationAnswerCategory.entries
                .filter { it != ApplicationAnswerCategory.CUSTOM }
                .map { ApplicationAnswer(category = it, title = it.starterTitle) },
        )
    }
}
