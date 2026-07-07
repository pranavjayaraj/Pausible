package com.reset.feature.profile.navigation

/** All user/UI intents for the Profile feature. */
sealed interface ProfileIntent {
    data object HandleBackPress : ProfileIntent
}
