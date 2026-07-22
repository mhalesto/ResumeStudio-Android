package com.resumestudio.model

import kotlinx.serialization.Serializable

/**
 * A cover letter, mirroring `CoverLetterDocument.swift`.
 *
 * The sender block duplicates what the résumé already knows. That is
 * deliberate on iOS and kept here: a letter is often written for one
 * application with a different phone number or a shortened headline, and
 * binding it to the résumé would make that edit change the résumé too.
 * [fromResume] is the convenience that seeds it.
 */
@Serializable
data class CoverLetterDocument(
    val senderName: String = "",
    val senderHeadline: String = "",
    val senderPhone: String = "",
    val senderEmail: String = "",
    /** Seconds since 1970, matching the iOS encoder. */
    val date: Double = nowSeconds(),
    val recipientName: String = "",
    val recipientTitle: String = "",
    val companyName: String = "",
    val companyAddress: String = "",
    val jobTitle: String = "",
    val subject: String = "",
    val greeting: String = "Dear Hiring Manager,",
    val bodyParagraphs: List<String> = emptyList(),
    val closing: String = "Kind regards,",
    val template: CoverLetterTemplate = CoverLetterTemplate.MODERN,
    val accent: ResumeAccent = ResumeAccent.ORANGE,
    val jobDescription: String = "",
) {
    val suggestedFilename: String
        get() {
            val source = senderName.trim()
            val base = if (source.isEmpty()) "Cover Letter" else "$source Cover Letter"
            return base.split(*"""/\:?%*|"<>""".toCharArray()).joinToString("-")
        }

    /**
     * Whether there is enough here to be worth rendering.
     *
     * A name and one paragraph. Previewing an empty page teaches nothing, and
     * the preview is expensive enough that it should not run on every keystroke
     * of an empty form.
     */
    val isReadyToPreview: Boolean
        get() = senderName.isNotBlank() && bodyParagraphs.any { it.isNotBlank() }

    val initials: String
        get() = senderName
            .split(' ', '\t', '\n')
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

    companion object {
        /**
         * A letter seeded from the résumé, wearing the paired template.
         *
         * The pairing is what makes the two documents read as one application
         * rather than two files that happen to be attached together.
         */
        fun fromResume(resume: ResumeDocument): CoverLetterDocument = CoverLetterDocument(
            senderName = resume.personal.fullName,
            senderHeadline = resume.personal.headline,
            senderPhone = resume.personal.phone,
            senderEmail = resume.personal.email,
            template = CoverLetterTemplate.pairedWith(resume.template) ?: CoverLetterTemplate.MODERN,
            accent = resume.accent,
            bodyParagraphs = listOf("", "", ""),
        )
    }
}
