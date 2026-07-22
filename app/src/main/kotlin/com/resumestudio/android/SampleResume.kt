package com.resumestudio.android

import com.resumestudio.model.EducationEntry
import com.resumestudio.model.ExperienceEntry
import com.resumestudio.model.PersonalDetails
import com.resumestudio.model.ReferenceEntry
import com.resumestudio.model.ResumeAccent
import com.resumestudio.model.ResumeAdditionalSection
import com.resumestudio.model.ResumeDocument

/**
 * The résumé the gallery previews are drawn from, until there is a real
 * document store to draw from instead.
 *
 * Written to exercise the layout rather than to read well: two roles so
 * experience styles have something to alternate, eight competencies so chips
 * wrap and meters have a range to rank across, and long enough highlight lines
 * that a narrow column has to break them.
 */
internal val sampleResume = ResumeDocument(
    personal = PersonalDetails(
        fullName = "Halalisani Mbanjwa",
        headline = "Senior Android Engineer",
        phone = "+27 82 000 0000",
        email = "mbanjwa.hg@gmail.com",
    ),
    professionalProfile = "Engineer with a decade of shipping consumer apps on mobile, " +
        "most recently a résumé builder whose rendering engine drives 140 templates " +
        "from a single shared layout vocabulary.",
    competencies = listOf(
        "Kotlin", "Jetpack Compose", "Swift", "SwiftUI",
        "PDF rendering", "Typography", "Gradle", "CI/CD",
    ),
    experience = listOf(
        ExperienceEntry(
            role = "Senior Engineer",
            company = "ResumeStudio",
            period = "2023 — present",
            highlights = listOf(
                "Built the template engine behind 140 résumé layouts.",
                "Ported the PDF renderer to Android without a second source of truth.",
            ),
        ),
        ExperienceEntry(
            role = "Android Engineer",
            company = "Earlier Co",
            period = "2019 — 2023",
            highlights = listOf(
                "Led the migration to Compose across a 40-screen app.",
                "Cut cold start by 38% with a lazy dependency graph.",
            ),
        ),
    ),
    education = listOf(
        EducationEntry(
            qualification = "BSc Computer Science",
            institution = "University of Cape Town",
            period = "2015 — 2018",
            details = "Distinction",
        ),
    ),
    references = listOf(
        ReferenceEntry(
            name = "A Referee",
            company = "Somewhere Ltd",
            phone = "+27 11 000 0000",
            email = "referee@example.com",
        ),
    ),
    additionalSections = listOf(
        ResumeAdditionalSection(title = "Languages", items = listOf("English", "isiZulu", "Afrikaans")),
    ),
    accent = ResumeAccent.COBALT,
)
