package com.reset.feature.profile.navigation

/** All user/UI intents for the Profile feature. */
sealed interface ProfileIntent {
    data object HandleBackPress : ProfileIntent

    /** Step the quiet-hours start boundary by [deltaHours] (wraps 0..23). */
    data class AdjustQuietHoursStart(val deltaHours: Int) : ProfileIntent

    /** Step the quiet-hours end boundary by [deltaHours] (wraps 0..23). */
    data class AdjustQuietHoursEnd(val deltaHours: Int) : ProfileIntent
}
