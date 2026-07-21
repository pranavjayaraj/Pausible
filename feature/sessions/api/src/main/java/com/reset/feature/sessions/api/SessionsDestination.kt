package com.reset.feature.sessions.api

import com.reset.navigation.Screen
import kotlinx.serialization.Serializable

/**
 * The Sessions feature's navigable destinations. Living in the feature's `api` submodule,
 * they are the only thing another feature (or the app host) needs to navigate here —
 * implementations stay in the sibling impl module, so destination churn there never
 * recompiles callers. The types themselves are the routes (Navigation 2.8 type-safe DSL);
 * args are constructor properties, compile-time-checked.
 */

/** The break-suggestion list — the middle tab of the app's dashboard. */
@Serializable
data object SessionsDestination : Screen

/**
 * The full-screen session experience. Two unrelated modes share one destination because
 * they share one route/screen shell:
 *  - [MODE_FOCUS]: the builder's user-timed sit (Home's Meditate hero, the builder's
 *    Begin, the reminder-notification reset) — an open-ended countdown, own duration.
 *  - [MODE_BREAK]: a catalog [scriptId] played by the generic three-act runtime (Arrival →
 *    Guide → Landing) — Sense's suggestions, the Sessions browse list, Home's break tiles.
 *    Duration is *not* a destination arg here — it's whatever the script's steps sum to
 *    (the honest-duration rule), so it is never passed in.
 */
@Serializable
data class SessionDestination(
    /** [MODE_FOCUS] or [MODE_BREAK]. */
    val mode: String,
    /** Required for [MODE_BREAK]: the [SessionScriptIds] catalog id to play. */
    val scriptId: String? = null,
    /** Optional for [MODE_BREAK]: the Sense trigger that suggested this break (a
     *  `BreakType.name`), used only to pick the script's context-aware Arrival copy. */
    val senseTrigger: String? = null,
    /** [MODE_FOCUS] only: the sit length in minutes. */
    val durationMin: Int = DEFAULT_FOCUS_MIN,
    /** [MODE_FOCUS] only: warm-up breathing pace, seconds per phase. */
    val paceSec: Int = DEFAULT_PACE_SEC,
    /** [MODE_FOCUS] only: soundscape key theming the backdrop; null = default teal. */
    val soundKey: String? = null,
    /** [MODE_FOCUS] only: start the sit with warm-up breaths first. */
    val warmup: Boolean = false,
    /** [MODE_FOCUS] only: play the end chime when the sit completes. */
    val gong: Boolean = true,
    /** [MODE_BREAK] only, optional: the Check In propensity trail's row id for this offer —
     *  carried through so a *real* completion (not just the offer's "Start" tap) can be
     *  logged back once the break actually finishes. Null for any non-Check-In launch. */
    val checkInLogRowId: Long? = null,
) : Screen {
    companion object {
        const val MODE_FOCUS = "focus"
        const val MODE_BREAK = "break"

        const val DEFAULT_FOCUS_MIN = 25
        const val DEFAULT_PACE_SEC = 4

        // SavedStateHandle keys — the constructor property names.
        const val ARG_MODE = "mode"
        const val ARG_SCRIPT_ID = "scriptId"
        const val ARG_SENSE_TRIGGER = "senseTrigger"
        const val ARG_DURATION_MIN = "durationMin"
        const val ARG_PACE_SEC = "paceSec"
        const val ARG_SOUND_KEY = "soundKey"
        const val ARG_WARMUP = "warmup"
        const val ARG_GONG = "gong"
        const val ARG_CHECKIN_LOG_ROW_ID = "checkInLogRowId"
    }
}

/**
 * Stable [SessionDestination.scriptId] values — the catalog's compile-checked contract for
 * callers outside `:feature:sessions` (which may depend only on this `api` module, never on
 * the impl module's `content` package where the actual [com.reset.feature.sessions.content
 * .SessionScript] data lives).
 */
object SessionScriptIds {
    const val THE_SIGH = "sigh_v1"
    const val HORIZON = "horizon_v1"
    const val THE_UNFOLD = "unfold_v1"
    const val EMBER = "ember_v1"
    const val UNCLENCH = "unclench_v1"
    const val THE_LOOP = "loop_v1"
    const val WRISTS_AND_HANDS = "wrists_hands_v1"
    const val FERN = "fern_v1"
    const val THE_CLIMB = "climb_v1"
    const val STEP_OUTSIDE = "step_outside_v1"
    const val WARMTH = "warmth_v1"
    const val THREE_GOOD_THINGS = "three_good_things_v1"
    const val MINI_UNPACK = "mini_unpack_v1"
    const val THE_SETTLE = "settle_v1"
    const val THE_SWEEP = "sweep_v1"
    const val THE_PLUNGE = "plunge_v1"
    const val REACH_OUT = "reach_out_v1"
}

/**
 * Soundscape keys shared by everyone who speaks [SessionDestination.soundKey] (the Home
 * builder persists them in presets; the Session experience maps them to a backdrop).
 */
object SessionSoundKeys {
    const val RAIN = "rain"
    const val OCEAN = "ocean"
    const val FOREST = "forest"
    const val FIRE = "fire"
    const val CHIMES = "chimes"
    const val SILENCE = "silence"
}
