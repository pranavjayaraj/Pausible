package com.reset.feature.sessions.content

import com.reset.model.domain.checkin.GateOutcome
import com.reset.model.domain.checkin.GateReason
import com.reset.model.domain.checkin.NeedState
import com.reset.model.domain.checkin.SelectionContext
import com.reset.model.domain.checkin.SessionSelection

/**
 * NeedState → one session + one alternate. Pure logic over the catalog: it takes only a
 * [NeedState] and a [SelectionContext] — never a chip id, chip label, or any UI type — which
 * is the whole point of the NeedState seam (see the module's `NeedState.kt` doc). Ranking is
 * fixed per need; [SessionRequirementResolver] gates never re-rank, they only exclude; the
 * one-hour freshness rule is a *soft* tiebreak layered on top of that, not a gate.
 */
object SessionSelector {

    /** The ranked shortlist per need: 1st choice first, then successive fallbacks. */
    private val SHORTLIST: Map<NeedState, List<SessionScript>> = mapOf(
        NeedState.BODY_TENSION to listOf(SessionScripts.THE_UNFOLD, SessionScripts.UNCLENCH, SessionScripts.THE_LOOP),
        NeedState.HAND_STRAIN to listOf(SessionScripts.WRISTS_AND_HANDS, SessionScripts.THE_UNFOLD),
        NeedState.EYE_STRAIN to listOf(SessionScripts.HORIZON, SessionScripts.FERN),
        NeedState.WOUND_UP to listOf(SessionScripts.THE_SIGH, SessionScripts.UNCLENCH, SessionScripts.THE_SETTLE),
        NeedState.SCATTERED to listOf(SessionScripts.THE_SETTLE, SessionScripts.THE_SIGH, SessionScripts.FERN),
        NeedState.DRAINED to listOf(SessionScripts.THE_CLIMB, SessionScripts.THE_LOOP, SessionScripts.STEP_OUTSIDE),
        NeedState.LOW_MOOD to listOf(SessionScripts.WARMTH, SessionScripts.THREE_GOOD_THINGS, SessionScripts.THE_LOOP),
        NeedState.STUCK_ON_A_THOUGHT to listOf(SessionScripts.MINI_UNPACK, SessionScripts.THE_SETTLE),
        NeedState.RESTLESS to listOf(SessionScripts.THE_LOOP, SessionScripts.STEP_OUTSIDE, SessionScripts.THE_CLIMB),
        NeedState.CANT_WIND_DOWN to listOf(SessionScripts.EMBER, SessionScripts.THE_SWEEP),
        NeedState.OVERWHELMED to listOf(SessionScripts.THE_PLUNGE, SessionScripts.THE_SIGH),
        NeedState.DISCONNECTED to listOf(SessionScripts.REACH_OUT, SessionScripts.WARMTH, SessionScripts.THREE_GOOD_THINGS),
        NeedState.CONFUSED_LOST to listOf(SessionScripts.THE_DETOUR, SessionScripts.STEP_OUTSIDE, SessionScripts.THE_SETTLE),
        NeedState.TASK_PARALYSIS to listOf(SessionScripts.THE_FIRST_MINUTE, SessionScripts.THE_SETTLE, SessionScripts.THE_SIGH),
        NeedState.SENSORY_OVERLOAD to listOf(SessionScripts.HEAD_DOWN, SessionScripts.THE_SWEEP, SessionScripts.FERN),
        // Water first; if there's no tap, movement and daylight are the next-best things for
        // a body that's been sitting hungry — neither pretends to be the actual fix.
        NeedState.BIOLOGICAL_DEPLETION to listOf(SessionScripts.TOP_UP, SessionScripts.STEP_OUTSIDE, SessionScripts.THE_LOOP),
        NeedState.ACCOMPLISHED_FLOW to listOf(SessionScripts.THE_LOG, SessionScripts.THREE_GOOD_THINGS, SessionScripts.THE_UNFOLD),
    )

    /** Requirement-free, always-servable — the universal fallback so this never returns empty. */
    private val FALLBACK = SessionScripts.FERN

    private const val FRESHNESS_WINDOW_MS = 60 * 60 * 1000L

    fun select(needState: NeedState, context: SelectionContext): SessionSelection {
        val ranked = SHORTLIST.getValue(needState)
        val gated = ranked.map { script -> script to SessionRequirementResolver.gate(script, context) }
        val hardSurvivors = gated.filter { (_, reason) -> reason == null }.map { (script, _) -> script }

        if (hardSurvivors.isEmpty()) {
            return fallbackSelection(gated)
        }

        // Freshness: drop a survivor completed within the hour under a DIFFERENT need — but
        // never let this empty the pool. A repeated identical complaint ("my neck hurts"
        // twice) always re-offers the same direct match; stated need overrides staleness.
        val fresh = hardSurvivors.filterNot { script -> wasRecentlyCompletedElsewhere(script, needState, context) }
        val ordered = fresh.ifEmpty { hardSurvivors }
        val demoted = hardSurvivors - ordered.toSet()

        val primary = ordered.first()
        val alternate = ordered.getOrNull(1) ?: FALLBACK

        val shortlist = gated.map { (script, reason) ->
            val effectiveReason = reason ?: if (script in demoted) GateReason.RECENTLY_COMPLETED else null
            GateOutcome(script.id, effectiveReason)
        }
        return SessionSelection(primary.id, alternate.id, shortlist)
    }

    private fun fallbackSelection(gated: List<Pair<SessionScript, GateReason?>>): SessionSelection {
        val shortlist = gated.map { (script, reason) -> GateOutcome(script.id, reason) } +
            GateOutcome(FALLBACK.id, null)
        return SessionSelection(FALLBACK.id, FALLBACK.id, shortlist)
    }

    private fun wasRecentlyCompletedElsewhere(
        script: SessionScript,
        needState: NeedState,
        context: SelectionContext,
    ): Boolean {
        val lastCompletion = context.recentCompletions
            .filter { it.scriptId == script.id }
            .maxByOrNull { it.completedAtMs } ?: return false
        val withinWindow = context.nowMillis - lastCompletion.completedAtMs < FRESHNESS_WINDOW_MS
        return withinWindow && lastCompletion.needState != needState
    }
}
