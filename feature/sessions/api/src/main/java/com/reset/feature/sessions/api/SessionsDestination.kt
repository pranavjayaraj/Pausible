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
 * The full-screen session experience: the focus countdown and the guided-breathing pulse.
 * Every entry point (Home's Meditate hero, the Deep Breathing tile, the builder's Begin,
 * the break cards) navigates here with typed args.
 */
@Serializable
data class SessionDestination(
    /** [MODE_FOCUS] or [MODE_BREAK]. */
    val mode: String,
    /** For [MODE_BREAK]: which break was picked ([KIND_STRETCH]/[KIND_MEDITATE]/[KIND_BREATHING]). */
    val breakKind: String? = null,
    val durationMin: Int = DEFAULT_FOCUS_MIN,
    /** Breathing pace, seconds per phase — drives the warm-up clock. */
    val paceSec: Int = DEFAULT_PACE_SEC,
    /** Soundscape key theming the focus backdrop (see [SessionSoundKeys]); null = default teal. */
    val soundKey: String? = null,
    /** Start the focus session with warm-up breaths first. */
    val warmup: Boolean = false,
    /** Play the end chime when the session completes. */
    val gong: Boolean = true,
) : Screen {
    companion object {
        const val MODE_FOCUS = "focus"
        const val MODE_BREAK = "break"

        const val KIND_STRETCH = "stretch"
        const val KIND_MEDITATE = "meditate"
        const val KIND_BREATHING = "breathing"

        const val DEFAULT_FOCUS_MIN = 25
        const val DEFAULT_PACE_SEC = 4

        // SavedStateHandle keys — the constructor property names.
        const val ARG_MODE = "mode"
        const val ARG_BREAK_KIND = "breakKind"
        const val ARG_DURATION_MIN = "durationMin"
        const val ARG_PACE_SEC = "paceSec"
        const val ARG_SOUND_KEY = "soundKey"
        const val ARG_WARMUP = "warmup"
        const val ARG_GONG = "gong"
    }
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
