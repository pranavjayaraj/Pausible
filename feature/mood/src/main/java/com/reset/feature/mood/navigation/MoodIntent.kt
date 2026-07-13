package com.reset.feature.mood.navigation

/** All user/UI intents for the Mood Log feature. */
sealed interface MoodIntent {
    /** The slider moved to a new 0–100 position. */
    data class LevelChanged(val level: Int) : MoodIntent

    /** "Save Mood" — persist the reported level and leave. */
    data object SaveMood : MoodIntent

    /** "Skip for now", the close button, or system back — leave without saving. */
    data object Dismiss : MoodIntent
}
