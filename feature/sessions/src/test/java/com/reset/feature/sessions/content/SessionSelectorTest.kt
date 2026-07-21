package com.reset.feature.sessions.content

import com.reset.model.domain.checkin.GateReason
import com.reset.model.domain.checkin.NeedState
import com.reset.model.domain.checkin.RecentCompletion
import com.reset.model.domain.checkin.SelectionContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSelectorTest {

    private val t0 = 1_700_000_000_000L

    private fun fullAccess(
        isNight: Boolean = false,
        audio: Boolean = true,
        stairs: Boolean = true,
        moveSpace: Boolean = true,
        waterAccess: Boolean = true,
        closePerson: Boolean = true,
        recentCompletions: List<RecentCompletion> = emptyList(),
        nowMillis: Long = t0,
    ) = SelectionContext(
        audioAvailable = audio,
        stairsAvailable = stairs,
        moveSpaceAvailable = moveSpace,
        waterAccessAvailable = waterAccess,
        hasClosePerson = closePerson,
        isNight = isNight,
        recentCompletions = recentCompletions,
        nowMillis = nowMillis,
    )

    // ------------------------------------------------------------ seam

    @Test
    fun `select takes only NeedState and SelectionContext, no chip or UI type`() {
        val method = SessionSelector::class.java.getMethod(
            "select",
            NeedState::class.java,
            SelectionContext::class.java,
        )
        assertEquals(2, method.parameterTypes.size)
        assertEquals(NeedState::class.java, method.parameterTypes[0])
        assertEquals(SelectionContext::class.java, method.parameterTypes[1])
    }

    // ------------------------------------------------------------ every need resolves

    @Test
    fun `every NeedState resolves to its ranked 1st choice with full access`() {
        val expected = mapOf(
            NeedState.BODY_TENSION to SessionScripts.THE_UNFOLD,
            NeedState.HAND_STRAIN to SessionScripts.WRISTS_AND_HANDS,
            NeedState.EYE_STRAIN to SessionScripts.HORIZON,
            NeedState.WOUND_UP to SessionScripts.THE_SIGH,
            NeedState.SCATTERED to SessionScripts.THE_SETTLE,
            NeedState.DRAINED to SessionScripts.THE_CLIMB,
            NeedState.LOW_MOOD to SessionScripts.WARMTH,
            NeedState.STUCK_ON_A_THOUGHT to SessionScripts.MINI_UNPACK,
            NeedState.RESTLESS to SessionScripts.THE_LOOP,
            NeedState.CANT_WIND_DOWN to SessionScripts.EMBER,
            NeedState.OVERWHELMED to SessionScripts.THE_PLUNGE,
            NeedState.DISCONNECTED to SessionScripts.REACH_OUT,
        )
        for ((need, script) in expected) {
            val selection = SessionSelector.select(need, fullAccess())
            assertEquals("$need should resolve to ${script.id}", script.id, selection.primaryScriptId)
            assertNotEquals("alternate should differ from primary", selection.primaryScriptId, selection.alternateScriptId)
        }
    }

    // ------------------------------------------------------------ requirement gates

    @Test
    fun `an audio-less office filters The Sigh so Unclench serves`() {
        val selection = SessionSelector.select(NeedState.WOUND_UP, fullAccess(audio = false))
        assertEquals(SessionScripts.UNCLENCH.id, selection.primaryScriptId)
        val sighOutcome = selection.shortlist.first { it.scriptId == SessionScripts.THE_SIGH.id }
        assertEquals(GateReason.REQUIREMENT_UNMET, sighOutcome.reason)
    }

    @Test
    fun `OVERWHELMED without water access falls through to The Sigh`() {
        val selection = SessionSelector.select(NeedState.OVERWHELMED, fullAccess(waterAccess = false))
        assertEquals(SessionScripts.THE_SIGH.id, selection.primaryScriptId)
    }

    @Test
    fun `DISCONNECTED without a close person falls through to Warmth`() {
        val selection = SessionSelector.select(NeedState.DISCONNECTED, fullAccess(closePerson = false))
        assertEquals(SessionScripts.WARMTH.id, selection.primaryScriptId)
    }

    // ------------------------------------------------------------ night veto

    @Test
    fun `night vetoes movement so DRAINED serves Step Outside instead of The Climb`() {
        val selection = SessionSelector.select(NeedState.DRAINED, fullAccess(isNight = true))
        assertEquals(SessionScripts.STEP_OUTSIDE.id, selection.primaryScriptId)
        val climbOutcome = selection.shortlist.first { it.scriptId == SessionScripts.THE_CLIMB.id }
        assertEquals(GateReason.NIGHT_VETOED, climbOutcome.reason)
        val loopOutcome = selection.shortlist.first { it.scriptId == SessionScripts.THE_LOOP.id }
        assertEquals(GateReason.NIGHT_VETOED, loopOutcome.reason)
    }

    @Test
    fun `night vetoes The Plunge explicitly even though its modality is calm`() {
        val selection = SessionSelector.select(NeedState.OVERWHELMED, fullAccess(isNight = true))
        assertEquals(SessionScripts.THE_SIGH.id, selection.primaryScriptId)
        val plungeOutcome = selection.shortlist.first { it.scriptId == SessionScripts.THE_PLUNGE.id }
        assertEquals(GateReason.NIGHT_VETOED, plungeOutcome.reason)
    }

    // ------------------------------------------------------------ frequency cap

    @Test
    fun `The Plunge respects its own frequency cap independent of the freshness window`() {
        val recent = listOf(
            RecentCompletion(SessionScripts.THE_PLUNGE.id, NeedState.OVERWHELMED, t0 - 90 * 60_000L), // 1.5h ago
        )
        val selection = SessionSelector.select(NeedState.OVERWHELMED, fullAccess(recentCompletions = recent))
        // Within the 4h cap but outside the 1h freshness window — cap gate, not freshness.
        assertEquals(SessionScripts.THE_SIGH.id, selection.primaryScriptId)
        val plungeOutcome = selection.shortlist.first { it.scriptId == SessionScripts.THE_PLUNGE.id }
        assertEquals(GateReason.FREQUENCY_CAPPED, plungeOutcome.reason)
    }

    // ------------------------------------------------------------ freshness tiebreak

    @Test
    fun `a script completed within the hour under a different need is demoted`() {
        val recent = listOf(
            RecentCompletion(SessionScripts.THE_UNFOLD.id, NeedState.HAND_STRAIN, t0 - 10 * 60_000L),
        )
        val selection = SessionSelector.select(NeedState.BODY_TENSION, fullAccess(recentCompletions = recent))
        assertEquals(SessionScripts.UNCLENCH.id, selection.primaryScriptId)
        val unfoldOutcome = selection.shortlist.first { it.scriptId == SessionScripts.THE_UNFOLD.id }
        assertEquals(GateReason.RECENTLY_COMPLETED, unfoldOutcome.reason)
    }

    @Test
    fun `a repeated identical complaint never denies the direct match twice`() {
        val recent = listOf(
            RecentCompletion(SessionScripts.THE_UNFOLD.id, NeedState.BODY_TENSION, t0 - 10 * 60_000L),
        )
        val selection = SessionSelector.select(NeedState.BODY_TENSION, fullAccess(recentCompletions = recent))
        assertEquals("repeated 'my neck hurts' -> Unfold again is correct", SessionScripts.THE_UNFOLD.id, selection.primaryScriptId)
    }

    @Test
    fun `freshness never demotes the sole survivor`() {
        // CANT_WIND_DOWN's whole chain is Ember/Sweep; strand Ember as the only servable
        // option (Sweep needs audio too, but pretend it's capped) and mark it freshly
        // completed under a different need — it must still be served, not left empty.
        val recent = listOf(
            RecentCompletion(SessionScripts.EMBER.id, NeedState.WOUND_UP, t0 - 5 * 60_000L),
        )
        val context = fullAccess(audio = true, recentCompletions = recent).copy(
            recentCompletions = recent + RecentCompletion(SessionScripts.THE_SWEEP.id, NeedState.CANT_WIND_DOWN, t0 - 30 * 60_000L),
        )
        val selection = SessionSelector.select(NeedState.CANT_WIND_DOWN, context)
        assertNotNull(selection.primaryScriptId)
        assertTrue(selection.primaryScriptId.isNotBlank())
    }

    // ------------------------------------------------------------ never-empty fallback

    @Test
    fun `everything filtered still resolves, to Fern`() {
        // CANT_WIND_DOWN's whole chain requires audio; strip it away.
        val selection = SessionSelector.select(NeedState.CANT_WIND_DOWN, fullAccess(audio = false))
        assertEquals(SessionScripts.FERN.id, selection.primaryScriptId)
        assertEquals(SessionScripts.FERN.id, selection.alternateScriptId)
    }

    @Test
    fun `Fern itself never has requirements, so it is always the safe fallback`() {
        assertTrue(SessionScripts.FERN.requirements.isEmpty())
        assertEquals(false, SessionScripts.FERN.nightVetoed)
        assertEquals(null, SessionScripts.FERN.minHoursBetween)
    }
}
