package com.reset.feature.home

import com.reset.model.domain.stats.StatsRepository
import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [StatsRepository] for Home ViewModel tests. */
class FakeStatsRepository(
    stats: SessionStats = SessionStats(),
    weeklyFocus: WeeklyFocus = WeeklyFocus(),
    todayBreaksTaken: Int = 0,
) : StatsRepository {

    val statsFlow = MutableStateFlow(stats)
    val weeklyFocusFlow = MutableStateFlow(weeklyFocus)
    val todayBreaksTakenFlow = MutableStateFlow(todayBreaksTaken)

    override val stats: Flow<SessionStats> = statsFlow

    override val weeklyFocus: Flow<WeeklyFocus> = weeklyFocusFlow

    override val todayBreaksTaken: Flow<Int> = todayBreaksTakenFlow

    override suspend fun recordFocusSession(minutes: Int) {
        statsFlow.value = statsFlow.value.let {
            it.copy(sessions = it.sessions + 1, totalMin = it.totalMin + minutes)
        }
    }

    override suspend fun recordBreak() {
        statsFlow.value = statsFlow.value.let { it.copy(breaksTaken = it.breaksTaken + 1) }
    }
}
