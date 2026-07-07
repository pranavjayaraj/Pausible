package com.reset.feature.home

import com.reset.model.domain.HomeRepository
import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.model.SessionPreset
import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/** In-memory [HomeRepository] for Home ViewModel tests. */
class FakeHomeRepository(
    preferences: HomePreferences = HomePreferences(),
    stats: SessionStats = SessionStats(),
    presets: List<SessionPreset> = emptyList(),
    private val failPreferences: Boolean = false,
) : HomeRepository {

    val preferencesFlow = MutableStateFlow(preferences)
    val statsFlow = MutableStateFlow(stats)
    val presetsFlow = MutableStateFlow(presets)

    var lastDuration: Int? = null
        private set
    var lastRemindersEnabled: Boolean? = null
        private set
    val savedPresets = mutableListOf<SessionPreset>()

    override val preferences: Flow<HomePreferences> =
        if (failPreferences) flow { throw IllegalStateException("boom") } else preferencesFlow

    override val stats: Flow<SessionStats> = statsFlow

    override val weeklyFocus: Flow<WeeklyFocus> = MutableStateFlow(WeeklyFocus())

    override val presets: Flow<List<SessionPreset>> = presetsFlow

    override suspend fun setDuration(minutes: Int) {
        lastDuration = minutes
        preferencesFlow.value = preferencesFlow.value.copy(durationMin = minutes)
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        lastRemindersEnabled = enabled
        preferencesFlow.value = preferencesFlow.value.copy(remindersEnabled = enabled)
    }

    override suspend fun setReminderEveryMin(minutes: Int) {
        preferencesFlow.value = preferencesFlow.value.copy(remindersEveryMin = minutes)
    }

    override suspend fun setReminderStartHour(hour: Int) {
        preferencesFlow.value = preferencesFlow.value.copy(remindersStartHour = hour)
    }

    override suspend fun setReminderEndHour(hour: Int) {
        preferencesFlow.value = preferencesFlow.value.copy(remindersEndHour = hour)
    }

    override suspend fun recordFocusSession(minutes: Int) {
        statsFlow.value = statsFlow.value.let {
            it.copy(sessions = it.sessions + 1, totalMin = it.totalMin + minutes)
        }
    }

    override suspend fun recordBreak() {
        statsFlow.value = statsFlow.value.let { it.copy(breaksTaken = it.breaksTaken + 1) }
    }

    override suspend fun savePreset(preset: SessionPreset) {
        savedPresets += preset
        presetsFlow.value = presetsFlow.value.filter { it.name != preset.name } + preset
    }
}
