package com.reset.model.domain.stats

import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for session history: aggregate stats, the trailing-week focus
 * buckets, and the writes recording finished sessions and breaks.
 */
interface StatsRepository {

    val stats: Flow<SessionStats>

    /** Trailing-week focus minutes for the Stats bar chart. */
    val weeklyFocus: Flow<WeeklyFocus>

    /** Records a finished focus session: counts, total time, today's bucket, streak. */
    suspend fun recordFocusSession(minutes: Int)

    /** Records a completed mindful break and keeps the daily streak alive. */
    suspend fun recordBreak()
}
