package com.reset.feature.profile

import com.reset.model.domain.stats.StatsRepository
import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [StatsRepository] for Profile ViewModel tests. */
class FakeStatsRepository(
    stats: SessionStats = SessionStats(),
    weeklyFocus: WeeklyFocus = WeeklyFocus(),
) : StatsRepository {

    val statsFlow = MutableStateFlow(stats)
    val weeklyFocusFlow = MutableStateFlow(weeklyFocus)

    override val stats: Flow<SessionStats> = statsFlow

    override val weeklyFocus: Flow<WeeklyFocus> = weeklyFocusFlow

    override suspend fun recordFocusSession(minutes: Int) = Unit

    override suspend fun recordBreak() = Unit
}
