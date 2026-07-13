package com.reset.repository.data.presets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.reset.model.domain.model.SessionPreset
import com.reset.model.domain.presets.PresetsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** [PresetsRepository] backed by Jetpack [DataStore] preferences. */
class PresetsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : PresetsRepository {

    override val presets: Flow<List<SessionPreset>> = dataStore.data.map { prefs ->
        decodePresets(prefs[KEY_PRESETS])
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

    private companion object {
        val KEY_PRESETS = stringPreferencesKey("builder_presets")
    }
}
