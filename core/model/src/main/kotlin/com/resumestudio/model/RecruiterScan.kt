package com.resumestudio.model

/**
 * The seven-second scan, mirroring `RecruiterScanService.swift`.
 *
 * The premise is the eye-tracking study the iOS file cites: a recruiter's first
 * pass over a résumé lasts about 7.4 seconds, and the score is not a rating of
 * the career — it is **how much of those seconds land on real information**.
 * Everything here is matching against the document, so it runs on device with
 * no AI credits and nothing leaves the phone.
 */
object RecruiterScan {

    /** The length of the first pass, from the study. */
    const val SCAN_SECONDS = 7.4

    /**
     * How hard the scan marks.
     *
     * The checks are the same at every level; only the bar each one has to
     * clear moves — how much proof the first bullet needs, whether prose is
     * forgiven, whether dates are required everywhere.
     */
    enum class Strictness(val title: String, val detail: String) {
        LOW("Low", "A generous first read — forgiving of an early career and prose bullets."),
        MEDIUM("Medium", "The study's averages, as measured. The default."),
        HIGH("High", "A competitive senior screen: numbers up front, dates everywhere, no dead space."),
    }

    enum class Severity { PASS, WARNING, ACTION }

    data class Finding(
        val id: String,
        val title: String,
        val detail: String,
        /** Seconds of the pass this element is given by the study. */
        val gazeSeconds: Double,
        val severity: Severity,
        val points: Int,
        val maxPoints: Int,
        val section: ResumeSection? = null,
    )

    data class Report(
        val findings: List<Finding>,
        val capturedFacts: List<String>,
        val missedFacts: List<String>,
        val strictness: Strictness,
    ) {
        val score: Int
            get() {
                val maximum = findings.sumOf { it.maxPoints }
                if (maximum == 0) return 0
                return Math.round(findings.sumOf { it.points }.toDouble() / maximum * 100).toInt()
            }

        val verdict: String
            get() = when {
                score >= 85 -> "Survives the first pass"
                score >= 65 -> "Scan-ready, with gaps"
                score >= 40 -> "The scan slips through"
                else -> "Invisible in seven seconds"
            }

        /** Seconds of the pass that land on something a reader can use. */
        val usefulSeconds: Double
            get() = findings.filter { it.severity != Severity.ACTION }.sumOf { it.gazeSeconds }
    }

    /** A bullet carries proof when it carries a figure a reader can weigh. */
    fun isQuantified(bullet: String): Boolean =
        bullet.contains('%') || bullet.any { it.isDigit() }

    fun analyze(
        document: ResumeDocument,
        strictness: Strictness = Strictness.MEDIUM,
    ): Report {
        val findings = mutableListOf<Finding>()
        val captured = mutableListOf<String>()
        val missed = mutableListOf<String>()

        val current = document.experience.firstOrNull()
        val previous = document.experience.getOrNull(1)

        // 1. Identity — the first thing the eye lands on, and the only element
        //    the study finds is read every single time.
        val hasName = document.personal.fullName.isNotBlank()
        val hasHeadline = document.personal.headline.isNotBlank()
        findings += Finding(
            id = "identity",
            title = "Name and headline",
            detail = when {
                hasName && hasHeadline -> "Both are there and read in one glance."
                hasName -> "The name reads, but nothing says what you do."
                else -> "There is no name at the top of the page."
            },
            gazeSeconds = 1.1,
            severity = when {
                hasName && hasHeadline -> Severity.PASS
                hasName -> Severity.WARNING
                else -> Severity.ACTION
            },
            points = if (hasName && hasHeadline) 14 else if (hasName) 7 else 0,
            maxPoints = 14,
            section = ResumeSection.PERSONAL,
        )
        if (hasName) captured += document.personal.fullName else missed += "Your name"
        if (hasHeadline) captured += document.personal.headline else missed += "What you do"

        // 2. Current role — title and company, together. A title with no
        //    employer is the commonest way this second lands on nothing.
        val hasRole = current?.role?.isNotBlank() == true
        val hasCompany = current?.company?.isNotBlank() == true
        findings += Finding(
            id = "current-role",
            title = "Current title and company",
            detail = when {
                hasRole && hasCompany -> "The most recent position is legible immediately."
                hasRole -> "A title with no employer beside it."
                else -> "No current position to read."
            },
            gazeSeconds = 1.3,
            severity = when {
                hasRole && hasCompany -> Severity.PASS
                hasRole -> Severity.WARNING
                else -> Severity.ACTION
            },
            points = if (hasRole && hasCompany) 16 else if (hasRole) 8 else 0,
            maxPoints = 16,
            section = ResumeSection.EXPERIENCE,
        )
        if (hasRole && hasCompany) captured += "${current?.role} at ${current?.company}"
        else missed += "Where you work now"

        // 3. Dates on the current role — the recruiter is checking you are
        //    available, before anything else about the job itself.
        val hasDates = current?.period?.isNotBlank() == true
        findings += Finding(
            id = "current-dates",
            title = "Dates on the current role",
            detail = if (hasDates) "The reader can tell when this started." else
                "Without dates the reader cannot tell if this is current.",
            gazeSeconds = 0.7,
            severity = if (hasDates) Severity.PASS else Severity.ACTION,
            points = if (hasDates) 9 else 0,
            maxPoints = 9,
            section = ResumeSection.EXPERIENCE,
        )
        if (hasDates) captured += current!!.period else missed += "When you started"

        // 4. Trajectory — one previous role is enough to show direction.
        val hasPrevious = previous?.role?.isNotBlank() == true
        val previousDated = previous?.period?.isNotBlank() == true
        findings += Finding(
            id = "trajectory",
            title = "Previous role and dates",
            detail = when {
                hasPrevious && previousDated -> "There is a direction to read, not just a position."
                hasPrevious -> "A previous role with no dates beside it."
                else -> "Nothing before the current role, so there is no trajectory."
            },
            gazeSeconds = 1.0,
            severity = when {
                hasPrevious && previousDated -> Severity.PASS
                hasPrevious -> Severity.WARNING
                // An early career is a fact, not a fault: only the strict read
                // treats a single role as a failure.
                strictness == Strictness.HIGH -> Severity.ACTION
                else -> Severity.WARNING
            },
            points = if (hasPrevious && previousDated) 12 else if (hasPrevious) 6 else 0,
            maxPoints = 12,
            section = ResumeSection.EXPERIENCE,
        )
        if (hasPrevious) captured += previous!!.role else missed += "Where you were before"

        // 5. Proof in the first bullet — the bar that moves most with strictness.
        val firstBullet = current?.highlights?.firstOrNull { it.isNotBlank() }
        val quantified = firstBullet?.let(::isQuantified) == true
        val requiresFigure = strictness != Strictness.LOW
        findings += Finding(
            id = "evidence",
            title = "Proof in the first bullet",
            detail = when {
                quantified -> "The opening bullet carries a figure a reader can weigh."
                firstBullet != null && !requiresFigure -> "Prose, which this read forgives."
                firstBullet != null -> "The opening bullet makes a claim with nothing behind it."
                else -> "There is no bullet under the current role."
            },
            gazeSeconds = 1.4,
            severity = when {
                quantified -> Severity.PASS
                firstBullet != null && !requiresFigure -> Severity.PASS
                firstBullet != null -> Severity.WARNING
                else -> Severity.ACTION
            },
            points = when {
                quantified -> 17
                firstBullet != null && !requiresFigure -> 17
                firstBullet != null -> 8
                else -> 0
            },
            maxPoints = 17,
            section = ResumeSection.EXPERIENCE,
        )
        if (quantified) captured += firstBullet!! else missed += "A number in your first bullet"

        // 6. Education — a glance, not a read, unless it is missing entirely.
        val hasEducation = document.education.any {
            it.qualification.isNotBlank() || it.institution.isNotBlank()
        }
        findings += Finding(
            id = "education",
            title = "Education check",
            detail = if (hasEducation) "Present, which is all this glance is for." else
                "Nothing to glance at where education is expected.",
            gazeSeconds = 0.6,
            severity = if (hasEducation) Severity.PASS else Severity.WARNING,
            points = if (hasEducation) 8 else 0,
            maxPoints = 8,
            section = ResumeSection.EDUCATION,
        )
        if (hasEducation) captured += document.education.first().qualification
        else missed += "Your qualification"

        // 7. Profile skim — a summary is read only if it is short enough to
        //    skim. Past roughly 55 words the eye leaves before the end.
        val words = document.professionalProfile.split(Regex("\\s+")).count { it.isNotBlank() }
        val skimLimit = when (strictness) {
            Strictness.LOW -> 80
            Strictness.MEDIUM -> 65
            Strictness.HIGH -> 55
        }
        findings += Finding(
            id = "profile-skim",
            title = "Profile skims in one sweep",
            detail = when {
                words == 0 -> "No summary, so this second goes to whatever is next."
                words <= skimLimit -> "$words words — short enough to be taken in."
                else -> "$words words. Past about $skimLimit the eye leaves before the end."
            },
            gazeSeconds = 0.9,
            severity = when {
                words == 0 -> Severity.WARNING
                words <= skimLimit -> Severity.PASS
                else -> Severity.WARNING
            },
            points = if (words in 1..skimLimit) 11 else 0,
            maxPoints = 11,
            section = ResumeSection.PROFILE,
        )
        if (words in 1..skimLimit) captured += "Your summary" else missed += "A summary that skims"

        // 8. The fact rail — a two-column template gives the scannable facts a
        //    column of their own, which is what the study says the eye hunts for.
        val plan = document.template.plan
        findings += Finding(
            id = "facts-rail",
            title = "A rail built for the fact-hunt",
            detail = if (plan.hasSideColumn) {
                "The template keeps contact and skills in a column the eye can hunt down."
            } else {
                "A single column, so the facts are found by reading rather than scanning."
            },
            gazeSeconds = 0.4,
            severity = if (plan.hasSideColumn) Severity.PASS else Severity.WARNING,
            points = if (plan.hasSideColumn) 5 else 2,
            maxPoints = 5,
        )

        return Report(findings, captured.filter { it.isNotBlank() }, missed, strictness)
    }
}

/** The sections a finding can point at, so the report can open the right editor. */
enum class ResumeSection { PERSONAL, PROFILE, COMPETENCIES, EXPERIENCE, EDUCATION, REFERENCES }
