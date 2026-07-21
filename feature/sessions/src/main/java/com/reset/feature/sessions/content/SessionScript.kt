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

    /**
     * A framed hand-off to an external action (Reach Out's "send warmth"). The player
     * shows [prompt] + a launch button for [dwellSec] before/while the actual Intent is
     * built and fired by the app host — never by the ViewModel (see the leak-safe
     * activity-launching rule). Completion is the Intent launching, not a response.
     */
    @Stable
    data class LaunchAction(
        val prompt: CopyPool,
        val actionKind: ActionKind,
        val dwellSec: Int,
    ) : GuideStep {
        override val durationSec: Int get() = dwellSec
    }
}

/** What [GuideStep.LaunchAction] hands off to the app host. */
enum class ActionKind {
    MESSAGE,
    CALL,
}

/** The line to show while this step is active, deterministically pool-rotated. */
fun GuideStep.copyLine(seed: Int, completedCount: Int): String = when (this) {
    is GuideStep.Breath -> cue.pick(seed, completedCount)
    is GuideStep.Move -> instruction.pick(seed, completedCount)
    is GuideStep.Prompt -> text.pick(seed, completedCount)
    is GuideStep.LaunchAction -> prompt.pick(seed, completedCount)
}

/** Whether the screen should dim toward black while this step runs. Breath steps are
 *  always eyes-closed (the audio/haptic pivot); Prompt opts in per instance. */
val GuideStep.isDimmed: Boolean
    get() = when (this) {
        is GuideStep.Breath -> true
        is GuideStep.Prompt -> dimScreen
        is GuideStep.Move, is GuideStep.LaunchAction -> false
    }

/**
 * The evidence backing a session, shown so the catalog stays honest about how strong the
 * science is — [grade] 5 is a direct, well-replicated physiological mechanism (the
 * physiological sigh); 1 is a plausible-but-thin rationale. Never blank: a session with no
 * defensible [finding] shouldn't ship.
 */
@Stable
data class Evidence(
    val grade: Int,
    val citation: String,
    val finding: String,
) {
    init {
        require(grade in 1..5) { "evidence grade must be 1..5, was $grade" }
        require(citation.isNotBlank()) { "evidence citation must not be blank" }
        require(finding.isNotBlank()) { "evidence finding must not be blank" }
    }
}

/** What kind of thing a session asks the body to do — drives iconography and grouping. */
enum class Modality {
    MOVE,
    STRETCH,
    EYES,
    CALM,
    MIND,
    AMBIENT,
    CONNECT,
}

/** A precondition the device/user must satisfy before a session is offerable. */
enum class Requirement {
    AUDIO,
    STAIRS,
    MOVE_SPACE,
    WATER_ACCESS,
    HAS_CLOSE_PERSON,
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
    /** What kind of thing this session asks the body to do. */
    val modality: Modality,
    /** The science backing this session — shown so the catalog stays honest. */
    val evidence: Evidence,
    /** Preconditions that must hold before this session is offerable. */
    val requirements: Set<Requirement> = emptySet(),
    /** Self-imposed frequency cap (e.g. The Plunge); null = no cap. */
    val minHoursBetween: Int? = null,
    /** True for sessions that are activating/jarring regardless of [modality] (e.g. The
     *  Plunge's cold water) and so should still drop out after quiet hours start. Every
     *  [Modality.MOVE] session is implicitly night-vetoed too — this flag is only for the
     *  exceptions modality alone doesn't catch. */
    val nightVetoed: Boolean = false,
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
        require(minHoursBetween == null || minHoursBetween > 0) {
            "$id: minHoursBetween must be positive, was $minHoursBetween"
        }
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
