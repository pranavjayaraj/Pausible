package com.reset.feature.sessions.navigation

/** All user/UI intents for the full-screen session experience. */
sealed interface SessionIntent {
    data object ToggleRunning : SessionIntent
    data object EndSession : SessionIntent
    data object FinishWarmupEarly : SessionIntent
    data object HandleBackPress : SessionIntent
}
