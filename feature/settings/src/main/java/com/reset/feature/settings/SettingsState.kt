package com.reset.feature.settings

import androidx.compose.runtime.Stable
import com.reset.model.domain.model.HomePreferences

/**
 * Immutable UI state for the full-screen Settings page (SettingsDestination) — the quiet
 * hours card and the notification switches from the BloomNow design. Seeded from the
 * persisted preferences; build new state only with [getDefault] + `copy`.
 */
@Stable
data class SettingsState(
    val loaded: Boolean = false,
    /** Quiet hours master switch; off greys the bounds rows and empties the Sense window. */
    val quietHoursEnabled: Boolean = true,
    /** Quiet-hours bounds, 24h clock 0..23; the window wraps midnight. */
    val quietHoursStart: Int = HomePreferences.DEFAULT_QUIET_HOURS_START,
    val quietHoursEnd: Int = HomePreferences.DEFAULT_QUIET_HOURS_END,
    val remindersEnabled: Boolean = true,
    val soundsEnabled: Boolean = true,
    /** Which quiet-hours bound the hour-picker dialog is editing; null = no dialog. */
    val hourPicker: HourPickerTarget? = null,
) {
    companion object {
        fun getDefault() = SettingsState()
    }
}

/** The two tappable time chips an hour-picker dialog can edit. */
sealed class HourPickerTarget {
    data object QuietStart : HourPickerTarget()
    data object QuietEnd : HourPickerTarget()
}
