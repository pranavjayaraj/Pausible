package com.reset.feature.profile.navigation

/** All user/UI intents for the Profile feature. */
sealed interface ProfileIntent {
    data object HandleBackPress : ProfileIntent

    /** The header gear button — open the Settings page (home of the quiet-hours controls). */
    data object OpenSettings : ProfileIntent
}
