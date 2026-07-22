package com.resumestudio.model

/**
 * The ranked queue behind "What should I do next?", mirroring `TodayAction`
 * and `TodayActionPriority`.
 *
 * The raw values are the iOS ones and the order matters: what the screen is
 * for is answering the question with one card, so the sort has to agree across
 * platforms or the two apps recommend different next moves from the same data.
 */
enum class TodayActionPriority(val rank: Int) {
    CAMPAIGN(20),
    RESUME_READINESS(60),
    SMART_LINK(70),
    APPLICATION(80),
    DUE_FOLLOW_UP(90),
    OUTCOME_REVIEW(92),
    OPPORTUNITY_SAFETY(94),
    IMMINENT_INTERVIEW(95),
    EXPIRING_HOSTED_WORK(100),
}

/** Where a Today card sends you. Only the destinations Android has so far. */
enum class TodayRoute { EDITOR, GALLERY, PREVIEW, CLAIMS, MOMENTUM, APPLICATIONS }

data class TodayAction(
    val id: String,
    val title: String,
    val detail: String,
    val route: TodayRoute,
    val priority: TodayActionPriority,
)

/**
 * Builds the queue from what this platform can actually see.
 *
 * iOS draws on seventeen sources; most need stores Android has not ported —
 * applications, contacts, interviews, hosted links. The three below are derived
 * from the résumé and the campaign alone, so they are real rather than
 * placeholders. The rest arrive with the stores behind them.
 */
object TodayActions {

    fun build(
        document: ResumeDocument,
        momentum: CareerMomentumSnapshot,
    ): List<TodayAction> = buildList {
        val incomplete = incompleteSections(document)
        if (incomplete.isNotEmpty()) {
            add(
                TodayAction(
                    id = "resume-readiness",
                    title = "Finish your ${incomplete.first()}",
                    detail = if (incomplete.size == 1) {
                        "One section is still empty. A recruiter reads it before anything else."
                    } else {
                        "${incomplete.size} sections are still empty, starting with ${incomplete.first()}."
                    },
                    route = TodayRoute.EDITOR,
                    priority = TodayActionPriority.RESUME_READINESS,
                ),
            )
        }

        val unsupported = ClaimAudit.unsupportedCompetencies(document)
        if (unsupported.isNotEmpty()) {
            add(
                TodayAction(
                    id = "claim-audit",
                    title = "Back up what your résumé claims",
                    detail = if (unsupported.size == 1) {
                        "\"${unsupported.first()}\" is listed but no bullet demonstrates it."
                    } else {
                        "${unsupported.size} skills are listed that no bullet demonstrates."
                    },
                    route = TodayRoute.CLAIMS,
                    priority = TodayActionPriority.APPLICATION,
                ),
            )
        }

        momentum.nextMission?.let { next ->
            add(
                TodayAction(
                    id = "campaign-${next.pillar.name.lowercase()}",
                    title = when (next.pillar) {
                        CareerMomentumPillar.OPPORTUNITIES -> "Move your weekly campaign forward"
                        CareerMomentumPillar.RELATIONSHIPS -> "Strengthen one career relationship"
                        CareerMomentumPillar.PRACTICE -> "Run one practice session"
                    },
                    // Written per pillar rather than assembled from the unit
                    // string: "4 captured left this week" is what you get when
                    // a label meant for a progress row is reused as a sentence.
                    detail = when (next.pillar) {
                        CareerMomentumPillar.OPPORTUNITIES ->
                            "Capture or progress ${next.remaining} more " +
                                if (next.remaining == 1) "opportunity this week." else "opportunities this week."
                        CareerMomentumPillar.RELATIONSHIPS ->
                            "You have ${next.remaining} networking touchpoint" +
                                (if (next.remaining == 1) "" else "s") + " left this week."
                        CareerMomentumPillar.PRACTICE ->
                            "One practice session would finish this week's goal."
                    },
                    route = TodayRoute.MOMENTUM,
                    priority = TodayActionPriority.CAMPAIGN,
                ),
            )
        }
    }.sortedByDescending { it.priority.rank }

    /** The sections a recruiter expects to find, in the order the editor shows them. */
    private fun incompleteSections(document: ResumeDocument): List<String> = buildList {
        if (document.personal.fullName.isBlank() || document.personal.email.isBlank()) add("personal details")
        if (document.professionalProfile.isBlank()) add("professional profile")
        if (document.competencies.none { it.isNotBlank() }) add("core competencies")
        if (document.experience.none { it.role.isNotBlank() || it.company.isNotBlank() }) add("experience")
        if (document.education.none { it.qualification.isNotBlank() || it.institution.isNotBlank() }) add("education")
        if (document.references.none { it.name.isNotBlank() }) add("references")
    }
}
