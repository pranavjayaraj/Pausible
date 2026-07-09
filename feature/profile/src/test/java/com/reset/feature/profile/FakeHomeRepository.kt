package com.reset.feature.profile

import com.reset.model.domain.HomeRepository
import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.model.SessionPreset
import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [HomeRepository] for Profile ViewModel tests. */
class FakeHomeRepository(
    stats: SessionStats = SessionStats(),
    weeklyFocus: WeeklyFocus = WeeklyFocus(),
) : HomeRepository {

    val statsFlow = MutableStateFlow(stats)
    val weeklyFocusFlow = MutableStateFlow(weeklyFocus)

    override val preferences: Flow<HomePreferences> = MutableStateFlow(HomePreferences())

    override val stats: Flow<SessionStats> = statsFlow

    override val weeklyFocus: Flow<WeeklyFocus> = weeklyFocusFlow

    override val presets: Flow<List<SessionPreset>> = MutableStateFlow(emptyList())

    override suspend fun setDuration(minutes: Int) = Unit

    override suspend fun setRemindersEnabled(enabled: Boolean) = Unit

    override suspend fun setReminderEveryMin(minutes: Int) = Unit

    override suspend fun setReminderStartHour(hour: Int) = Unit

    override suspend fun setReminderEndHour(hour: Int) = Unit

    override suspend fun recordFocusSession(minutes: Int) = Unit

    override suspend fun recordBreak() = Unit

    override suspend fun recordMood(level: Int) = Unit

    override suspend fun savePreset(preset: SessionPreset) = Unit
}
