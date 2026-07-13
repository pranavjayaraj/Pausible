package com.reset.feature.home

import com.reset.model.domain.preferences.PreferencesRepository
import com.reset.model.domain.model.HomePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/** In-memory [PreferencesRepository] for Home ViewModel tests. */
class FakePreferencesRepository(
    preferences: HomePreferences = HomePreferences(),
    private val failPreferences: Boolean = false,
) : PreferencesRepository {

    val preferencesFlow = MutableStateFlow(preferences)

    override val preferences: Flow<HomePreferences> =
        if (failPreferences) flow { throw IllegalStateException("boom") } else preferencesFlow

    override suspend fun setDuration(minutes: Int) {
        preferencesFlow.value = preferencesFlow.value.copy(durationMin = minutes)
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
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

    override suspend fun setQuietHoursStart(hour: Int) {
        preferencesFlow.value = preferencesFlow.value.copy(quietHoursStartHour = hour)
    }

    override suspend fun setQuietHoursEnd(hour: Int) {
        preferencesFlow.value = preferencesFlow.value.copy(quietHoursEndHour = hour)
    }
}
