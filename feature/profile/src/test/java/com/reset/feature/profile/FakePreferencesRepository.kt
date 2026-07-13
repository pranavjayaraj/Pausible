package com.reset.feature.profile

import com.reset.model.domain.preferences.PreferencesRepository
import com.reset.model.domain.model.HomePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [PreferencesRepository] for Profile ViewModel tests. */
class FakePreferencesRepository : PreferencesRepository {

    val preferencesFlow = MutableStateFlow(HomePreferences())

    override val preferences: Flow<HomePreferences> = preferencesFlow

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
