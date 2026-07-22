package com.resumestudio.model

/**
 * Audits what a résumé claims against what the same page demonstrates,
 * mirroring `ClaimAuditService.swift`.
 *
 * Entirely on device and free of AI credits: this is matching, not generation.
 * Nothing here is sent anywhere, which matters, because the input is the
 * reader's whole career.
 *
 * The service never rewrites and never contradicts a claim. Its only assertion
 * is about **visibility** — whether support for a line can be found without
 * taking the writer's word for it.
 */
object ClaimAudit {

    /**
     * Competencies the page never demonstrates.
     *
     * Competencies are excluded from the evidence on purpose: a skills list
     * cannot be its own evidence, and treating it as such would hide the exact
     * failure this is here to surface.
     */
    fun unsupportedCompetencies(document: ResumeDocument): List<String> {
        val demonstrations = buildList {
            document.experience.forEach { entry ->
                add(entry.role)
                addAll(entry.highlights)
            }
            document.education.forEach { add("${it.qualification} ${it.details}") }
            document.additionalSections.forEach { addAll(it.items) }
        }

        return document.competencies
            .filter { it.isNotBlank() }
            .filterNot { claim -> demonstrations.any { demonstrates(claim, it) } }
    }

    /** Whether every meaningful word of [claim] shows up somewhere in [text]. */
    fun demonstrates(claim: String, text: String): Boolean {
        val claimStems = stems(claim).toSet()
        if (claimStems.isEmpty()) return false
        val textStems = stems(text)
        return claimStems.all { stem -> textStems.any { matches(it, stem) } }
    }

    /**
     * Reduces a word to a comparable root.
     *
     * A five-character prefix, after mapping the irregular verbs a résumé leans
     * on. Deliberately blunt rather than a real stemmer: it converges
     * manage/managed/management/manager, negotiate/negotiation and
     * analyse/analysis/analytical, which is most of the vocabulary in play. It
     * will occasionally collide two unrelated words — the cost is one claim
     * reported as supported when it is not, which is why everything built on
     * this stays advisory.
     */
    fun stem(word: String): String {
        val normalized = word.lowercase().filter { it.isLetter() }
        // The irregular map runs before any length test: "led" is three
        // characters and would otherwise be discarded as noise before it could
        // become "lead", losing the most common leadership evidence there is.
        val root = IRREGULARS[normalized] ?: normalized
        return if (root.length > 3) root.take(5) else root
    }

    private fun matches(lhs: String, rhs: String): Boolean =
        lhs.length >= 4 && rhs.length >= 4 && (lhs.startsWith(rhs) || rhs.startsWith(lhs))

    private fun stems(text: String): List<String> =
        text.lowercase()
            .split(*NON_LETTERS)
            .filter { it.isNotEmpty() && it !in STOP_WORDS }
            .map(::stem)
            // Length is judged on the stem, so short irregulars survive while
            // genuine noise ("the", "and") still drops out.
            .filter { it.length > 3 }

    private val NON_LETTERS: CharArray =
        (0..127).map { it.toChar() }.filterNot { it.isLetter() }.toCharArray()

    /**
     * Past tenses no prefix rule reaches, limited to verbs that actually carry
     * weight on a résumé.
     */
    private val IRREGULARS = mapOf(
        "led" to "lead", "built" to "build", "grew" to "grow", "ran" to "run",
        "wrote" to "write", "drove" to "drive", "won" to "win", "sold" to "sell",
        "spoke" to "speak", "taught" to "teach", "brought" to "bring", "made" to "make",
        "met" to "meet", "held" to "hold", "took" to "take", "gave" to "give",
        "began" to "begin", "chose" to "choose", "spent" to "spend", "sent" to "send",
        "rebuilt" to "build", "oversaw" to "oversee", "shrank" to "shrink",
    )

    private val STOP_WORDS = setOf(
        "with", "from", "that", "this", "were", "have", "into", "their", "them",
        "then", "than", "these", "those", "which", "while", "about", "across",
        "after", "also", "been", "being", "both", "each", "more", "most", "other",
        "over", "such", "through", "under", "using", "within", "would", "your",
    )
}
