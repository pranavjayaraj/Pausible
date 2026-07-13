package com.reset.repository.data.stats

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.reset.model.domain.TimeProvider
import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import com.reset.model.domain.stats.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** [StatsRepository] backed by Jetpack [DataStore] preferences. */
class StatsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val timeProvider: TimeProvider,
) : StatsRepository {

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

        val KEY_SESSIONS = intPreferencesKey("sessions")
        val KEY_TOTAL_MIN = intPreferencesKey("total_min")
        val KEY_STREAK = intPreferencesKey("streak")
        val KEY_BREAKS = intPreferencesKey("breaks_taken")
        val KEY_LAST_ACTIVE_DAY = longPreferencesKey("last_active_epoch_day")

        fun keyBucketMin(dayIndex: Int) = intPreferencesKey("focus_min_day_$dayIndex")
        fun keyBucketDay(dayIndex: Int) = longPreferencesKey("focus_epoch_day_$dayIndex")
    }
}
