package com.reset.feature.profile

import androidx.compose.runtime.Stable
import com.reset.model.domain.model.WeeklyFocus

/**
 * Immutable UI state for the Profile tab — the "Weekly Progress" stats summary. Seeded from
 * the persisted session history; build new state only with [getDefault] + `copy`.
 */
@Stable
data class ProfileState(
    val loaded: Boolean = false,
    val breaksTaken: Int = 0,
    val streakDays: Int = 0,
    /** Trailing-week focus minutes, Monday-first, always [WeeklyFocus.DAYS_PER_WEEK] long. */
    val focusMinutesPerDay: List<Int> = List(WeeklyFocus.DAYS_PER_WEEK) { 0 },
    /** Index of today's bar — the highlighted one. */
    val todayIndex: Int = 0,
    /** Sense quiet hours (24h clock, wraps midnight): no break prompts inside
     *  the window, except the single silent night wind-down. */
    val quietHoursStart: Int = QUIET_HOURS_START_DEFAULT,
    val quietHoursEnd: Int = QUIET_HOURS_END_DEFAULT,
) {
    companion object {
        const val QUIET_HOURS_START_DEFAULT = 22
        const val QUIET_HOURS_END_DEFAULT = 7

        fun getDefault() = ProfileState()
    }
}
