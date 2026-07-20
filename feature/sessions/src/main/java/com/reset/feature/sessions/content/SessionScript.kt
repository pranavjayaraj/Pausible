package com.reset.feature.sessions.content

import androidx.compose.runtime.Stable

/**
 * The session-content engine's data model. A microbreak session is not code —
 * it is a [SessionScript]: a three-act structure (Arrival → Guide → Landing)
 * whose steps, pacing, and copy are all data. New sessions are new configs,
 * not new screens; the session-variant bandit (future) picks between scripts
 * per user, and A/B copy is just another pool entry.
 *
 * Copy lives here as versioned CONTENT DATA, not in strings.xml — pools are
 * rotated collections with authorial voice, shipped and iterated like the
 * model artifact, and localized later as content packs.
 */

/** One line of guidance with rotating variants. Never let session #40 read like #1. */
@Stable
data class CopyPool(val lines: List<String>) {

    init {
        require(lines.isNotEmpty()) { "a copy pool must have at least one line" }
    }

    /**
     * Deterministic rotation: same (seed, completedCount) → same line, and
     * consecutive sessions walk the pool instead of repeating. Seed with
     * something user-stable (e.g. day-of-year) so two sessions on the same
     * day still differ via [completedCount].
     */
    fun pick(seed: Int, completedCount: Int): String =
        lines[Math.floorMod(seed + completedCount, lines.size)]
}

/**
 * A breath cycle's timing. [secondInhaleMs] > 0 models the physiological
 * sigh's short top-up inhale before the long exhale. All phases with 0ms are
 * skipped. The same clock that drives the animation drives the haptics.
 */
@Stable
data class BreathPattern(
    val inhaleMs: Int,
    val holdInMs: Int = 0,
    val secondInhaleMs: Int = 0,
    val exhaleMs: Int,
    val holdOutMs: Int = 0,
) {
    val cycleMs: Int get() = inhaleMs + holdInMs + secondInhaleMs + exhaleMs + holdOutMs

    companion object {
        /** Huberman-style physiological sigh: double inhale, long exhale. */
        val SIGH = BreathPattern(inhaleMs = 2_500, secondInhaleMs = 1_000, exhaleMs = 6_000)

        /** Box breathing: equal sides, haptic tick at each corner. */
        val BOX = BreathPattern(inhaleMs = 4_000, holdInMs = 4_000, exhaleMs = 4_000, holdOutMs = 4_000)

        /** Wind-down cadence: slow, exhale-weighted, sleep-compatible. */
        val EMBER = BreathPattern(inhaleMs = 4_000, exhaleMs = 6_000, holdOutMs = 2_000)
    }
}

/** One step of the Guide act. The UI renders each kind natively (Compose motion, Rive, dim). */
sealed interface GuideStep {

    /** Seconds this step occupies (used for the honest total-duration promise). */
    val durationSec: Int

    /** Paced breathing with an animated shape + synchronized haptics. */
    @Stable
    data class Breath(
        val pattern: BreathPattern,
        val cycles: Int,
        /** Shown once as the step begins (pool-rotated). */
        val cue: CopyPool,
    ) : GuideStep {
        override val durationSec: Int get() = (pattern.cycleMs * cycles) / 1000
    }

    /** A physical instruction held for a fixed time (stretch poses, palming). */
    @Stable
    data class Move(
        val instruction: CopyPool,
        val holdSec: Int,
        /** Optional Rive/Lottie asset key showing the pose; null = text + motion cues only. */
        val poseAsset: String? = null,
    ) : GuideStep {
        override val durationSec: Int get() = holdSec
    }

    /** A single attention prompt dwelled on (distant focus, noticing). Screen may dim. */
    @Stable
    data class Prompt(
        val text: CopyPool,
        val dwellSec: Int,
        /** Dim the screen toward black while dwelling (Horizon, Ember close). */
        val dimScreen: Boolean = false,
    ) : GuideStep {
        override val durationSec: Int get() = dwellSec
    }
}

/**
 * A complete session: Arrival (why this break, now — Sense's context, spoken)
 * → Guide (one exercise, fully led) → Landing (closure + the bridge back).
 */
@Stable
data class SessionScript(
    /** Stable id — logged with outcomes; the future session bandit's arm id. */
    val id: String,
    /** Display name — named things become rituals ("do a Sigh"). */
    val displayName: String,
    /** Arrival line pools keyed by Sense trigger; [arrivalDefault] covers the rest. */
    val arrivalByTrigger: Map<String, CopyPool> = emptyMap(),
    val arrivalDefault: CopyPool,
    val guide: List<GuideStep>,
    /** Physical closure ("notice your shoulders now"). */
    val landingClose: CopyPool,
    /** The bridge back to the day — the last thing read. */
    val landingBridge: CopyPool,
    /** Seconds the Arrival + Landing acts occupy around the guide steps. */
    val framingSec: Int = ARRIVAL_SEC + LANDING_SEC,
) {
    init {
        require(guide.isNotEmpty()) { "$id: a session needs at least one guide step" }
    }

    /** The honest duration — what the notification promised is what runs. */
    val totalSec: Int get() = framingSec + guide.sumOf { it.durationSec }

    fun arrivalFor(triggerName: String?): CopyPool =
        triggerName?.let { arrivalByTrigger[it] } ?: arrivalDefault

    companion object {
        const val ARRIVAL_SEC = 4
        const val LANDING_SEC = 7
    }
}
