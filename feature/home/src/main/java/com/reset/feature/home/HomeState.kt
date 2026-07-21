package com.reset.feature.home

import androidx.compose.runtime.Stable

/**
 * Immutable UI state for the Home feature — the design's "Check In" Home tab: a greeting,
 * a single check-in entry card (resting / Sense pre-lit / night), and two small stats.
 *
 * [status] is the initial data-load lifecycle (loading / content / error). `HomeRoute`
 * renders directly from these fields; cross-feature navigation goes through the injected
 * Navigator, not state. Build new state only with [getDefault] + `copy`.
 */
@Stable
data class HomeState(
    val status: HomeStatus = HomeStatus.Loading,
    /** Coarse time-of-day bucket driving the greeting text and (for [DayPeriod.NIGHT]) the
     *  check-in card's theme. */
    val dayPeriod: DayPeriod = DayPeriod.MORNING,
    /** 0 = Monday … 6 = Sunday — indexes `home_weekday_names`. */
    val dayOfWeekIndex: Int = 0,
    val checkIn: CheckInCardState = CheckInCardState.Resting,
    /** Breaks taken today — resets at midnight, unlike [streak]. */
    val todayPauses: Int = 0,
    val todayQuietMin: Int = 0,
    /** Formatted "5:00 pm"-style estimate of the next reminder; null when reminders are off. */
    val nextNudgeAt: String? = null,
    val streak: Int = 0,
    /** Non-null while the "Nice breather!" banner is popped over Home. */
    val celebration: CelebrationBanner? = null,
) {
    companion object {
        fun getDefault() = HomeState()
    }
}

/** Initial data-load lifecycle for prefs + stats. */
sealed interface HomeStatus {
    data object Loading : HomeStatus
    data object Content : HomeStatus
    data class Error(val message: String?) : HomeStatus
}

/** Coarse time-of-day bucket; see [HomeState.dayPeriod]. */
enum class DayPeriod { MORNING, AFTERNOON, EVENING, NIGHT }

/** Which check-in entry-card variant Home renders. */
sealed interface CheckInCardState {
    /** The default, quiet-companionship card. */
    data object Resting : CheckInCardState

    /** Sense noticed a long sitting stretch; [decisionId] is what dismissal acknowledges
     *  back to the decision log. */
    data class SensePreLit(val decisionId: Long) : CheckInCardState

    /** Post-quiet-hours theme, shown instead of [SensePreLit] even when one is pending. */
    data object Night : CheckInCardState
}

/** Payload for the celebration banner popped over Home after a finished break. */
@Stable
data class CelebrationBanner(val streakDays: Int)
