package com.reset.feature.mood.navigation

import com.reset.model.domain.model.ChimeKind

/**
 * Feature-local one-shot effects for the Mood Log. Navigation (leaving after save/skip) is
 * NOT here — it goes through the injected [com.reset.navigation.Navigator].
 */
sealed class MoodSideEffect {
    /** Play the soft confirmation chime when a mood is saved. */
    data class PlayChime(val kind: ChimeKind) : MoodSideEffect()
}
