package com.resumestudio.model

import kotlinx.serialization.Serializable

/**
 * One saved résumé, mirroring `ResumeDraft` in `ResumeDocument.swift`.
 *
 * Timestamps are seconds since 1970 as `Double`, because that is what the iOS
 * encoder is configured to write (`dateEncodingStrategy = .secondsSince1970`).
 * Writing ISO-8601 here — the obvious Kotlin default — would produce a file iOS
 * could not read, and the failure would surface as a lost résumé rather than a
 * parse error anyone could act on.
 */
@Serializable
data class ResumeDraft(
    val id: String = uuid(),
    val title: String,
    val document: ResumeDocument,
    val createdAt: Double = nowSeconds(),
    val updatedAt: Double = nowSeconds(),
)

/**
 * The whole library as it sits on disk, mirroring `ResumeLibraryArchive`.
 *
 * This is the on-disk shape of `draft.json`, which is the file the two
 * platforms hand back and forth — so the property names are the iOS ones.
 */
@Serializable
data class ResumeLibraryArchive(
    val activeResumeID: String,
    val resumes: List<ResumeDraft>,
)

fun uuid(): String = java.util.UUID.randomUUID().toString().uppercase()

fun nowSeconds(): Double = System.currentTimeMillis() / 1000.0

/** The title a new draft takes, mirroring `defaultTitle(for:)`. */
fun defaultDraftTitle(document: ResumeDocument): String =
    document.personal.headline.trim().ifBlank { "My Résumé" }
