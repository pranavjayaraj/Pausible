package com.reset.feature.sessions.navigation

import com.reset.model.domain.model.ChimeKind

/** Feature-local one-shot effects for the session experience. Navigation is NOT here —
 *  it goes through the injected [com.reset.navigation.Navigator]. */
sealed class SessionSideEffect {
    data class PlayChime(val kind: ChimeKind) : SessionSideEffect()
}
