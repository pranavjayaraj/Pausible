package com.reset.feature.sessions

import androidx.compose.runtime.Stable
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.content.SessionScript

/**
 * Immutable UI state for the full-screen session experience (SessionDestination): the
 * focus countdown, its optional warm-up breaths, and the catalog-driven break player.
 * [step] is the current sub-screen — an intra-feature transition; leaving the experience
 * goes through the injected Navigator. Build new state only with [getDefault] + `copy`.
 */
@Stable
data class SessionState(
    val step: SessionStep = SessionStep.Focus,
    /** The launch mode, from the destination args. */
    val mode: String = SessionDestination.MODE_FOCUS,
    /** Soundscape key theming the focus backdrop; null = the default teal. */
    val soundKey: String? = null,
    val gong: Boolean = true,
    val focus: FocusState = FocusState.getDefault(),
    val warmup: WarmupState = WarmupState.getDefault(),
    val breakPlayer: BreakPlayerState = BreakPlayerState.getDefault(),
) {
    companion object {
        fun getDefault() = SessionState()
    }
}

/** Which session sub-screen is showing. */
sealed interface SessionStep {
    /** The countdown ring (MODE_FOCUS). */
    data object Focus : SessionStep

    /** The warm-up breathing pulse before a custom focus sit (MODE_FOCUS only). */
    data object Warmup : SessionStep

    /** The catalog three-act player (MODE_BREAK) — [BreakPlayerState.act] carries the step. */
    data object Break : SessionStep
}

/** The active focus countdown. */
@Stable
data class FocusState(
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val running: Boolean = false,
) {
    companion object {
        fun getDefault() = FocusState()
    }
}

/** The warm-up breathing pulse ahead of a custom focus sit: phases alternate on a fixed clock. */
@Stable
data class WarmupState(
    val inhale: Boolean = true,
    /** Completed phase ticks; two ticks make one cycle. */
    val ticks: Int = 0,
    val totalTicks: Int = SessionConstants.WARMUP_BREATH_TICKS,
    val phaseDurationMs: Int = SessionConstants.BREATH_PHASE_MS,
) {
    val completedCycles: Int get() = ticks / 2
    val totalCycles: Int get() = totalTicks / 2

    companion object {
        fun getDefault() = WarmupState()
    }
}

/** Which act of a catalog session is showing. */
sealed interface Act {
    data object Arrival : Act
    data class Guide(val stepIndex: Int) : Act
    data object Landing : Act
}

/**
 * The generic three-act player's state: the resolved [script], which [act] is showing, the
 * honest countdown ring ([remainingSec] of [totalSec] — the script's own summed duration),
 * and the current pool-rotated copy line for whatever the act is showing.
 */
@Stable
data class BreakPlayerState(
    val script: SessionScript? = null,
    val act: Act = Act.Arrival,
    val totalSec: Int = 0,
    val remainingSec: Int = 0,
    /** Deterministic copy-rotation inputs, resolved once at session start. */
    val copySeed: Int = 0,
    val copyCompletedCount: Int = 0,
    /** The line currently on screen for [act] (Arrival line, a Guide step's cue, or the
     *  Landing close/bridge), picked once when the act was entered — not re-picked on
     *  recomposition, so pausing/resuming never changes the line mid-read. */
    val currentLine: String = "",
    /** Landing only: the second, bridge-back line shown under [currentLine]. */
    val landingBridgeLine: String = "",
) {
    companion object {
        fun getDefault() = BreakPlayerState()
    }
}
