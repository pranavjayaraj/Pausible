package com.reset.repository.data.mood

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.reset.model.domain.TimeProvider
import com.reset.model.domain.mood.MoodRepository
import javax.inject.Inject

/** [MoodRepository] backed by Jetpack [DataStore] preferences. */
class MoodRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val timeProvider: TimeProvider,
) : MoodRepository {

    override suspend fun recordMood(level: Int) {
        val today = timeProvider.todayEpochDay()
        dataStore.edit { prefs ->
            prefs[KEY_LAST_MOOD] = level.coerceIn(MOOD_MIN, MOOD_MAX)
            prefs[KEY_LAST_MOOD_DAY] = today
            prefs[KEY_MOODS_LOGGED] = (prefs[KEY_MOODS_LOGGED] ?: 0) + 1
        }
    }

    private companion object {
        /** The mood slider's inclusive bounds; writes are clamped to this range. */
        const val MOOD_MIN = 0
        const val MOOD_MAX = 100

        val KEY_LAST_MOOD = intPreferencesKey("last_mood_level")
        val KEY_LAST_MOOD_DAY = longPreferencesKey("last_mood_epoch_day")
        val KEY_MOODS_LOGGED = intPreferencesKey("moods_logged")
    }
}
