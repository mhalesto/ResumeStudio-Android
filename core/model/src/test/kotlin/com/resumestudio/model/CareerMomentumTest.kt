package com.resumestudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scoring rules, which are the part of the campaign a user would notice
 * being wrong: a tier that reads "Strong momentum" at 20 points, or a card
 * claiming 100 when a mission is unfinished.
 */
class CareerMomentumTest {

    private fun snapshot(opportunities: Int, relationships: Int, practice: Int) =
        CareerMomentumSnapshot(
            missions = listOf(
                CareerMomentumMission(CareerMomentumPillar.OPPORTUNITIES, opportunities, 4),
                CareerMomentumMission(CareerMomentumPillar.RELATIONSHIPS, relationships, 3),
                CareerMomentumMission(CareerMomentumPillar.PRACTICE, practice, 1),
            ),
        )

    @Test
    fun `the three pillars are worth exactly a hundred between them`() {
        assertEquals(100, CareerMomentumPillar.entries.sumOf { it.availablePoints })
    }

    @Test
    fun `an untouched week scores zero and reads as ready`() {
        val empty = CareerMomentumSnapshot.empty()
        assertEquals(0, empty.score)
        assertEquals(CareerMomentumTier.READY, empty.tier)
        assertFalse(empty.isComplete)
        assertEquals(0, empty.activePillars)
    }

    @Test
    fun `every goal met scores a hundred and completes`() {
        val full = snapshot(4, 3, 1)
        assertEquals(100, full.score)
        assertEquals(CareerMomentumTier.COMPLETE, full.tier)
        assertTrue(full.isComplete)
        assertNull(full.nextMission)
        assertNull(full.nextMilestone)
    }

    @Test
    fun `overshooting a goal cannot push the score past a hundred`() {
        // Somebody having a very good week must not produce 140/100.
        val over = snapshot(40, 30, 10)
        assertEquals(100, over.score)
        assertEquals(4, over.missions[0].cappedValue)
    }

    @Test
    fun `the tier thresholds are the ones iOS uses`() {
        assertEquals(CareerMomentumTier.READY, snapshot(0, 0, 0).tier)      // 0
        assertEquals(CareerMomentumTier.MOVING, snapshot(1, 0, 0).tier)      // 10
        assertEquals(CareerMomentumTier.MOVING, snapshot(2, 1, 0).tier)      // 32, still short of 35
        assertEquals(CareerMomentumTier.BUILDING, snapshot(3, 1, 0).tier)    // 42
        assertEquals(CareerMomentumTier.BUILDING, snapshot(3, 3, 0).tier)    // 65, still short of 70
        assertEquals(CareerMomentumTier.STRONG, snapshot(4, 3, 0).tier)      // 75
        assertEquals(CareerMomentumTier.COMPLETE, snapshot(4, 3, 1).tier)    // 100
    }

    @Test
    fun `points are earned in proportion to the pillar's weight`() {
        // Opportunities is worth 40; half its goal is 20 points.
        assertEquals(20, snapshot(2, 0, 0).score)
        // Relationships is worth 35; all of it is 35.
        assertEquals(35, snapshot(0, 3, 0).score)
        // Practice is worth 25.
        assertEquals(25, snapshot(0, 0, 1).score)
    }

    @Test
    fun `the next charge is the mission with the most still to gain`() {
        // Relationships done, practice done: opportunities has 40 left.
        val next = snapshot(0, 3, 1).nextMission
        assertEquals(CareerMomentumPillar.OPPORTUNITIES, next?.pillar)
    }

    @Test
    fun `milestones count up and report what is left to the next one`() {
        val moving = snapshot(2, 0, 0)   // 20 points
        assertEquals(0, moving.reachedMilestone)
        assertEquals(25, moving.nextMilestone)
        assertEquals(5, moving.pointsToNextMilestone)

        val building = snapshot(0, 3, 1) // 60 points
        assertEquals(50, building.reachedMilestone)
        assertEquals(75, building.nextMilestone)
    }

    @Test
    fun `a goal of zero cannot divide by zero`() {
        val odd = CareerMomentumSnapshot(
            missions = listOf(CareerMomentumMission(CareerMomentumPillar.PRACTICE, 0, 0)),
        )
        assertEquals(0, odd.score)
        assertTrue(odd.missions.first().isComplete)
    }
}
