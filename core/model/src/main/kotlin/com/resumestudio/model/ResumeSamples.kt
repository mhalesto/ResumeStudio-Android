package com.resumestudio.model

/**
 * The two documents a résumé can start from, mirroring `ResumeDocument.blank`
 * and `ResumeDocument.example`.
 *
 * The example is copied from iOS rather than rewritten. It is deliberately
 * fictional and deliberately *full* — every section populated, three roles,
 * eight competencies — because it doubles as the fixture that shows a template
 * doing its job. A thinner sample would make half the catalogue look identical.
 */
val ResumeDocument.Companion.blank: ResumeDocument
    get() = ResumeDocument(
        schemaVersion = ResumeDocument.CURRENT_SCHEMA_VERSION,
        personal = PersonalDetails(),
        professionalProfile = "",
        competencies = emptyList(),
        experience = emptyList(),
        education = emptyList(),
        references = emptyList(),
        additionalSections = emptyList(),
        accent = ResumeAccent.ORANGE,
        template = ResumeTemplate.MODERN,
    )

val ResumeDocument.Companion.example: ResumeDocument
    get() = ResumeDocument(
        schemaVersion = ResumeDocument.CURRENT_SCHEMA_VERSION,
        personal = PersonalDetails(
            fullName = "Avery Sample",
            headline = "People Operations Manager | Employee Experience",
            phone = "+1 202 555 0147",
            email = "avery.sample@example.com",
        ),
        professionalProfile = "People-focused operations professional with 7+ years of experience " +
            "building inclusive employee programmes, improving manager support, and turning " +
            "workforce insights into practical action. Known for clear communication, thoughtful " +
            "problem-solving, and creating scalable processes that strengthen culture while " +
            "supporting business growth.",
        competencies = listOf(
            "People Operations Strategy",
            "Employee Experience",
            "Manager Coaching",
            "Workforce Planning",
            "People Analytics",
            "Talent Acquisition",
            "Policy & Process Design",
            "Change Communication",
        ),
        experience = listOf(
            ExperienceEntry(
                role = "People Operations Manager",
                company = "Northstar Works",
                period = "Jan 2023 - Present",
                highlights = listOf(
                    "Built a quarterly people-planning rhythm that connected hiring, development, and retention priorities.",
                    "Introduced a manager toolkit and coaching programme that improved confidence in performance conversations.",
                    "Created a clear employee-listening process and translated recurring themes into measurable action plans.",
                    "Simplified onboarding workflows and reduced manual administration across the employee lifecycle.",
                    "Partnered with leaders on organisation design, role clarity, and change communication.",
                ),
            ),
            ExperienceEntry(
                role = "Employee Experience Partner",
                company = "Brightside Labs",
                period = "Mar 2020 - Dec 2022",
                highlights = listOf(
                    "Designed employee journeys for onboarding, internal mobility, and returning from extended leave.",
                    "Facilitated workshops on feedback, team agreements, and inclusive meeting practices.",
                    "Maintained people dashboards and prepared monthly insights for leadership reviews.",
                    "Supported policy updates with plain-language guidance for employees and managers.",
                    "Coordinated engagement initiatives across hybrid teams in three regions.",
                ),
            ),
            ExperienceEntry(
                role = "Talent Coordinator",
                company = "Cedar Street Group",
                period = "Jun 2018 - Feb 2020",
                highlights = listOf(
                    "Coordinated interview scheduling, candidate communication, and offer documentation.",
                    "Created weekly recruiting reports that highlighted pipeline health and hiring bottlenecks.",
                    "Improved candidate templates and interview guidance for a more consistent experience.",
                    "Supported university outreach and early-career hiring events.",
                ),
            ),
        ),
        education = listOf(
            EducationEntry(
                qualification = "Bachelor of Business Administration",
                institution = "Example State University",
                period = "2014 - 2018",
                details = "Concentration in Human Resource Management",
            ),
            EducationEntry(
                qualification = "Certificate in People Analytics",
                institution = "Sample Learning Institute",
                period = "2021",
                details = "",
            ),
        ),
        references = listOf(
            ReferenceEntry(
                name = "Riley Example",
                company = "Northstar Works",
                phone = "+1 202 555 0198",
                email = "riley.example@example.com",
            ),
            ReferenceEntry(
                name = "Morgan Sample",
                company = "Brightside Labs",
                phone = "+1 202 555 0164",
                email = "morgan.sample@example.com",
            ),
        ),
        additionalSections = listOf(
            ResumeAdditionalSection(
                title = "Certifications",
                items = listOf("Certificate in People Analytics — Sample Learning Institute, 2021"),
            ),
            ResumeAdditionalSection(
                title = "Languages",
                items = listOf("English — Fluent", "Spanish — Conversational"),
            ),
        ),
        accent = ResumeAccent.ORANGE,
        template = ResumeTemplate.MODERN,
    )
