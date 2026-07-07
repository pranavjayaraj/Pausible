package com.reset.feature.sessions

import androidx.compose.runtime.Stable
import com.reset.feature.sessions.api.SessionDestination

/**
 * Immutable UI state for the full-screen session experience (SessionDestination): the
 * focus countdown and the guided-breathing pulse. [step] is the current sub-screen — an
 * intra-feature transition; leaving the experience goes through the injected Navigator.
 * Build new state only with [getDefault] + `copy`.
 */
@Stable
data class SessionState(
    val step: SessionStep = SessionStep.Focus,
    /** The launch mode, from the destination args. */
    val mode: String = SessionDestination.MODE_FOCUS,
    /** SessionDestination.KIND_* for break mode; null for a plain focus session. */
    val breakKind: String? = null,
    /** Soundscape key theming the focus backdrop; null = the default teal. */
    val soundKey: String? = null,
    val gong: Boolean = true,
    val focus: FocusState = FocusState.getDefault(),
    val breathing: BreathingState = BreathingState.getDefault(),
) {
    companion object {
        fun getDefault() = SessionState()
    }
}

/** Which session sub-screen is showing. */
sealed interface SessionStep {
    /** The countdown ring. */
    data object Focus : SessionStep

    /** The guided-breathing pulse — a break, or the warm-up before a custom focus. */
    data object Breathing : SessionStep
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

/** The guided breathing pulse: phases alternate inhale/exhale on a fixed clock. */
@Stable
data class BreathingState(
    val inhale: Boolean = true,
    /** Completed phase ticks; two ticks make one cycle. */
    val ticks: Int = 0,
    val totalTicks: Int = SessionConstants.BREAK_BREATH_TICKS,
    val phaseDurationMs: Int = SessionConstants.BREATH_PHASE_MS,
    /** True while breathing is the warm-up that flows into the focus countdown. */
    val isWarmup: Boolean = false,
) {
    val completedCycles: Int get() = ticks / 2
    val totalCycles: Int get() = totalTicks / 2

    companion object {
        fun getDefault() = BreathingState()
    }
}
