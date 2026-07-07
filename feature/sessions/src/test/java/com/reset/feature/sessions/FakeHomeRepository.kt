package com.reset.feature.sessions

import com.reset.model.domain.HomeRepository
import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.model.SessionPreset
import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [HomeRepository] for session ViewModel tests. */
class FakeHomeRepository : HomeRepository {

    val statsFlow = MutableStateFlow(SessionStats())

    /** Minutes handed to [recordFocusSession], in call order. */
    val recordedFocusMinutes = mutableListOf<Int>()
    var recordedBreaks = 0
        private set

    override val preferences: Flow<HomePreferences> = MutableStateFlow(HomePreferences())

    override val stats: Flow<SessionStats> = statsFlow

    override val weeklyFocus: Flow<WeeklyFocus> = MutableStateFlow(WeeklyFocus())

    override val presets: Flow<List<SessionPreset>> = MutableStateFlow(emptyList())

    override suspend fun setDuration(minutes: Int) = Unit

    override suspend fun setRemindersEnabled(enabled: Boolean) = Unit

    override suspend fun setReminderEveryMin(minutes: Int) = Unit

    override suspend fun setReminderStartHour(hour: Int) = Unit

    override suspend fun setReminderEndHour(hour: Int) = Unit

    override suspend fun recordFocusSession(minutes: Int) {
        recordedFocusMinutes += minutes
    }

    override suspend fun recordBreak() {
        recordedBreaks += 1
    }

    override suspend fun savePreset(preset: SessionPreset) = Unit
}
