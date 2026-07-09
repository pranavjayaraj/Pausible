package com.reset.feature.mood

import com.reset.model.domain.HomeRepository
import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.model.SessionPreset
import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [HomeRepository] for Mood ViewModel tests. */
class FakeHomeRepository : HomeRepository {

    /** Levels handed to [recordMood], in call order. */
    val recordedMoods = mutableListOf<Int>()

    override val preferences: Flow<HomePreferences> = MutableStateFlow(HomePreferences())

    override val stats: Flow<SessionStats> = MutableStateFlow(SessionStats())

    override val weeklyFocus: Flow<WeeklyFocus> = MutableStateFlow(WeeklyFocus())

    override val presets: Flow<List<SessionPreset>> = MutableStateFlow(emptyList())

    override suspend fun setDuration(minutes: Int) = Unit

    override suspend fun setRemindersEnabled(enabled: Boolean) = Unit

    override suspend fun setReminderEveryMin(minutes: Int) = Unit

    override suspend fun setReminderStartHour(hour: Int) = Unit

    override suspend fun setReminderEndHour(hour: Int) = Unit

    override suspend fun recordFocusSession(minutes: Int) = Unit

    override suspend fun recordBreak() = Unit

    override suspend fun recordMood(level: Int) {
        recordedMoods += level
    }

    override suspend fun savePreset(preset: SessionPreset) = Unit
}
