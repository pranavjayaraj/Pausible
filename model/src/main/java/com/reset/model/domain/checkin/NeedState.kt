package com.reset.model.domain.checkin

/**
 * What the user says they need, resolved from however they said it (a chip today; text or
 * voice in a future phase — see [InputMethod]). Session selection consumes only this seam:
 * it never reads chip identity, chip label, or any other UI state, so adding a new input
 * method later is a one-function swap onto this enum, not a selection-logic rewrite.
 */
enum class NeedState {
    BODY_TENSION,
    HAND_STRAIN,
    EYE_STRAIN,
    WOUND_UP,
    SCATTERED,
    DRAINED,
    LOW_MOOD,
    STUCK_ON_A_THOUGHT,
    RESTLESS,
    CANT_WIND_DOWN,
    OVERWHELMED,
    DISCONNECTED,

    // ---------------------------------------------------------- text-only needs
    // Below here: needs a person describes but wouldn't hunt for in a ten-chip grid
    // (see [textOnly]). Appended, never reordered — the propensity log and the break
    // preference echo both persist `name`, but PrototypeVectorCache keys its file by
    // `ordinal`, so an insertion in the middle would silently mislabel a cached vector.

    /** Blocked, no idea what the next step is — wants incubation, not calming. */
    CONFUSED_LOST,

    /** Knows the task, can't start it — the threat response to beginning, not overload. */
    TASK_PARALYSIS,

    /** Too much input (noise, light, people, pings) rather than too many tasks. */
    SENSORY_OVERLOAD,

    /** Hasn't eaten, drunk, or stood up — a body problem wearing a focus problem's clothes. */
    BIOLOGICAL_DEPLETION,

    /** The positive case: in flow or just landed a win. The break protects it or logs it. */
    ACCOMPLISHED_FLOW,
    ;

    /**
     * True for needs that only text can reach: the chip grid is a fixed ten (see `ChipId`),
     * and these five are either too situational to scan for or — [ACCOMPLISHED_FLOW] — a state
     * nobody opens a wellness app to *tap*. They still classify, select, and log exactly like
     * chip-reachable needs; the only difference is the input method that can produce them.
     */
    val textOnly: Boolean
        get() = this in TEXT_ONLY

    companion object {
        private val TEXT_ONLY = setOf(
            CONFUSED_LOST, TASK_PARALYSIS, SENSORY_OVERLOAD, BIOLOGICAL_DEPLETION, ACCOMPLISHED_FLOW,
        )
    }
}

/** How a [NeedState] was resolved — logged with every selection for the propensity trail.
 *  [TEXT]/[VOICE] are reserved for a future phase; only [CHIP] writes today. */
enum class InputMethod { CHIP, TEXT, VOICE }

/**
 * Real-world context the selector's gates check against — assembled by the caller (today,
 * the Check In feature) from real preferences/repositories. Never chip identity or UI state.
 */
data class SelectionContext(
    val audioAvailable: Boolean,
    val stairsAvailable: Boolean,
    val moveSpaceAvailable: Boolean,
    val waterAccessAvailable: Boolean,
    val hasClosePerson: Boolean,
    val isNight: Boolean,
    /** Recently completed sessions — the freshness tiebreak's only input. */
    val recentCompletions: List<RecentCompletion> = emptyList(),
    val nowMillis: Long,
)

/** One completed session, for the freshness tiebreak: which script, under which need, when. */
data class RecentCompletion(
    val scriptId: String,
    val needState: NeedState,
    val completedAtMs: Long,
)

/** Why a shortlist candidate didn't win — the propensity trail's "gates applied" field. */
enum class GateReason { REQUIREMENT_UNMET, NIGHT_VETOED, FREQUENCY_CAPPED, RECENTLY_COMPLETED }

/** One shortlist candidate's fate, in ranked order — the propensity trail's raw trace. */
data class GateOutcome(val scriptId: String, val reason: GateReason?)

/**
 * The selector's answer: one session to offer plus one ready alternate, both catalog script
 * ids ([com.reset.feature.sessions.api.SessionScriptIds] keys) — never richer catalog types,
 * so callers outside `:feature:sessions` never see catalog internals. [shortlist] carries the
 * full ranked trace (winner, alternate, and every gated-out candidate) for the propensity log.
 */
data class SessionSelection(
    val primaryScriptId: String,
    val alternateScriptId: String,
    val shortlist: List<GateOutcome>,
)
