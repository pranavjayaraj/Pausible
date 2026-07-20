package com.reset.feature.settings.navigation

import com.reset.feature.settings.HourPickerTarget

/** All user/UI intents for the Settings page. */
sealed interface SettingsIntent {
    /** The header back button or system back — leave the page. */
    data object HandleBackPress : SettingsIntent

    /** The "Enable Quiet Hours" switch. */
    data object ToggleQuietHours : SettingsIntent

    /** The "Break reminders" switch. */
    data object ToggleBreakReminders : SettingsIntent

    /** The "Sounds & haptics" switch. */
    data object ToggleSounds : SettingsIntent

    /** A quiet-hours time chip was tapped — open the hour picker for [target]. */
    data class OpenHourPicker(val target: HourPickerTarget) : SettingsIntent

    /** The hour picker was dismissed without choosing. */
    data object DismissHourPicker : SettingsIntent

    /** An hour was chosen in the open picker (24h clock, 0..23). */
    data class SelectHour(val hour: Int) : SettingsIntent
}
