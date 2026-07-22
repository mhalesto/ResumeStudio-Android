package com.resumestudio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A tracked opportunity, mirroring `JobApplication` in `CareerModels.swift`.
 *
 * Only the fields Android can fill are here. The AI analyses, interview plans
 * and packets iOS carries are absent rather than stubbed — a nullable field
 * that is always null reads as a feature that is broken, where an absent one
 * reads as a feature that has not arrived.
 */
@Serializable
data class JobApplication(
    val id: String = uuid(),
    val company: String = "",
    val role: String = "",
    val jobDescription: String = "",
    val sourceURL: String = "",
    val status: JobApplicationStatus = JobApplicationStatus.SAVED,
    val notes: String = "",
    val createdAt: Double = nowSeconds(),
    val updatedAt: Double = nowSeconds(),
) {
    val title: String get() = listOf(role, company).filter { it.isNotBlank() }.joinToString(" · ")
        .ifBlank { "Untitled opportunity" }
}

@Serializable
enum class JobApplicationStatus(val title: String) {
    // Spelled out rather than capitalising the wire name — a capitalised raw
    // value is an English word by accident and can never be translated.
    @SerialName("saved") SAVED("Saved"),
    @SerialName("applied") APPLIED("Applied"),
    @SerialName("interview") INTERVIEW("Interview"),
    @SerialName("offer") OFFER("Offer"),
    @SerialName("rejected") REJECTED("Rejected"),
    ;

    /** The pipeline order, which is not the declaration order for `rejected`. */
    val isOpen: Boolean get() = this != REJECTED
}

/** The whole pipeline as it sits on disk. */
@Serializable
data class ApplicationArchive(
    val applications: List<JobApplication> = emptyList(),
)
