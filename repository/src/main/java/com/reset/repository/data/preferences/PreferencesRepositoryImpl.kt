package com.reset.repository.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** [PreferencesRepository] backed by Jetpack [DataStore] preferences. */
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    override val preferences: Flow<HomePreferences> = dataStore.data.map { prefs ->
        HomePreferences(
            durationMin = prefs[KEY_DURATION] ?: HomePreferences.DEFAULT_DURATION_MIN,
            remindersEnabled = prefs[KEY_REMINDERS_ENABLED] ?: true,
            remindersEveryMin = prefs[KEY_REMINDER_EVERY] ?: HomePreferences.DEFAULT_REMINDER_EVERY_MIN,
            remindersStartHour = prefs[KEY_REMINDER_START] ?: HomePreferences.DEFAULT_REMINDER_START_HOUR,
            remindersEndHour = prefs[KEY_REMINDER_END] ?: HomePreferences.DEFAULT_REMINDER_END_HOUR,
            quietHoursEnabled = prefs[KEY_QUIET_ENABLED] ?: true,
            quietHoursStartHour = prefs[KEY_QUIET_START] ?: HomePreferences.DEFAULT_QUIET_HOURS_START,
            quietHoursEndHour = prefs[KEY_QUIET_END] ?: HomePreferences.DEFAULT_QUIET_HOURS_END,
            soundsEnabled = prefs[KEY_SOUNDS_ENABLED] ?: true,
        )
    }

    override suspend fun setDuration(minutes: Int) {
        dataStore.edit { it[KEY_DURATION] = minutes }
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_REMINDERS_ENABLED] = enabled }
    }

    override suspend fun setReminderEveryMin(minutes: Int) {
        dataStore.edit { it[KEY_REMINDER_EVERY] = minutes }
    }

    override suspend fun setReminderStartHour(hour: Int) {
        dataStore.edit { it[KEY_REMINDER_START] = hour }
    }

    override suspend fun setReminderEndHour(hour: Int) {
        dataStore.edit { it[KEY_REMINDER_END] = hour }
    }

    override suspend fun setQuietHoursStart(hour: Int) {
        dataStore.edit { it[KEY_QUIET_START] = hour.coerceIn(0, 23) }
    }

    override suspend fun setQuietHoursEnd(hour: Int) {
        dataStore.edit { it[KEY_QUIET_END] = hour.coerceIn(0, 23) }
    }

    override suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_QUIET_ENABLED] = enabled }
    }

    override suspend fun setSoundsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SOUNDS_ENABLED] = enabled }
    }

    private companion object {
        val KEY_DURATION = intPreferencesKey("duration_min")
        val KEY_REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val KEY_REMINDER_EVERY = intPreferencesKey("reminder_every_min")
        val KEY_REMINDER_START = intPreferencesKey("reminder_start_hour")
        val KEY_REMINDER_END = intPreferencesKey("reminder_end_hour")
        val KEY_QUIET_START = intPreferencesKey("quiet_hours_start")
        val KEY_QUIET_END = intPreferencesKey("quiet_hours_end")
        val KEY_QUIET_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val KEY_SOUNDS_ENABLED = booleanPreferencesKey("sounds_enabled")
    }
}
