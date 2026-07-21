package com.reset.feature.sessions.navigation

import com.reset.feature.sessions.content.ActionKind
import com.reset.model.domain.model.ChimeKind

/** Feature-local one-shot effects for the session experience. Navigation is NOT here —
 *  it goes through the injected [com.reset.navigation.Navigator]. */
sealed class SessionSideEffect {
    data class PlayChime(val kind: ChimeKind) : SessionSideEffect()

    /**
     * A [com.reset.feature.sessions.content.GuideStep.LaunchAction] step was reached. The
     * ViewModel carries only [actionKind] — no contact data, no Context, no Intent — the
     * Route resolves the close-person target and the app host builds/fires the actual
     * `ACTION_SENDTO`/`ACTION_DIAL` Intent (see the leak-safe activity-launching rule).
     */
    data class LaunchActionRequested(val actionKind: ActionKind) : SessionSideEffect()
}
