package com.reset.feature.settings.utils

/** Pure display helpers for the Settings page. */
object SettingsUtils {

    /**
     * 24h hour → "10:00 PM" / "7:00 AM" / "12:00 AM", matching the design's fmt12. The
     * bounds persist at hour granularity, so minutes are always ":00". AM/PM stay literal
     * (not resources) like the profile card's formatter — they mirror the design, not copy.
     */
    fun formatHour(hour: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val display = when (hour % 12) {
            0 -> 12
            else -> hour % 12
        }
        return "$display:00 $amPm"
    }
}
