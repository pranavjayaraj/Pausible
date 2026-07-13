package com.reset.feature.sessions

import com.reset.model.domain.stats.StatsRepository
import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [StatsRepository] for session ViewModel tests. */
class FakeStatsRepository : StatsRepository {

    val statsFlow = MutableStateFlow(SessionStats())

    /** Minutes handed to [recordFocusSession], in call order. */
    val recordedFocusMinutes = mutableListOf<Int>()
    var recordedBreaks = 0
        private set

    override val stats: Flow<SessionStats> = statsFlow

    override val weeklyFocus: Flow<WeeklyFocus> = MutableStateFlow(WeeklyFocus())

    override suspend fun recordFocusSession(minutes: Int) {
        recordedFocusMinutes += minutes
    }

    override suspend fun recordBreak() {
        recordedBreaks += 1
    }
}
