package com.reset.repository.data.presets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.reset.model.domain.model.SessionPreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PresetsRepositoryImplTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.repository(): PresetsRepositoryImpl {
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "afk_${System.nanoTime()}.preferences_pb")
        }
        return PresetsRepositoryImpl(store, Json { ignoreUnknownKeys = true })
    }

    @Test
    fun `exposes no presets when nothing persisted`() = runTest {
        assertTrue(repository().presets.first().isEmpty())
    }

    @Test
    fun `saves presets newest-last, replaces by name and keeps only the cap`() = runTest {
        val repo = repository()
        fun preset(name: String, minutes: Int = 15) = SessionPreset(
            name = name,
            durationMin = minutes,
            soundKey = "rain",
            paceSec = 4,
            intentKey = "focus",
            remindKey = "off",
        )

        repo.savePreset(preset("morning", minutes = 10))
        repo.savePreset(preset("evening"))
        repo.savePreset(preset("morning", minutes = 20))

        val replaced = repo.presets.first()
        assertEquals(listOf("evening", "morning"), replaced.map { it.name })
        assertEquals(20, replaced.last().durationMin)

        repeat(SessionPreset.MAX_PRESETS) { repo.savePreset(preset("extra$it")) }
        val capped = repo.presets.first()
        assertEquals(SessionPreset.MAX_PRESETS, capped.size)
        assertEquals("extra0", capped.first().name)
    }
}
