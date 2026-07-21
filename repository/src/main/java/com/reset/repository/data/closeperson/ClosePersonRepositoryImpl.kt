package com.reset.repository.data.closeperson

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.reset.model.domain.closeperson.ClosePerson
import com.reset.model.domain.closeperson.ClosePersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** [ClosePersonRepository] backed by Jetpack [DataStore] preferences. */
class ClosePersonRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ClosePersonRepository {

    override val closePerson: Flow<ClosePerson?> = dataStore.data.map { prefs ->
        val name = prefs[KEY_NAME]
        val contactUri = prefs[KEY_CONTACT_URI]
        if (name != null && contactUri != null) ClosePerson(name, contactUri) else null
    }

    override suspend fun setClosePerson(closePerson: ClosePerson) {
        dataStore.edit { prefs ->
            prefs[KEY_NAME] = closePerson.name
            prefs[KEY_CONTACT_URI] = closePerson.contactUri
        }
    }

    override suspend fun clearClosePerson() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_NAME)
            prefs.remove(KEY_CONTACT_URI)
        }
    }

    private companion object {
        val KEY_NAME = stringPreferencesKey("close_person_name")
        val KEY_CONTACT_URI = stringPreferencesKey("close_person_contact_uri")
    }
}
