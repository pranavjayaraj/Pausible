package com.reset.feature.home.navigation

/** All user/UI intents for the Home feature. */
sealed interface HomeIntent {
    data object Load : HomeIntent
    data object Retry : HomeIntent

    data object StartMeditate : HomeIntent
    data object StartDeepBreathing : HomeIntent
    data object OpenBuilder : HomeIntent
    data object ExploreMore : HomeIntent
}
