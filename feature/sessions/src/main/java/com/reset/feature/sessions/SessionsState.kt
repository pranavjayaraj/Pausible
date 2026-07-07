package com.reset.feature.sessions

import androidx.compose.runtime.Stable

/**
 * Immutable UI state for the break-suggestion list (the Sessions tab). The list itself is
 * static design content; picking a card navigates to the Session experience through the
 * injected Navigator. Build new state only with [getDefault] + `copy`.
 */
@Stable
data class SessionsState(
    /** Reserved — the list is static today; the field keeps the MVI seam uniform. */
    val initialized: Boolean = true,
) {
    companion object {
        fun getDefault() = SessionsState()
    }
}
