package com.reset.feature.checkin

import com.reset.model.domain.checkin.NeedState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChipIdTest {

    @Test
    fun `every base chip maps to its documented NeedState`() {
        val expected = mapOf(
            ChipId.STRESSED to NeedState.WOUND_UP,
            ChipId.CANT_FOCUS to NeedState.SCATTERED,
            ChipId.NO_ENERGY to NeedState.DRAINED,
            ChipId.FEELING_LOW to NeedState.LOW_MOOD,
            ChipId.STIFF_ACHY to NeedState.BODY_TENSION,
            ChipId.WRISTS_TIRED to NeedState.HAND_STRAIN,
            ChipId.EYES_TIRED to NeedState.EYE_STRAIN,
            ChipId.OVERTHINKING to NeedState.STUCK_ON_A_THOUGHT,
            ChipId.OVERWHELMED to NeedState.OVERWHELMED,
            ChipId.FEELING_DISCONNECTED to NeedState.DISCONNECTED,
            ChipId.CANT_SWITCH_OFF to NeedState.CANT_WIND_DOWN,
            ChipId.BEEN_SITTING_FOREVER to NeedState.RESTLESS,
        )
        for ((chip, needState) in expected) {
            assertEquals("$chip should map to $needState", needState, chip.needState)
        }
    }

    @Test
    fun `only WOUND_UP and BODY_TENSION chips carry a follow-up`() {
        assertTrue(ChipId.STRESSED.hasFollowUp)
        assertTrue(ChipId.STIFF_ACHY.hasFollowUp)
        assertFalse(ChipId.CANT_FOCUS.hasFollowUp)
        assertFalse(ChipId.WRISTS_TIRED.hasFollowUp)
        assertFalse(ChipId.OVERWHELMED.hasFollowUp)
    }

    @Test
    fun `the default grid is ten chips, zero scroll, with no contextual chips present`() {
        val grid = ChipId.gridFor(isNight = false, longStillness = false)
        assertEquals(10, grid.size)
        assertFalse(ChipId.CANT_SWITCH_OFF in grid)
        assertFalse(ChipId.BEEN_SITTING_FOREVER in grid)
        assertTrue(ChipId.NO_ENERGY in grid)
        assertTrue(ChipId.STIFF_ACHY in grid)
    }

    @Test
    fun `night swaps No energy for Can't switch off, nothing else`() {
        val grid = ChipId.gridFor(isNight = true, longStillness = false)
        assertEquals(10, grid.size)
        assertTrue(ChipId.CANT_SWITCH_OFF in grid)
        assertFalse(ChipId.NO_ENERGY in grid)
        assertTrue(ChipId.STIFF_ACHY in grid)
        assertFalse(ChipId.BEEN_SITTING_FOREVER in grid)
    }

    @Test
    fun `long stillness swaps Stiff achy for Been sitting forever, nothing else`() {
        val grid = ChipId.gridFor(isNight = false, longStillness = true)
        assertEquals(10, grid.size)
        assertTrue(ChipId.BEEN_SITTING_FOREVER in grid)
        assertFalse(ChipId.STIFF_ACHY in grid)
        assertTrue(ChipId.NO_ENERGY in grid)
        assertFalse(ChipId.CANT_SWITCH_OFF in grid)
    }

    @Test
    fun `both contextual swaps can be active at once`() {
        val grid = ChipId.gridFor(isNight = true, longStillness = true)
        assertEquals(10, grid.size)
        assertTrue(ChipId.CANT_SWITCH_OFF in grid)
        assertTrue(ChipId.BEEN_SITTING_FOREVER in grid)
        assertFalse(ChipId.NO_ENERGY in grid)
        assertFalse(ChipId.STIFF_ACHY in grid)
    }

    @Test
    fun `chip identities are twelve, covering every NeedState exactly once`() {
        val allNeedStatesFromChips = ChipId.entries.map { it.needState }.toSet()
        assertEquals(NeedState.entries.toSet(), allNeedStatesFromChips)
        assertEquals(12, ChipId.entries.size)
    }
}
