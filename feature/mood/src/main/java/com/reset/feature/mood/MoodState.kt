package com.reset.feature.mood

import androidx.compose.runtime.Stable
import com.reset.feature.mood.ui.MoodTone

/**
 * Immutable UI state for the full-screen "Mood Log" (MoodDestination). The whole screen is
 * driven by a single [level] on a 0–100 scale — the slider position — from which the face,
 * palette and copy are all derived. Build new state only with [getDefault] + `copy`.
 */
@Stable
data class MoodState(
    val level: Int = DEFAULT_LEVEL,
) {
    /** The three-way mood band the current [level] falls into. */
    val tone: MoodTone get() = MoodTone.fromLevel(level)

    companion object {
        /** Midpoint — the slider opens centred, like the design. */
        const val DEFAULT_LEVEL = 50

        fun getDefault() = MoodState()
    }
}
