package com.reset.feature.checkin

import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePreferencesRepository(
    preferences: HomePreferences = HomePreferences(),
) : PreferencesRepository {

    val preferencesFlow = MutableStateFlow(preferences)
    override val preferences: Flow<HomePreferences> = preferencesFlow

    override suspend fun setDuration(minutes: Int) = Unit
    override suspend fun setRemindersEnabled(enabled: Boolean) = Unit
    override suspend fun setReminderEveryMin(minutes: Int) = Unit
    override suspend fun setReminderStartHour(hour: Int) = Unit
    override suspend fun setReminderEndHour(hour: Int) = Unit
    override suspend fun setQuietHoursStart(hour: Int) = Unit
    override suspend fun setQuietHoursEnd(hour: Int) = Unit
    override suspend fun setQuietHoursEnabled(enabled: Boolean) = Unit
    override suspend fun setSoundsEnabled(enabled: Boolean) = Unit
}
