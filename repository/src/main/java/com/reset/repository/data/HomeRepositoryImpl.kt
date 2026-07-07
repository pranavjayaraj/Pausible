package com.reset.repository.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.reset.model.domain.HomeRepository
import com.reset.model.domain.TimeProvider
import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.model.SessionPreset
import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** [HomeRepository] backed by Jetpack [DataStore] preferences. */
class HomeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val timeProvider: TimeProvider,
    private val json: Json,
) : HomeRepository {

    override val preferences: Flow<HomePreferences> = dataStore.data.map { prefs ->
        HomePreferences(
            durationMin = prefs[KEY_DURATION] ?: HomePreferences.DEFAULT_DURATION_MIN,
            remindersEnabled = prefs[KEY_REMINDERS_ENABLED] ?: true,
            remindersEveryMin = prefs[KEY_REMINDER_EVERY] ?: HomePreferences.DEFAULT_REMINDER_EVERY_MIN,
            remindersStartHour = prefs[KEY_REMINDER_START] ?: HomePreferences.DEFAULT_REMINDER_START_HOUR,
            remindersEndHour = prefs[KEY_REMINDER_END] ?: HomePreferences.DEFAULT_REMINDER_END_HOUR,
        )
    }

    override val stats: Flow<SessionStats> = dataStore.data.map { prefs ->
        SessionStats(
            sessions = prefs[KEY_SESSIONS] ?: 0,
            totalMin = prefs[KEY_TOTAL_MIN] ?: 0,
            streak = prefs[KEY_STREAK] ?: 0,
            breaksTaken = prefs[KEY_BREAKS] ?: 0,
        )
    }

    override val weeklyFocus: Flow<WeeklyFocus> = dataStore.data.map { prefs ->
        val today = timeProvider.todayEpochDay()
        WeeklyFocus(
            minutesPerDay = List(WeeklyFocus.DAYS_PER_WEEK) { day ->
                val bucketDay = prefs[keyBucketDay(day)] ?: NO_DAY
                // A bucket only counts while its recorded day is inside the trailing week;
                // stale buckets from previous weeks read as zero.
                val fresh = bucketDay in (today - WeeklyFocus.DAYS_PER_WEEK + 1)..today
                if (fresh) prefs[keyBucketMin(day)] ?: 0 else 0
            },
            todayIndex = timeProvider.dayOfWeekIndex(),
        )
    }

    override val presets: Flow<List<SessionPreset>> = dataStore.data.map { prefs ->
        decodePresets(prefs[KEY_PRESETS])
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

    override suspend fun recordFocusSession(minutes: Int) {
        val today = timeProvider.todayEpochDay()
        val dayIndex = timeProvider.dayOfWeekIndex()
        dataStore.edit { prefs ->
            prefs[KEY_SESSIONS] = (prefs[KEY_SESSIONS] ?: 0) + 1
            prefs[KEY_TOTAL_MIN] = (prefs[KEY_TOTAL_MIN] ?: 0) + minutes
            val sameDay = prefs[keyBucketDay(dayIndex)] == today
            prefs[keyBucketMin(dayIndex)] = (if (sameDay) prefs[keyBucketMin(dayIndex)] ?: 0 else 0) + minutes
            prefs[keyBucketDay(dayIndex)] = today
            prefs.touchStreak(today)
        }
    }

    override suspend fun recordBreak() {
        val today = timeProvider.todayEpochDay()
        dataStore.edit { prefs ->
            prefs[KEY_BREAKS] = (prefs[KEY_BREAKS] ?: 0) + 1
            prefs.touchStreak(today)
        }
    }

    override suspend fun savePreset(preset: SessionPreset) {
        dataStore.edit { prefs ->
            val updated = decodePresets(prefs[KEY_PRESETS])
                .filter { it.name != preset.name } + preset
            prefs[KEY_PRESETS] = json.encodeToString(
                ListSerializer(SessionPreset.serializer()),
                updated.takeLast(SessionPreset.MAX_PRESETS),
            )
        }
    }

    private fun decodePresets(raw: String?): List<SessionPreset> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(SessionPreset.serializer()), raw)
        } catch (_: SerializationException) {
            // A corrupt blob (e.g. schema drift) resets the list instead of wedging reads.
            emptyList()
        }
    }

    /** Same-day activity keeps the streak, consecutive days grow it, a gap resets it. */
    private fun MutablePreferences.touchStreak(today: Long) {
        val lastActive = this[KEY_LAST_ACTIVE_DAY] ?: NO_DAY
        when (lastActive) {
            today -> Unit
            today - 1 -> this[KEY_STREAK] = (this[KEY_STREAK] ?: 0) + 1
            else -> this[KEY_STREAK] = 1
        }
        this[KEY_LAST_ACTIVE_DAY] = today
    }

    private companion object {
        const val NO_DAY = -1L

        val KEY_DURATION = intPreferencesKey("duration_min")
        val KEY_REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val KEY_REMINDER_EVERY = intPreferencesKey("reminder_every_min")
        val KEY_REMINDER_START = intPreferencesKey("reminder_start_hour")
        val KEY_REMINDER_END = intPreferencesKey("reminder_end_hour")
        val KEY_SESSIONS = intPreferencesKey("sessions")
        val KEY_TOTAL_MIN = intPreferencesKey("total_min")
        val KEY_STREAK = intPreferencesKey("streak")
        val KEY_BREAKS = intPreferencesKey("breaks_taken")
        val KEY_LAST_ACTIVE_DAY = longPreferencesKey("last_active_epoch_day")
        val KEY_PRESETS = stringPreferencesKey("builder_presets")

        fun keyBucketMin(dayIndex: Int) = intPreferencesKey("focus_min_day_$dayIndex")
        fun keyBucketDay(dayIndex: Int) = longPreferencesKey("focus_epoch_day_$dayIndex")
    }
}
