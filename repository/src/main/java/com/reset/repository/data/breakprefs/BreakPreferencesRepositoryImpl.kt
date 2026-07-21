package com.reset.repository.data.breakprefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.reset.model.domain.breakprefs.BreakPreferences
import com.reset.model.domain.breakprefs.BreakPreferencesRepository
import com.reset.model.domain.checkin.NeedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** [BreakPreferencesRepository] backed by Jetpack [DataStore] preferences. */
class BreakPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : BreakPreferencesRepository {

    override val preferences: Flow<BreakPreferences> = dataStore.data.map { prefs ->
        BreakPreferences(
            audioAvailable = prefs[KEY_AUDIO] ?: true,
            stairsAvailable = prefs[KEY_STAIRS] ?: false,
            moveSpaceAvailable = prefs[KEY_MOVE_SPACE] ?: true,
            waterAccessAvailable = prefs[KEY_WATER_ACCESS] ?: true,
            needStateEcho = NeedState.entries.associateWith { need -> prefs[echoKey(need)] ?: 0f },
        )
    }

    override suspend fun setAudioAvailable(available: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_AUDIO] = available }
    }

    override suspend fun setStairsAvailable(available: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_STAIRS] = available }
    }

    override suspend fun setMoveSpaceAvailable(available: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_MOVE_SPACE] = available }
    }

    override suspend fun setWaterAccessAvailable(available: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_WATER_ACCESS] = available }
    }

    override suspend fun recordNeedStateEcho(needState: NeedState) {
        dataStore.edit { prefs ->
            for (need in NeedState.entries) {
                val decayed = (prefs[echoKey(need)] ?: 0f) * DECAY
                prefs[echoKey(need)] = if (need == needState) decayed + ECHO_BUMP else decayed
            }
        }
    }

    private fun echoKey(needState: NeedState) = floatPreferencesKey("break_prefs_echo_${needState.name}")

    private companion object {
        val KEY_AUDIO = booleanPreferencesKey("break_prefs_audio_available")
        val KEY_STAIRS = booleanPreferencesKey("break_prefs_stairs_available")
        val KEY_MOVE_SPACE = booleanPreferencesKey("break_prefs_move_space_available")
        val KEY_WATER_ACCESS = booleanPreferencesKey("break_prefs_water_access_available")

        /** Each new check-in ages out prior weight by 10% before adding its own — a slow,
         *  standing signal rather than a single overriding data point. */
        const val DECAY = 0.9f
        const val ECHO_BUMP = 1f
    }
}
