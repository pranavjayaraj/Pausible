package com.reset.feature.home

import androidx.compose.runtime.Stable
import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.model.SessionStats

/**
 * Immutable UI state for the Home feature — the design's Home tab.
 *
 * [status] is the initial data-load lifecycle (loading / content / error).
 * `HomeRoute` renders directly from these fields; cross-feature navigation 
 * goes through the injected Navigator, not state. Build new state only
 * with [getDefault] + `copy`.
 */
@Stable
data class HomeState(
    val status: HomeStatus = HomeStatus.Loading,
    /** Index into the break-fact-of-the-day array, rotated by day of month. */
    val factIndex: Int = 0,
    /** The Meditate hero card's preset interval. */
    val durationMin: Int = HomePreferences.DEFAULT_DURATION_MIN,
    val stats: SessionStats = SessionStats(),
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

/** Payload for the celebration banner popped over Home after a finished break. */
@Stable
data class CelebrationBanner(val streakDays: Int)
