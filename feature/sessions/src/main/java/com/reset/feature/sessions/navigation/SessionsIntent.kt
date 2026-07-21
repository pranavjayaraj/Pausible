package com.reset.feature.sessions.navigation

/** All user/UI intents for the break-suggestion list. */
sealed interface SessionsIntent {
    /** A catalog card; [scriptId] is a SessionScriptIds key. */
    data class PickScript(val scriptId: String) : SessionsIntent
    data object SkipBreak : SessionsIntent
    data object HandleBackPress : SessionsIntent
}
