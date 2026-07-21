package com.reset.feature.home.navigation

/** All user/UI intents for the Home feature. */
sealed interface HomeIntent {
    data object Load : HomeIntent
    data object Retry : HomeIntent

    /** The check-in card's primary CTA ("Tell me what's going on"). */
    data object OpenCheckIn : HomeIntent

    /** The check-in card's secondary text ("I'm okay, keep going" / "Not now — just
     *  browsing" / "Heading to bed"). */
    data object DismissCheckIn : HomeIntent

    data object StartDeepBreathing : HomeIntent
    data object OpenBuilder : HomeIntent
}
