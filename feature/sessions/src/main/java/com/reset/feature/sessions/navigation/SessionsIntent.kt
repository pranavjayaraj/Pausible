package com.reset.feature.sessions.navigation

/** All user/UI intents for the break-suggestion list. */
sealed interface SessionsIntent {
    /** One of the three break cards; [kind] is a SessionDestination.KIND_* key. */
    data class PickBreak(val kind: String) : SessionsIntent
    data object SkipBreak : SessionsIntent
    data object HandleBackPress : SessionsIntent
}
