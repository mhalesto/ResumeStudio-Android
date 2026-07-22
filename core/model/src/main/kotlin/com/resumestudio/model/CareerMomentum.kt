package com.resumestudio.model

/**
 * The weekly campaign, mirroring `CareerMomentumService.swift`.
 *
 * The score is not a rating of the person. It is progress against goals they
 * set themselves, which is why the points are fixed per pillar rather than
 * weighted by anything the app infers — see the disclaimer iOS prints under the
 * missions.
 */
enum class CareerMomentumPillar(val title: String, val unit: String) {
    OPPORTUNITIES("Opportunities", "captured this week"),
    RELATIONSHIPS("Relationships", "people contacted"),
    PRACTICE("Practice", "sessions run"),
    ;

    /** What the pillar is worth when its goal is met. The three sum to 100. */
    val availablePoints: Int
        get() = when (this) {
            OPPORTUNITIES -> 40
            RELATIONSHIPS -> 35
            PRACTICE -> 25
        }
}

data class CareerMomentumMission(
    val pillar: CareerMomentumPillar,
    val value: Int,
    val goal: Int,
) {
    val cappedValue: Int get() = minOf(value, goal)
    val remaining: Int get() = maxOf(0, goal - value)
    val isComplete: Boolean get() = value >= goal
    val progress: Float get() = cappedValue.toFloat() / maxOf(goal, 1)
    val earnedPoints: Int get() = Math.round(progress * pillar.availablePoints)
}

enum class CareerMomentumTier(val title: String) {
    READY("Ready when you are"),
    MOVING("This week is moving"),
    BUILDING("Building momentum"),
    STRONG("Strong momentum"),
    COMPLETE("Weekly goals complete"),
}

data class CareerMomentumSnapshot(
    val missions: List<CareerMomentumMission>,
    val rhythmWeeks: Int = 0,
) {
    val score: Int get() = minOf(100, missions.sumOf { it.earnedPoints })
    val completedMissions: Int get() = missions.count { it.isComplete }
    val activePillars: Int get() = missions.count { it.value > 0 }
    val isComplete: Boolean get() = missions.isNotEmpty() && completedMissions == missions.size

    val tier: CareerMomentumTier
        get() = when {
            score == 100 -> CareerMomentumTier.COMPLETE
            score >= 70 -> CareerMomentumTier.STRONG
            score >= 35 -> CareerMomentumTier.BUILDING
            score >= 1 -> CareerMomentumTier.MOVING
            else -> CareerMomentumTier.READY
        }

    /** The mission worth charging next: the one with the most left to gain. */
    val nextMission: CareerMomentumMission?
        get() = missions.filterNot { it.isComplete }
            .maxByOrNull { it.pillar.availablePoints - it.earnedPoints }

    val reachedMilestone: Int get() = MILESTONES.lastOrNull { score >= it } ?: 0
    val nextMilestone: Int? get() = MILESTONES.firstOrNull { score < it }
    val pointsToNextMilestone: Int get() = maxOf(0, (nextMilestone ?: score) - score)

    companion object {
        val MILESTONES = listOf(25, 50, 75, 100)

        /** The iOS default goals, until the campaign settings are ported. */
        fun empty(
            opportunityGoal: Int = 4,
            relationshipGoal: Int = 3,
            practiceGoal: Int = 1,
        ) = CareerMomentumSnapshot(
            missions = listOf(
                CareerMomentumMission(CareerMomentumPillar.OPPORTUNITIES, 0, maxOf(opportunityGoal, 1)),
                CareerMomentumMission(CareerMomentumPillar.RELATIONSHIPS, 0, maxOf(relationshipGoal, 1)),
                CareerMomentumMission(CareerMomentumPillar.PRACTICE, 0, maxOf(practiceGoal, 1)),
            ),
        )
    }
}
